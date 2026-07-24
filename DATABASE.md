# TrailsSpring — Database Structure

## Overview

TrailsSpring uses **PostgreSQL** with **Flyway** for schema migrations. The database is named `trails` (configurable via `application.properties`).

The design centers around **GPX tracks** (bike/mountainbike tours) stored with a **JOINED inheritance** strategy: a base `gpx_track` table holds all common columns, while subclass tables (`tour`, `trail`, `uphill`, `downhill`) store only the primary key referencing the base row. A `track_kind` discriminator column (`VARCHAR(31)`) distinguishes the concrete Java entity type at the row level.

> **Legacy columns** from the initial schema (V1) remain in the database but are no longer actively written by the application: `gpx_file_name`, `length_meters`, `number_of_track_points`, `start_latitude`, `start_longitude`, `end_latitude`, `end_longitude`, `start_elevation_m`, `end_elevation_m`, `track_type` (replaced by `type`), `created_by_user_id` (replaced by `created_by_id`). Hibernate's `ddl-auto=update` does not drop unused columns, so these are preserved for backwards compatibility.

---

## Entity Relationship Diagram

```mermaid
erDiagram
    app_user ||--o{ gpx_track : "created_by"
    app_user ||--o{ gpx_track : "last_edited_by"
    app_user ||--o{ track_comment : "author"
    app_user ||--o{ garmin_credential : ""
    app_user ||--o{ garmin_activity_cache : ""

    gpx_track ||--o| tour : "is-a"
    gpx_track ||--o| trail : "is-a"
    gpx_track ||--o| uphill : "is-a"
    gpx_track ||--o| downhill : "is-a"
    gpx_track ||--o{ track_comment : "has"

    spring_session ||--o{ spring_session_attributes : ""

    app_user {
        uuid id PK
        varchar username "UNIQUE"
        varchar password_hash "BCrypt"
        varchar role "ADMIN | USER"
        timestamptz created_at
        timestamptz updated_at
    }

    gpx_track {
        uuid id PK
        varchar track_kind "Discriminator: Tour | Trail | Uphill | Downhill"
        varchar type "TOUR | UPHILL | DOWNHILL"
        varchar status "DRAFT | PUBLISHED | ARCHIVED"
        varchar visibility "PRIVATE | FRIENDS | PUBLIC"
        varchar name "255 chars"
        uuid created_by_id FK
        uuid last_edited_by_id FK "nullable"
        text bounding_box "nullable"
        double precision distance_meters
        double precision elevation_gain_meters
        double precision elevation_loss_meters
        double precision highest_point_altitude_meters
        double precision lowest_point_altitude_meters
        bytea gpx_file "nullable, LAZY"
        varchar gpx_file_checksum "nullable"
        int overall_rating "0..10"
        int exposition "0..10"
        int uphill_rating "0..10"
        boolean ride_again
        varchar difficulty_min "nullable e.g. S2"
        varchar difficulty_max "nullable e.g. S4"
        double precision start_lat
        double precision start_lon
        timestamptz created_at
        timestamptz updated_at
    }

    tour {
        uuid id PK "FK → gpx_track"
    }

    trail {
        uuid id PK "FK → gpx_track"
    }

    uphill {
        uuid id PK "FK → gpx_track"
    }

    downhill {
        uuid id PK "FK → gpx_track"
    }

    track_comment {
        uuid id PK
        uuid track_id FK "ON DELETE CASCADE"
        uuid user_id FK
        text text
        text photos_json "nullable placeholder"
        timestamptz created_at
        timestamptz updated_at
    }

    garmin_credential {
        uuid id PK
        uuid user_id FK
        varchar encrypted_email
        varchar encrypted_password
        timestamptz created_at
        timestamptz updated_at
    }

    garmin_activity_cache {
        uuid id PK
        uuid user_id FK
        jsonb activities "Garmin API response"
        timestamptz cached_at
        timestamptz expires_at "default: +1 hour"
    }

    spring_session {
        char primary_id PK
        char session_id "UNIQUE INDEX"
        bigint creation_time
        bigint last_access_time
        int max_inactive_interval
        bigint expiry_time
        varchar principal_name "nullable"
    }

    spring_session_attributes {
        char session_primary_id PK,FK
        varchar attribute_name PK
        bytea attribute_bytes
    }
```

---

## Tables

### 1. `app_user` — User accounts

| Column        | Type          | Constraints             | Notes                                          |
|---------------|---------------|-------------------------|------------------------------------------------|
| `id`          | `UUID`        | PK                      | Generated automatically                        |
| `username`    | `VARCHAR(64)` | UNIQUE, NOT NULL        | Login name                                     |
| `password_hash` | `VARCHAR(255)` | NOT NULL                | BCrypt hashed password                         |
| `role`        | `VARCHAR(64)` | NOT NULL                | `ADMIN` or `USER` (entity: `VARCHAR(32)`)      |
| `created_at`  | `TIMESTAMPTZ` | NOT NULL, DEFAULT now() |                                                |
| `updated_at`  | `TIMESTAMPTZ` | NOT NULL, DEFAULT now() | Updated on every modification                  |

**Relationships:**
- One user can create many GPX tracks (`gpx_track.created_by_id`)
- One user can last-edit many GPX tracks (`gpx_track.last_edited_by_id`)
- One user can write many comments (`track_comment`)
- One user can have one Garmin credential (`garmin_credential`)
- One user can have one cached Garmin activity set (`garmin_activity_cache`)

---

### 2. `gpx_track` — Base track table (JOINED inheritance root)

| Column                    | Type              | Constraints       | Notes                                              |
|---------------------------|-------------------|-------------------|----------------------------------------------------|
| `id`                      | `UUID`            | PK                |                                                    |
| `track_kind`              | `VARCHAR(31)`     |                   | Hibernate discriminator (`Tour`, `Trail`, `Uphill`, `Downhill`) |
| `type` (legacy: `track_type`) | `VARCHAR(32)` | NOT NULL          | Enum: `TOUR`, `UPHILL`, `DOWNHILL`                 |
| `status`                  | `VARCHAR(32)`     | NOT NULL          | `DRAFT`, `PUBLISHED`, `ARCHIVED`                   |
| `visibility`              | `VARCHAR(32)`     | NOT NULL          | `PRIVATE`, `FRIENDS`, `PUBLIC`                     |
| `name`                    | `VARCHAR(255)`    | NOT NULL          | Track display name                                 |
| `created_by_id`           | `UUID`            | NOT NULL → `app_user` | Creator (legacy `created_by_user_id` exists)   |
| `last_edited_by_id`       | `UUID`            | → `app_user`      | nullable, last modifier                            |
| `created_at`              | `TIMESTAMPTZ`     | NOT NULL          |                                                    |
| `updated_at`              | `TIMESTAMPTZ`     | NOT NULL          |                                                    |
| `start_lat` / `start_lon` | `DOUBLE`         | NOT NULL          | Starting coordinates                               |
| `gpx_file`                | `BYTEA`           | LAZY, nullable    | Raw uploaded GPX bytes                             |
| `gpx_file_checksum`       | `VARCHAR(64)`     | nullable          | Integrity check                                    |
| `distance_meters`         | `DOUBLE`          |                   | Total distance                                     |
| `elevation_gain_meters`   | `DOUBLE`          |                   | Total ascent                                       |
| `elevation_loss_meters`   | `DOUBLE`          |                   | Total descent                                      |
| `highest_point_altitude_meters` | `DOUBLE`   |                   |                                                    |
| `lowest_point_altitude_meters`  | `DOUBLE`   |                   |                                                    |
| `bounding_box`            | `TEXT`            | nullable          | "minLat,minLon,maxLat,maxLon"                      |
| `overall_rating`          | `INT`             | 0–10              | Overall evaluation                                 |
| `exposition`              | `INT`             | 0–10              | Exposure rating                                    |
| `uphill_rating`           | `INT`             | 0–10              | Uphill difficulty rating                           |
| `ride_again`              | `BOOLEAN`         |                   | Whether user would ride it again                   |
| `difficulty_min`          | `VARCHAR(2)`      | nullable          | e.g. "S0", "S5"                                    |
| `difficulty_max`          | `VARCHAR(2)`      | nullable          | e.g. "S3", "S5"                                    |

**Inheritance:** The subclass tables (`tour`, `trail`, `uphill`, `downhill`) each have the same `UUID` primary key that also acts as a foreign key to `gpx_track(id)`. No additional columns exist in those tables — all data lives in the parent.

---

### 3. `tour` — Tour subclass

| Column | Type   | Constraints              |
|--------|--------|--------------------------|
| `id`   | `UUID` | PK, FK → `gpx_track(id)` |

A **tour** represents a complete ride combining multiple sections (e.g., uphill + downhill + trail). It's the main entity users create from GPX uploads.

---

### 4. `trail` — Trail subclass

| Column | Type   | Constraints              |
|--------|--------|--------------------------|
| `id`   | `UUID` | PK, FK → `gpx_track(id)` |

A **trail** is a defined mountainbike trail segment, typically a known route on a trail network.

---

### 5. `uphill` — Uphill subclass

| Column | Type   | Constraints              |
|--------|--------|--------------------------|
| `id`   | `UUID` | PK, FK → `gpx_track(id)` |

A dedicated **uphill** segment (climb), allowing users to evaluate and track ascents separately.

---

### 6. `downhill` — Downhill subclass

| Column | Type   | Constraints              |
|--------|--------|--------------------------|
| `id`   | `UUID` | PK, FK → `gpx_track(id)` |

A dedicated **downhill** segment (descent), allowing users to evaluate descents separately.

---

### 7. `track_comment` — Comments on tracks

| Column        | Type          | Constraints                          | Notes              |
|---------------|---------------|--------------------------------------|--------------------|
| `id`          | `UUID`        | PK                                   |                    |
| `track_id`    | `UUID`        | NOT NULL → `gpx_track(id)` ON DELETE CASCADE |            |
| `user_id`     | `UUID`        | NOT NULL → `app_user(id)`            | Comment author     |
| `text`        | `TEXT`        | NOT NULL, max 4000 chars             |                    |
| `photos_json` | `TEXT`        | nullable                             | Placeholder for future photo support |
| `created_at`  | `TIMESTAMPTZ` | NOT NULL                             |                    |
| `updated_at`  | `TIMESTAMPTZ` | NOT NULL                             |                    |

**Indexes:** `(track_id)`, `(user_id)`

---

### 8. `garmin_credential` — Stored Garmin Connect credentials

| Column             | Type          | Constraints                     |
|--------------------|---------------|---------------------------------|
| `id`               | `UUID`        | PK                              |
| `user_id`          | `UUID`        | NOT NULL → `app_user(id)`       |
| `encrypted_email`  | `VARCHAR(500)`| NOT NULL (AES encrypted)        |
| `encrypted_password` | `VARCHAR(500)`| NOT NULL (AES encrypted)      |
| `created_at`       | `TIMESTAMPTZ` | NOT NULL                        |
| `updated_at`       | `TIMESTAMPTZ` | NOT NULL                        |

Users can store their Garmin Connect login credentials (encrypted at rest via AES) so the app can automatically fetch activities from Garmin.

---

### 9. `garmin_activity_cache` — Cached Garmin activities

| Column       | Type          | Constraints                     | Notes                        |
|--------------|---------------|---------------------------------|------------------------------|
| `id`         | `UUID`        | PK                              |                              |
| `user_id`    | `UUID`        | NOT NULL → `app_user(id)`       |                              |
| `activities` | `JSONB` (TEXT)| NOT NULL                        | Garmin API response as JSON  |
| `cached_at`  | `TIMESTAMPTZ` | NOT NULL                        | Time of caching              |
| `expires_at` | `TIMESTAMPTZ` | NOT NULL                        | Default: cached_at + 1 hour  |

Caches the Garmin API response per user so subsequent logins are faster. Automatically refreshed when expired.

---

### 10. `spring_session` / `spring_session_attributes` — Persistent HTTP sessions

Spring Session JDBC tables that keep authentication data in PostgreSQL (instead of in-memory), so sessions survive application restarts.

**`spring_session`**

| Column                 | Type       | Constraints      |
|------------------------|------------|------------------|
| `primary_id`           | `CHAR(36)` | PK               |
| `session_id`           | `CHAR(36)` | UNIQUE INDEX     |
| `creation_time`        | `BIGINT`   |                  |
| `last_access_time`     | `BIGINT`   |                  |
| `max_inactive_interval`| `INT`      |                  |
| `expiry_time`          | `BIGINT`   | INDEX            |
| `principal_name`       | `VARCHAR`  | INDEX, nullable  |

**`spring_session_attributes`**

| Column               | Type       | Constraints                              |
|----------------------|------------|------------------------------------------|
| `session_primary_id` | `CHAR(36)` | PK, FK → `spring_session(primary_id)` ON DELETE CASCADE |
| `attribute_name`     | `VARCHAR`  | PK                                       |
| `attribute_bytes`    | `BYTEA`    | Serialized session attribute             |

---

## Enums

| Enum             | Values                                      | Used in              |
|------------------|---------------------------------------------|----------------------|
| `GPXTrackType`   | `TOUR`, `UPHILL`, `DOWNHILL`                | `gpx_track.type`     |
| `GPXTrackStatus` | `DRAFT`, `PUBLISHED`, `ARCHIVED`            | `gpx_track.status`   |
| `Visibility`     | `PRIVATE`, `FRIENDS`, `PUBLIC`              | `gpx_track.visibility`|

---

## Migration History (Flyway)

| Migration | Description                          |
|-----------|--------------------------------------|
| V1        | Initial schema (all core tables)     |
| V2        | Add legacy `type` column, populate from `track_type` |
| V3        | Make `track_type` column nullable    |
| V4        | Add legacy `created_by_id` column, align with `created_by_user_id` |
| V5        | Add `spring_session` tables for persistent HTTP sessions |
| V6        | Add `difficulty_min` / `difficulty_max` columns |

---

## Key Design Decisions

1. **JOINED inheritance** — The `gpx_track` base table holds all columns; subclass tables (`tour`, `trail`, `uphill`, `downhill`) are mostly placeholder tables with just a PK/FK. This allows polymorphic queries via Hibernate's `@Inheritance(strategy = InheritanceType.JOINED)`.

2. **Legacy column aliasing** — The `type` column (mapped in JPA) coexists with `track_type` due to schema evolution. The application writes to `type`; `track_type` is preserved for backwards compatibility.

3. **Encrypted credentials** — Garmin Connect credentials are AES-encrypted at the application layer before being stored.

4. **JSONB caching** — The Garmin activity cache uses a JSON column to store the full Garmin API response, enabling flexible querying.

5. **Persistent sessions** — Spring Session JDBC keeps authentication data in PostgreSQL, allowing sessions to survive app restarts and container redeployments.

