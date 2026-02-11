# Claude Instructions – Breathe & Go

You are Claude Code working in this repository.

This is a **full-stack portfolio project** called **“Breathe & Go”**: a Weather + AQI Daily Decision App that helps users decide when it is best to go outside based on real-world data.

Your goal is to build, improve, and test this application autonomously while maintaining high engineering and product quality.

---

## 1. Project Goals

- Build a **production-quality full-stack app**
- Use **real-world data** (weather + air quality)
- Keep logic **simple, explainable, and user-centric**
- Prioritize **clarity over cleverness**
- Treat this as a portfolio project suitable for senior-level interviews

---

## 2. Architecture Overview

**Monorepo structure**
- `/api` → Spring Boot 3 (Java 17, Maven)
- `/web` → Next.js (TypeScript, Tailwind)
- `/infra` → Docker Compose (Postgres)

**High-level flow**
- Frontend calls backend only
- Backend fetches + caches external data
- External APIs are never called directly from the frontend

---

## 3. Core Features (MVP)

### Dashboard
- Selected location
- Today + next 3 days
- “Go Outside Score” (0–100)
- Recommendation text:
  - Great
  - Okay (with caution)
  - Avoid
- Human-readable explanation of contributing factors

### Trends
- AQI trend (14–30 days)
- Temperature trend
- Simple, readable charts

### Locations
- Add / remove saved locations
- Store lat/lon + timezone
- Support guest mode or authenticated users

---

## 4. External Data Sources

### Weather
- Provider: **Open-Meteo**
- No API key required
- Data used:
  - Min/Max temperature
  - Precipitation
  - Wind speed
  - UV index

### Air Quality
- Provider: **OpenAQ** (or equivalent)
- Data used:
  - AQI
  - PM2.5
  - Ozone (O3)

**Important:**  
Implement AQI access behind an interface so providers can be swapped later.

---

## 5. Scoring Algorithm

The score must be:
- Deterministic
- Explainable
- Easy to adjust

### Weighting
- AQI: 0–60 points
- Precipitation: 0–15 points
- Temperature comfort: 0–15 points
- Wind comfort: 0–10 points

### Score Bands
- 80–100 → Great
- 60–79 → Okay
- 40–59 → Caution
- 0–39 → Avoid

Each score must return:
- Numeric score
- Recommendation
- List of textual reasons (e.g. “AQI elevated”, “Heavy rain expected”)

---

## 6. Backend Guidelines (Spring Boot)

- Java 17
- Maven
- Postgres with Flyway migrations
- RESTful APIs
- JWT-based authentication (if auth is enabled)
- Cache daily metrics in DB
- Refresh cached data if older than 6 hours
- Use `@ControllerAdvice` for error handling
- Validate all inputs

### Entities
- User
- Location
- DailyMetrics

---

## 7. Frontend Guidelines (Next.js)

- TypeScript
- Tailwind CSS
- Minimal, clean UI
- Mobile-friendly layouts
- Clear hierarchy (score → explanation → data)
- Charts should favor readability over density

---

## 8. Development Rules

- Work in **small, logical commits**
- Do not introduce unused dependencies
- Add tests where logic exists (especially scoring)
- Do not over-engineer
- Avoid premature optimization
- Prefer clarity to abstraction

---

## 9. How to Work

When making changes:
1. Explain what you plan to do
2. Implement incrementally
3. Run or update tests
4. Keep the project runnable at all times

If a requirement is ambiguous:
- Ask clarifying questions before proceeding

---

## 10. What Not to Do

- Do not scrape unofficial APIs
- Do not add financial/medical advice language
- Do not hardcode API responses
- Do not tightly couple frontend to external APIs

---

## 11. Success Criteria

This project is successful if:
- It can be demoed live with real data
- The reasoning behind the score is understandable
- The codebase is clean and navigable
- A reviewer can understand the system in under 10 minutes

---

## 12. Tone & Quality Bar

Assume:
- This project will be reviewed by senior engineers
- Tradeoffs should be intentional and documented
- Simplicity is a feature

Build like you care about the user.