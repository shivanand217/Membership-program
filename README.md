# FirstClub Membership Program

A Spring Boot backend for a subscription membership program. Users pick a **plan** (Monthly, Quarterly,
Yearly), subscribe to a **tier** (Silver, Gold, Platinum), and move up tiers as they shop. Higher tiers
unlock configurable benefits (free delivery, extra discounts, priority support, and so on).

It runs entirely in-memory, so there is nothing to install or configure — clone, run one command, and open
Swagger.

---

## Prerequisites

- **JDK 21 or newer** and **Maven 3.9+**.
- Check what you have:
  ```bash
  java -version
  mvn -version
  ```

> This was developed and tested on **JDK 25**. The code is compiled at Java 21 language level, so any JDK
> from 21 up works. The Maven config already passes the one flag newer JDKs need for the bytecode library
> (`-Dnet.bytebuddy.experimental=true`), so `mvn spring-boot:run` and `mvn test` work without extra setup.

---

## Run the application

From the project root:

```bash
mvn spring-boot:run
```

That's it. The app starts on **http://localhost:8080**, builds the database schema in memory, and loads a
small demo dataset (plans, tiers, benefits, and four sample users) on startup. You should see a log line
like `Seeded plans, tiers, benefits, cohorts and 4 demo users (ids 1-4)`.

Stop it with `Ctrl+C`.

Alternatively, build a jar and run that:
```bash
mvn clean package
java -jar target/membership-0.0.1-SNAPSHOT.jar
```

---

## See the APIs (Swagger UI)

Once the app is running, open:

- **Swagger UI:** http://localhost:8080/swagger-ui/index.html

This is the easiest way to explore and try the endpoints — every operation has a "Try it out" button, so
you can subscribe, upgrade, preview benefits, etc. straight from the browser without writing any curl.

The raw OpenAPI spec is at http://localhost:8080/v3/api-docs if you want to import it into Postman.

Health check: http://localhost:8080/actuator/health

### The endpoints at a glance

Base path is `/api/v1`.

| Method & path | What it does |
|---|---|
| `GET /plans` | List the available plans and prices |
| `GET /tiers` | List tiers with their benefits and the criteria to earn them |
| `GET /tiers/{code}/benefits` | Benefits of one tier (`code` = SILVER / GOLD / PLATINUM) |
| `POST /subscriptions` | Subscribe a user to a plan + tier |
| `POST /subscriptions/{id}/upgrade` | Upgrade tier (takes effect immediately, prorated) |
| `POST /subscriptions/{id}/downgrade` | Downgrade tier (takes effect at period end) |
| `POST /subscriptions/{id}/cancel` | Cancel (stays active until the period ends) |
| `GET /subscriptions/{id}` | A subscription with its full history and charges |
| `GET /users/{id}/membership` | A user's current membership, tier, and expiry |
| `GET /users/{id}/tier-eligibility` | Which tiers a user qualifies for, and why |
| `POST /users/{id}/tier/evaluate` | Recompute the user's earned tier now |
| `POST /demo/orders` | Record an order for a user (drives tier progression) |
| `POST /demo/benefits/preview` | Apply a user's benefits to a sample cart |

---

## See the data (H2 database console)

The app uses an **H2 in-memory database**. Nothing is installed and nothing is written to disk — the data
lives only while the app is running and is rebuilt fresh (with the seed data) on every restart.

To browse the tables:

1. Open **http://localhost:8080/h2-console**
2. Enter these connection settings (they match `src/main/resources/application.yml`):
   - **JDBC URL:** `jdbc:h2:mem:membership`
   - **User Name:** `sa`
   - **Password:** *(leave blank)*
3. Click **Connect**.

   Note: the H2 console remembers the last URL you used on your machine (in `~/.h2.server.properties`), so
   if the field shows a different value, set it to `jdbc:h2:mem:membership` once and it will be remembered.

You can then run SQL against the live data, for example:
```sql
SELECT * FROM SUBSCRIPTION;
SELECT * FROM TIER;
SELECT * FROM TIER_BENEFIT;         -- benefit config (JSON params per tier)
SELECT * FROM SUBSCRIPTION_EVENT;   -- audit trail of every lifecycle change
SELECT * FROM BILLING_TRANSACTION;  -- the charges ledger
SELECT * FROM USER_ORDER_STATS;     -- order counts/value used for tier progression
```

---

## Seed data

Loaded automatically on startup:

- **Plans:** Monthly (199), Quarterly (499), Yearly (1499) — prices in INR.
- **Tiers:** Silver (base), Gold, Platinum — each with its own benefits and progression criteria.
- **Four demo users** (ids 1 to 4) set up to show the different ways a tier is earned:

| User id | Setup | Result |
|---|---|---|
| 1 | no activity | stays Silver |
| 2 | 4 orders, ~1800 spent this month | Silver — one more order pushes them to Gold |
| 3 | in the `prime_metro` cohort | earns Platinum by cohort (no orders needed) |
| 4 | 12 orders, 12000 spent this month | earns Platinum by volume |

---

## Try the main flows

The quickest way is Swagger UI. If you prefer the command line, here are the key calls (all against
`http://localhost:8080/api/v1`):

```bash
# Browse the catalog
curl http://localhost:8080/api/v1/plans
curl http://localhost:8080/api/v1/tiers

# Subscribe user 1 to Monthly / Silver
curl -X POST http://localhost:8080/api/v1/subscriptions \
  -H 'Content-Type: application/json' \
  -d '{"userId":1,"planCode":"MONTHLY","tierCode":"SILVER"}'

# Upgrade that subscription (id 1) to Platinum
curl -X POST http://localhost:8080/api/v1/subscriptions/1/upgrade \
  -H 'Content-Type: application/json' \
  -d '{"targetTierCode":"PLATINUM"}'

# Show a user's current membership
curl http://localhost:8080/api/v1/users/1/membership

# See why user 4 qualifies for Platinum
curl http://localhost:8080/api/v1/users/4/tier-eligibility

# Preview the benefits user 4 gets on a sample cart
curl -X POST http://localhost:8080/api/v1/demo/benefits/preview \
  -H 'Content-Type: application/json' \
  -d '{"userId":4,"subtotal":1000,"deliveryFee":50,"category":"grocery"}'
```

An optional `Idempotency-Key` header on `POST /subscriptions` makes retries safe (a repeat with the same
key returns the original subscription instead of erroring).

---

## Run the tests

```bash
mvn test
```

This runs the full suite (unit tests plus integration tests that boot the app and hit the real HTTP
endpoints). It covers pricing and proration, tier progression, benefit stacking, the subscribe/upgrade/
downgrade/cancel lifecycle, the one-active-subscription-per-user rule under concurrent requests, and the
scheduled expiry/renewal job.

To run a single test class:
```bash
mvn test -Dtest=MembershipApiIntegrationTest
```

---

## How it is put together (brief)

The design keeps three ideas separate on purpose:

- **Plan** = how you are billed (cadence + price).
- **Tier** = what benefits you get, and how you earn them.
- **Subscription** = a user's current plan + tier + lifecycle.

A subscription tracks two tiers at once: the one the user **bought** (priced) and the one they've
**earned** through activity (free). The user always gets the better of the two. This is what lets someone
buy Silver but automatically enjoy Platinum benefits after qualifying — without ever being charged more by
a background process.

Benefits are **configuration, not code**: each benefit's settings (discount percentage, caps, categories,
delivery minimums, support SLA) are stored as data per tier, and each benefit type is handled by its own
small strategy class. Adding a new benefit type or a new tier-progression rule means adding one class, not
editing existing ones.

Concurrency is handled with a database constraint for the one-active-subscription rule, optimistic locking
with bounded retries for tier changes, and idempotent handling for subscribe and the renewal job.

Configurable settings (business timezone for the monthly window, currency, discount cap, the renewal job's
schedule) live under `membership.*` in `src/main/resources/application.yml`.

### Where things live

```
src/main/java/com/firstclub/membership
  web/          controllers, request/response DTOs, error handling
  application/  the services that do the work
  domain/       entities, value objects, benefit + tier-rule strategies
  repository/   Spring Data JPA repositories
  scheduler/    the expiry/renewal job
  config/       seed data, startup checks, OpenAPI
```
