# Kendi POS — Backend

**Spring Boot REST API** for the Kendi POS restaurant management system.

Built with **Spring Boot 3.5**, **Java 21**, **PostgreSQL 16**, and **Flyway** migrations. Serves as the authoritative source of truth for a Vue 3 + Tauri desktop point-of-sale, and supports an **offline-first client** through dedicated sync endpoints (health check, staff PIN cache, client-generated UUIDs). Ships with an **AI Analytics endpoint** that converts natural-language Albanian business questions into safe PostgreSQL queries and natural-language answers via Claude.

---

## Highlights

- **Offline-first friendly** — dedicated `/api/health` endpoint for the client's sync engine, `/api/staff/cache` exposes BCrypt PIN hashes for offline authentication, and all mutation endpoints accept client-generated UUIDs so the frontend can create objects offline and reference them immediately.
- **AI Analytics endpoint** — `POST /api/ai/analytics` implements a Text-to-SQL pipeline: Claude generates a `SELECT` query from a natural-language question in Albanian, the backend validates and executes it, then Claude formats the result into an Albanian answer. Read-only by construction (destructive keywords blocked).
- **Automatic state synchronization** — creating an order for a reserved table atomically transitions the reservation to `ARRIVED` and the table to `ON_DINE`; paying or cancelling releases the table back to `AVAILABLE`. No orchestration required on the client.
- **Reservation lifecycle** — complete workflow from `PENDING_REQUEST` through `CONFIRMED`, `ARRIVED`, `NO_SHOW`, `DECLINED`, and `CANCELLED`, with automatic table state transitions at each step.
- **Table management with visual layout** — tables carry `positionX`, `positionY`, and `size` fields so admins can drag-and-drop the floor plan in the frontend and it persists atomically.
- **AI-powered invoice scanning** — `POST /api/ai/invoice/scan` sends uploaded PDFs to the Anthropic Claude API and returns extracted supplier, line items, and totals.
- **Flexible stock tracking** — products can track stock in `PIECE` or `KG` units with automatic deduction on payment for eligible items.
- **PDF generation** — supplier purchase orders and reports produced server-side via Apache PDFBox.
- **Flyway migrations** — schema versioning with V2 through V5, applied automatically on startup.
- **Statistical reports** — daily Z-report, monthly report, per-staff analytics with tips.

---

## AI Analytics — Text-to-SQL Pipeline

`POST /api/ai/analytics` implements a three-step pipeline that turns a natural-language business question into a factual, data-grounded answer:

### 1. SQL generation

The service sends the user's question to Claude Sonnet 4.5 along with:

- A description of every table in the schema (columns, types, meanings)
- A set of formatting rules (cents-to-euros conversion, timestamp handling, UUID column types)
- Product-name matching guidance (variants like "Coca Cola 0.33l" vs "Coca Cola 0/0.33l" require `ILIKE '%cola%'`, not exact match)
- Comparison patterns (`UNION ALL` for today-vs-yesterday, `DATE_TRUNC` for weekly grouping)

Claude returns a single `SELECT` (or `WITH`) statement.

### 2. Safety validation and execution

Before the query is executed, `AIAnalyticsService.isSafe()` checks that:

- The statement starts with `SELECT` or `WITH`
- It does **not** contain any of: `DELETE`, `DROP`, `INSERT`, `UPDATE`, `ALTER`, `TRUNCATE`, `GRANT`, `REVOKE`

Only if both checks pass does `JdbcTemplate` execute the query. This turns the endpoint into a strict read-only lens over the database.

### 3. Answer formatting and chart detection

The rows returned by the query are sent back to Claude with instructions to compose an Albanian answer for a café owner — short, concrete, with emoji where helpful. Cents are converted to euros in the prose. Comparisons express both the delta and the percentage change.

In parallel, `detectChartType()` inspects the shape of the result and the phrasing of the question:

- **Bar chart** — for ranking questions ("cili produkt shitet me shume", "top 5 …")
- **Line chart** — for time-series questions ("trend i shitjeve kete jave", "cdo dite …")
- **No chart** — for single-value answers ("sa fitim pata sot?")

The response therefore contains:

```json
{
  "answer": "Sot ke pas €24.00 total shitje me 5 porosi.",
  "sql": "SELECT COALESCE(SUM(total),0)/100.0 ...",
  "data": [ ... ],
  "chartType": "bar",
  "success": true,
  "error": null
}
```

The frontend renders `answer` as chat text and, when `chartType` is present, renders `data` as a Chart.js bar or line chart in the same message bubble.

### Example questions the assistant handles

- "Sa fitim pata sot?"
- "Cili produkt shitet me shume kete jave?"
- "Sa Coca-Cola kane mbet?"
- "Cili banakier ka bo me shume shitje?"
- "Krahaso sot me dje"
- "Sa rezervime kena kete jave?"
- "Cilat tavolina kane sjelle me shume te ardhura?"

---

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker + Docker Compose (for PostgreSQL)
- An Anthropic API key (for AI invoice scanning and analytics)

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

---

## Tech Stack

| Layer               | Technology                                              |
|---------------------|---------------------------------------------------------|
| Framework           | Spring Boot 3.5 (Web, Data JPA, Security, Validation)   |
| Language            | Java 21                                                 |
| Database            | PostgreSQL 16 (Docker)                                  |
| Schema versioning   | Flyway (V2–V5)                                          |
| PDF generation      | Apache PDFBox 2.0.30                                    |
| AI extraction & Q&A | Anthropic Claude Sonnet 4.5                             |
| Password hashing    | Spring Security BCrypt                                  |
| Build tool          | Maven 3.9                                               |

---

## Package Layout

```
com.kendi.pos/
├── ai/           # AI-powered features
│   ├── AIAnalyticsController.java   # POST /api/ai/analytics
│   ├── AIAnalyticsService.java      # Text-to-SQL pipeline via Claude
│   ├── AIAnalyticsDtos.java         # Request/response records
│   └── ...                          # AI invoice scanning
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

---

## API Overview

All endpoints are prefixed with `/api`.

### Sync (offline-first client support)

- `GET /health` — Connectivity check for the client's sync engine
- `GET /staff/cache` — Returns staff records with BCrypt PIN hashes for offline login validation

### AI

- `POST /ai/analytics` — Natural-language business questions (Text-to-SQL via Claude). Returns answer, generated SQL, structured data for charts, and chart type
- `POST /ai/invoice/scan` — Upload PDF invoice, receive extracted supplier and line items via Claude

### Products

- `GET /products` / `POST /products` / `PUT /products/{id}` / `DELETE /products/{id}`
- `PATCH /products/{id}/stock` — Adjust stock quantity

### Orders

- `GET /orders` — List with optional `status` or `tableId` filter
- `POST /orders` — Create; accepts client-generated UUID, idempotent, auto-triggers reservation `ARRIVED`
- `PUT /orders/{id}` — Update items
- `POST /orders/{id}/close` — Close order
- `POST /orders/{id}/pay` — Process payment (deducts stock, releases table)
- `POST /orders/{id}/cancel` — Cancel
- `POST /orders/table/{tableId}/pay-all` — Combined payment for a table with proportional tip distribution

### Tables

- `GET /tables` / `POST /tables` / `PUT /tables/{id}` / `DELETE /tables/{id}`
- `PATCH /tables/{id}/position` — Drag-and-drop coordinates
- `PATCH /tables/{id}/size` — Visual size
- `PATCH /tables/{id}/status` — Manual status update

### Reservations

- `GET /reservations` — List, filterable by `status`
- `GET /reservations/history?status=&from=&to=` — Historical
- `GET /reservations/stats/today` / `/stats/range?from=&to=` — Statistics
- `POST /reservations/requests` — Create request (from waiter)
- `PATCH /reservations/{id}/confirm` / `/decline` / `/arrived` / `/no-show`

### Staff and Auth

- `POST /auth/login` — Login with PIN
- `GET /staff` / `POST /staff` / `PUT /staff/{id}` / `DELETE /staff/{id}`
- `GET /staff/cache` — BCrypt hashes for offline validation

### Reports

- `GET /reports/z-report?date=YYYY-MM-DD` — Daily Z-report
- `GET /reports/staff?staffId=&date=` — Per-staff performance
- `GET /reports/monthly?from=&to=` — Monthly accountant report

### Suppliers and Deliveries

- `GET /suppliers` / `POST /suppliers` / `POST /suppliers/{id}/order`
- `GET /deliveries` / `POST /deliveries`

---

## Business Logic Highlights

### Automatic Table–Order–Reservation Synchronization

The system maintains **automatic consistency** between three related entities so the client never has to orchestrate multi-step state updates:

1. **On order creation** for a table with a confirmed reservation:
   - Reservation status → `ARRIVED`
   - Table status → `ON_DINE`
2. **On order payment or cancellation** (no other open orders):
   - Table status → `AVAILABLE`
3. **On admin reservation confirmation**:
   - Table status → `RESERVED`

### Idempotent Order Creation

`POST /orders` is idempotent: if the client sends the same `id` twice (common when a sync retry hits after an ambiguous timeout), the server returns the existing order instead of creating a duplicate. Essential for a reliable offline-first client.

### Stock Deduction

Only products with `trackStock=true`, `autoDeductOnSale=true`, and `stockUnit=PIECE` are auto-deducted on payment. KG products require manual adjustment.

### Tip Handling

Orders carry `tipAmount` and `tipPercent`. When combining payments for a table (`/pay-all`), the total tip is distributed proportionally across orders based on each order's total, with rounding delta applied to the last order.

---

## Security

Current setup is **development-friendly**:

- CORS allows all origins (for Tauri desktop app + web dev)
- Session management is **stateless**
- All endpoints are **permitAll** for now
- Method-level security (`@EnableMethodSecurity`) is enabled and ready for role-based access
- **AI analytics endpoint is read-only by construction** — the SQL safety guard blocks all destructive statements before execution

**Planned:** JWT authentication with role-based `@PreAuthorize` guards for admin-only operations (add/edit/delete tables, confirm/decline reservations, staff management, AI analytics).

---

## Frontend

The Vue 3 + Tauri desktop frontend lives in a separate repository:
**[kendi-pos-frontend](https://github.com/BrikendGjyliqi/kendi-pos-frontend)**

The frontend uses a local SQLite database as its read source of truth and reconciles with this backend via a 15-second polling sync engine. It expects the backend on `http://localhost:8080`, and continues to operate in offline mode when the backend is unreachable, queuing all mutations for later flush. The Ask AI chat interface communicates exclusively with `POST /api/ai/analytics`.

---

## Roadmap

Completed for the current thesis milestone:

- ✅ Full REST API for menu, tables, orders, payments, tipping
- ✅ Reservation workflow with automatic state transitions
- ✅ Table drag-and-drop layout persistence
- ✅ Offline-first client support (health endpoint, staff cache, idempotent creation, client-generated UUIDs)
- ✅ AI invoice scanning via Anthropic Claude
- ✅ **AI Analytics endpoint — Text-to-SQL business intelligence in Albanian**
- ✅ PDF supplier orders via Apache PDFBox
- ✅ Statistical reports (daily, monthly, per-staff)
- ✅ Flexible stock tracking with automatic deduction

Deferred to future work:

- JWT authentication with role-based guards
- Auto no-show scheduler
- WebSocket push notifications for new reservation requests
- Fiscal integration (ATK Kosovo)
- Proactive AI insights (daily digest, predictive stock reordering)

---

## License

Private project — part of the diploma thesis **"Design and Implementation of an Offline-First POS System for Restaurants in Kosovo"** at the University of Hildesheim.

Author: **Brikend Gjyliqi**
