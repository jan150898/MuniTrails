# Dockerization tasks checklist

- [x] Add `Dockerfile` (multi-stage: build with Maven, run with JRE)
- [x] Add `docker-compose.yml` with PostgreSQL + app services
- [x] Add `.env.example`

Next (optional) improvements:
- [ ] Make application read env vars for datasource properties (or rely on Spring’s default env var mapping)
- [ ] Add a Flyway/password seeding strategy that doesn’t rely on `REPLACE_ME_*` placeholders
- [ ] Add healthchecks so app waits for DB readiness

