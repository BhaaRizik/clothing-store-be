# clothing-store-be

Spring Boot 4 (Java 25) backend for the Clothing Store. Data is stored in MongoDB via Spring Data MongoDB.

## Configuration

The backend has no `.env` files. Configuration lives in:

| File | Purpose |
|---|---|
| `src/main/resources/application.yaml` | Shared settings: port, active profile, currency, springdoc, Mongo auto-index creation |
| `src/main/resources/application-local.yaml` | Local profile: non-secret defaults for local development |
| `src/main/resources/application-prod.yaml` | Production profile: every value comes from the environment, with **no defaults** |

### Environment variables

| Variable | Property | Required | Default | Description |
|---|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `spring.profiles.active` | in prod | `local` | `local` for development, `prod` for deployments |
| `MONGODB_URI` | `spring.mongodb.uri` | in prod | local: `mongodb://localhost:27018/clothing_store` | MongoDB connection string. It must include the database name. In prod this is the Atlas `mongodb+srv://...` URI. Never commit it. |
| `FRONTEND_ORIGINS` | `app.cors.allowed-origins` | in prod | local: `http://localhost:5173` | Comma-separated list of browser origins allowed to call `/api/**`, for example `https://shop.example.com,https://www.shop.example.com`. Wildcards are not supported. |
| `STORE_CURRENCY` | `store.currency` | no | `ILS` | ISO 4217 code returned with every price |
| `PORT` | `server.port` | no | `8080` | HTTP port. Render sets this automatically. |

When the `prod` profile is active and `MONGODB_URI` or `FRONTEND_ORIGINS` is missing, startup fails on purpose.

## Running locally

### Local MongoDB (Docker)

- `docker compose up -d` starts the `clothing-store-mongo` container on **localhost:27018**. `docker compose ps` shows when it is healthy.
- Local connection string: `mongodb://localhost:27018/clothing_store`. The app uses it by default with the `local` profile. Port 27018 avoids a clash with a native `mongod` on 27017.
- Sample data comes from `docker/mongo-init/seed.js`. It loads **only on the first start with an empty volume**. To re-seed, run `docker compose down -v && docker compose up -d`. Note that `-v` deletes all local data.
- Sample product and category images are SVG illustrations served by the backend at `/images/catalog/<slug>.svg` (files in `src/main/resources/static/images/catalog/`). The seed stores them as root-relative URLs, which the frontend resolves against `VITE_API_BASE_URL`. Regenerate them with `python3 scripts/generate_catalog_images.py src/main/resources/static/images/catalog /tmp/catalog-sheet.svg`.
- The container has no authentication and binds only to 127.0.0.1, so use it for local development only. Deployed environments use MongoDB Atlas via `MONGODB_URI`.

### Start the API

```bash
docker compose up -d        # MongoDB with sample data
./mvnw spring-boot:run      # API on http://localhost:8080 (profile: local)
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

### Tests

```bash
./mvnw verify
```

Integration tests start a throwaway MongoDB with Testcontainers (`@ServiceConnection`). They need a running Docker daemon but no local MongoDB.

## API (v1, public, read-only)

| Endpoint | Description |
|---|---|
| `GET /api/v1/categories` | Active categories, sorted by `displayOrder` and then `name` |
| `GET /api/v1/categories/{slug}` | One active category. Returns 404 `CATEGORY_NOT_FOUND` if missing. |
| `GET /api/v1/products?category=&featured=&page=0&size=20&sort=newest` | Paginated active product cards. `page` is 0..10000 and `size` is 1..50. `sort` is one of `newest`, `name`, `price_asc`, `price_desc`. |
| `GET /api/v1/products/{slug}` | Details of one active product. Returns 404 `PRODUCT_NOT_FOUND` if missing. |

Money is returned as `{ "amount": "49.90", "currency": "ILS" }`. Errors use RFC 9457 problem details (`application/problem+json`) with a stable `code` (`CATEGORY_NOT_FOUND`, `PRODUCT_NOT_FOUND`, `VALIDATION_ERROR`, `INTERNAL_ERROR`). Validation errors also include `errors: [{ field, message }]`.

## Deployment (Render)

Set these environment variables on the Render service:

- `SPRING_PROFILES_ACTIVE=prod`
- `MONGODB_URI`: the Atlas connection string for a least-privilege database user. This user also needs permission to create indexes, because `spring.data.mongodb.auto-index-creation` is enabled.
- `FRONTEND_ORIGINS`: the deployed frontend origin(s)

`PORT` is provided by Render.
