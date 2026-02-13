#!/usr/bin/env bash
set -e

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"

cleanup() {
  echo ""
  echo "Shutting down..."
  kill $BE_PID $FE_PID 2>/dev/null || true
  docker compose -f "$ROOT_DIR/infra/docker-compose.yml" down
  echo "All services stopped."
}
trap cleanup EXIT INT TERM

# 1. Start PostgreSQL
echo "Starting PostgreSQL..."
docker compose -f "$ROOT_DIR/infra/docker-compose.yml" up -d

echo "Waiting for PostgreSQL to be ready..."
until docker exec breathego-db pg_isready -U breathego -d breathego >/dev/null 2>&1; do
  sleep 1
done
echo "PostgreSQL is ready."

# 2. Start backend
echo "Starting backend (Spring Boot)..."
cd "$ROOT_DIR/api"
./mvnw -q spring-boot:run &
BE_PID=$!

# Wait for backend to be available
echo "Waiting for backend on port 8080..."
while ! curl -s http://localhost:8080/actuator/health >/dev/null 2>&1 && kill -0 $BE_PID 2>/dev/null; do
  sleep 2
done
echo "Backend is ready."

# 3. Start frontend
echo "Starting frontend (Next.js)..."
cd "$ROOT_DIR/web"
npm run dev &
FE_PID=$!

echo ""
echo "========================================="
echo "  Breathe & Go is running!"
echo "  Frontend: http://localhost:3000"
echo "  Backend:  http://localhost:8080"
echo "  Database: localhost:5432"
echo "========================================="
echo "Press Ctrl+C to stop all services."
echo ""

wait
