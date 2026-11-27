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
| `CART_MAX_ITEM_QUANTITY` | `app.cart.max-item-quantity` | `10` (default in `application.yaml`) | `10` (default) | Maximum units of **one cart line**, that is one size/color variant. Must be positive. Requests above it get `400 VALIDATION_ERROR`; adding to a line past it gets `409 CART_ITEM_LIMIT_EXCEEDED`. |
| `CART_INACTIVE_EXPIRY` | `app.cart.inactive-cart-expiry` | `30d` (default) | `30d` (default) | How long a cart may stay untouched before MongoDB deletes it (TTL index on `carts.updatedAt`). Spring duration format, for example `30d`, `12h`, `90m`. |
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

### Cart limits

`app.cart` in `application.yaml` is bound to `CartProperties`. Both keys have defaults (`10` and `30d`), so profile files do not need to set them. At startup the backend creates the TTL index `ttl_carts_updated_at` on `carts.updatedAt` with the configured expiry; when `CART_INACTIVE_EXPIRY` changes, the existing index is updated in place (`collMod`) on the next start. MongoDB's TTL monitor runs about once a minute, so expired carts can live slightly longer than the expiry.

## Running locally

### Local MongoDB (Docker)

- `docker compose up -d` starts the `clothing-store-mongo` container on **localhost:27017**. `docker compose ps` shows when it is healthy.
- Local connection string: `mongodb://localhost:27017/clothing_store`. The `local` profile uses it. It uses the MongoDB default port, so stop any native `mongod` on 27017 first.
- Sample data comes from `docker/mongo-init/seed.js`. It loads **only on the first start with an empty volume**. To re-seed, run `docker compose down -v && docker compose up -d`. Note that `-v` deletes all local data.
- The seed also adds sample homepage promotions to the `promotions` collection when it is empty. Their banner images are served at `/images/promotions/<name>.svg`.
- The seed tags product images by color when they are not tagged yet, and adds product variants (sizes, colors, stock) to the `product_variants` collection whenever that collection is empty, **even if categories and products already exist**. To add variants to an existing local database without wiping it, run `docker compose exec mongo mongosh --quiet /docker-entrypoint-initdb.d/seed.js`; the catalog and promotion blocks skip themselves when their data is present.
- Sample product and category images are SVG illustrations served by the backend at `/images/catalog/<slug>.svg` (files in `src/main/resources/static/images/catalog/`). The seed stores them as root-relative URLs, which the frontend resolves against `VITE_API_BASE_URL`. Regenerate them with `python3 scripts/generate_catalog_images.py src/main/resources/static/images/catalog /tmp/catalog-sheet.svg`. The script also writes one `<slug>-<color-slug>.svg` per color for the color-tagged products (see `COLOR_ITEMS` in the script).
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
model/        MongoDB documents (Category, Product, ProductVariant, Promotion, Cart), the ProductSort option and the priced cart values the cart mapper reads
dto/          response records, one top-level type per file
mapper/       MapStruct mappers for every document-to-DTO conversion, money formatting and page envelopes
exception/    exceptions, error codes and the global ProblemDetail handler
config/       configuration classes, the UTC Clock bean and @ConfigurationProperties records
constants/    fixed literals: messages, patterns, sort parameter names
validation/   Bean Validation constraints that check request values against configured limits
```

Inside `controller`, `service`, `repository`, `model`, `dto`, `mapper` and `exception`, classes are grouped in domain sub-packages: `cart/`, `category/`, `product/`, `promotion/`, plus `common/` for shared types (`PageResponse`, `Money`, `ImageResponse`, `FieldErrorResponse`, `MoneyMapper`, `ErrorCode`, `ResourceNotFoundException`, `ConflictException`, `GlobalExceptionHandler`). Example: `repository/product/ProductRepository`. `config`, `constants` and `validation` stay flat. Tests mirror the same packages.

Conventions: Lombok `@RequiredArgsConstructor` for injection and `@Slf4j` for logging; MapStruct for all mapping; DTOs and documents hold fields only; tunables come from `application.yaml`; no comments in source code.

## API (v1, public, no authentication)

| Endpoint | Description |
|---|---|
| `GET /api/v1/categories` | Active categories, sorted by `displayOrder` and then `name` |
| `GET /api/v1/categories/{slug}` | One active category. Returns 404 `CATEGORY_NOT_FOUND` if missing. |
| `GET /api/v1/products?category=&featured=&page=0&size=20&sort=newest` | Paginated active product cards. `page` is 0..`max-page` (10000) and `size` is 1..`max-page-size` (50), with `size` defaulting to `default-page-size` (20). `sort` is one of `newest`, `name`, `price_asc`, `price_desc`. |
| `GET /api/v1/products/{slug}` | Details of one active product, including its active `variants`. Sent with `Cache-Control: no-store`. Returns 404 `PRODUCT_NOT_FOUND` if missing. |
| `GET /api/v1/promotions` | Homepage promotions that are live right now, sorted by `displayOrder` and then `title`. Always returns 200 with a JSON array, which is `[]` when there are none. |
| `POST /api/v1/cart` | Creates an empty guest cart. `201`, `Location: /api/v1/cart`, body is the cart with its new `id`. |
| `GET /api/v1/cart` | The cart named by the `X-Cart-Id` header, with current prices, availability and subtotal. |
| `POST /api/v1/cart/items` | Adds `{ variantId, quantity }` to the cart. |
| `PATCH /api/v1/cart/items/{variantId}` | Sets the line quantity to `{ quantity }`. |
| `DELETE /api/v1/cart/items/{variantId}` | Removes the line. |

Money is returned as `{ "amount": "49.90", "currency": "ILS" }`. Errors use RFC 9457 problem details (`application/problem+json`) with a stable `code` (`CATEGORY_NOT_FOUND`, `PRODUCT_NOT_FOUND`, `CART_NOT_FOUND`, `CART_ITEM_NOT_FOUND`, `VARIANT_UNAVAILABLE`, `INSUFFICIENT_STOCK`, `CART_ITEM_LIMIT_EXCEEDED`, `CART_CONFLICT`, `VALIDATION_ERROR`, `INTERNAL_ERROR`). Validation errors also include `errors: [{ field, message }]`.

### Product details and variants

`GET /api/v1/products/{slug}` returns the product details followed by a `variants` array (never null, `[]` when the product has no active variants):

```json
{ "id": "...", "slug": "men-classic-oxford-shirt", "name": "...", "description": "...", "brand": "...", "images": [ { "url": "/images/catalog/men-classic-oxford-shirt-light-blue.svg", "alt": "Light blue Oxford shirt on a hanger", "color": "Light blue" } ], "price": { "amount": "49.90", "currency": "ILS" }, "salePrice": null, "category": { "slug": "men", "name": "Men" },
  "variants": [ { "id": "...", "size": "S", "color": { "name": "Light blue", "hexCode": "#9DB9D8" }, "available": true } ] }
```

- Only variants with `active: true` are returned, ordered by `displayOrder` ascending and then `_id`.
- `size` is null for products without sizes (for example sunglasses). `color` is null for products without colors, and `color.hexCode` is null when a color has no single swatch (for example "Floral print").
- Availability rule: `available` is `true` only when `stockQuantity > 0`. The stock count itself, the `sku`, `active` and `displayOrder` are never exposed by the API.
- The response carries `Cache-Control: no-store` so browsers and proxies never show stale availability.

Product images and colors:

- Each item in the details `images` array is `{ url, alt, color }`. `color` is null for untagged images; otherwise it equals, as an exact string, the `color.name` of one of the product's variants. Images keep their stored order, and the first image is the product's default image.
- In MongoDB, `products.images[]` items are `{ url, alt, color }`, and `color` is optional. Documents written before color tagging have no `color` field, which maps to null.
- Product cards (`GET /api/v1/products`) are unchanged: `primaryImage` is `{ url, alt }`, built from the first image, with no `color`.
- Gallery rule for the frontend: when a color is selected, show that color's images plus the untagged images. If no image is tagged with the selected color, show all images.
- The seed tags the images of the seven two-color products that have artwork (`men-classic-oxford-shirt`, `men-slim-chino-trousers`, `women-cable-knit-sweater`, `women-ribbed-tank-top`, `shoes-running-trainers`, `acc-wool-beanie`, `acc-classic-sunglasses`), using one image per color at `/images/catalog/<slug>-<color-slug>.svg`. The first image is the color that comes first in the variants' `displayOrder`. On an existing database, the seed sets these images only for products that have no color-tagged image yet, so running it again changes nothing.

Variants live in the `product_variants` collection:

| Field | Type | Notes |
|---|---|---|
| `_id` | ObjectId | returned as `id` |
| `productId` | ObjectId | references `products._id` |
| `sku` | string | unique, for example `MEN-OXF-BLU-M`; not returned |
| `size` | string or null | for example `S`..`XXL` for apparel, `40`..`45` for shoes |
| `color` | object or null | `{ name, hexCode }`, `hexCode` may be null |
| `stockQuantity` | int | not returned; drives `available` |
| `active` | bool | `false` hides the variant |
| `displayOrder` | int | ascending sort key; the seed uses `sizeIndex * 10 + colorIndex` so sizes list in size order |
| `createdAt`, `updatedAt` | date | not returned by the API |

Indexes: `uk_product_variants_sku` (unique) on `{ sku: 1 }` and `ix_product_variants_product_active_order` on `{ productId: 1, active: 1, displayOrder: 1 }`.

Seed data notes: apparel uses sizes S, M, L, XL, XXL with one or two colors and some undefined size/color combinations; shoes use EU sizes 40 to 45; `shoes-canvas-slip-ons` has every variant out of stock; `MEN-CHN-NVY-XXL` is an inactive variant; `acc-classic-sunglasses` and `acc-wool-beanie` are color-only; `acc-leather-belt` is size-only (S, M, L with no color); `acc-silk-scarf` and `acc-canvas-tote-bag` have a single variant with no size and no color.

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

### Guest cart

The cart is identified by the `X-Cart-Id` request header. `POST /api/v1/cart` generates a random UUID (`UUID.randomUUID()`, backed by `SecureRandom`) and returns it as `id`. The frontend stores it and sends it on every other cart request. There is no cookie. Treat the id like a bearer token: anyone who has it can read and change the cart, so the backend never logs it.

Cart responses carry `Cache-Control: no-store` and have this shape:

```json
{ "id": "3f2b8c1e-9a4d-4e7b-8c2a-1d5e6f7a8b9c", "itemCount": 3, "subtotal": { "amount": "120.00", "currency": "ILS" }, "maxItemQuantity": 10,
  "items": [ { "variantId": "...", "productId": "...", "productSlug": "men-classic-oxford-shirt", "productName": "Classic Oxford Shirt",
    "image": { "url": "/images/catalog/men-classic-oxford-shirt-light-blue.svg", "alt": "..." }, "size": "M", "color": { "name": "Light blue", "hexCode": "#9DB9D8" },
    "quantity": 3, "unitPrice": { "amount": "40.00", "currency": "ILS" }, "originalPrice": { "amount": "49.90", "currency": "ILS" },
    "lineTotal": { "amount": "120.00", "currency": "ILS" }, "status": "AVAILABLE", "priceChanged": false } ] }
```

Rules:

- Prices always come from the catalog. Unknown JSON fields such as a client `price` or `total` are ignored.
- `unitPrice` is `salePrice` when set, otherwise `basePrice`. `originalPrice` is `basePrice` when the product is on sale, otherwise `null`. `lineTotal` is `unitPrice × quantity`. Amounts have 2 decimals, rounded half up.
- `status` is `UNAVAILABLE` when the product is missing or inactive, the variant is missing or inactive, or its stock is 0 or less; `INSUFFICIENT_STOCK` when stock is positive but below the line quantity; otherwise `AVAILABLE`.
- `subtotal` is the sum of `lineTotal` over lines that are not `UNAVAILABLE`, and is `"0.00"` for an empty cart. `itemCount` is the sum of quantities over all lines.
- `priceChanged` is `true` when the current `unitPrice` differs from the price saved when the line was created or its quantity last changed.
- Items are ordered by the time they were first added. Adding a variant that is already in the cart increases the quantity of that line.
- `productSlug`, `productName`, `image`, `unitPrice`, `originalPrice` and `lineTotal` are `null` when the product no longer exists; `size` and `color` are `null` when the variant no longer exists (or has no size or color).
- `image` is the first product image tagged with the variant color, else the first untagged image, else the first image, else `null`.
- Stock is checked but never reserved or decremented, and stock counts are never returned.

Errors:

| Status | `code` | When |
|---|---|---|
| 400 | `VALIDATION_ERROR` | `X-Cart-Id` header missing (`field: "X-Cart-Id"`), blank `variantId`, `quantity` missing or not a JSON integer (`1.5`, `"2"` and `true` are rejected), `quantity` below 1 or above `CART_MAX_ITEM_QUANTITY`, malformed JSON. Range errors on `quantity` carry the message `Quantity must be between 1 and <max>.` with the configured maximum filled in, so the frontend can show it as is; type errors carry `must be a valid integer`. |
| 404 | `CART_NOT_FOUND` | Cart id unknown, expired or not a UUID (the same response for all three) |
| 404 | `CART_ITEM_NOT_FOUND` | `PATCH`/`DELETE` of a variant that is not in the cart |
| 409 | `VARIANT_UNAVAILABLE` | Add or `PATCH` of a variant that is unknown, inactive, out of stock or whose product is missing or inactive |
| 409 | `CART_ITEM_LIMIT_EXCEEDED` | Adding would take the line above `CART_MAX_ITEM_QUANTITY`; the problem has a `maxItemQuantity` property |
| 409 | `INSUFFICIENT_STOCK` | The resulting quantity is above the variant stock |
| 409 | `CART_CONFLICT` | The cart was changed by another request at the same time (optimistic locking); reload and retry |

Carts live in the `carts` collection:

| Field | Type | Notes |
|---|---|---|
| `_id` | string | random UUID, returned as `id` |
| `items[]` | array | `{ productId: ObjectId, variantId: ObjectId, quantity: int, unitPriceSnapshot: Decimal128, addedAt: date }` |
| `createdAt`, `updatedAt` | date | every write updates `updatedAt` |
| `version` | long | optimistic locking |

Index: `ttl_carts_updated_at` on `{ updatedAt: 1 }` with `expireAfterSeconds` from `CART_INACTIVE_EXPIRY`.

CORS: `/api/v1/cart/**` allows `GET`, `HEAD`, `POST`, `PATCH`, `DELETE` and `OPTIONS`; the rest of `/api/**` stays read-only (`GET`, `HEAD`, `OPTIONS`). Both allow only the `Content-Type`, `Accept` and `X-Cart-Id` request headers, the configured origins, and no credentials.

## Deployment (Render)

Set these environment variables on the Render service:

- `SPRING_PROFILES_ACTIVE=prod`
- `MONGODB_URI`: the Atlas connection string for a least-privilege database user. This user also needs permission to create indexes, because `spring.data.mongodb.auto-index-creation` is enabled.
- `FRONTEND_ORIGINS`: the deployed frontend origin(s)
- `STORE_CURRENCY`: optional, since `application-prod.yaml` already sets `ILS`

`PORT` is provided by Render. The OS environment overrides the empty values in `application-prod.yaml`.
