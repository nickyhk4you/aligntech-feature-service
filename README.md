# AlignTech Feature Management Service

Feature flag service for an e-commerce platform. Lets you toggle features across web, backend and mobile apps without redeploying.



## Tech Stack

- Java 17, Spring Boot 3.2
- PostgreSQL (flags + audit log)
- Redis (cache + real-time pub/sub)
- Flyway (schema migration)


## Getting Started

```bash
# create db
psql postgres -c "CREATE USER aligntech WITH PASSWORD 'aligntech' CREATEDB;"
psql postgres -c "CREATE DATABASE feature_service OWNER aligntech;"

# start redis
brew services start redis

# run
cd service && mvn spring-boot:run
```


## APIs

### Management (`/api/v1/admin`)

```
POST   /flags              create a flag
GET    /flags              list/search (optional ?status=active)
GET    /flags/{id}         get one
POST   /flags/{id}/activate   activate
DELETE /flags/{id}         archive
```

### Evaluation (`/api/v1`)

```
POST /evaluate             evaluate flags for a context
GET  /snapshot             dump all active flags (for SDK init)
GET  /stream               SSE stream for live flag changes
GET  /health               just "OK"
```