# clothing-store-be

Spring Boot 4 (Java 25) backend for the Clothing Store. Data is stored in MongoDB via Spring Data MongoDB.

## Configuration

The backend has no `.env` files and no default profile. You choose the profile when you start the app.

| File | Purpose |
|---|---|
| `src/main/resources/application.yaml` | Every property key. Environment-specific keys are bound to environment-variable placeholders with no defaults, for example `spring.mongodb.uri: ${MONGODB_URI}`. Fixed tunables such as the pagination limits also live here. |
| `src/main/resources/application-local.yaml` | Only the variable values for local development (`MONGODB_URI`, `FRONTEND_ORIGINS`, `STORE_CURRENCY`). |
| `src/main/resources/application-prod.yaml` | Only the variable values for production. Secrets are deliberately empty (`''`); the deployment environment supplies them. |
| `src/test/resources/application-test.yaml` | Variable values for the test suite, activated with `@ActiveProfiles("test")`. Testcontainers (`@ServiceConnection`) overrides the Mongo connection. |

### How the YAML pattern works

`application.yaml` says *which* property reads *which* variable:

```yaml
spring:
  mongodb:
    uri: ${MONGODB_URI}
```

The profile file says what the variable is for that environment:

```yaml
MONGODB_URI: mongodb://localhost:27017/clothing_store
```

Spring resolves `${MONGODB_URI}` against all property sources. OS environment variables have a higher precedence than any YAML file, so a real `MONGODB_URI` environment variable always overrides the value in the profile file. Render relies on this.

There is no `spring.profiles.active` in any YAML file. If you start the app without a profile, the placeholders cannot be resolved and startup fails immediately.

### Environment variables

| Variable | Property | local value | prod value | Description |
|---|---|---|---|---|
| `MONGODB_URI` | `spring.mongodb.uri` | `mongodb://localhost:27017/clothing_store` | `''` (must be set in the environment) | MongoDB connection string, including the database name. In prod this is the Atlas `mongodb+srv://...` URI. Never commit it. |
| `FRONTEND_ORIGINS` | `app.cors.allowed-origins` | `http://localhost:5173` | `''` (must be set in the environment) | Comma-separated list of browser origins allowed to call `/api/**`, for example `https://shop.example.com,https://www.shop.example.com`. Spaces around entries are trimmed. Wildcards, paths and trailing slashes are rejected at startup. |
| `STORE_CURRENCY` | `store.currency` | `ILS` | `ILS` | ISO 4217 code returned with every price |
| `PORT` | `server.port` | `8080` | set by Render | HTTP port |
| `SPRING_PROFILES_ACTIVE` | `spring.profiles.active` | — | `prod` | The profile to use, given at run time only |

With the `prod` profile and no environment variables, startup fails fast:

- An empty `FRONTEND_ORIGINS` fails `CorsProperties` validation (`app.cors.allowedOrigins: must not be empty`).
- An empty `MONGODB_URI` fails when the Mongo client is created (`The connection string is invalid. Connection strings must start with either 'mongodb://' or 'mongodb+srv://'`). It never falls back to `localhost`.

### Pagination limits

These limits are configured in `application.yaml` under `app.catalog.pagination` and bound to `CatalogPaginationProperties`:

| Key | Value | Meaning |
|---|---|---|
| `max-page` | `10000` | Highest allowed zero-based `page` index. This keeps `page * size` within the range Spring Data MongoDB can skip. |
| `max-page-size` | `50` | Highest allowed `size` |
| `default-page-size` | `20` | `size` when the request omits it |

Values above a maximum return `400 VALIDATION_ERROR` with, for example, `errors: [{ "field": "page", "message": "must be less than or equal to 10000" }]`.

## Running locally

### Local MongoDB (Docker)

- `docker compose up -d` starts the `clothing-store-mongo` container on **localhost:27017**. `docker compose ps` shows when it is healthy.
- Local connection string: `mongodb://localhost:27017/clothing_store`. The `local` profile uses it. It uses the MongoDB default port, so stop any native `mongod` on 27017 first.
- Sample data comes from `docker/mongo-init/seed.js`. It loads **only on the first start with an empty volume**. To re-seed, run `docker compose down -v && docker compose up -d`. Note that `-v` deletes all local data.
- The seed also adds sample homepage promotions to the `promotions` collection when it is empty. Their banner images are served at `/images/promotions/<name>.svg`.
- Sample product and category images are SVG illustrations served by the backend at `/images/catalog/<slug>.svg` (files in `src/main/resources/static/images/catalog/`). The seed stores them as root-relative URLs, which the frontend resolves against `VITE_API_BASE_URL`. Regenerate them with `python3 scripts/generate_catalog_images.py src/main/resources/static/images/catalog /tmp/catalog-sheet.svg`.
- The container has no authentication and binds only to 127.0.0.1, so use it for local development only. Deployed environments use MongoDB Atlas via `MONGODB_URI`.

### Start the API

Always pass the `local` profile:

```bash
docker compose up -d                                        # MongoDB with sample data
./mvnw spring-boot:run -Dspring-boot.run.profiles=local     # API on http://localhost:8080
```

Or run the packaged jar:

```bash
./mvnw package -DskipTests
java -jar target/*.jar --spring.profiles.active=local
```

In IntelliJ IDEA, open the `ClothingStoreBeApplication` run configuration and set **Active profiles** to `local`.

When the app is ready it logs `Clothing Store backend started successfully with active profiles: [local]`.

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

### Tests

```bash
./mvnw verify
```

Integration tests start a throwaway MongoDB with Testcontainers (`@ServiceConnection`). They need a running Docker daemon but no local MongoDB.

## Code structure

The backend uses a Traditional Layered Architecture under `com.clothingstore.clothing_store_be`:

```text
ClothingStoreBeApplication.java   entry point, logs the startup message
controller/   REST controllers (HTTP only)
service/      business logic
repository/   Spring Data repositories and the @Repository custom fragment
model/        MongoDB documents (Category, Product, Promotion) and the ProductSort option
dto/          response records, one top-level type per file
mapper/       MapStruct mappers for every document-to-DTO conversion, money formatting and page envelopes
exception/    exceptions, error codes and the global ProblemDetail handler
config/       configuration classes, the UTC Clock bean and @ConfigurationProperties records
constants/    fixed literals: messages, patterns, sort parameter names
validation/   Bean Validation constraints that check request values against configured limits
```

Inside `controller`, `service`, `repository`, `model`, `dto`, `mapper` and `exception`, classes are grouped in domain sub-packages: `category/`, `product/`, `promotion/`, plus `common/` for shared types (`PageResponse`, `Money`, `ImageResponse`, `FieldErrorResponse`, `MoneyMapper`, `ErrorCode`, `ResourceNotFoundException`, `GlobalExceptionHandler`). Example: `repository/product/ProductRepository`. `config`, `constants` and `validation` stay flat. Tests mirror the same packages.

Conventions: Lombok `@RequiredArgsConstructor` for injection and `@Slf4j` for logging; MapStruct for all mapping; DTOs and documents hold fields only; tunables come from `application.yaml`; no comments in source code.

## API (v1, public, read-only)

| Endpoint | Description |
|---|---|
| `GET /api/v1/categories` | Active categories, sorted by `displayOrder` and then `name` |
| `GET /api/v1/categories/{slug}` | One active category. Returns 404 `CATEGORY_NOT_FOUND` if missing. |
| `GET /api/v1/products?category=&featured=&page=0&size=20&sort=newest` | Paginated active product cards. `page` is 0..`max-page` (10000) and `size` is 1..`max-page-size` (50), with `size` defaulting to `default-page-size` (20). `sort` is one of `newest`, `name`, `price_asc`, `price_desc`. |
| `GET /api/v1/products/{slug}` | Details of one active product. Returns 404 `PRODUCT_NOT_FOUND` if missing. |
| `GET /api/v1/promotions` | Homepage promotions that are live right now, sorted by `displayOrder` and then `title`. Always returns 200 with a JSON array, which is `[]` when there are none. |

Money is returned as `{ "amount": "49.90", "currency": "ILS" }`. Errors use RFC 9457 problem details (`application/problem+json`) with a stable `code` (`CATEGORY_NOT_FOUND`, `PRODUCT_NOT_FOUND`, `VALIDATION_ERROR`, `INTERNAL_ERROR`). Validation errors also include `errors: [{ field, message }]`.

### Promotions

`GET /api/v1/promotions` takes no parameters and returns an array of:

```json
{ "id": "...", "title": "New season arrivals", "subtitle": "... or null", "imageUrl": "/images/promotions/new-season.svg", "imageAlt": "...", "ctaLabel": "Shop women", "ctaUrl": "/categories/women" }
```

A promotion is returned only when `active` is `true`, `startsAt` is missing/null or not after the current time, and `endsAt` is missing/null or after the current time (`endsAt` is exclusive). The schedule window is applied in the MongoDB query, using the UTC time from the application `Clock` bean.

Call-to-action rule: `ctaUrl` is returned only when it is an internal path that matches `^/(?![/\\])[^\s\p{Cntrl}]*$`. That means it starts with a single `/`, not with `//` or `/\`, and contains no whitespace or control characters. Anything else, such as `https://...`, `//evil.com` or `javascript:...`, returns both `ctaUrl` and `ctaLabel` as `null`. A null or blank `ctaLabel` also returns both as `null`. The frontend should render a CTA only when both are present.

Promotions live in the `promotions` collection:

| Field | Type | Notes |
|---|---|---|
| `_id` | ObjectId | returned as `id` |
| `title` | string | required |
| `subtitle` | string or null | |
| `imageUrl` | string | root-relative like `/images/promotions/<name>.svg`, or an absolute URL |
| `imageAlt` | string | alt text for the banner image |
| `ctaLabel` | string or null | |
| `ctaUrl` | string or null | internal path only (see the rule above) |
| `displayOrder` | int | ascending sort key, then `title` |
| `active` | bool | `false` hides the promotion |
| `startsAt` | date or null | shown from this moment (inclusive) |
| `endsAt` | date or null | hidden from this moment (exclusive) |
| `createdAt`, `updatedAt` | date | not returned by the API |

Index: `ix_promotions_active_order` on `{ active: 1, displayOrder: 1 }`.

There is no admin API for promotions yet. An administrator manages them directly in MongoDB (for example with `mongosh` or Atlas Data Explorer). Banner images are served by the backend from `src/main/resources/static/images/promotions/`. The local seed adds two active promotions and one inactive promotion. It runs only on an empty volume, so run `docker compose down -v && docker compose up -d` to re-seed.

## Deployment (Render)

Set these environment variables on the Render service:

- `SPRING_PROFILES_ACTIVE=prod`
- `MONGODB_URI`: the Atlas connection string for a least-privilege database user. This user also needs permission to create indexes, because `spring.data.mongodb.auto-index-creation` is enabled.
- `FRONTEND_ORIGINS`: the deployed frontend origin(s)
- `STORE_CURRENCY`: optional, since `application-prod.yaml` already sets `ILS`

`PORT` is provided by Render. The OS environment overrides the empty values in `application-prod.yaml`.
