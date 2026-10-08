# Cycle Tracker

A private, self-hosted period and cycle tracker built with **Java 21 + Spring Boot**.
Log periods, symptoms and mood, see a calendar with predicted dates, and get
insights into cycle patterns — with the user's data kept private by design.

> 🚧 **Work in progress** — being built over two weeks. See the
> [issues](../../issues) for the day-by-day plan.

> ⚠️ Predictions are estimates based on past cycles. They are **not medical
> advice** and **must not be relied on for contraception**.

## Planned features
- **Logging** — period start/end, flow level, symptoms and mood
- **Calendar** — past periods, predicted next period and estimated fertile window
- **Predictions** — next period shown as a date range, not a single day
- **Prediction comparison** — average vs. median vs. weighted vs. exponential
  smoothing, evaluated by error in days on past cycles; the best one is used
- **Insights** — cycle-length trend chart, averages, symptom patterns
- **Reminders** — email two days before the predicted start
- **Health nudges** — suggests seeing a doctor if cycles are often < 21 or > 35 days
- **Installable on phone** (PWA)

## Privacy by design
- Login required; passwords hashed with BCrypt; users only ever see their own data
- One-click **export** and **delete all my data**
- Real data never goes in this repository — the public demo uses generated sample data only
- Database credentials come from environment variables, never from code

## Tech stack
Java 21 · Spring Boot · Spring Data JPA · Spring Security · Thymeleaf ·
PostgreSQL (production) / H2 (local) · Chart.js · JUnit · GitHub Actions

## Run locally
Requires JDK 21.
```bash
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```
Then open http://localhost:8080. Local data is stored in `./data/` (git-ignored).

Run the tests:
```bash
./mvnw test
```

## Project structure
```
src/main/java/com/lakshya/cycletracker/
  CycleTrackerApplication.java   entry point
  web/                           controllers (HTTP pages and forms)
src/main/resources/
  templates/                     Thymeleaf HTML pages
  application.properties         local config (H2)
  application-prod.properties    production config (PostgreSQL via env vars)
```
