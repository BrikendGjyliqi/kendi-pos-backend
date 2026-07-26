# Kendi POS — Backend

**Spring Boot REST API** for the Kendi POS restaurant management system.

Built with **Spring Boot 3.5**, **Java 21**, **PostgreSQL 16**, and **Flyway** migrations. Serves as the authoritative source of truth for a Vue 3 + Tauri desktop point-of-sale, and supports an **offline-first client** through dedicated sync endpoints (health check, staff PIN cache, client-generated UUIDs).

---

## Highlights

- **Offline-first friendly** — dedicated `/api/health` endpoint for the client's sync engine, `/api/staff/cache` exposes BCrypt PIN hashes for offline authentication, and all mutation endpoints accept client-generated UUIDs so the frontend can create objects offline and reference them immediately.
- **Automatic state synchronization** — creating an order for a reserved table atomically transitions the reservation to `ARRIVED` and the table to `ON_DINE`; paying or cancelling releases the table back to `AVAILABLE`. No orchestration required on the client.
- **Reservation lifecycle** — complete workflow from `PENDING_REQUEST` through `CONFIRMED`, `ARRIVED`, `NO_SHOW`, `DECLINED`, and `CANCELLED`, with automatic table state transitions at each step.
- **Table management with visual layout** — tables carry `positionX`, `positionY`, and `size` fields so admins can drag-and-drop the floor plan in the frontend and it persists atomically.
- **AI-powered invoice scanning** — `POST /api/ai/invoice/scan` sends uploaded PDFs to the Anthropic Claude API and returns extracted supplier, line items, and totals.
- **Flexible stock tracking** — products can track stock in `PIECE` or `KG` units with automatic deduction on payment for eligible items.
- **PDF generation** — supplier purchase orders and reports produced server-side via Apache PDFBox.
- **Flyway migrations** — schema versioning with V2 through V5, applied automatically on startup.
- **Statistical reports** — daily Z-report, monthly report, per-staff analytics with tips.

---

## Architecture at a Glance

```
┌────────────────────────────────────────────────────────────┐
│                   Spring Boot Backend                       │
│                                                              │
│   ┌────────────────┐        ┌─────────────────────┐        │
│   │  REST API      │◄──────►│  Service Layer      │        │
│   │  Controllers   │        │  (business logic,   │        │
│   │  (validated    │        │   state transitions)│        │
│   │   with Jakarta)│        └──────────┬──────────┘        │
│   └────────┬───────┘                   │                    │
│            │                            ▼                    │
│            │                 ┌─────────────────────┐        │
│            │                 │  JPA Repositories    │        │
│            │                 │  (Spring Data)       │        │
│            │                 └──────────┬──────────┘        │
│            │                            │                    │
│            │                            ▼                    │
│            │                 ┌─────────────────────┐        │
│            │                 │  PostgreSQL 16       │        │
│            │                 │  (Docker container)  │        │
│            │                 └─────────────────────┘        │
│            │                                                 │
│   ┌────────▼─────────────────────────────────────┐         │
│   │  Sync-oriented endpoints                      │         │
│   │  • GET /api/health   (connectivity check)     │         │
│   │  • GET /api/staff/cache (BCrypt hashes)       │         │
│   │  • POST /api/orders (accepts client UUIDs)    │         │
│   └───────────────────────────────────────────────┘         │
│                                                              │
└──────────────────────────────────────────────┬──────────────┘
                                                │ HTTP
                                                ▼
                             ┌──────────────────────────────┐
                             │  Tauri Desktop Client         │
                             │  (Vue 3, local SQLite,        │
                             │   sync engine with retry)     │
                             └──────────────────────────────┘
```

---

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker + Docker Compose (for PostgreSQL)
- An Anthropic API key (for AI invoice scanning)

### 1. Start the database

```bash
docker compose up -d
```

Starts PostgreSQL 16 on port `5432` with:

- Database: `kendi_pos`
- User: `postgres`
- Password: `kendi123`

### 2. Configure secrets

Create `src/main/resources/application-local.properties` (git-ignored):

```properties
anthropic.api.key=sk-ant-your-real-key-here
```

The main `application.properties` includes:

```
spring.config.import=optional:application-local.properties
```

so local secrets are automatically merged if present.

### 3. Run the app

```bash
./mvnw spring-boot:run
```

The server starts on `http://localhost:8080`.

On first startup:

- Flyway applies migrations V2 through V5 automatically
- 10 sample tables are seeded across Main Dining, Terrace, and Outdoor sections
- Sample products, categories, staff, and suppliers are seeded if the database is empty
- Default PINs: **Admin `0000`**, **Cashier `1234`**

---

## Tech Stack

| Layer               | Technology                                              |
|---------------------|---------------------------------------------------------|
| Framework           | Spring Boot 3.5 (Web, Data JPA, Security, Validation)   |
| Language            | Java 21 (records, pattern matching, sealed classes)     |
| Database            | PostgreSQL 16 (Docker)                                  |
| Schema versioning   | Flyway (V2–V5)                                          |
| PDF generation      | Apache PDFBox 2.0.30                                    |
| AI extraction       | Anthropic Claude API                                    |
| Password hashing    | Spring Security BCrypt                                  |
| Build tool          | Maven 3.9                                               |

---

## Package Layout

```
com.kendi.pos/
├── ai/           # AI-powered invoice scanning (Anthropic Claude)
├── auth/         # PIN-based staff authentication (BCrypt)
├── category/     # Product categories
├── config/       # Spring Security and CORS configuration
├── delivery/     # Delivery orders and history
├── health/       # /api/health for client sync engine
├── order/        # POS orders, items, and payment processing
├── product/      # Products, stock tracking, receipts
├── report/       # Sales, staff, and daily reports
├── restotable/   # Restaurant tables and reservations
├── staff/        # Staff members with PIN and role
├── supplier/     # Suppliers and purchase orders (PDF)
└── PosApplication.java
```

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
- Tip tracking with `tipAmount` and `tipPercent` per order, proportional distribution across per-table combined payments
- Automatic subtotal and total recalculation on every mutation
- Stock deduction happens on payment for eligible products
- **Accepts client-generated UUIDs** — order IDs and item IDs can be provided by the offline client
- **Idempotent creation** — repeated `POST /orders` with the same ID returns the existing order (crucial for retry-safe sync)
- **Auto-triggers reservation `ARRIVED`** when created for a table with a confirmed reservation
- **Auto-releases table to `AVAILABLE`** when paid or cancelled and no other open orders remain

### RestaurantTable

Full table management with visual layout support:

- `name` — Display name (unique)
- `seatCount` — Number of seats (2, 4, 6, 8, 10)
- `section` — `MAIN_DINING`, `TERRACE`, or `OUTDOOR`
- `status` — `AVAILABLE`, `ON_DINE`, or `RESERVED`
- `positionX` / `positionY` — Drag-and-drop coordinates for floor plan
- `size` — Individual table size for visual rendering (100–250 px)
- `sortOrder` — Fallback ordering for grid layout

### Reservation

Complete reservation lifecycle with automatic table state sync:

- `guestName`, `guestPhone`, `guestCount` — Guest details
- `reservationTime` — Requested date and time
- `requestedBy` — Staff member who created the request
- `status` — `PENDING_REQUEST`, `CONFIRMED`, `ARRIVED`, `NO_SHOW`, `DECLINED`, or `CANCELLED`
- Timestamps: `confirmedAt`, `arrivedAt`, `noShowAt`, `createdAt`, `updatedAt`

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
- BCrypt hashes exposed via `/api/staff/cache` for **offline PIN validation** in the client

### Supplier

- Suppliers linked to products
- Purchase orders generated as PDFs with itemized line items
- Delivery history tracked separately for reconciliation

### Category

Simple product taxonomy — name, colour, and `sortOrder` for menu display order.

---

## API Overview

All endpoints are prefixed with `/api`.

### Sync (offline-first client support)

- `GET /health` — Connectivity check for the client's sync engine, returns 200 with a small JSON body
- `GET /staff/cache` — Returns staff records with BCrypt PIN hashes for offline login validation

### Products

- `GET /products` — List all
- `POST /products` — Create
- `PUT /products/{id}` — Update
- `DELETE /products/{id}` — Delete
- `PATCH /products/{id}/stock` — Adjust stock quantity (positive or negative delta)

### Orders

- `GET /orders` — List with optional `status` or `tableId` filter
- `POST /orders` — Create new order; accepts client-generated UUID, idempotent, auto-triggers reservation `ARRIVED`
- `PUT /orders/{id}` — Update items on an open order (also triggers auto-arrived)
- `POST /orders/{id}/close` — Close order (finalize before payment)
- `POST /orders/{id}/pay` — Process payment (deducts stock, releases table)
- `POST /orders/{id}/cancel` — Cancel order (releases table if no other open orders)
- `POST /orders/table/{tableId}/pay-all` — Combined payment for all open orders on a table, with proportional tip distribution

### Tables

- `GET /tables` — List all, filterable by `section`
- `GET /tables/{id}` — Get single table
- `POST /tables` — Create new table
- `PUT /tables/{id}` — Update table (name, seats, section)
- `PATCH /tables/{id}/position` — Update `x` / `y` coordinates (drag-and-drop)
- `PATCH /tables/{id}/size` — Update visual size
- `PATCH /tables/{id}/status` — Update status manually
- `DELETE /tables/{id}` — Delete table

### Reservations

- `GET /reservations` — List all, filterable by `status`
- `GET /reservations/history?status=&from=&to=` — Historical reservations with date range and status filter
- `GET /reservations/stats/today` — Today's statistics (arrived, no-shows, show-up rate)
- `GET /reservations/stats/range?from=YYYY-MM-DD&to=YYYY-MM-DD` — Statistics for a date range
- `POST /reservations/requests` — Create reservation request (from waiter)
- `PATCH /reservations/{id}/confirm` — Admin confirms (table becomes `RESERVED`)
- `PATCH /reservations/{id}/decline` — Admin declines
- `PATCH /reservations/{id}/arrived` — Manually mark as arrived (table becomes `ON_DINE`)
- `PATCH /reservations/{id}/no-show` — Mark as no-show (table returns to `AVAILABLE`)

### Staff and Auth

- `POST /auth/login` — Login with PIN, returns token and staff info
- `GET /staff` — List all staff (admin)
- `GET /staff/cache` — BCrypt hashes for offline validation
- `POST /staff` — Create staff member
- `PUT /staff/{id}` — Update
- `DELETE /staff/{id}` — Deactivate

### AI Invoice Scanning

- `POST /ai/invoice/scan` — Upload PDF invoice, receive extracted supplier and line items via Claude

### Reports

- `GET /reports/z-report?date=YYYY-MM-DD` — Daily Z-report (total, cash, card, top products, orders)
- `GET /reports/staff?staffId=&date=` — Per-staff performance report with tips
- `GET /reports/monthly?from=&to=` — Monthly accountant report (available client-side by aggregating daily z-reports)

### Suppliers and Deliveries

- `GET /suppliers` — List all
- `POST /suppliers` — Create
- `POST /suppliers/{id}/order` — Generate PDF purchase order
- `GET /deliveries` — Delivery history
- `POST /deliveries` — Record incoming delivery (updates stock)

---

## Database Migrations

Flyway migrations live in `src/main/resources/db/migration/`. Applied automatically on startup:

- **V2** — Create `restaurant_tables` (name, seat_count, section, status, position_x/y)
- **V3** — Add `sort_order` column for grid fallback
- **V4** — Create `reservations` (guest info, status lifecycle, timestamps)
- **V5** — Add `size` column to tables for per-table visual sizing

To add a new migration:

1. Create `V{N}__description.sql` in `db/migration/`
2. Increment the version number
3. Restart the app — Flyway applies it automatically

---

## Business Logic Highlights

### Automatic Table–Order–Reservation Synchronization

The system maintains **automatic consistency** between three related entities so the client never has to orchestrate multi-step state updates:

1. **On order creation** for a table with a confirmed reservation:
   - Reservation status → `ARRIVED`
   - Table status → `ON_DINE`
   - `arrivedAt` timestamp populated
2. **On order payment or cancellation** when no other open orders exist for the table:
   - Table status → `AVAILABLE`
3. **On admin reservation confirmation**:
   - Table status → `RESERVED`
   - `confirmedAt` timestamp populated

Waitstaff and admins see real-time, consistent state across all views without manual synchronization.

### Idempotent Order Creation

`POST /orders` is idempotent: if the client sends the same `id` twice (common when a sync retry hits after an ambiguous timeout), the server returns the existing order instead of creating a duplicate. This is essential for a reliable offline-first client.

### Stock Deduction

Only products with `trackStock=true`, `autoDeductOnSale=true`, and `stockUnit=PIECE` are auto-deducted on payment. KG products require manual adjustment (recipe-based deduction is planned for a future release).

### Tip Handling

Orders carry `tipAmount` and `tipPercent`. When combining payments for a table (`/pay-all`), the total tip is distributed proportionally across orders based on each order's total, with rounding delta applied to the last order to keep the sum exact.

---

## Security

Current setup is **development-friendly**:

- CORS allows all origins (for Tauri desktop app + web dev)
- Session management is **stateless**
- All endpoints are **permitAll** for now
- Method-level security (`@EnableMethodSecurity`) is enabled and ready for role-based access

**Planned:** JWT authentication with role-based `@PreAuthorize` guards for admin-only operations (add/edit/delete tables, confirm/decline reservations, staff management).

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

The frontend uses a local SQLite database as its read source of truth and reconciles with this backend via a 15-second polling sync engine. It expects the backend on `http://localhost:8080`, and continues to operate in offline mode when the backend is unreachable, queuing all mutations for later flush.

---

## Roadmap

Completed for the current thesis milestone:

- ✅ Full REST API for menu, tables, orders, payments, tipping
- ✅ Reservation workflow with automatic state transitions
- ✅ Table drag-and-drop layout persistence
- ✅ Offline-first client support (health endpoint, staff cache, idempotent creation, client-generated UUIDs)
- ✅ AI invoice scanning via Anthropic Claude
- ✅ PDF supplier orders via Apache PDFBox
- ✅ Statistical reports (daily, monthly, per-staff)
- ✅ Flexible stock tracking with automatic deduction

Deferred to future work:

- JWT authentication with proper role-based `@PreAuthorize` guards
- Auto no-show scheduler (cron job to mark stale reservations)
- WebSocket push notifications for new reservation requests
- Fiscal integration (ATK Kosovo)
- Recipe-based stock deduction for KG products

---

## License

Private project — part of the diploma thesis **"Design and Implementation of an Offline-First POS System for Restaurants in Kosovo"** at the University of Hildesheim.

Author: **Brikend Gjyliqi**
