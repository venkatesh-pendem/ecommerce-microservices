# 🛒 Ecommerce Microservices

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=java" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.4.5-brightgreen?style=for-the-badge&logo=springboot" />
  <img src="https://img.shields.io/badge/Spring_Cloud-2024.0.1-brightgreen?style=for-the-badge&logo=spring" />
  <img src="https://img.shields.io/badge/MySQL-8.0-blue?style=for-the-badge&logo=mysql" />
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker" />
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge" />
</p>

A **production-ready ecommerce backend** built with Spring Boot 3, Spring Cloud, and MySQL following a microservices architecture. Features JWT authentication, service discovery, an API gateway, and full Docker support.

---

## 📐 Architecture

```
                        ┌─────────────────────────┐
         Clients ──────▶│     API Gateway :8080    │
                        │  (JWT filter + routing)  │
                        └────────────┬────────────┘
                                     │  lb:// (Eureka)
              ┌──────────────────────┼──────────────────────┐
              ▼                      ▼                       ▼
     ┌────────────────┐   ┌──────────────────┐   ┌──────────────────┐
     │  user-service  │   │ product-service  │   │  order-service   │
     │    :8081       │   │     :8082        │   │     :8083        │
     └───────┬────────┘   └────────┬─────────┘   └────────┬─────────┘
             │                     │                       │
         users_db            products_db              orders_db
             └─────────────────────┴───────────────────────┘
                                   │
                             MySQL :3306

                   ┌─────────────────────────┐
                   │   Eureka Server :8761    │
                   │   (service registry)     │
                   └─────────────────────────┘
```

---

## 🗂️ Project Structure

```
ecommerce-microservices/
├── eureka-server/          # Service discovery (port 8761)
├── api-gateway/            # Single entry point + JWT validation (port 8080)
├── user-service/           # Auth + User CRUD (port 8081)
├── product-service/        # Product catalog + search (port 8082)
├── order-service/          # Order management + status machine (port 8083)
├── docker-compose.yml      # One-command full stack startup
├── init-db.sql             # MySQL databases + users bootstrap
├── .gitignore
└── README.md
```

---

## 🚀 Quick Start

### Prerequisites
- **Java 17+**
- **Maven 3.8+** (or use the included `./mvnw`)
- **Docker + Docker Compose** (for containerised run)
- **MySQL 8** (for local run only)

---

### ▶️ Option 1 — Docker (Recommended)

**Step 1: Build all JARs**
```bash
for svc in eureka-server user-service product-service order-service api-gateway; do
  cd $svc && ./mvnw clean package -DskipTests && cd ..
done
```

**Step 2: Start the full stack**
```bash
docker compose up --build
```

**Step 3: Verify**
- Eureka Dashboard → http://localhost:8761
- API Gateway → http://localhost:8080

---

### ▶️ Option 2 — Run Locally (without Docker)

**Step 1:** Start MySQL and run:
```bash
mysql -u root -p < init-db.sql
```

**Step 2:** Start each service in a separate terminal, **in order**:
```bash
# 1 — Service Discovery (start first)
cd eureka-server && ./mvnw spring-boot:run

# 2 — Business services (any order)
cd user-service    && ./mvnw spring-boot:run
cd product-service && ./mvnw spring-boot:run
cd order-service   && ./mvnw spring-boot:run

# 3 — Gateway (start last)
cd api-gateway && ./mvnw spring-boot:run
```

---

## 🔒 Authentication Flow

All requests must go through the **API Gateway on port 8080**.

```
1.  POST /api/v1/auth/register   →  create account
2.  POST /api/v1/auth/login      →  receive JWT token
3.  Add header to every request:  Authorization: Bearer <token>
4.  Gateway validates token → injects X-Auth-User header → routes to service
```

**Register example:**
```json
POST /api/v1/auth/register
{
  "name": "Venkatesh",
  "email": "venkatesh@example.com",
  "password": "secret123"
}
```

**Login example:**
```json
POST /api/v1/auth/login
{
  "email": "venkatesh@example.com",
  "password": "secret123"
}

// Response
{
  "success": true,
  "data": { "token": "eyJhbGciOiJIUzI1NiJ9..." }
}
```

---

## 📡 API Reference

### 👤 Users (`/api/v1/users`)
| Method | Endpoint | Auth | Description |
|--------|----------|:----:|-------------|
| POST | `/api/v1/auth/register` | ❌ | Register new user |
| POST | `/api/v1/auth/login` | ❌ | Login — returns JWT |
| GET | `/api/v1/users` | ✅ | Get all users (paginated) |
| GET | `/api/v1/users/{id}` | ✅ | Get user by ID |
| DELETE | `/api/v1/users/{id}` | ✅ | Delete user |

### 📦 Products (`/api/v1/products`)
| Method | Endpoint | Auth | Description |
|--------|----------|:----:|-------------|
| GET | `/api/v1/products` | ❌ | List all active products |
| GET | `/api/v1/products/{id}` | ❌ | Get product by ID |
| GET | `/api/v1/products/category/{category}` | ❌ | Filter by category |
| GET | `/api/v1/products/search?name=` | ❌ | Search by name |
| POST | `/api/v1/products` | ✅ | Create product |
| PATCH | `/api/v1/products/{id}` | ✅ | Partial update |
| PATCH | `/api/v1/products/{id}/deactivate` | ✅ | Soft-delete |
| DELETE | `/api/v1/products/{id}` | ✅ | Hard-delete |

**Product categories:** `ELECTRONICS` `CLOTHING` `BOOKS` `HOME_AND_KITCHEN` `SPORTS` `BEAUTY` `TOYS` `AUTOMOTIVE` `GROCERY` `OTHER`

**Create product example:**
```json
POST /api/v1/products
Authorization: Bearer <token>

{
  "name": "iPhone 15",
  "description": "Apple iPhone 15 128GB",
  "price": 79999.00,
  "stockQuantity": 50,
  "category": "ELECTRONICS",
  "imageUrl": "https://example.com/iphone15.jpg"
}
```

### 🧾 Orders (`/api/v1/orders`)
| Method | Endpoint | Auth | Description |
|--------|----------|:----:|-------------|
| POST | `/api/v1/orders` | ✅ | Place new order |
| GET | `/api/v1/orders/{id}` | ✅ | Get order by ID |
| GET | `/api/v1/orders` | ✅ | All orders (paginated) |
| GET | `/api/v1/orders/user/{userId}` | ✅ | Orders by user |
| GET | `/api/v1/orders/user/{userId}/status/{status}` | ✅ | Filter by status |
| PATCH | `/api/v1/orders/{id}/status` | ✅ | Update order status |
| PATCH | `/api/v1/orders/{id}/cancel` | ✅ | Cancel order |

**Place order example:**
```json
POST /api/v1/orders
Authorization: Bearer <token>

{
  "userId": 1,
  "shippingAddress": "123 Main St, Hyderabad, India",
  "items": [
    {
      "productId": 1,
      "productName": "iPhone 15",
      "quantity": 2,
      "unitPrice": 79999.00
    }
  ]
}
```

---

## 📦 Order Status Machine

```
  PENDING ──▶ CONFIRMED ──▶ SHIPPED ──▶ DELIVERED
     │              │
     └──────────────┴──▶ CANCELLED
```

| Transition | Allowed |
|-----------|---------|
| PENDING → CONFIRMED | ✅ |
| PENDING → CANCELLED | ✅ |
| CONFIRMED → SHIPPED | ✅ |
| CONFIRMED → CANCELLED | ✅ |
| SHIPPED → DELIVERED | ✅ |
| DELIVERED → any | ❌ |
| CANCELLED → any | ❌ |

---

## 📖 Swagger / OpenAPI

Each service exposes interactive API docs (accessible directly, bypass gateway):

| Service | Swagger UI |
|---------|-----------|
| user-service | http://localhost:8081/swagger-ui.html |
| product-service | http://localhost:8082/swagger-ui.html |
| order-service | http://localhost:8083/swagger-ui.html |

---

## ⚙️ Environment Variables

Docker Compose overrides these automatically. For local runs, edit the respective `application.properties`.

| Variable | Default | Used By |
|----------|---------|---------|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/...` | All services |
| `SPRING_DATASOURCE_USERNAME` | `user_service` / `product_service` / `order_service` | All services |
| `SPRING_DATASOURCE_PASSWORD` | `user123` / `product123` / `order123` | All services |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | `http://localhost:8761/eureka/` | All services + gateway |
| `APP_JWT_SECRET` | 256-bit hex key | All services + gateway |
| `APP_JWT_EXPIRATION_MS` | `86400000` (24 h) | user-service |

> ⚠️ **Change the JWT secret before deploying to production!**

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.4.5 |
| Service Discovery | Spring Cloud Netflix Eureka 2024.0.1 |
| API Gateway | Spring Cloud Gateway |
| Security | Spring Security + JJWT 0.12.6 |
| Database | MySQL 8.0 |
| ORM | Spring Data JPA / Hibernate |
| API Docs | SpringDoc OpenAPI 3 (Swagger UI) |
| Containerisation | Docker + Docker Compose |
| Build Tool | Maven (Maven Wrapper included) |
| Boilerplate | Lombok |

---

## 📋 Service Port Summary

| Service | Port |
|---------|------|
| API Gateway | **8080** |
| User Service | 8081 |
| Product Service | 8082 |
| Order Service | 8083 |
| Eureka Server | 8761 |
| MySQL | 3306 |

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m "Add your feature"`
4. Push to the branch: `git push origin feature/your-feature`
5. Open a Pull Request

---

## 📄 License

This project is licensed under the **MIT License** — feel free to use it for learning or as a base for your own projects.

---

<p align="center">Built with ❤️ by <strong>Venkatesh</strong></p>
