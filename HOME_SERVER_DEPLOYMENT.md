# Home Server Deployment

This deployment uses Docker Compose on a Linux home server. The database is
bound to localhost only, and containers restart automatically after a reboot.
The deployment script applies `docker-compose.home.yml`, which disables the
optional pgAdmin service by default.

## 1. Install Docker

On Ubuntu or Debian, install Docker Engine and the Compose plugin using the
official Docker instructions, then verify:

```bash
docker --version
docker compose version
```

Add your Linux user to the Docker group if you do not want to use `sudo`:

```bash
sudo usermod -aG docker "$USER"
```

Log out and back in after this command.

## 2. Copy the project

Clone the repository on the server, or copy the project directory to a
location such as `/opt/trails`:

```bash
sudo mkdir -p /opt/trails
sudo chown "$USER":"$USER" /opt/trails
cd /opt
git clone <repository-url> trails
cd trails
```

## 3. Create the server environment

```bash
cp .env.example .env
chmod 600 .env
openssl rand -hex 32
```

Edit `.env` and set `DB_PASSWORD`, `APP_ENCRYPTION_KEY`, and
`GARMIN_SERVICE_AUTH_TOKEN` to strong unique values. For HTTPS, set
`TLS_KEYSTORE_FILE`, `TLS_KEYSTORE_PASSWORD`, and `TLS_KEY_ALIAS`. The
encryption key must be exactly 64 hexadecimal characters. Keep `.env` private
and never commit it.

Create a PKCS12 keystore from a certificate and private key for your domain:

```bash
sudo mkdir -p /opt/trails/secrets
sudo openssl pkcs12 -export \
  -in fullchain.pem -inkey privkey.pem \
  -out /opt/trails/secrets/trails.p12 \
  -name trails
sudo chown "$USER":"$USER" /opt/trails/secrets/trails.p12
chmod 600 /opt/trails/secrets/trails.p12
```

Set `TLS_KEYSTORE_FILE=/opt/trails/secrets/trails.p12` and use the export
password as `TLS_KEYSTORE_PASSWORD` in `.env`.

## 4. Start the application

```bash
chmod +x deploy-home-server.sh
./deploy-home-server.sh
```

Check the services and logs:

```bash
docker compose -f docker-compose.yml -f docker-compose.home.yml --env-file .env ps
docker compose -f docker-compose.yml -f docker-compose.home.yml --env-file .env logs -f app
```

From the home network, open `https://SERVER_LAN_IP` or, preferably, the domain
covered by the certificate. A certificate for a domain will normally produce a
browser warning when accessed by IP address.

## 5. Firewall

For LAN-only access, allow the application port only from your local network
and do not forward ports 5432 or 5000 on the router:

```bash
sudo ufw allow from 192.168.1.0/24 to any port 443 proto tcp
sudo ufw enable
```

Replace `192.168.1.0/24` with your actual LAN range. Do not expose port 8080
directly to the internet. For public access, put the app behind a reverse
proxy or tunnel with HTTPS and set `COOKIE_SECURE=true`.

## 6. Back up PostgreSQL

Create a compressed database backup from the server:

```bash
mkdir -p backups
docker compose --env-file .env exec -T db \
  pg_dump -U trails_user -d trails | gzip > "backups/trails-$(date +%F-%H%M).sql.gz"
```

Test restoring backups on a separate database before relying on them.

## 7. Updating

```bash
git pull
./deploy-home-server.sh
```

Flyway migrations run when the application starts. Keep a database backup
before updates that include schema changes.