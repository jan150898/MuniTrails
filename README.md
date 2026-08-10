# TrailsSpring (Basic Secure Login)

A basic Spring Boot (Maven) project with:
- Secure login page (Spring Security)
- Two account types: **Admin** and **User**
- Landing page after login
- Admin-only page

## Initial access

No default accounts are created. Provision the first administrator through your
deployment process using a unique password before exposing the application.

## Run
```bash
mvn spring-boot:run
```
Then open:
- http://localhost:8080/login

## Docker and Cloud Run

For local development, copy `.env.example` to `.env` and run `docker compose up --build`.
The Compose file starts PostgreSQL and the Garmin service beside the application.

Cloud Run does not run `docker-compose.yml`; deploy the application and Garmin
service as separate services, and use a managed PostgreSQL instance. Configure
the application service with `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and
`GARMIN_SERVICE_URL`. Cloud Run sets `PORT` automatically.

## Endpoints
- `GET /login` - login page
- `POST /login` - authenticate
- `GET /landing` - landing page (requires auth)
- `GET /admin` - admin page (requires role ADMIN)
- `POST /logout` - logout

