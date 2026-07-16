# Kendi POS — Backend

**Backend REST API for the Kendi POS system**

Built with **Spring Boot 3.5**, **Java 21**, **PostgreSQL 16**, and **Flyway migrations**. Provides a stateful backend for a Vue 3 + Tauri restaurant POS with orders, products, staff management, suppliers, inventory tracking, AI-powered invoice scanning, table management with drag-drop layout, and complete reservation workflow.

---

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker + Docker Compose (for PostgreSQL)
- An Anthropic API key (for AI invoice scanning feature)

### 1. Start the database

```bash
docker compose up -d
```

This starts PostgreSQL 16 on port `5432` with:
- Database: `kendi_pos`
- User: `postgres`
- Password: `kendi123`

### 2. Configure secrets

Create `src/main/resources/application-local.properties` (this file is git-ignored and never committed):

```properties
anthropic.api.key=sk-ant-your-real-key-here
```

The main `application.properties` includes:
spring.config.import=optional:application-local.properties

so local secrets are automatically merged if present.

### 3. Run the app

```bash
./mvnw spring-boot:run
```

The server starts on `http://localhost:8080`.

On first startup:
- Flyway applies migrations V2 through V5 automatically
- 10 tables are seeded across Main Dining, Terrace, and Outdoor sections
- Sample products, categories, staff and suppliers are seeded if the database is empty

---

## Architecture

### Tech Stack

- **Spring Boot 3.5** — Web, Data JPA, Security, Validation
- **Java 21** — Records for DTOs, pattern matching, sealed classes
- **PostgreSQL 16** — Primary data store (via Docker)
- **Flyway** — Database schema versioning (V2–V5)
- **Apache PDFBox 2.0.30** — PDF generation for supplier orders and reports
- **Anthropic Claude API** — AI-powered invoice extraction from PDF documents
- **Lombok** — Reduced boilerplate on selected entities

### Package Layout
com.kendi.pos/
├── ai/           # AI-powered invoice scanning (Anthropic Claude)
├── auth/         # PIN-based staff authentication
├── category/     # Product categories
├── config/       # Spring Security and CORS configuration
├── delivery/     # Delivery orders and history
├── order/        # POS orders, items, and payment processing
├── product/      # Products, stock tracking, receipts
├── report/       # Sales, staff, and daily reports
├── restotable/   # Restaurant tables and reservations
├── staff/        # Staff members with PIN and role
├── supplier/     # Suppliers and purchase orders (PDF)
└── PosApplication.java

Each package follows a consistent pattern: `Entity`, `Repository`, `Controller`, and `Service` (where business logic warrants separation).

---

## Domain Model

### Product

Core entity with **flexible stock tracking**:

- `stockUnit` — `PIECE` (whole units) or `KG` (weight-based)
- `stockQuantity` — Current inventory (double for KG precision)
- `trackStock` — If false, stock is ignored (e.g., for services)
- `autoDeductOnSale` — For `PIECE` only; decrements stock on payment
- `pricePerKg` / `defaultWeightG` — For KG-based products (e.g., mishi, djathi)

### Order

- Status flow: `open` → `closed` → `paid` (or `cancelled`)
- Items reference products by ID and store `price` snapshot at order time
- Payment supports cash, card, and per-table combined payment (`/pay-all`)
- Automatic subtotal and total recalculation on every mutation
- Stock deduction happens on payment for eligible products
- **Auto-triggers reservation ARRIVED status when order is created for a reserved table**
- **Auto-releases table to AVAILABLE when order is paid or cancelled**

### RestaurantTable

Full table management with visual layout support:

- `name` — Display name (unique)
- `seatCount` — Number of seats (2, 4, 6, 8, 10)
- `section` — `MAIN_DINING`, `TERRACE`, or `OUTDOOR`
- `status` — `AVAILABLE`, `ON_DINE`, or `RESERVED`
- `positionX` / `positionY` — Drag-and-drop coordinates for floor plan
- `size` — Individual table size for visual rendering (100–250px)
- `sortOrder` — Fallback ordering for grid layout

### Reservation

Complete reservation lifecycle with automatic table state sync:

- `guestName`, `guestPhone`, `guestCount` — Guest details
- `reservationTime` — Requested date and time
- `requestedBy` — Staff member who created the request
- `status` — `PENDING_REQUEST`, `CONFIRMED`, `ARRIVED`, `NO_SHOW`, `DECLINED`, or `CANCELLED`
- Timestamps: `confirmedAt`, `arrivedAt`, `noShowAt`

**Lifecycle:**
1. Waiter creates request → `PENDING_REQUEST`
2. Admin confirms → `CONFIRMED`, table becomes `RESERVED`
3. Order opens for table → `ARRIVED`, table becomes `ON_DINE` (automatic)
4. Order paid/cancelled → table returns to `AVAILABLE` (automatic)
5. Admin can also manually mark `ARRIVED` or `NO_SHOW`

### Staff

- Roles: `admin` (full access) or `cashier` (POS only)
- Authentication via 4-digit PIN (BCrypt-hashed)
- `active` flag for enabling/disabling accounts

### Supplier

- Suppliers linked to products
- Purchase orders generated as PDFs with itemized line items
- Delivery history tracked separately for reconciliation

### Category

Simple product taxonomy — name and sortOrder for menu display order.

---

## API Overview

All endpoints are prefixed with `/api`.

### Products
- `GET /products` — List all
- `POST /products` — Create
- `PUT /products/{id}` — Update
- `DELETE /products/{id}` — Delete

### Orders
- `GET /orders` — List with optional `status` or `tableId` filter
- `POST /orders` — Create new order (auto-triggers ARRIVED for reservation)
- `PUT /orders/{id}` — Update items on an open order
- `POST /orders/{id}/close` — Close order (finalize before payment)
- `POST /orders/{id}/pay` — Process payment (deducts stock, releases table)
- `POST /orders/{id}/cancel` — Cancel order (releases table)
- `POST /orders/table/{tableId}/pay-all` — Combined payment for all open orders on a table

### Tables
- `GET /tables` — List all, filterable by `section`
- `GET /tables/{id}` — Get single table
- `POST /tables` — Create new table
- `PUT /tables/{id}` — Update table (name, seats, section)
- `PATCH /tables/{id}/position` — Update x/y coordinates (drag-and-drop)
- `PATCH /tables/{id}/size` — Update visual size
- `PATCH /tables/{id}/status` — Update status manually
- `DELETE /tables/{id}` — Delete table

### Reservations
- `GET /reservations` — List all, filterable by `status`
- `GET /reservations/history` — Historical reservations with date range filter
- `GET /reservations/stats/today` — Today's statistics (arrived, no-shows, show-up rate)
- `GET /reservations/stats/range?from=YYYY-MM-DD&to=YYYY-MM-DD` — Statistics for date range
- `POST /reservations/requests` — Create reservation request (from waiter)
- `PATCH /reservations/{id}/confirm` — Admin confirms (table becomes RESERVED)
- `PATCH /reservations/{id}/decline` — Admin declines
- `PATCH /reservations/{id}/arrived` — Manually mark as arrived (table becomes ON_DINE)
- `PATCH /reservations/{id}/no-show` — Mark as no-show (table becomes AVAILABLE)

### Staff & Auth
- `POST /auth/login` — Login with PIN, returns token and staff info
- `GET /staff` — List all staff (admin)
- `POST /staff` — Create staff member
- `PUT /staff/{id}` — Update
- `DELETE /staff/{id}` — Deactivate

### AI Invoice Scanning
- `POST /ai/invoice/scan` — Upload PDF invoice, receive extracted line items via Claude

### Reports
- `GET /reports/daily` — Daily sales summary
- `GET /reports/staff` — Staff performance report
- `GET /reports/monthly` — Monthly accountant report

### Suppliers
- `GET /suppliers` — List all
- `POST /suppliers` — Create
- `POST /suppliers/{id}/order` — Generate PDF purchase order

---

## Database Migrations

Flyway migrations live in `src/main/resources/db/migration/`. Applied automatically on startup:

- **V2** — Create `restaurant_tables` (name, seat_count, section, status, position_x/y)
- **V3** — Add `sort_order` column for grid fallback
- **V4** — Create `reservations` (guest info, status lifecycle, timestamps)
- **V5** — Add `size` column to tables for per-table visual sizing

To add a new migration:
1. Create `V{N}__description.sql` in `db/migration/`
2. Increment version number
3. Restart the app — Flyway will apply it automatically

---

## Business Logic Highlights

### Table–Order–Reservation Sync

The system maintains **automatic consistency** between three related entities:

1. **On order creation** for a table with a confirmed reservation:
   - Reservation status → `ARRIVED`
   - Table status → `ON_DINE`
   - `arrivedAt` timestamp populated

2. **On order payment or cancellation** when no other open orders exist:
   - Table status → `AVAILABLE`

3. **On admin reservation confirmation**:
   - Table status → `RESERVED`
   - `confirmedAt` timestamp populated

This means waitstaff and admins see real-time, consistent state across all views without manual synchronization.

### Stock Deduction

Only products with `trackStock=true`, `autoDeductOnSale=true`, and `stockUnit=PIECE` are auto-deducted on payment. KG products require manual adjustment (recipe-based deduction is planned for a future release).

---

## Security

Current setup is **development-friendly**:
- CORS allows all origins (for Tauri desktop app + web dev)
- Session management is **stateless**
- All endpoints are **permitAll** for now
- Method-level security (`@EnableMethodSecurity`) is enabled and ready for role-based access

**Planned:** JWT authentication with role-based `@PreAuthorize` guards for admin-only operations (add/edit/delete tables, confirm/decline reservations).

---

## Development

### Run tests

```bash
./mvnw test
```

### Build a jar

```bash
./mvnw clean package
```

The runnable jar lands in `target/pos-0.0.1-SNAPSHOT.jar`.

### Hot reload

Spring Boot DevTools is included. Edit code, save, and the app restarts automatically.

---

## Frontend

The Vue 3 + Tauri desktop frontend lives in a separate repository:  
**[kendi-pos-frontend](https://github.com/BrikendGjyliqi/kendi-pos-frontend)**

---

## License

Private project — part of the diploma thesis "Design and Implementation of an Offline-First POS System for Restaurants in Kosovo" at the University of Hildesheim.

Author: **Brikend Gjyliqi**
