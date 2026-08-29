# Querying the database with psql

The project's Postgres runs in Docker via `compose.yaml`. Spring Boot's
docker-compose integration only starts the container during an app or test run
and tears it down afterwards, so for ad-hoc querying you usually start it
yourself.

## Start the database

```bash
docker compose up -d
docker compose ps        # confirm hibernatelab-postgres-1 is "Up"
```

Stop it again when you're done:

```bash
docker compose down
```

## Credentials

From `compose.yaml`:

| Setting    | Value                     |
|------------|---------------------------|
| database   | `mydatabase`              |
| user       | `myuser`                  |
| password   | `secret`                  |
| container  | `hibernatelab-postgres-1` |
| host port  | **random** (see below)    |

> `compose.yaml` maps port `5432` with no fixed host port, so Docker assigns a
> random host port on each run. To pin it, change the mapping to `'5432:5432'`.

## Option 1 — exec into the container (recommended)

No port juggling required.

Interactive shell:

```bash
docker compose exec postgres psql -U myuser -d mydatabase
```

One-off query:

```bash
docker compose exec postgres psql -U myuser -d mydatabase -c "SELECT * FROM editor;"
```

## Option 2 — from the host with a local psql client

Because the port is randomized, look it up first:

```bash
docker compose port postgres 5432        # e.g. 0.0.0.0:38881
PGPASSWORD=secret psql -h localhost -p 38881 -U myuser -d mydatabase
```

## Handy psql commands

| Command          | Purpose                              |
|------------------|--------------------------------------|
| `\dt`            | list tables                          |
| `\d editor_book` | describe a table                     |
| `\d+ book`       | table with column details            |
| `\x`             | toggle expanded (row-per-line) output|
| `\q`             | quit                                 |

## Notes

- Flyway owns the schema; the tables (`author`, `author_book`, `book`,
  `book_tag`, `editor`, `editor_book`, `tag`, plus `event_publication` and
  `flyway_schema_history`) are created by the migrations in
  `src/main/resources/db/migration`.
- Integration tests run inside a rolled-back `@Transactional`, so test data is
  not left behind in the database.
