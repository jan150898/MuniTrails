FROM python:3.12-slim

WORKDIR /app

# curl_cffi needs curl headers at build time
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app.py .

ENV PORT=5000
EXPOSE 5000

# Garmin client sessions are held in app.py's in-memory token map.  They must
# stay in one process so that login, activity analysis, and import see the
# same token.
CMD ["gunicorn", "--bind", "0.0.0.0:5000", "--workers", "1", "--timeout", "60", "app:app"]
