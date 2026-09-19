# PRD — Registration & Login Web Application

## 1. Overview

A secure, runnable registration and login web application built with Spring Boot (Java),
MySQL, JWT, HTML, CSS, and JavaScript. The system is split into **three completely
independent Spring Boot applications**:

| Application           | Port  | Responsibility                                          |
|-----------------------|-------|---------------------------------------------------------|
| FrontendRegLogin      | 8080  | Serves Signup, Login, and Home pages (static HTML/CSS/JS) |
| UserService           | 8081  | User registration only: `POST /api/reg`                 |
| AuthenticationService | 8082  | Login, JWT issuance, JWT validation, logout. `POST /api/login`, `GET /api/me`, `POST /api/logout` |

MySQL runs on `localhost:3306` with database name **`reglogin`**.

## 2. Goals

- G1: New users can register with Name, Password, Confirm Password, Email, Phone.
- G2: Registered users can log in with Name + Password.
- G3: After successful login the user is redirected to the Home page showing **"Welcome <username>"**.
- G4: Authentication is done with a JWT delivered as an **HttpOnly cookie**.
- G5: The frontend **never** stores the JWT in `localStorage`/`sessionStorage` and never connects to MySQL directly.
- G6: Passwords are **always BCrypt-hashed** before storage; plain text is never stored or returned.
- G7: JWT details (token, uid, creation time, expiry time) are persisted in a `jwt_token` table.
- G8: JWT secret and token lifetime are configurable through `application.properties` (never hard-coded in Java).
- G9: All sensitive information (passwords, JWT secrets, hashes) is never exposed in responses or logs.
- G10: Logout invalidates the JWT and clears the HttpOnly cookie.

## 3. Non-Goals

- No email verification, password reset, refresh tokens, role-based access, or remember-me.
- Single-user-role application (no admin panel).

## 4. Functional Requirements

### FR-1 Signup (`FrontendRegLogin/signup.html` → UserService)
- Fields: User Name, Password, Confirm Password, Email, Phone Number, **Signup** button,
  hyperlink **"Login"** to the login page.
- Frontend + backend validation (required, email format, phone format, min password length).
- Password and Confirm Password must match.
- Duplicate username or email is rejected.
- BCrypt hash of the password is stored in `reglogin.user`.
- Success → return to login page.

### FR-2 Login (`FrontendRegLogin/login.html` → AuthenticationService)
- Fields: User Name, Password, **Login** button, hyperlink with exact text
  **"New user? Sign Up"** navigating to the signup page.
- Invalid credentials → 401 with a clear error message, no token issued.
- Valid credentials → JWT generated, stored in `jwt_token`, set as HttpOnly cookie,
  response returns `{message, username}`.
- Client redirects to `home.html`.

### FR-3 Home (`FrontendRegLogin/home.html`)
- Calls `GET /api/me` with the HttpOnly cookie to resolve the current username.
- Displays **"Welcome <username>"**.
- **Logout** button: calls `POST /api/logout`, which invalidates the JWT in the DB
  and clears the cookie, then redirects to the login page.
- Expired/invalid cookie → 401 → redirect to login page.

## 5. Business Rules

- `username` (name), `email`, `phone` are `NOT NULL`; `name` and `email` must be unique.
- Password minimum length 6, must match confirm password.
- BCrypt hashing is done in **UserService only**; AuthenticationService only verifies.
- JWT is bound to the logged-in user. The `jwt_token.uid` references `user.id`.

## 6. Acceptance Criteria

- AC-1: Signup with valid data stores a user row; `user.password` contains a `$2a$10$...` BCrypt hash, never the plain text.
- AC-2: Duplicate username returns HTTP 409; duplicate email returns HTTP 409; invalid/empty fields return HTTP 400.
- AC-3: Mismatched confirm password returns HTTP 400 and no row is written.
- AC-4: Login with correct credentials issues a JWT, inserts a `jwt_token` row with correct `uid`, `creation_time`, `expiry_time`, and sets an HttpOnly `accessToken` cookie.
- AC-5: Login with wrong password returns 401 and no token row is inserted.
- AC-6: Home page shows "Welcome <username>" only when a valid cookie exists; otherwise redirects to login.
- AC-7: Logout deletes/invalidates the token row and clears the cookie; subsequent `/api/me` returns 401.
- AC-8: CORS allows only `http://localhost:8080`; requests from other origins are rejected.
- AC-9: No plain-text passwords or JWT secrets appear in any response, log, or `localStorage`.
- AC-10: All three apps start independently on 8080 / 8081 / 8082 and share MySQL `reglogin`.