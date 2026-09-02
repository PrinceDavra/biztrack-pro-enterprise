# BizTrack Pro

A multi-tenant **Business Intelligence Dashboard for Indian D2C e-commerce brands**. Import your Shopify orders, Meta ad spend, shipping costs and product COGS; get KPIs, city analytics, a rule-based advisor, a monthly P&L, and one-click CA-ready CSV exports.

Built as a layered Jakarta enterprise app: **JAX-RS (Jersey) → Service beans (CDI) → JPA repositories → Hibernate/MySQL**, with a single-file vanilla-JS frontend (Chart.js + PapaParse).

---

## Architecture

```
Controller (JAX-RS Resource)   com.biztrackpro.resource
      │
Service (CDI bean)             com.biztrackpro.service     ← all business logic lives here
      │
Repository (JPA EntityManager) com.biztrackpro.repository
      │
Entity (JPA @Entity)           com.biztrackpro.entity
```

Controllers only marshal HTTP ↔ DTOs and enforce auth; repositories only run JPQL. Every calculation is in the service layer, and **every monetary value is a `BigDecimal`** (scale 2 for money, 4 for ratios), never `double`/`float`.

## Two deliberate, documented deviations from the brief

The brief mixes a few mutually-incompatible versions. To produce something that actually **compiles and runs**, two infrastructure choices were made — **no feature, calculation, endpoint, import rule or UI behaviour was changed**:

1. **Namespace is `jakarta.*` (Jakarta EE 10 API), not "Jakarta EE 8".**
   Jersey 3.x, Hibernate 6.x and Tomcat 10 — all named in the brief — use the `jakarta.*` namespace and refuse to run under the `javax.*` `jakartaee-api 8.0.0`. The only internally consistent choice (and the one matching 3 of the named versions) is `jakarta.platform:jakarta.jakartaee-api:10.0.0` (`provided`). Source annotations (`@Path`, `@Inject`, `@Entity`, `@Produces`…) are otherwise identical.

2. **Service beans are CDI `@ApplicationScoped` (not EJB `@Stateless`), with JPA `RESOURCE_LOCAL` transactions.**
   Plain **Apache Tomcat 10** is a servlet container with **no EJB container**, so `@Stateless` session beans cannot run there. The layered "controller → service → repository → entity" design is preserved exactly; services are simply CDI beans, and writes are wrapped in a tiny `Tx` helper (`util/Tx.java`) around `EntityManager` transactions. Bean wiring still uses `@Inject`.
   Because plain Tomcat also has no CDI, the POM includes **Weld** (`weld-servlet-shaded`) so `@Inject` works on Tomcat 10 out of the box. Remove that one dependency if you deploy on a full Jakarta EE server or **Apache TomEE 10** (which already provides CDI).

Everything else follows the brief to the letter.

---

## Tech stack

| Concern        | Choice                                                        |
|----------------|--------------------------------------------------------------|
| Language/build | Java 17, Maven 3.9 (WAR)                                      |
| Web / REST     | JAX-RS via **Jersey 3.1**                                     |
| DI / logic     | **CDI 4.0 (Weld)** service beans                              |
| ORM            | **JPA 3.1 / Hibernate 6.4**, HikariCP pool                    |
| Database       | **MySQL 8.0**                                                 |
| CSV            | Apache Commons CSV 1.10                                       |
| Auth           | **JWT** (Auth0 `java-jwt` 4.4) + **BCrypt** (`jBCrypt` 0.4)   |
| JSON           | Jackson 2.16 (`BigDecimal` as plain, `java.time` as ISO)     |
| Server         | Apache Tomcat 10 (or TomEE 10)                                |
| Frontend       | HTML5 + CSS3 + vanilla JS, Chart.js 4.4, PapaParse 5.4        |

---

## Getting started

### 1. Database
Either let Hibernate build the schema automatically (default: `hibernate.hbm2ddl.auto=update`), or run the DDL yourself:

```bash
mysql -u root -p < db/schema.sql
```

`db/schema.sql` also seeds the `system_properties` table with the AI-advisor thresholds.

### 2. Configuration (environment variables)
`EntityManagerProducer` and `JwtUtil` read these at startup; sensible dev defaults exist for everything except production secrets.

| Variable          | Default (dev)                                              | Purpose                        |
|-------------------|-----------------------------------------------------------|--------------------------------|
| `DB_URL`          | `jdbc:mysql://localhost:3306/biztrackpro?...`             | JDBC URL                       |
| `DB_USER`         | `biztrack`                                                | DB user                        |
| `DB_PASSWORD`     | `biztrack`                                                | DB password                    |
| `HIBERNATE_DDL_AUTO` | `update`                                              | `update` / `validate` / `none` |
| `JWT_SECRET`      | dev fallback (⚠ **set this in production**)               | HMAC-256 signing key           |

### 3. Build

```bash
mvn clean package
# → target/biztrack-pro.war
```

### 4. Deploy

**Option A — Apache Tomcat 10** (matches the brief; CDI supplied by the bundled Weld)
```bash
cp target/biztrack-pro.war $CATALINA_HOME/webapps/ROOT.war   # ROOT.war = served at /
$CATALINA_HOME/bin/startup.sh
```

**Option B — Apache TomEE 10** (drop-in Tomcat superset with CDI/JPA built in)
Remove the `weld-servlet-shaded` dependency from `pom.xml`, rebuild, then copy the WAR into TomEE's `webapps/`.

### 5. Use it
Open **http://localhost:8080/**, register a tenant, then import the CSVs in `sample-data/`:

- `sample-data/shopify_orders_sample.csv` → Shopify Import
- `sample-data/meta_ads_sample.csv` → Meta Ads Import
- `sample-data/shipping_recharge_sample.csv` → Expenses (shipping import)
- `sample-data/product_costs_sample.csv` → Product Costs (auto-applies COGS)

The sample SKUs/product names line up across files so COGS matching and profit populate immediately.

---

## Project layout

```
biztrack-pro/
├── pom.xml
├── db/schema.sql                     # MySQL DDL + advisor threshold seed
├── sample-data/*.csv                 # ready-to-import demo data
└── src/main/
    ├── java/com/biztrackpro/
    │   ├── config/        JaxRsApplication, EntityManagerProducer, mappers, CORS, Jackson
    │   ├── entity/        Tenant, Order, Expense, AdCampaign, ProductCost, BusinessProfile, SystemProperty
    │   ├── repository/    one JPA repo per entity (JPQL only)
    │   ├── service/       Auth, Import, Financial, Analysis, Export, Sales, Expense, Ad, Cost, Profile
    │   ├── resource/      Auth, Import, Dashboard, Sales, Expense, Ads, Costs, Analytics, Advisor, Export, Profile
    │   ├── dto/           Kpi, Monthly, MonthlyPnl, City, Breakdown, ImportResult, AnalysisResult, Page, Profile, Requests, ApiResponse
    │   ├── filter/        JwtAuthFilter (skips /api/auth/*)
    │   ├── security/      JwtUtil, AuthPrincipal
    │   └── util/          CsvParserUtil, DateUtil, BigDecimalUtil, Display, Tx
    └── webapp/
        ├── index.html                # entire single-page frontend
        ├── WEB-INF/web.xml, beans.xml
        └── (META-INF/persistence.xml under resources/)
```

## API

All responses use the envelope `{ "status": "success|error", "data": …, "message": "" }`. Every endpoint except `/api/auth/*` requires `Authorization: Bearer <jwt>`.

Auth, Import (`shopify|meta|shipping|costs`), Dashboard (`kpis|monthly|expenses/breakdown`), `sales`, `expenses`, `ads`, `costs` (+`/apply`), Analytics (`cities?sort=…|pnl`), `advisor/analyse`, Export (`pnl|sales|expenses|ads|full|summary|backup`), and `profile` — exactly as listed in the specification.

## Security notes
- JWT (HMAC-256, 7-day expiry) enforced by `JwtAuthFilter`; passwords hashed with BCrypt (cost 12).
- Every tenant query is scoped by `tenant_id`; a tenant can only read/delete its own rows.
- CSV uploads are capped at 10MB and checked for a `.csv`/CSV media type before parsing.
- All DB access is JPQL (parameterised) — no string-concatenated SQL.
- Monetary values and personal identifiers are never logged.

## Financial model (as specified)
```
Gross Profit  = Revenue − (COGS × Qty)
Net Profit    = Revenue − (COGS × Qty) − Operating Expenses − Ad Spend − Refunds
Total Expenses= COGS + Operating Expenses + Ad Spend        (ad spend always included)
ROAS = AdRevenue / AdSpend    CPC = Spend / Clicks    CPL = Spend / Conversions
AOV  = Revenue / Orders       Margins = (Profit / Revenue) × 100
```
In the monthly P&L, refunds are folded into "Other Operating Expenses" so each month's Net Profit equals the master formula above.
