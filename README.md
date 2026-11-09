# Bitly Clone - Full Stack URL Shortener

A production-grade, high-performance URL shortener application built with **Spring Boot 3**, **React 18 (Vite + Tailwind CSS)**, **PostgreSQL**, **Redis** (in-memory caching), and containerized with **Docker & Docker Compose**. 

Based on the [EmbarkX Mastery Course](https://youtu.be/icm4JQdi9NU).

---

## 🏗️ Architecture & Tech Stack

```
                        +----------------------------+
                        |   React 18 Frontend (SPA)  |
                        |   (Port 3000 / 5173)       |
                        +--------------+-------------+
                                       |
                           REST API / JSON & JWT
                                       |
                                       v
                        +----------------------------+
                        |  Spring Boot 3 REST Backend|
                        |  (Java 23, Spring Security)|
                        |  (Port 8080)               |
                        +-------+------------+-------+
                                |            |
                     Read / Cache            | Persist / Query
                                v            v
                    +--------------+   +-------------------+
                    | Redis 7      |   | PostgreSQL 16     |
                    | (Port 6379)  |   | (Port 5432)       |
                    +--------------+   +-------------------+
```

- **Backend:** Java 23, Spring Boot 3.4.0, Spring Data JPA, Spring Security, JWT (jjwt 0.12.6), Spring Data Redis (Lettuce).
- **Frontend:** React 18, Vite 6, Tailwind CSS, Material-UI, Chart.js, React Router 7, Axios.
- **Cache Layer:** Redis 7 (stores shortUrl-to-originalUrl lookups with TTL for sub-millisecond redirects).
- **Relational Database:** PostgreSQL 16 (persists users, URL mappings, and click events for analytics).
- **DevOps:** Multi-stage Dockerfiles for backend and frontend, Docker Compose orchestration, Nginx reverse proxy / SPA fallback.

---

## 🚀 Quick Start with Docker Compose

Running the entire stack locally (PostgreSQL + Redis + Backend + Frontend) requires just one command:

### Prerequisites
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running.

### Run Stack

```bash
# Clone or navigate to the project directory
cd url-shortener-project-main

# Start all 4 services (PostgreSQL, Redis, Backend, Frontend)
docker compose up --build
```

### Access Services
- **Frontend UI:** [http://localhost:3000](http://localhost:3000)
- **Backend API:** [http://localhost:8080](http://localhost:8080)
- **PostgreSQL Database:** `localhost:5432` (db: `urlshortener`, user: `postgres`, pass: `postgres`)
- **Redis Cache:** `localhost:6379`

To stop all services:
```bash
docker compose down
```

---

## ⚡ Redis In-Memory Caching

A URL shortener is heavily read-intensive (99% reads for redirection, 1% writes for link creation). 

1. **On Link Creation (`POST /api/urls/shorten`):**
   - Saves record to PostgreSQL.
   - Immediately warms the Redis cache (`short:<shortUrl> -> originalUrl`) with a 7-day TTL.
2. **On Link Redirection (`GET /{shortUrl}`):**
   - Checks Redis cache first.
   - **Cache Hit:** Resolves the destination URL in microseconds.
   - **Cache Miss:** Queries PostgreSQL, updates Redis cache, and increments click analytics.
3. **Resilience & Graceful Fallback:**
   - If Redis is unavailable or down, the service automatically falls back to PostgreSQL without disrupting user traffic.

---

## 🌐 Free Cloud Deployment Guide (as in the Tutorial)

This guide walks you through deploying the database, backend, Redis cache, and frontend completely **FREE** using Render, Upstash, and Vercel/Netlify.

---

### Step 1: Deploy Free PostgreSQL Database on Render

1. Go to [Render.com](https://render.com) and create an account.
2. In the Render Dashboard, click **New +** -> **PostgreSQL**.
3. Fill in the details:
   - **Name:** `url-shortener-db`
   - **Database:** `urlshortener`
   - **User:** `postgres`
   - **Region:** Choose the region closest to you (e.g., Frankfurt, Ohio, Singapore).
   - **Instance Type:** **Free**
4. Click **Create Database**.
5. Once provisioned, copy:
   - **Internal Database URL** (e.g. `postgres://postgres:password@dpg-xxxx-a:5432/urlshortener`): Use this if your backend is also on Render.
   - **External Database URL**: Use this for connecting from external tools.
   - For Spring Boot, format the JDBC URL as:
     ```
     jdbc:postgresql://<host>:5432/<database_name>
     ```

---

### Step 2: (Optional) Set Up Free Redis on Upstash

Render does not offer a free Redis tier, but **Upstash** provides a 100% free serverless Redis database (10,000 requests/day forever free):

1. Go to [Upstash.com](https://upstash.com) and create a free account.
2. Click **Create Database** -> Name: `url-shortener-redis` -> Select Redis -> Choose primary region.
3. In database details, find your **Endpoint**, **Port**, and **Password**.
4. Note these credentials for the backend environment variables:
   - `REDIS_HOST`: `<your-upstash-endpoint>`
   - `REDIS_PORT`: `<your-upstash-port>`
   - `REDIS_PASSWORD`: `<your-upstash-password>`

*(Note: If you skip Redis in cloud deployment, the backend will gracefully query PostgreSQL directly).*

---

### Step 3: Deploy Backend Web Service on Render

1. Push your repository to **GitHub**.
2. On Render Dashboard, click **New +** -> **Web Service**.
3. Connect your GitHub repository.
4. Configure the service:
   - **Name:** `url-shortener-sb`
   - **Root Directory:** `url-shortener-sb`
   - **Environment / Runtime:** **Docker**
   - **Instance Type:** **Free**
5. Scroll down to **Environment Variables** and add:
   | Key | Value |
   |---|---|
   | `DATABASE_URL` | `jdbc:postgresql://<render-db-host>:5432/urlshortener` |
   | `DATABASE_USERNAME` | `<render-db-user>` |
   | `DATABASE_PASSWORD` | `<render-db-password>` |
   | `DATABASE_DIALECT` | `org.hibernate.dialect.PostgreSQLDialect` |
   | `JWT_SECRET` | `c29tZXN1cGVyc2VjcmV0a2V5Zm9ydXJsc2hvcnRlbmVycHJvamVjdDEyMzQ1Ng==` |
   | `FRONTEND_URL` | `https://your-frontend.onrender.com,https://your-app.vercel.app` |
   | `REDIS_HOST` | *(Optional Upstash endpoint or leave blank)* |
   | `REDIS_PORT` | `6379` |
   | `REDIS_PASSWORD` | *(Optional Upstash password)* |
6. Click **Create Web Service**.
7. Render will build the Docker container and deploy. Once live, note your backend URL (e.g., `https://url-shortener-sb.onrender.com`).

---

### Step 4: Deploy Frontend on Render / Vercel / Netlify

#### Option A: Deploy on Render (Static Site)
1. On Render Dashboard, click **New +** -> **Static Site**.
2. Connect your GitHub repository.
3. Configure:
   - **Name:** `url-shortener-react`
   - **Root Directory:** `url-shortener-react`
   - **Build Command:** `npm run build`
   - **Publish Directory:** `dist`
4. Add **Environment Variables**:
   - `VITE_BACKEND_URL`: `https://url-shortener-sb.onrender.com`
   - `VITE_REACT_FRONT_END_URL`: `https://url-shortener-react.onrender.com`
   - `VITE_REACT_SUBDOMAIN`: `https://url-shortener-react.onrender.com`
5. Go to **Redirects / Rewrites** in the Render settings:
   - Source: `/*`
   - Destination: `/index.html`
   - Action: **Rewrite** *(prevents 404 on page refresh)*
6. Click **Save Changes** and deploy!

#### Option B: Deploy on Vercel
1. Import the repository in [Vercel](https://vercel.com).
2. Set **Root Directory** to `url-shortener-react`.
3. Add the environment variables (`VITE_BACKEND_URL`, etc.).
4. Click **Deploy**. (The included `vercel.json` automatically handles SPA routing).

---

## 🛠️ Local Development Without Docker

### 1. Run PostgreSQL & Redis Locally
Ensure local instances of PostgreSQL (port 5432) and Redis (port 6379) are running, or run them with Docker:
```bash
docker run -d --name local-postgres -p 5432:5432 -e POSTGRES_DB=urlshortener -e POSTGRES_PASSWORD=postgres postgres:16-alpine
docker run -d --name local-redis -p 6379:6379 redis:7-alpine
```

### 2. Run Backend (Spring Boot)
```bash
cd url-shortener-sb
./mvnw clean spring-boot:run
```
Backend will start on `http://localhost:8080`.

### 3. Run Frontend (React)
```bash
cd url-shortener-react
npm install
npm run dev
```
Frontend will start on `http://localhost:5173`.

---

## 🧪 Testing the Application

1. Open frontend at [http://localhost:3000](http://localhost:3000) (or `http://localhost:5173`).
2. Click **Register** to create a new user account.
3. Log in with your new credentials.
4. Enter any URL (e.g. `https://github.com`) to generate a shortened URL.
5. Click the shortened URL (or paste it into your browser).
6. Verify instant redirection to the destination URL.
7. Open the **Dashboard** to view click analytics and charts.
