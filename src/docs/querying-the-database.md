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

## Example queries

### Books with their authors, editors, and tags

Each relationship is a separate many-to-many, so every table is `LEFT JOIN`ed
(books with no author/editor/tag still appear) and collapsed with
`string_agg(DISTINCT ...)`. The `DISTINCT` is important: without it the multiple
join tables multiply each other's rows and names get duplicated.

```sql
SELECT
    b.id,
    b.title,
    string_agg(DISTINCT a.name, ', ') AS authors,
    string_agg(DISTINCT e.name, ', ') AS editors,
    string_agg(DISTINCT t.name, ', ') AS tags
FROM book b
LEFT JOIN author_book ab ON ab.book_id = b.id
LEFT JOIN author a       ON a.id = ab.author_id
LEFT JOIN editor_book eb ON eb.book_id = b.id
LEFT JOIN editor e       ON e.id = eb.editor_id
LEFT JOIN book_tag bt    ON bt.book_id = b.id
LEFT JOIN tag t          ON t.id = bt.tag_id
GROUP BY b.id, b.title
ORDER BY b.id;
```

Example output:

```
 id |            title             |   authors    |    editors     |         tags
----+------------------------------+--------------+----------------+----------------------
  2 | Effective Java               | Joshua Bloch | Addison-Wesley | best-practices, java
  3 | Java Concurrency in Practice | Brian Goetz  | Addison-Wesley | concurrency, java
```

## Notes

- Flyway owns the schema; the tables (`author`, `author_book`, `book`,
  `book_tag`, `editor`, `editor_book`, `tag`, plus `event_publication` and
  `flyway_schema_history`) are created by the migrations in
  `src/main/resources/db/migration`.
- Integration tests run inside a rolled-back `@Transactional`, so test data is
  not left behind in the database.
