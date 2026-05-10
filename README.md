# AlignTech Feature Management Service

Feature flag service for an e-commerce platform. Lets you toggle features across web, backend without redeploying.



## Tech Stack

- Java 17, Spring Boot 3.2
- PostgreSQL (flags + audit log)
- Redis (cache + real-time pub/sub)
- Flyway (schema migration)


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