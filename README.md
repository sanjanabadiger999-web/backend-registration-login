# Registration & Login Web Application (Spring Boot + MySQL + JWT)

A secure registration and login application split into **three independent Spring Boot
applications**. Passwords are BCrypt-hashed in MySQL; authentication uses a JWT stored in an
**HttpOnly cookie**. The frontend never issues SQL; it only calls the backend services.

| App | Port | Purpose |
|-----|------|---------|
| **FrontendRegLogin** | 8080 | Signup, Login, Home pages (HTML/CSS/JS) |
| **UserService** | 8081 | `POST /api/reg` — validate + BCrypt hash + save user |
| **AuthenticationService** | 8082 | `POST /api/login`, `GET /api/me`, `POST /api/logout` — JWT + cookie |
| **MySQL** | 3306 | database `reglogin` (`user`, `jwt_token`) |

> Design documents: [`docs/PRD.md`](docs/PRD.md) and [`docs/DESIGN.md`](docs/DESIGN.md).

---

## 1. Project structure

```
Default Project/
├── docs/                      PRD + system design
├── sql/
│   ├── setup.sql              creates database, tables, app user
│   └── setup-db.bat           one-click DB setup (Windows)
├── run-all.ps1                builds (if needed) + starts all 3 apps
├── FrontendRegLogin/          port 8080
│   └── src/main/resources/static/
│       ├── signup.html  login.html  home.html  index.html
│       ├── css/style.css
│       └── js/signup.js  login.js  home.js
├── UserService/               port 8081
│   └── src/main/java/com/reglogin/user/
│       ├── UserServiceApplication.java
│       ├── controller/  service/  repository/  entity/  dto/
│       ├── config/ (PasswordConfig, CorsConfig)
│       └── exception/ (GlobalExceptionHandler, ...)
└── AuthenticationService/     port 8082
    └── src/main/java/com/reglogin/auth/
        ├── AuthenticationServiceApplication.java
        ├── controller/  service/  repository/  entity/  dto/
        ├── config/ (SecurityConfig, CorsConfig, JwtProperties, PasswordConfig)
        ├── security/ (JwtService, JwtAuthenticationFilter)
        └── exception/ (GlobalExceptionHandler, ...)
```

---

## 2. Prerequisites

- JDK 21 (or 17+) and Maven 3.9+
- MySQL 8 running on `localhost:3306`
- A modern browser

---

## 3. Create the database (MySQL)

Run once, using your MySQL **root** account:

```powershell
Get-Content ".\sql\setup.sql" | mysql -u root -p
```

On Windows you can also just double-click `sql\setup-db.bat`.

This creates:
- database `reglogin`
- tables `user` (id, name, password, email, phone) and
  `jwt_token` (tid, uid, token, creation_time, expiry_time)
- app user `reglogin` / password `frontend`

If you prefer different credentials, edit `sql/setup.sql` and both
`UserService/src/main/resources/application.properties` and
`AuthenticationService/src/main/resources/application.properties`.

The tables are also auto-created/updated by Hibernate (`ddl-auto=update`).

---

## 4. Configuration

### MySQL (both services — default profile)
```
spring.datasource.url=jdbc:mysql://localhost:3306/reglogin?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=reglogin
spring.datasource.password=frontend
```

### JWT (`AuthenticationService/src/main/resources/application.properties`)
```
jwt.secret=<Base64 256-bit secret>       # NEVER hard-coded in Java
jwt.expiry.minutes=30                     # creation_time + expiry_time + cookie max-age
jwt.issuer=reglogin-auth-service
jwt.cookie.name=accessToken
jwt.cookie.secure=false                   # set true when serving over HTTPS
jwt.cookie.same-site=Lax
app.cors.allowed-origin=http://localhost:8080
```

Generate a new secret (example):
```powershell
$b = New-Object byte[] 32; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

### Demo profile (no MySQL required)
Both services include an optional `demo` profile backed by a shared file H2 database.
Use it only for quickly demonstrating the flow when MySQL isn't available:
```
java -jar target/user-service-1.0.0.jar           --spring.profiles.active=demo
java -jar target/authentication-service-1.0.0.jar --spring.profiles.active=demo
```
MySQL remains the default profile of the finished application.

---

## 5. Build

From the project root:
```powershell
mvn -q -DskipTests package --file UserService/pom.xml
mvn -q -DskipTests package --file AuthenticationService/pom.xml
mvn -q -DskipTests package --file FrontendRegLogin/pom.xml
```

## 6. Run

Easiest (Windows):
```powershell
powershell -ExecutionPolicy Bypass -File .\run-all.ps1
```

Or start each app separately:
```powershell
java -jar FrontendRegLogin\target\frontend-reg-login-1.0.0.jar                 # http://localhost:8080
java -jar UserService\target\user-service-1.0.0.jar                            # http://localhost:8081
java -jar AuthenticationService\target\authentication-service-1.0.0.jar        # http://localhost:8082
```

### Import into Eclipse / IntelliJ IDEA
- **IntelliJ IDEA:** `File > Open` → select the project folder (e.g. `UserService`) → it is
  auto-detected as a Maven project. Repeat for the other two. Run each `*Application` class,
  or the packaged jar.
- **Eclipse:** `File > Import > Maven > Existing Maven Projects` → select each project folder →
  Run As > Java Application (or Spring Boot App) on the `*Application` class.

---

## 7. Using the app

Open **http://localhost:8080** (redirects to the login page).

1. **Signup** — click “New user? Sign Up” (or go to `/signup.html`). Enter User Name, Password,
   Confirm Password, Email, Phone → **Sign Up**. Success → redirected to Login.
2. **Login** — enter User Name + Password → **Login**.
3. **Home** — you are redirected to `/home.html`, which shows **“Welcome <username>”** and a
   **Logout** button.

> Use `http://localhost:8080` — **not** `http://127.0.0.1:8080`. CORS allows only
> `http://localhost:8080`, so a different origin will be blocked.

---

## 8. Verify security manually

- **BCrypt in MySQL**
  ```sql
  USE reglogin;
  SELECT id, name, password, email, phone FROM user;
  ```
  `password` must look like `$2a$10$...` — never the plain text.

- **JWT stored in DB**
  ```sql
  SELECT tid, uid, LEFT(token,25) AS token_prefix, creation_time, expiry_time FROM jwt_token;
  ```
  `uid` references `user.id`; `expiry_time - creation_time` equals `jwt.expiry.minutes`.

- **HttpOnly cookie in the browser** — DevTools → Application → Cookies → `http://localhost`
  → `accessToken` shows `HttpOnly` ✔ and `SameSite=Lax`. It is **not** visible in
  `localStorage`/`sessionStorage` (the app never stores it there).

- **Logout** — click Logout, then check DevTools: the cookie is cleared and the `jwt_token`
  row is deleted; visiting `/home.html` redirects back to Login.

- **Expired/tampered token** — delete the row in `jwt_token`, or edit the cookie value; `/api/me`
  returns `401` and the Home page redirects to Login.

---

## 9. API reference

### UserService (:8081)
`POST /api/reg`
```json
{ "name":"jane_doe", "password":"secret123", "confirmPassword":"secret123",
  "email":"jane@example.com", "phone":"9876543210" }
```
- `201` `{"message":"Registration successful! Please login.","name":"jane_doe","email":"jane@example.com"}`
- `400` validation / password mismatch · `409` duplicate username or email

### AuthenticationService (:8082)
`POST /api/login`
```json
{ "name":"jane_doe", "password":"secret123" }
```
- `200` `{"message":"Login successful","username":"jane_doe"}` + `Set-Cookie: accessToken=...; HttpOnly; SameSite=Lax`
- `401` invalid credentials (no token issued)

`GET /api/me` (cookie required) → `200 {"username":"jane_doe","email":"...","phone":"..."}` or `401`

`POST /api/logout` (cookie optional) → `200 {"message":"Logged out successfully"}`, clears cookie and deletes the token row

All errors use a consistent JSON: `{timestamp, status, error, message, path}`.

---

## 10. End-to-end test results (verified)

| # | Test | Result |
|---|------|--------|
| 1 | Signup valid user → `POST /api/reg` | `201`, user row saved |
| 2 | Duplicate username | `409` |
| 3 | Duplicate email | `409` |
| 4 | Password ≠ Confirm Password | `400` |
| 5 | Invalid email / phone | `400` |
| 6 | Login wrong password | `401`, no token row |
| 7 | Login valid | `200`, `HttpOnly` cookie set, `jwt_token` row inserted |
| 8 | `GET /api/me` with cookie | `200` username |
| 9 | `GET /api/me` without cookie | `401` |
| 10 | Logout | cookie cleared, token row deleted |
| 11 | Reuse old cookie after logout | `401` |
| 12 | Tampered JWT | `401` |
| 13 | CORS from `http://localhost:8080` | allowed` |
| 14 | CORS from other origin | `403` |
| 15 | `user.password` in DB | BCrypt `$2a$10$...` |
| 16 | `jwt_token` row | correct `uid` + 30-minute expiry |

---

## 11. Troubleshooting

| Symptom | Cause / Fix |
|---------|-------------|
| Login/Signup button shows “Cannot reach … 8081/8082” | Backend not running. Start UserService and AuthenticationService. |
| Browser shows another app (e.g. Jenkins) at `localhost:8080` | Another service owns 8080. Stop it: `Stop-Service Jenkins` (admin), then start FrontendRegLogin. |
| CORS error in console | Open the page via `http://localhost:8080` (not `127.0.0.1`/`file://`). Only that origin is allowed. |
| `Access denied for user 'reglogin'@'localhost'` | Run `sql/setup.sql` as root, or fix `spring.datasource.password`. |
| `Communications link failure` on startup | MySQL not running / wrong port. |
| Cookie not stored by the browser | For HTTPS set `jwt.cookie.secure=true`; for cross-site needs `SameSite=None; Secure`. On localhost the defaults work. |
| Home page immediately redirects to Login | No/expired cookie, or token row missing (logged out). Log in again. |
| Port already in use | `netstat -ano | findstr :8080` then stop the owning process, or change `server.port`. |
| `user` table name error in MySQL | `spring.jpa.properties.hibernate.globally_quoted_identifiers=true` (already set) quotes the reserved word. |

---

## 12. Security summary

- BCrypt (strength 10) hashing in UserService; verification only in AuthenticationService.
- Plain-text passwords are never stored, returned, or logged.
- JWT: HS256, claims `sub`, `uid`, `iat`, `iss`, `exp`; secret and lifetime come from
  `application.properties` (never hard-coded in Java).
- Token delivered in an `HttpOnly` cookie (`SameSite=Lax`, `Secure` configurable).
- CORS restricted to `http://localhost:8080`.
- CSRF disabled for the stateless API; session policy `STATELESS`.
- Centralized `@RestControllerAdvice` error handling with meaningful HTTP status codes.
- DTOs are used at every API boundary — entities are never exposed.