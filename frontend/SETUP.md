# EpiSante — Frontend Setup Guide

## Prerequisites

- **Node.js**: Version 18+ ([Download](https://nodejs.org/))
- **Java JDK**: Version 21 ([Download](https://www.oracle.com/java/technologies/downloads/))
- **Git**: Latest version ([Download](https://git-scm.com/))
- **Docker Desktop**: Required for running the full platform

---

## Environment Setup

### Java Configuration (Windows)

1. **Set JAVA_HOME:**
   - Open Environment Variables (Win + X → System → Advanced → Environment Variables)
   - Under "User variables", create `JAVA_HOME` = `C:\Program Files\Java\jdk-21`

2. **Update PATH:**
   - Add `%JAVA_HOME%\bin` to your PATH (move to top)

3. **Verify:**
   ```bash
   java -version
   ```

---

## Project Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd episante
```

### 2. Frontend Setup

```bash
cd frontend
npm install
npm run dev
```

The app runs on `http://localhost:5173`.

### 3. Backend (via Docker Compose)

Build the shared library first, then start all services:

```bash
cd episante-common
mvn clean install -DskipTests
cd ../infra/compose
docker-compose up -d --build
```

The API gateway is available at `http://localhost:8089/api/`.

---

## Common Issues

### Tailwind CSS Not Applying

The project uses **Tailwind CSS v3**. If styles are missing:

```bash
npm list tailwindcss           # verify version
npm uninstall tailwindcss      # if v4, downgrade:
npm install -D tailwindcss@^3 postcss autoprefixer
npm run dev                    # restart
```

### Port Already in Use

```bash
# Find and kill process on a port (Windows)
netstat -ano | findstr :5173
taskkill /PID <PID> /F
```

---

## Development Workflow

1. Pull latest changes: `git pull origin master`
2. Install deps if needed: `cd frontend && npm install`
3. Run frontend: `npm run dev`
4. Run backend: `cd infra/compose && docker-compose up -d`

---

## Tech Stack

| Layer | Technologies |
|-------|-------------|
| **Frontend** | React 18, TypeScript, Vite, Tailwind CSS v3, React Router, Axios |
| **Backend** | Spring Boot 3, Java 21, Maven |
| **Infrastructure** | Docker Compose, Nginx (API Gateway), Kafka |
