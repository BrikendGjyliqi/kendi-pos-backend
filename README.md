# Kendi POS — Backend

Full-stack Point-of-Sale system built for cafés and restaurants in Kosovo, with **AI-powered invoice scanning** and **automated stock management**.

## What it does

Kendi POS is a production-grade POS system that handles the complete restaurant workflow: taking orders, processing payments, managing stock, tracking tips, and automating supplier deliveries with computer vision.

The standout feature is an **AI-powered invoice scanner**: managers upload a supplier's delivery PDF, Claude Vision extracts every product with quantities and prices, matches them to the database with fuzzy matching, and updates stock automatically. What used to take 30+ minutes of manual data entry now takes under 2 minutes.

## Tech stack

- **Java 21** + **Spring Boot 3.4**
- **PostgreSQL 16** (Docker)
- **Spring Data JPA** + Hibernate
- **Spring Security** with BCrypt PIN authentication
- **Anthropic Claude API** (Vision) — for invoice extraction
- **Apache PDFBox** — for PDF generation (supplier orders)
- **RESTful API** (~40 endpoints)

## Key features

### Point-of-Sale
- Multi-table order management with per-table subtotals
- Cash & card payments with fiscal receipts
- Tip tracking (% or custom amount) with proportional distribution across split orders
- Real-time stock warnings — products with low stock are visually flagged; out-of-stock items are disabled to prevent overselling
- Multi-staff support with per-staff sales reports

### Stock management
- Track any product with unit-based inventory (`PIECE`, `KG`)
- Auto-deduction on sale (configurable per product)
- Low-stock threshold with automatic alerts
- Manual stock adjustments with audit trail

### Supplier management
- Full CRUD for suppliers with contact info and notes
- Delivery registration — manual entry or AI-scanned
- **AI Invoice Scan**: upload a supplier's delivery PDF/image → Claude Vision extracts products, quantities, prices → fuzzy-matched to database → stock updated automatically
- Delivery history with search, filters, and stored original PDF for audit
- **Purchase Orders**: create orders for suppliers with auto-suggested quantities based on stock levels; generates professional PDF ready to send via email/WhatsApp

### Analytics
- Z-Reports (daily sales, cash vs card, top products, tip totals)
- Per-staff reports (sales, tips, product breakdown)
- Order history with fiscal traceability

## Architecture

Domain-driven package structure:

```
com.kendi.pos/
├── ai/          # Anthropic Claude client + invoice scanner
├── auth/        # PIN-based authentication with BCrypt
├── category/    # Menu categories
├── product/     # Products + stock tracking
├── order/       # POS orders (open/closed/paid/cancelled)
├── restotable/  # Restaurant tables
├── staff/       # Staff members and roles
├── supplier/    # Suppliers + purchase orders
├── delivery/    # Incoming deliveries + file storage
├── report/      # Z-reports and staff reports
└── config/      # Security config
```

## Setup

### Prerequisites
- Java 21
- Docker (for PostgreSQL)
- Anthropic API key ([console.anthropic.com](https://console.anthropic.com))

### 1. Start PostgreSQL
```bash
docker run -d --name kendi-db \
  -e POSTGRES_PASSWORD=kendi123 \
  -e POSTGRES_DB=kendi_pos \
  -p 5432:5432 postgres:16
```

### 2. Configure secrets
Create `src/main/resources/application-local.properties`:
```properties
anthropic.api.key=sk-ant-your-key-here
```

This file is gitignored — never committed.

### 3. Run
```bash
./mvnw spring-boot:run
```

Server starts on `http://localhost:8080`.

## Notable technical decisions

**AI invoice extraction with structured output.** The prompt sent to Claude Vision includes the current product catalog and supplier list, asking the model to return a strictly-typed JSON with product IDs pre-matched via fuzzy matching. Confidence scores (`high`/`medium`/`low`/`none`) let the frontend highlight uncertain matches for human review — full AI, but human-in-the-loop.

**Money in cents.** All monetary values stored as `int` (cents), never `float`. Prevents floating-point rounding errors that plague many financial systems.

**Snapshot fields on orders.** `OrderItem` stores `productName` and `price` at the time of order, so historical reports remain accurate even if products are renamed or repriced later.

**PDF storage in DB.** Delivery invoices are stored as `bytea` in PostgreSQL alongside the delivery record. Trade-off: simpler backup/restore, no external file storage to manage, at the cost of DB size (acceptable given expected volume of 10-30 deliveries/month per venue).

**Transactional stock updates.** All stock mutations (sale, delivery, manual adjustment) run inside `@Transactional` boundaries with pessimistic ordering to prevent race conditions.

## Frontend

The frontend (Vue 3 + TypeScript) lives in a separate repository: [kendi-pos-frontend](https://github.com/BrikendGjyliqi/kendi-pos-frontend)

## Author

Built by **Brikend Gjyliqi** — Computer Science student at University of Hildesheim, Germany.
