# Aiven MySQL as the backend database

The application uses **Aiven MySQL** as its shared production database. Both backend
services (`UserService`, `AuthenticationService`) connect to it with the same connection
settings supplied through environment variables.

You still cannot "host" the Java backend on Aiven (Aiven = managed data services), so:
- Backends run on **Render** (or locally on your PC for dev).
- MySQL data lives on **Aiven**.

## 1. How the backend connects

Both services read these environment variables (no values are hardcoded/committed):

| Variable       | Meaning                                                        |
|----------------|----------------------------------------------------------------|
| `DB_URL`       | JDBC URL, e.g. `jdbc:mysql://HOST:PORT/DBNAME?ssl-mode=REQUIRED&serverTimezone=UTC&allowPublicKeyRetrieval=true` |
| `DB_USERNAME`  | Aiven user (default `avnadmin`)                                |
| `DB_PASSWORD`  | Aiven password                                                 |

`AuthenticationService` additionally needs:

| Variable             | Meaning                                                        |
|----------------------|----------------------------------------------------------------|
| `JWT_SECRET`         | Base64 256-bit secret (prod)                                   |
| `JWT_COOKIE_SECURE`  | `true` in production                                          |
| `JWT_COOKIE_SAME_SITE`| `None` in production                                         |

Aiven requires TLS, hence `ssl-mode=REQUIRED`. The default database name is the Aiven
service name. Tables are created automatically by Hibernate (`ddl-auto=update`), so no
schema script is needed for a fresh Aiven instance.

## 2. Create the Aiven MySQL service

1. Sign in at https://console.aiven.io.
2. **Create a new service** → choose **MySQL** → free/hobby plan or paid.
3. Pick a cloud/region → **Create service** (a few minutes).
4. Open the service → **Connection information** tab.
5. You get:
   - `Host` (e.g. `reglogin-db-xxxx.aivencloud.com`)
   - `Port` (e.g. `16167`)
   - `Service URI` like `mysql://avnadmin:PASSWORD@HOST:PORT/defaultdb?ssl-mode=REQUIRED`
   - DB name = service name (lowercase), user `avnadmin`, password shown there.
6. In **User management** you may create a dedicated `reglogin` user if preferred.

Connection values → put into `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` below.

## 3. Run the backends locally against Aiven

PowerShell, one tab per service (run from the project root):

Start `UserService`:

```powershell
$env:DB_URL = "jdbc:mysql://HOST:PORT/DBNAME?ssl-mode=REQUIRED&serverTimezone=UTC&allowPublicKeyRetrieval=true"
$env:DB_USERNAME = "avnadmin"
$env:DB_PASSWORD = "your_aiven_password"
mvn -f UserService/pom.xml spring-boot:run
```

Start `AuthenticationService`:

```powershell
$env:DB_URL = "jdbc:mysql://HOST:PORT/DBNAME?ssl-mode=REQUIRED&serverTimezone=UTC&allowPublicKeyRetrieval=true"
$env:DB_USERNAME = "avnadmin"
$env:DB_PASSWORD = "your_aiven_password"
$env:JWT_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
$env:JWT_COOKIE_SECURE = "false"
$env:JWT_COOKIE_SAME_SITE = "Lax"
mvn -f AuthenticationService/pom.xml spring-boot:run
```

Frontend unchanged: `cd Frontend; npm run dev` → http://localhost:5173/login.html

## 4. Use Aiven on Render (production)

In each Render service (dashboards or `render.yaml`), set:

- `DB_URL` → Aiven JDBC URL (same as above)
- `DB_USERNAME` → `avnadmin`
- `DB_PASSWORD` → the Aiven password (never in a committed file)

`reglogin-auth-service` also sets `JWT_SECRET`, `JWT_COOKIE_SECURE=true`,
`JWT_COOKIE_SAME_SITE=None`.

## 5. Troubleshooting

- **`Communications link failure`** → wrong Host/Port, or the Aiven service is
  still initializing (wait a few minutes).
- **TLS errors** → keep `ssl-mode=REQUIRED`; don't use `useSSL=false`.
- **Too many connections** → Aiven free plans have a low max_connections; each
  backend keeps its own pool. If you run 4 app instances, raise the plan.