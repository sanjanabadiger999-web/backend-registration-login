# System Design — Registration & Login Web Application

## 1. Architecture

```
                          +------------------------------------------------------------+
                          |                        Browser (Client)                     |
                          |  FrontendRegLogin :8080  (signup.html, login.html,          |
                          |  home.html, css, js)                                        |
                          +------------------------------------------------------------+
                                       | fetch (credentials: include)
                    CORS allow only http://localhost:8080 (credentials true)
              +---------------------------+---------------------------+
              |                           |                           |
              v                           v                           v
   +---------------------+    +------------------------+    +----------------------+
   |  UserService :8081   |    | AuthenticationService  |    |  (static files)     |
   |  POST /api/reg       |    |  :8082                 |    |  served by Frontend |
   |  validation          |    |  POST /api/login       |    |  app itself (8080)  |
   |  BCrypt hash         |    |  GET  /api/me          |    +----------------------+
   |  persist user        |    |  POST /api/logout      |
   +---------------------+    |  JwtService            |
              |               |  HttpOnly cookie set    |
              |               |  jwt_token persist      |
              |               +------------------------+
              |                           |
              |            +--------------+--------------+
              |            |              |              |
              v            v              v              v
        +-----------------------------------------------------------------+
        |                MySQL :3306  database = reglogin                 |
        |   user(id, name, password, email, phone)                        |
        |   jwt_token(tid, uid, token, creation_time, expiry_time)        |
        +-----------------------------------------------------------------+
```

- The frontend app is only a static server. All data access flows through the two
  microservices. The frontend holds no JDBC/JPA code.
- JWT is transported exclusively in an **HttpOnly cookie** so client-side JS cannot read it.

## 2. Technologies

- Java 21, Spring Boot 3.x, Spring Data JPA, Spring Validation, Spring Security
  (only in AuthenticationService), spring-security-crypto (BCrypt) in both services,
  jjwt 0.12.x, MySQL Connector/J, Maven, HTML5, CSS3, vanilla JavaScript (Fetch API).

## 3. Database Design

```
Database: reglogin (charset utf8mb4)

CREATE TABLE user (
  id       BIGINT       NOT NULL AUTO_INCREMENT,
  name     VARCHAR(50)  NOT NULL,
  password VARCHAR(100) NOT NULL,                -- BCrypt hash only
  email    VARCHAR(100) NOT NULL,
  phone    VARCHAR(20)  NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_user_name  (name),
  UNIQUE KEY uq_user_email (email)
) ENGINE=InnoDB;

CREATE TABLE jwt_token (
  tid           BIGINT       NOT NULL AUTO_INCREMENT,
  uid           BIGINT       NOT NULL,
  token         VARCHAR(512) NOT NULL,
  creation_time DATETIME(6)  NOT NULL,
  expiry_time   DATETIME(6)  NOT NULL,
  PRIMARY KEY (tid),
  KEY ix_jwt_uid (uid),
  CONSTRAINT fk_jwt_user FOREIGN KEY (uid) REFERENCES user (id)
) ENGINE=InnoDB;
```

Notes:
- `user.password` stores the BCrypt hash (`$2a$10$...`), never plain text.
- `jwt_token.uid` → `user.id` (FK association, one token row per login).
- Spring Data JPA auto-creates the tables via `ddl-auto=update`; the SQL script in
  `sql/setup.sql` creates them explicitly for a clean install.

## 4. API Design

### UserService (:8081)

| Method | Path        | Body / Params           | Success            | Errors |
|--------|-------------|-------------------------|--------------------|--------|
| POST   | `/api/reg`  | JSON `SignupRequest`    | 201 `SignupResponse` | 400 validation, 409 duplicate, 500 |

`SignupRequest`: `name`, `password`, `confirmPassword`, `email`, `phone`
`SignupResponse`: `message`, `name`, `email`

### AuthenticationService (:8082)

| Method | Path         | Description                                                    | Success                                      | Errors |
|--------|--------------|----------------------------------------------------------------|----------------------------------------------|--------|
| POST   | `/api/login` | Validate creds, issue JWT, persist token, set HttpOnly cookie  | 200 `LoginResponse {message, username}`       | 400/401 |
| GET    | `/api/me`    | Resolve current user from HttpOnly cookie                      | 200 `MeResponse {username, name, email, phone}` | 401 |
| POST   | `/api/logout`| Invalidate JWT in DB, clear cookie                             | 200 `LogoutResponse {message}`                | 401 if no/invalid token |

`LoginRequest`: `name`, `password` — `LoginResponse`: `message`, `username`

## 5. JWT Design

- Algorithm: HS256. `jwt.secret` read from `application.properties` (Base64-encoded),
  **never hard-coded** in Java. Expiry configurable via `jwt.expiry.minutes`.
- Claims: `sub` = username, `uid` = user id, `iat`, `exp`. Additional claim `"name"`.
- flow: created at login → persisted in `jwt_token` with `creation_time`/`expiry_time`
  computed from the same config → validated on `/api/me` against both signature and the
  stored row → invalidated on logout by deleting the row (and clearing cookie).

## 6. Security

- BCrypt (strength 10) for password hashing (UserService) / verification (AuthenticationService).
- HttpOnly cookie `accessToken`: `HttpOnly=true`, `Secure=<configurable>`, `SameSite=Lax`,
  `Path=/` so the browser sends it to :8082 across pages on localhost.
- CORS: only `http://localhost:8080`, `allowCredentials=true`, methods `GET, POST, OPTIONS`.
- CSRF disabled for the stateless JWT API (no Spring session cookies).
- Centralized error handling with `@RestControllerAdvice`; all API errors return a
  consistent `ErrorResponse {timestamp, status, error, message, path}` JSON.
- Passwords, hashes, and JWT secrets are never logged or returned in responses.
- Frontend: password is not stored anywhere; JWT lives only in the HttpOnly cookie.

## 7. Cookie Strategy (localhost)

Cookies are host-scoped, not port-scoped: a cookie set by `localhost:8082` with `Path=/`
is automatically sent back to `localhost:8082` by the browser. The frontend (8080) never
needs to read it — it simply sends `credentials: 'include'` on the fetch to 8082.

## 8. User Flows

### Signup
```
User → signup.html → [frontend validation] → POST http://localhost:8081/api/reg
  → UserService Controller → @Valid → duplicate check (name, email)
  → BCrypt encode password → save User → 201
  → frontend shows success → redirect login.html
```

### Login
```
User → login.html → POST http://localhost:8082/api/login (credentials: include)
  → validate DTO → find user by name → BCrypt matches(password, hash)
  → generate JWT (sub, uid, iat, exp) → insert jwt_token row
  → set HttpOnly accessToken cookie → 200 {message, username}
  → JS redirects to home.html
```

### Home / Logout
```
home.html → GET /api/me (cookie auto-sent) → JwtService validates → find user → 200 {username}
  → render "Welcome <username>"
Logout → POST /api/logout → delete token row → clear cookie → 200 → redirect login.html
```

## 9. Implementation Plan / Development Order

1. FrontendRegLogin (HTML/CSS/JS + minimal Spring Boot static app, port 8080)
2. UserService (port 8081)
3. AuthenticationService (port 8082)
4. MySQL setup (`sql/setup.sql`), config wiring
5. Integration: build all three, run, exercise full E2E + error cases
6. README with full run/test/troubleshooting guide