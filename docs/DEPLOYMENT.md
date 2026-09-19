# Deployment Guide

Architecture:

```
                 ┌─────────────────────┐
                 │       Vercel        │
                 │   Frontend/ (Vite)  │
                 └──────────┬──────────┘
                            │   HTTPS REST API (+ HttpOnly JWT cookie)
                            ├───────────────────────────────┐
                            ▼                               ▼
                 ┌──────────────────────┐        ┌──────────────────────┐
                 │  reglogin-user-service│        │ `reglogin-auth-service` │
                 │  (Render, POST /api/reg)      │  (Render) /api/login   │
                 └──────────┬──────────┘        │        /api/me         │
                            │                   │        /api/logout      │
                            └──────────┬────────┴──────────┬─────────────┘
                                       ▼                   │
                               ┌───────────────────────────────┐
                               │       MySQL (shared DB)       │
                               └───────────────────────────────┘
```

Two backend services are deployed separately on Render (one per service, each with its own
`render.yaml`). The Vite frontend talks to both through REST.

## Local development

Backends (run each in its own terminal, H2 demo profile so no MySQL needed):

```
mvn -f UserService/pom.xml spring-boot:run -Dspring-boot.run.profiles=demo
mvn -f AuthenticationService/pom.xml spring-boot:run -Dspring-boot.run.profiles=demo
```

Frontend:

```
cd Frontend
npm install
npm run dev            # http://localhost:5173/login.html
```

Optional frontend `.env` (copy `Frontend/.env.example` to `.env`). Defaults point to
`localhost:8081` / `localhost:8082`, so no `.env` is needed for local backends.

## Environment variables

Frontend on **Vercel** (Project Settings > Environment Variables):

| Key                  | Example                                        |
|----------------------|------------------------------------------------|
| VITE_API_URL         | https://reglogin-auth-service.onrender.com     |
| VITE_REGISTER_URL    | https://reglogin-user-service.onrender.com     |

Backend on **Render** (`reglogin-user-service`):

| Key                  | Example                                                                             |
|----------------------|-------------------------------------------------------------------------------------|
| DB_URL               | jdbc:mysql://<host>:3306/reglogin?useSSL=false&allowPublicKeyRetrieval=true&...     |
| DB_USERNAME          | reglogin                                                                             |
| DB_PASSWORD          | (secret)                                                                             |
| CORS_ALLOWED_ORIGINS | https://your-app.vercel.app,http://localhost:5173                                    |

Backend on **Render** (`reglogin-auth-service`): same DB vars plus:

| Key                 | Value             |
|---------------------|-------------------|
| JWT_SECRET          | (generate a new one, secret) |
| JWT_COOKIE_SECURE   | true              |
| JWT_COOKIE_SAME_SITE| None              |

Never set these locally in `.env` files that get committed. `.gitignore` covers them.

## Deploy backend to Render

Each service is deployed the same way (twice):

1. Push the repo to GitHub.
2. Render dashboard > **New > Web Service** > connect your repo.
3. Set **Root Directory** = `UserService` (first) and `AuthenticationService` (second).
4. Render detects `Dockerfile` (Java 21, multi-stage). Set the env vars from the table.
5. Deploy. URLs are `https://<service-name>.onrender.com` (HTTPS).

Alternative: the `render.yaml` files record the same settings; if Render can't auto-detect a
blueprint in a subfolder, fall back to the dashboard steps above.

## Deploy frontend to Vercel

1. Push the repo to GitHub.
2. Vercel dashboard > **Add New Project** > import the repo.
3. **Root Directory** = `Frontend` (Vercel auto-detects Vite; `vercel.json` already set).
4. Build: framework preset **Vite**, build `npm run build`, output `dist`.
5. Add the environment variables `VITE_API_URL` and `VITE_REGISTER_URL`, then Deploy.

In production the auth service sends the JWT via an HttpOnly, `Secure`, `SameSite=None`
cookie, and both `login.js`/`home.js` send `credentials: 'include'`.

## Test checklist

- [ ] `GET https://reglogin-user-service.onrender.com/health` → 200
- [ ] `GET https://reglogin-auth-service.onrender.com/health` → 200
- [ ] Vercel frontend loads (login + signup pages)
- [ ] Sign up a new user on the deployed site → success message
- [ ] Log in → redirects to Home showing "Welcome <username>"
- [ ] Refresh Home (cookie round-trip via /api/me) → still shows the user
- [ ] Open DevTools > Network: login response has `Set-Cookie: accessToken=...; HttpOnly; Secure; SameSite=None`
- [ ] Authenticated API (`/api/me`) returns 200 with the JWT cookie
- [ ] Failing login returns an error (no redirect)