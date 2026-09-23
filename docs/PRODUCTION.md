# Production Deployment README (Backend → Aiven MySQL, Render; Frontend → Vercel)

This guide covers making the Spring Boot backend production-ready and connecting it to an
**Aiven MySQL** database.

## Architecture

```
┌──────────────────┐         HTTPS / REST + HttpOnly JWT cookie          ┌──────────────────┐
│     Vercel       │  ─────────────────────────────────────────────────▶ │     Render       │
│  Frontend/ (Vite)│  POST /api/reg → reglogin-user-service             │  2 Spring Boot   │
│                  │  POST /api/login, GET /api/me, POST /api/logout    │  services (prod) │
└──────────────────┘        → reglogin-auth-service                     └────────┬─────────┘
                                                                                 │ JDBC + TLS
                                                                                 ▼
                                                                      ┌──────────────────┐
                                                                      │  Aiven MySQL     │
                                                                      │  (managed DB)    │
                                                                      └──────────────────┘
```

Two separate backends (separate deployments) share one Aiven MySQL. Tables (`user`,
`jwt_tokens`) are created automatically by Hibernate (`ddl-auto=update`).

---

## 1. Create an Aiven MySQL service

1. Go to https://console.aiven.io and sign in (create an account if needed).
2. **Create a new service** → select **MySQL** → choose a plan (Hobbyist/free works to test).
3. Pick a cloud + region → name it (e.g. `reglogin-db`) → **Create service**. Wait ~1-2 min.
4. Open the service → **Connection information** tab. You will find:
   - `Service URI` (e.g. `mysql://avnadmin:xxxx@reglogin-db-abcd.aivencloud.com:16167/reglogin-db...`)
   - `Host` — e.g. `reglogin-db-abcd.aivencloud.com`
   - `Port` — e.g. `16167`
   - `Database Name` — e.g. `reglogin-db` (defaults to the service name)
   - `User` — default `avnadmin`
   - `Password` — shown once; note it (or create your own user in **User management**).
5. Aiven requires TLS: in **Connection information** you can download a **CA certificate**.
   With `ssl-mode=REQUIRED` (used below) the public Aiven CA is trusted automatically, so no
   certificate file is needed for the app.

## 2. Wire the connection via environment variables

Each Aiven field maps to an environment variable. Pick **one** of the two options below and use
it for BOTH backends (they share one DB).

**Option A — full URL (simplest):** set `DB_URL` directly:
```
jdbc:mysql://<HOST>:<PORT>/<DB_NAME>?ssl-mode=REQUIRED&serverTimezone=UTC&allowPublicKeyRetrieval=true
```
Example:
```
jdbc:mysql://reglogin-db-abcd.aivencloud.com:16167/reglogin-db?ssl-mode=REQUIRED&serverTimezone=UTC&allowPublicKeyRetrieval=true
```

**Option B — per-field env vars** (used automatically when `DB_URL` is empty;
`AivenJdbcEnvironmentPostProcessor` builds the JDBC URL for you):

| Aiven "Connection information" field | Env variable | Example |
|--------------------------------------|--------------|---------|
| Host | `DB_HOST` | `reglogin-db-abcd.aivencloud.com` |
| Port | `DB_PORT` | `16167` |
| Database Name | `DB_NAME` | `reglogin-db` |
| User | `DB_USERNAME` | `avnadmin` |
| Password | `DB_PASSWORD` | Aiven password |
| SSL (TLS) | `DB_SSL` | `true` |
| Connection URL | `DB_URL` | see Option A |

`DB_SSL=true` produces `ssl-mode=REQUIRED` (TLS is required by Aiven). Both services add
`spring.datasource.url=${DB_URL:}` in the prod profile, so either option works with no further
changes.

- `ssl-mode=REQUIRED` → TLS between the backend and Aiven (required by Aiven).
- Latest `mysql-connector-j` is already a dependency (runtime scope) in both services' `pom.xml`.

## 3. Environment variables

Both backends read everything from the environment (`application-prod.properties`).

| Variable            | Where (User / Auth) | Required | Example |
|---------------------|---------------------|----------|---------|
| `DB_URL`            | both                | *        | `jdbc:mysql://<HOST>:<PORT>/<DB_NAME>?ssl-mode=REQUIRED&serverTimezone=UTC&allowPublicKeyRetrieval=true` (**or** per-field vars below — see §2) |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_SSL` | both | * | `reglogin-db-abcd.aivencloud.com` / `16167` / `reglogin-db` / `true` |
| `DB_USERNAME`       | both                | ✔        | `avnadmin` |
| `DB_PASSWORD`       | both                | ✔        | Aiven password |
| `PORT`              | both (defaults `8080`) | –     | Render injects it |
| `FRONTEND_URL`      | both                | ✔        | `https://your-app.vercel.app` |
| `JWT_SECRET`        | auth only           | ✔        | `openssl rand -base64 32` output |
| `JWT_COOKIE_SECURE` | auth only (default `true`) | – | `true` |
| `JWT_COOKIE_SAME_SITE` | auth only (default `None`) | – | `None` |
| `JWT_EXPIRY_MINUTES`| auth only (default `30`) | –    | `30` |

Generate a JWT secret:
```
openssl rand -base64 32
```

## 4. Build the backend

```
mvn -f UserService/pom.xml clean package
mvn -f AuthenticationService/pom.xml clean package
```

Artifacts: `UserService/target/user-service-1.0.0.jar`,
`AuthenticationService/target/authentication-service-1.0.0.jar`.

## 5. Run locally with production-style env (against Aiven)

PowerShell, one terminal per service (from the project root):

```powershell
$env:DB_URL = "jdbc:mysql://HOST:PORT/DBNAME?ssl-mode=REQUIRED&serverTimezone=UTC&allowPublicKeyRetrieval=true"
$env:DB_USERNAME = "avnadmin"
$env:DB_PASSWORD = "the-aiven-password"
$env:FRONTEND_URL = "http://localhost:5173"
$env:PORT = "8081"
mvn -f UserService/pom.xml spring-boot:run -Dspring-boot.run.profiles=prod
```

```powershell
$env:DB_URL = "jdbc:mysql://HOST:PORT/DBNAME?ssl-mode=REQUIRED&serverTimezone=UTC&allowPublicKeyRetrieval=true"
$env:DB_USERNAME = "avnadmin"
$env:DB_PASSWORD = "the-aiven-password"
$env:JWT_SECRET = "<openssl rand -base64 32>"
$env:FRONTEND_URL = "http://localhost:5173"
$env:PORT = "8082"
mvn -f AuthenticationService/pom.xml spring-boot:run -Dspring-boot.run.profiles=prod
```

Both use the **same** `DB_URL`, so `user` + `jwt_tokens` are shared.

## 6. Deploy the backends to Render

Each service = one Render Web Service (Docker runtime). The included `Dockerfile` runs the jar
with `--spring.profiles.active=prod`.

1. Push the repo to GitHub.
2. Render dashboard → **New → Web Service** → connect your repo.
3. **Root Directory = `UserService`** → Render builds the `Dockerfile`. Set env vars:
   `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `FRONTEND_URL` (the Vercel URL).
4. **Root Directory = `AuthenticationService`** → same plus `JWT_SECRET`
   (`JWT_COOKIE_SECURE=true`, `JWT_COOKIE_SAME_SITE=None` already defaulted in prod profile).
5. Render injects `PORT` — the prod profile binds `server.port=${PORT:8080}`.
6. Deploy. URLs: `https://reglogin-user-service.onrender.com`,
   `https://reglogin-auth-service.onrender.com`.

The per-service `render.yaml` files contain the same settings as a blueprint reference.

## 7. Deploy the frontend to Vercel

1. Vercel dashboard → **Add New Project** → import the GitHub repo.
2. **Root Directory = `Frontend`** (Vite detected; `vercel.json` included).
3. Environment variables: `VITE_API_URL=https://reglogin-auth-service.onrender.com`,
   `VITE_REGISTER_URL=https://reglogin-user-service.onrender.com`.
4. Deploy, then set the same URL as the backend's `FRONTEND_URL` env var on Render (and Redeploy).

## 8. Test the deployed APIs

Health:
```
curl https://reglogin-user-service.onrender.com/health
curl https://reglogin-auth-service.onrender.com/health
```
→ `{"status":"UP"}`

Registration:
```
curl -X POST https://reglogin-user-service.onrender.com/api/reg \
  -H "Content-Type: application/json" \
  -d '{"name":"tester1","password":"Secret@123","confirmPassword":"Secret@123","email":"tester1@example.com","phone":"9876500000"}'
```

Login (stores HttpOnly cookie):
```
curl -i -c cookies.txt -X POST https://reglogin-auth-service.onrender.com/api/login \
  -H "Content-Type: application/json" \
  -d '{"name":"tester1","password":"Secret@123"}'
```
→ check the Set-Cookie header contains `HttpOnly; Secure; SameSite=None`.

Authenticated call:
```
curl -i -b cookies.txt https://reglogin-auth-service.onrender.com/api/me
```

Logout:
```
curl -i -b cookies.txt -X POST https://reglogin-auth-service.onrender.com/api/logout
```

In-browser: open the Vercel URL → sign up → log in → Home shows `Welcome <username>` →
refresh to confirm the cookie round-trips via `/api/me`.

## 9. Security summary

- No secrets in source: all values are env vars (see `.env.example`).
- `.gitignore` blocks `.env`, `.pem`, `.crt`, `.key`.
- CORS allows exact origins (`FRONTEND_URL`) — never `*` (cookies require credentials).
- JWT cookie: `HttpOnly`, `Secure` (prod), `SameSite=None` (prod), path `/`.
- Local development stays separate: `demo` profile uses H2; `prod` profile uses Aiven MySQL.