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

**Save the encryption key the last step shows.** It encrypts what your household keeps in Nido — finances,
calendar, shopping lists, tasks, recipes, the names of your spaces — and two-factor authentication. It is shown
once; keep it in a password manager.

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

Behind your own reverse proxy on the same machine instead, add to `.env`:

```bash
NIDO_LISTEN=127.0.0.1
NIDO_TRUSTED_PROXIES=172\.31\.250\.1
```

so that port 8080 stays local and Nido trusts the client address your proxy passes on (it reaches Nido
through the Docker gateway, `172.31.250.1`). Running Nido without this compose file, set
`SERVER_TOMCAT_REMOTEIP_INTERNALPROXIES` to your proxy's address (a regular expression, `127\.0\.0\.1` for
one on the same machine): by default Nido believes any private address, and a client of your network that
reaches it directly could pass for anyone and dodge the limits on sign-in attempts. Then forward to port
8080 with the client's address and scheme:

```nginx
location / {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    # A calendar import sends a file of up to 5 MB; nginx refuses anything over 1 MB unless told otherwise.
    client_max_body_size 6m;
}
```

Without HTTPS, passwords and links travel in clear: fine on a home network, not on the internet.

## Update

```bash
docker compose pull
docker compose up -d
```

Pin the version with `NIDO_VERSION=0.12.0` in `.env`, so that `pull` never moves you to a new version by surprise.

Before updating to 0.14.0 or to 0.16.0, back up the database (see below): their first start rewrites every
encrypted value, and the previous version cannot read them afterwards. That backup is the only way back: restore it
whole, database and all, and start the previous version.

Updating to 0.17.0: the finance balance now carries over from month to month, from an opening balance set with
**Adjust the balance** on the balance card. If you entered an income every month to carry the balance over by hand,
delete those incomes, then set the opening balance. The first opening of each space's finance page after the update
takes a little longer: it builds the monthly totals, once. To go back to 0.16, restore the backup taken before the
update. If 0.16 ran on the database of 0.17 instead, run `DELETE FROM finance_totals_states;` before updating again:
0.16 does not keep the monthly totals up to date, and the statement makes every space rebuild them at its next read.

Updating to 0.18.0: the calendar exports `.ics` files, gives calendar apps secret addresses to subscribe to, imports
`.ics` files and follows calendars published at an address. Recurring events repeat in more ways: on several days of
the week ("Mondays and Thursdays"), on a weekday of the month ("the 2nd Tuesday", "the last Friday") or on the last day
of the month; series made before keep their day. Subscription addresses begin with Nido's public address
(**Administration → Instance settings**): set it before giving any out. Followed calendars are downloaded by the
server, which needs to reach the internet; it refuses addresses of its own network unless
`NIDO_CALENDAR_ALLOW_PRIVATE_NETWORK` (or the matching setting) allows them — a Nextcloud at home, for instance. Opening
it lets every member of every space make Nido fetch addresses of that network; Nido itself — this machine, its database
and its Redis — stays closed even then, so give a Nextcloud running on the same machine by its network address. A very
large calendar (beyond about 3 MB) may be refused as too large to follow. Google Calendar reads a subscription at its
own pace, every 8 to 24 hours. A subscription address is all a calendar app needs to read the space: Nido never logs
it, but the access log of a reverse proxy in front of it does — keep that log as private as the addresses, or leave
`/ical/` out of it. Behind nginx, add `client_max_body_size 6m;` to Nido's `location` (see the reverse proxy above)
before updating: without it, nginx refuses the import of any file over 1 MB.

## Backups

Back up two things, **and keep them apart**:

- **the database**: `docker compose exec -T postgres pg_dump -U nido nido > nido.sql`;
- **the encryption key**: the one the setup showed you, or the file `/data/secrets/encryption-key` of the
  `nido_data` volume — `docker compose cp nido:/data/secrets/encryption-key ./nido-encryption-key` copies it out.

Nido encrypts sensitive data with that key, and keeps the key out of the database on purpose: a stolen
database dump alone reveals nothing. Stored together, the two would undo that.

Losing the key makes all of that unreadable. Nido refuses to start with a key that is not the one its data was
encrypted with, rather than writing data nobody can read.

## When Nido refuses a stored value

Every encrypted value is sealed to the row it belongs to. One that was moved, copied or altered in the database — by
hand, by a partial restore, or by someone with write access to it — is refused rather than shown: the page shows an
error, and the log has an ERROR line naming the table, the column and the row, never the value:

```
The value of finance_transactions.amount_encrypted in row 3f… belongs to another place
```

To get the page back, put that value right, in this order of preference:

1. Restore the row from a backup taken before the change — on 0.16.0 or later, from one taken since the update to
   0.16.0: a value an earlier version wrote is refused (see below).
2. If the column may be empty — the description of a space, a recipe or an event, the note of a recipe, the place of
   an event, recurring events included — empty it: `UPDATE <table> SET <column> = NULL WHERE id = '<row>';`
3. Otherwise, if you can do without the row, delete it — knowing that Postgres deletes what depends on it too:
   - **never delete a row of `spaces`**: it is the whole space, with everything in it. Restore it instead;
   - a finance category takes its transactions, budget and recurring transactions with it, a shopping aisle its
     items, a recurring event its events;
   - a task, a recipe, a transaction or a savings goal only takes what belongs to it: subtasks, ingredients, steps
     and menu entries, shares, contributions.

After restoring or deleting a finance row by hand — an operation, a share, a settlement, a category with its
operations — also run `DELETE FROM finance_totals_states;`. Nido keeps each month's totals to read balances quickly,
and a change made outside Nido does not update them: that statement makes every space rebuild them from its operations
at its next read. When the refused value is itself one of those totals, in `finance_month_totals` or
`finance_month_member_totals`, run that statement instead of deleting the row.

The same goes for the start that updates to 0.14.0 or to 0.16.0: if it stops on `Could not seal row … of
<table>.<column>` or `Could not re-encrypt row … of <table>.<column>`, put that row right the same way, then start
again — the work already done is kept. For `two_factor_methods.secret`, delete the row: its owner turns the
authenticator app on again at their next sign-in.

Once a start of 0.16.0 has brought every value to the current format, values of an earlier format are refused for
good: one put back since — from a backup taken before that update, or by anyone with write access to the database —
is refused where it is read, and every start names its column in an ERROR line:

```
shopping_items.name_encrypted holds values of a format before 0.16.0, written after every value was brought to the current one: they are refused, never converted
```

Put those rows right as above, from a backup taken after the update. This guards against a value put back into the
database, not against someone who can also change what Nido records there of that update: keep the database, and its
backups, out of reach.

## Settings

Mail, Nido's address, session lengths, the API documentation and the calendar's local network are set from
**Administration → Instance settings**, by the super administrator. Each can also be set by a variable of the
environment, which then wins and shows as locked on that page:

| Variable | Setting | Default |
|---|---|---|
| `NIDO_SMTP_HOST`, `NIDO_SMTP_PORT`, `NIDO_SMTP_SECURITY`, `NIDO_SMTP_USERNAME`, `NIDO_SMTP_PASSWORD`, `NIDO_MAIL_FROM` | Mail (all together: one of them set locks the whole group) | off, 587, `starttls` |
| `NIDO_APP_URL` | Nido's public address | asked during the setup |
| `NIDO_JWT_EXPIRY_MINUTES` | Access token lifetime | 15 |
| `NIDO_REFRESH_TOKEN_EXPIRY_DAYS` | Session lifetime | 30 |
| `SWAGGER_ENABLED` | API documentation at `/swagger-ui.html` | `false` |
| `NIDO_CALENDAR_ALLOW_PRIVATE_NETWORK` | Followed calendars may be on the local network | `false` |

And the ones only the environment sets, all optional:

| Variable | What |
|---|---|
| `NIDO_JWT_SECRET`, `NIDO_ENCRYPTION_SECRET` | Nido's secrets, generated in `/data` when absent |
| `NIDO_SEED_USERNAME`, `NIDO_SEED_EMAIL`, `NIDO_SEED_PASSWORD` | The first administrator, without the setup screen (scripted installs) — held to its rules |
| `NIDO_COOKIE_SECURE` | Forces the cookies' Secure flag; by default it follows the address (`https` or not) |
| `NIDO_DATA_DIR` | Where Nido keeps what it generates (`/data` in the image) |
| `NIDO_DB_URL`, `NIDO_DB_USER`, `NIDO_DB_PASSWORD`, `NIDO_REDIS_URL` | Database and Redis — set by `compose.yaml` |

Any of them can be passed as a file under `/run/secrets` named after the variable (Docker secrets): it
then stays out of `docker inspect`.

## I can't sign in any more

Most often, Nido's address was set to `https://…` before HTTPS worked: the browser refuses the cookies
over plain http. Add `NIDO_COOKIE_SECURE=false` to the `.env` next to `compose.yaml`, `docker compose up -d`,
sign in, fix the address on the settings page, then remove the line and `docker compose up -d` again.

## Development

Spring Boot (Java 21) backend with an embedded React/TypeScript frontend, built with Maven.

- Java 21, Spring Boot 4, Spring Data JPA + PostgreSQL, Liquibase, Spring Security + JWT, optional two-factor authentication (authenticator app or code by mail)
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
