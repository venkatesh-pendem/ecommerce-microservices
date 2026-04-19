# Ecommerce Microservices

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=java" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.4.5-brightgreen?style=for-the-badge&logo=springboot" />
  <img src="https://img.shields.io/badge/Spring_Cloud-2024.0.1-brightgreen?style=for-the-badge&logo=spring" />
  <img src="https://img.shields.io/badge/MySQL-8.0-blue?style=for-the-badge&logo=mysql" />
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker" />
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge" />
</p>

A backend for an ecommerce platform split into microservices. Built with Spring Boot 3 and Spring Cloud. Each service has its own database, they all register with Eureka, and everything goes through an API gateway that handles JWT validation before routing.

---

## Architecture

```
                     ┌─────────────────────────┐
      Clients ──────▶│    API Gateway :8080     │
                     │  (JWT check + routing)   │
                     └───────────┬─────────────┘
                                 │  lb:// via Eureka
           ┌─────────────────────┼────────────────────┐
           ▼                     ▼                     ▼
  ┌────────────────┐   ┌──────────────────┐   ┌──────────────────┐
  │  user-service  │   │ product-service  │   │  order-service   │
  │    :8081       │   │     :8082        │   │     :8083        │
  └───────┬────────┘   └───────┬──────────┘   └───────┬──────────┘
          │                    │                       │
      users_db           products_db              orders_db
          └────────────────────┴───────────────────────┘
                               │
                         MySQL :3306

               ┌──────────────────────────┐
               │    Eureka Server :8761   │
               └──────────────────────────┘
```

---

## Services

| Service | Port | What it does |
|---------|------|--------------|
| api-gateway | 8080 | single entry point, validates JWT, routes to services |
| user-service | 8081 | register/login, user management |
| product-service | 8082 | product catalog, categories, search |
| order-service | 8083 | place orders, track status |
| eureka-server | 8761 | service registry (Eureka dashboard) |

---

## Running it

### With Docker (easiest)

You need Docker and Docker Compose installed. First build all the JARs:

```bash
for svc in eureka-server user-service product-service order-service api-gateway; do
  cd $svc && ./mvnw clean package -DskipTests && cd ..
done
```

Then start everything:

```bash
docker compose up --build
```

Give it about 30 seconds to start up. Check http://localhost:8761 to see if all services registered.

### Running locally

If you'd rather run without Docker, you need MySQL running locally first. Create the databases:

```bash
mysql -u root -p < init-db.sql
```

Then start each service in a separate terminal, starting with eureka:

```bash
# start this first
cd eureka-server && ./mvnw spring-boot:run

# then these in any order
cd user-service    && ./mvnw spring-boot:run
cd product-service && ./mvnw spring-boot:run
cd order-service   && ./mvnw spring-boot:run

# gateway last
cd api-gateway && ./mvnw spring-boot:run
```

---

## Auth

All API calls go through the gateway on port 8080.

Register and get a token first:

```bash
# register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Venkatesh","email":"v@example.com","password":"pass123"}'

# login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"v@example.com","password":"pass123"}'
```

Login returns a JWT. Add it to requests that need auth:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

## API

### Users

| Method | Path | Auth needed |
|--------|------|:-----------:|
| POST | `/api/v1/auth/register` | no |
| POST | `/api/v1/auth/login` | no |
| GET | `/api/v1/users` | yes |
| GET | `/api/v1/users/{id}` | yes |
| DELETE | `/api/v1/users/{id}` | yes |

### Products

GET requests are public. Everything else needs a token.

| Method | Path | Auth needed |
|--------|------|:-----------:|
| GET | `/api/v1/products` | no |
| GET | `/api/v1/products/{id}` | no |
| GET | `/api/v1/products/category/{category}` | no |
| GET | `/api/v1/products/search?name=phone` | no |
| POST | `/api/v1/products` | yes |
| PATCH | `/api/v1/products/{id}` | yes |
| PATCH | `/api/v1/products/{id}/deactivate` | yes |
| DELETE | `/api/v1/products/{id}` | yes |

Categories: `ELECTRONICS`, `CLOTHING`, `BOOKS`, `HOME_AND_KITCHEN`, `SPORTS`, `BEAUTY`, `TOYS`, `AUTOMOTIVE`, `GROCERY`, `OTHER`

Example - create a product:

```json
POST /api/v1/products
Authorization: Bearer <token>

{
  "name": "iPhone 15",
  "description": "128GB, black",
  "price": 79999.00,
  "stockQuantity": 50,
  "category": "ELECTRONICS"
}
```

### Orders

All order endpoints require auth.

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/orders` | place an order |
| GET | `/api/v1/orders/{id}` | get one order |
| GET | `/api/v1/orders` | all orders (paginated) |
| GET | `/api/v1/orders/user/{userId}` | orders for a user |
| GET | `/api/v1/orders/user/{userId}/status/{status}` | filter by status |
| PATCH | `/api/v1/orders/{id}/status` | update status |
| PATCH | `/api/v1/orders/{id}/cancel` | cancel |

Example - place an order:

```json
POST /api/v1/orders
Authorization: Bearer <token>

{
  "userId": 1,
  "shippingAddress": "123 MG Road, Hyderabad",
  "items": [
    {
      "productId": 1,
      "productName": "iPhone 15",
      "quantity": 1,
      "unitPrice": 79999.00
    }
  ]
}
```

#### Order status flow

```
PENDING → CONFIRMED → SHIPPED → DELIVERED
   ↓            ↓
CANCELLED   CANCELLED
```

Once delivered or cancelled, the status can't change.

---

## Swagger UI

Each service has its own Swagger UI running directly (no gateway):

- user-service → http://localhost:8081/swagger-ui.html
- product-service → http://localhost:8082/swagger-ui.html
- order-service → http://localhost:8083/swagger-ui.html

---

## Config

The default config works out of the box for local dev. For Docker, the compose file overrides the datasource URLs and Eureka address automatically.

Important env vars if you need to override:

| Var | Default |
|-----|---------|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/...` |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | `http://localhost:8761/eureka/` |
| `APP_JWT_SECRET` | hex key in application.properties |

> **Note:** Change the JWT secret before putting this anywhere public.

---

## Tech used

- Java 17
- Spring Boot 3.4.5
- Spring Cloud 2024.0.1 (Eureka + Gateway)
- Spring Security + JJWT 0.12.6
- MySQL 8 / Spring Data JPA
- SpringDoc OpenAPI (Swagger UI)
- Docker + Docker Compose
- Lombok, Maven

---

## Project structure

```
ecommerce-microservices/
├── eureka-server/
├── api-gateway/
├── user-service/
├── product-service/
├── order-service/
├── docker-compose.yml
├── init-db.sql
└── README.md
```

Each service follows the same layout: `controller → service → repository`, with `dto`, `model`, `exception`, `security`, and `config` packages.

---

## License

MIT
