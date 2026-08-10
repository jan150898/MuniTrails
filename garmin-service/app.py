"""
Garmin Connect microservice for Muni Trails.

Endpoints:
  POST /login              { email, password }  -> { token }
  POST /logout             { token }
  GET  /activities?limit=20&offset=0  (X-Garmin-Session header)
  GET  /activity/<id>/gpx             (X-Garmin-Session header)
  GET  /health
"""

import logging
import os
import secrets
import tempfile
from pathlib import Path

from flask import Flask, jsonify, request
from garminconnect import (
    Garmin,
    GarminConnectAuthenticationError,
    GarminConnectConnectionError,
    GarminConnectTooManyRequestsError,
)

logging.basicConfig(level=logging.INFO)
log = logging.getLogger(__name__)

app = Flask(__name__)

# In-memory session store: token -> Garmin client
# Good enough for a single-instance service
_sessions: dict[str, Garmin] = {}


def _get_client(token: str) -> Garmin | None:
    return _sessions.get(token)


def _session_token() -> str:
    return request.headers.get("X-Garmin-Session", "")


def _err(msg: str, status: int = 400):
    return jsonify({"error": msg}), status


# ---------------------------------------------------------------------------
# Health
# ---------------------------------------------------------------------------

@app.get("/health")
def health():
    return jsonify({"status": "ok"})


# ---------------------------------------------------------------------------
# Login / logout
# ---------------------------------------------------------------------------

@app.post("/login")
def login():
    body = request.get_json(force=True, silent=True) or {}
    email = (body.get("email") or "").strip()
    password = body.get("password") or ""

    if not email or not password:
        return _err("email and password required")

    # Each login gets its own token dir so sessions don't collide
    token_dir = Path(tempfile.mkdtemp(prefix="garmin_"))

    try:
        client = Garmin(
            email=email,
            password=password,
            prompt_mfa=None,   # MFA not supported in headless mode
        )
        client.login(str(token_dir))
    except GarminConnectAuthenticationError as e:
        return _err(f"Authentication failed: {e}", 401)
    except GarminConnectTooManyRequestsError:
        return _err("Garmin rate-limit reached, try again later", 429)
    except GarminConnectConnectionError as e:
        return _err(f"Connection error: {e}", 502)
    except Exception as e:
        log.exception("Unexpected login error")
        return _err(f"Login failed: {e}", 500)

    token = secrets.token_urlsafe(32)
    _sessions[token] = client
    log.info("New Garmin session for %s (token prefix %s)", email, token[:8])
    return jsonify({"token": token})


@app.post("/logout")
def logout():
    body = request.get_json(force=True, silent=True) or {}
    token = body.get("token") or ""
    client = _sessions.pop(token, None)
    if client:
        try:
            client.logout()
        except Exception:
            pass
    return jsonify({"status": "ok"})


# ---------------------------------------------------------------------------
# Activities
# ---------------------------------------------------------------------------

@app.get("/activities")
def activities():
    token = _session_token()
    limit = int(request.args.get("limit", 20))
    offset = int(request.args.get("offset", 0))

    client = _get_client(token)
    if not client:
        return _err("invalid or expired session token", 401)

    try:
        raw = client.get_activities(offset, limit)
    except GarminConnectAuthenticationError:
        return _err("Garmin session expired, please log in again", 401)
    except GarminConnectTooManyRequestsError:
        return _err("Garmin rate-limit reached", 429)
    except Exception as e:
        log.exception("Error fetching activities")
        return _err(f"Failed to fetch activities: {e}", 502)

    # Filter to cycling/MTB activities and map to a clean shape
    CYCLING_TYPES = {
        "cycling", "mountain_biking", "road_biking", "gravel_cycling",
        "indoor_cycling", "bmx", "cyclocross", "e_bike_fitness",
        "e_bike_mountain",
    }

    result = []
    for a in raw:
        activity_type = (
            a.get("activityType", {}).get("typeKey", "") or ""
        ).lower()

        result.append({
            "id":           a.get("activityId"),
            "name":         a.get("activityName", "Unnamed"),
            "type":         activity_type,
            "date":         a.get("startTimeLocal", "")[:10],
            "distanceM":    a.get("distance", 0),
            "durationSecs": a.get("duration", 0),
            "elevGainM":    a.get("elevationGain", 0),
            "elevLossM":    a.get("elevationLoss", 0),
            "isCycling":    activity_type in CYCLING_TYPES,
        })

    return jsonify(result)


# ---------------------------------------------------------------------------
# Download GPX for a single activity
# ---------------------------------------------------------------------------

@app.get("/activity/<int:activity_id>/gpx")
def activity_gpx(activity_id: int):
    token = _session_token()
    client = _get_client(token)
    if not client:
        return _err("invalid or expired session token", 401)

    try:
        gpx_data = client.download_activity(
            activity_id, dl_fmt=client.ActivityDownloadFormat.GPX
        )
    except GarminConnectAuthenticationError:
        return _err("Garmin session expired, please log in again", 401)
    except GarminConnectTooManyRequestsError:
        return _err("Garmin rate-limit reached", 429)
    except Exception as e:
        log.exception("Error downloading GPX for activity %s", activity_id)
        return _err(f"Failed to download GPX: {e}", 502)

    return app.response_class(
        response=gpx_data,
        status=200,
        mimetype="application/gpx+xml",
    )


if __name__ == "__main__":
    port = int(os.getenv("PORT", 5000))
    app.run(host="0.0.0.0", port=port)
