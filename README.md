# TrailsSpring (Basic Secure Login)

A basic Spring Boot (Maven) project with:
- Secure login page (Spring Security)
- Two account types: **Admin** and **User**
- Landing page after login
- Admin-only page

## Demo accounts
- **Admin**: `admin` / `admin123`
- **User**: `user` / `user123`

## Run
```bash
mvn spring-boot:run
```
Then open:
- http://localhost:8080/login

## Endpoints
- `GET /login` - login page
- `POST /login` - authenticate
- `GET /landing` - landing page (requires auth)
- `GET /admin` - admin page (requires role ADMIN)
- `POST /logout` - logout

