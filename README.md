# Nido

Everything a household shares, in one place: finances, shopping, tasks, menus, calendar. Self-hosted.

## Install

You need a machine with Docker. Then:

```bash
curl -O https://raw.githubusercontent.com/Le-Gall-Valentin/nido/main/deploy/compose.yaml
docker compose up -d
docker compose logs nido
```

The logs show a setup code:

```
╔════════════════════════════════════════╗
║  Nido is not set up yet.               ║
║  Setup code: K7QM-3XRP-W9TD            ║
║  Open Nido in a browser and enter it.  ║
╚════════════════════════════════════════╝
```

Open `http://<this machine>:8080`, enter the code, and follow the steps: your administrator account,
Nido's address, email if you wish, and your encryption key.

**Save the encryption key the last step shows.** It encrypts two-factor authentication, finances and the
calendar. It is shown once; keep it in a password manager.

Nothing else to fill in: the database and Redis passwords are generated at the first start, and so are
Nido's own secrets. The code changes at every restart.

## HTTPS

With a domain name pointing to the machine and ports 80 and 443 open, create a `.env` file next to
`compose.yaml`:

```bash
COMPOSE_PROFILES=https
NIDO_DOMAIN=nido.example.com
NIDO_LISTEN=127.0.0.1
```

then `docker compose up -d`. Caddy obtains and renews the certificate on its own. Enter
`https://nido.example.com` as Nido's address during the setup.

Behind your own reverse proxy instead, forward to port 8080 and pass the client's address and scheme:

```nginx
location / {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

Without HTTPS, passwords and links travel in clear: fine on a home network, not on the internet.

## Update

```bash
docker compose pull
docker compose up -d
```

Pin the version with `NIDO_VERSION=0.12.0` in `.env`, so that `pull` never moves you to a new version by surprise.

## Backups

Back up two things, **and keep them apart**:

- **the database**: `docker compose exec postgres pg_dump -U nido nido > nido.sql`;
- **the encryption key**: the one the setup showed you, or the file `/data/secrets/encryption-key` of the
  `nido_data` volume.

Nido encrypts sensitive data with that key, and keeps the key out of the database on purpose: a stolen
database dump alone reveals nothing. Stored together, the two would undo that.

Losing the key makes two-factor authentication, finances and the calendar unreadable. Nido refuses to
start with a key that is not the one its data was encrypted with, rather than writing data nobody can read.

## Settings

Mail, Nido's address, session lengths and the API documentation are set from **Administration → Instance
settings**, by the super administrator. Each can also be set by a variable of the environment, which then
wins and shows as locked on that page:

| Variable | Setting | Default |
|---|---|---|
| `NIDO_SMTP_HOST`, `NIDO_SMTP_PORT`, `NIDO_SMTP_SECURITY`, `NIDO_SMTP_USERNAME`, `NIDO_SMTP_PASSWORD`, `NIDO_MAIL_FROM` | Mail (all together: one of them set locks the whole group) | off, 587, `starttls` |
| `NIDO_APP_URL` | Nido's public address | asked during the setup |
| `NIDO_JWT_EXPIRY_MINUTES` | Access token lifetime | 15 |
| `NIDO_REFRESH_TOKEN_EXPIRY_DAYS` | Session lifetime | 30 |
| `SWAGGER_ENABLED` | API documentation at `/swagger-ui.html` | `false` |

And the ones only the environment sets, all optional:

| Variable | What |
|---|---|
| `NIDO_JWT_SECRET`, `NIDO_ENCRYPTION_SECRET` | Nido's secrets, generated in `/data` when absent |
| `NIDO_SEED_USERNAME`, `NIDO_SEED_EMAIL`, `NIDO_SEED_PASSWORD` | The first administrator, without the setup screen (scripted installs) |
| `NIDO_COOKIE_SECURE` | Forces the cookies' Secure flag; by default it follows the address (`https` or not) |
| `NIDO_DATA_DIR` | Where Nido keeps what it generates (`/data` in the image) |
| `NIDO_DB_URL`, `NIDO_DB_USER`, `NIDO_DB_PASSWORD`, `NIDO_REDIS_URL` | Database and Redis — set by `compose.yaml` |

Any of them can be passed as a file under `/run/secrets` named after the variable (Docker secrets): it
then stays out of `docker inspect`.

## I can't sign in any more

Most often, Nido's address was set to `https://…` before HTTPS worked: the browser refuses the cookies
over plain http. Add `NIDO_COOKIE_SECURE=false` to the `environment` of the `nido` service, restart, sign
in, fix the address on the settings page, then remove the variable.

## Development

Spring Boot (Java 21) backend with an embedded React/TypeScript frontend, built with Maven.

- Java 21, Spring Boot 4, Spring Data JPA + PostgreSQL, Liquibase, Spring Security + JWT, optional TOTP
- Redis (rate limiting, ephemeral stores)
- React/TypeScript frontend (`src/main/frontend`), bundled at build time
- Java 21+, Docker; Node.js is provided at build time by the Maven frontend plugin

```bash
cp .env.example .env     # development configuration
make up                  # PostgreSQL and Redis (docker-compose.yml)
./mvnw spring-boot:run   # http://localhost:8080
```

Frontend with hot reload: `cd src/main/frontend && npm install && npm run dev` (http://localhost:5173 —
set `NIDO_CORS_ALLOWED_ORIGINS` accordingly).

Tests: `./mvnw test` (integration tests use Testcontainers: a running Docker daemon is required), and
`npx vitest run` in `src/main/frontend`. Mail in development: `docker compose --profile mail up -d`, then
the Mailpit settings of `.env.example`.

## License

See [LICENSE](LICENSE).
