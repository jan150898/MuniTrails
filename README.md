# TrailsSpring (Basic Secure Login)

A basic Spring Boot (Maven) project with:
- Secure login page (Spring Security)
- Two account types: **Admin** and **User**
- Landing page after login
- Admin-only page

## Initial access

No default accounts are created. Provision the first administrator through your
deployment process using a unique password before exposing the application.

## User sign-in

Users sign in with their email address. Their username remains their public
display name on comments and uploads. Before a user can sign in, assign a
unique email address to the account, for example:

```sql
UPDATE app_user SET email = 'rider@example.com' WHERE username = 'rider-name';
```

Email addresses are normalized to lowercase and must be unique.

## Run
```bash
mvn spring-boot:run
```
Then open:
- http://localhost:8080/login

## Docker and Cloud Run

For local development, copy `.env.example` to `.env` and run `docker compose up --build`.
The Compose file starts the Garmin service beside the application and connects
the app to the Supabase database configured in `.env`.

Cloud Run does not run `docker-compose.yml`; deploy the application and Garmin
service as separate services, and use a managed PostgreSQL instance. Configure
the application service with `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and
`GARMIN_SERVICE_URL`. Configure the same long random
`GARMIN_SERVICE_AUTH_TOKEN` value in both services; the Garmin service rejects
all non-health requests without it. Cloud Run sets `PORT` automatically.

## Endpoints
- `GET /login` - login page
- `POST /login` - authenticate
- `GET /landing` - landing page (requires auth)
- `GET /admin` - admin page (requires role ADMIN)
- `POST /logout` - logout
