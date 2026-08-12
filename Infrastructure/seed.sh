#!/usr/bin/env bash
# Load scripts/seed_dummy_data.sql into the project's Postgres container.
#
# Usage (from anywhere):
#   ./scripts/seed.sh
#
# The Spring Boot app must have been started at least once so Liquibase
# has created the tables. Re-running this script wipes previous dummy
# rows (by demo email) and inserts a fresh set.

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SQL="$ROOT/scripts/seed_dummy_data.sql"
COMPOSE="$ROOT/Infrastructure/docker-compose.yaml"
CONTAINER="${CONTAINER:-eventticketing-postgres}"
DB_USER="${POSTGRES_USER:-postgres}"
DB_NAME="${POSTGRES_DB:-eventticketing}"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker is not on PATH" >&2
  exit 1
fi

if [[ ! -f "$SQL" ]]; then
  echo "missing $SQL" >&2
  exit 1
fi

if ! docker ps --format '{{.Names}}' | grep -qx "$CONTAINER"; then
  echo "Postgres container '$CONTAINER' is not running — starting it…"
  docker compose -f "$COMPOSE" up -d postgres
fi

echo "Waiting for $CONTAINER to accept connections…"
ready=0
for _ in $(seq 1 40); do
  if docker exec "$CONTAINER" pg_isready -U "$DB_USER" -d "$DB_NAME" >/dev/null 2>&1; then
    ready=1
    break
  fi
  sleep 1
done
if [[ "$ready" -ne 1 ]]; then
  echo "Postgres did not become ready in time" >&2
  exit 1
fi

has_users="$(docker exec "$CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -tAc "SELECT to_regclass('public.users')")"
if [[ "$has_users" != "users" ]]; then
  cat >&2 <<'EOF'
Tables do not exist yet (Liquibase has not run).

Start the Spring Boot server once so it creates the schema:

  cd Server && ./mvnw spring-boot:run

Then re-run ./scripts/seed.sh
EOF
  exit 1
fi

echo "Loading dummy data into $DB_NAME…"
docker exec -i "$CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 < "$SQL"

cat <<'EOF'

Dummy data loaded.

  Role        Email                              Password
  ----------  ---------------------------------  --------------------
  ADMIN       admin@example.com                  Admin12345678Admin
  ORGANIZER   nader.farouk@nileevents.eg         Password123!
  ORGANIZER   salma.khalil@cairolive.eg          Password123!
  ORGANIZER   yasmine.nabil@pyramidarts.eg       Password123!
  CUSTOMER    layla.hassan@demo.local            Password123!
  CUSTOMER    omar.said@demo.local               Password123!

Other customers: noor.adel / karim.mostafa / hana.fouad /
                 tamer.rashad / dina.magdy / youssef.ibrahim
                 @demo.local   (same password)
EOF
