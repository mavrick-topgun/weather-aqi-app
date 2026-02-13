# Breathe & Go - Capacity & Performance Analysis

## Current Deployment Topology

```
Single Machine (Development)
+--------------------------------------------------+
|  Next.js Dev Server     (port 3000, single proc)  |
|  Spring Boot + Tomcat   (port 8080, single proc)  |
|  PostgreSQL 16          (port 5432, Docker)        |
+--------------------------------------------------+
```

This is a **single-instance, single-machine** setup with no load balancing, no CDN, and no horizontal scaling.

## Traffic Capacity Estimates

### External API Bottleneck (Primary Limiter)

Open-Meteo free tier: **10,000 requests/day** across all endpoints.

Each user action triggers the following API calls:

| User Action              | Open-Meteo API Calls | Notes                             |
|--------------------------|----------------------|-----------------------------------|
| Load dashboard           | 2-4                  | 1 weather + 1 AQI + trend calls   |
| Switch location          | 2-4                  | Same as above                      |
| Open map popup           | 2                    | 1 weather + 1 AQI per popup       |
| Add new location         | 0                    | Only geocoding (separate limit)    |
| Search locations         | 1                    | Geocoding API                      |

**With caching (daily_metrics table):** Trend data is cached after first fetch. Repeated loads of the same location on the same day reuse cached data for historical dates.

**Estimated capacity:**
- Without cache hits: ~10,000 / 3 = **~3,300 dashboard loads/day**
- With typical cache hits: **~5,000-7,000 dashboard loads/day**
- Per concurrent user (assuming 10 loads/session): **~300-500 unique users/day**

### Application Server Capacity

**Spring Boot (Tomcat embedded):**
- Default thread pool: 200 threads
- Each request blocks on Open-Meteo HTTP call (~200-500ms)
- Effective throughput: ~400-1,000 req/sec (for cached/DB-only requests)
- For API-bound requests: ~50-100 req/sec (limited by upstream latency)

**Next.js Dev Server:**
- Single Node.js process
- Handles SSR + static serving + API proxying
- Effective throughput: ~100-500 req/sec depending on page complexity

### Database Capacity

**PostgreSQL 16 (Docker, default config):**
- Connection pool: Spring Boot default (10 connections via HikariCP)
- Query complexity: Simple selects/inserts with indexes
- Estimated throughput: **5,000-10,000 queries/sec** (far from bottleneck)
- Data volume: Minimal (< 100 MB for years of data)

### Summary: Current Throughput

| Layer              | Bottleneck           | Capacity                |
|--------------------|----------------------|-------------------------|
| Open-Meteo API     | 10K req/day limit    | ~300-500 users/day      |
| Spring Boot        | Thread pool + I/O    | ~50-100 req/sec (live)  |
| Next.js            | Single process       | ~100-500 req/sec        |
| PostgreSQL         | Not a bottleneck     | ~5,000-10,000 qps       |
| **Overall**        | **Open-Meteo limit** | **~300-500 users/day**  |

## Latency Profile

| Operation                    | Expected Latency  |
|------------------------------|-------------------|
| Page load (cached)           | 50-150ms          |
| Dashboard (API fetch)        | 300-800ms         |
| Location search              | 200-500ms         |
| Map tile load                | 100-300ms         |
| DB query (simple)            | 1-5ms             |
| Open-Meteo API call          | 150-400ms         |

## Scaling Recommendations

### Short-Term (Support 1,000+ users/day)

1. **Aggressive caching:** Cache forecast responses in-memory (e.g., Caffeine) with 30-min TTL. Most users in the same city get the same data.

2. **Batch forecast fetching:** Pre-fetch forecasts for all saved locations on a cron schedule (every 30 min) instead of on-demand.

3. **Production Next.js build:** `npm run build && npm start` instead of `npm run dev` for 3-5x better throughput.

### Medium-Term (Support 10,000+ users/day)

4. **Open-Meteo paid tier:** Removes rate limits. Or self-host Open-Meteo for unlimited calls.

5. **Redis cache layer:** Share cached data across multiple backend instances.

6. **Connection pooling:** Tune HikariCP pool size (currently default 10).

7. **CDN for frontend:** Deploy Next.js to Vercel or behind Cloudflare for static asset caching.

### Long-Term (Support 100,000+ users/day)

8. **Horizontal scaling:** Multiple Spring Boot instances behind a load balancer.

9. **Database read replicas:** Offload trend/read queries.

10. **API response compression:** Enable gzip in Spring Boot.

11. **WebSocket for live updates:** Push forecast updates instead of polling.

## Resource Requirements

### Current (Development)

| Resource | Requirement     |
|----------|-----------------|
| CPU      | 2 cores         |
| RAM      | 2 GB            |
| Disk     | 1 GB            |
| Network  | Outbound HTTPS  |

### Recommended (Production, 500 users/day)

| Resource | Requirement     |
|----------|-----------------|
| CPU      | 2 cores         |
| RAM      | 4 GB            |
| Disk     | 10 GB SSD       |
| Network  | 100 Mbps        |

### Recommended (Production, 5,000+ users/day)

| Resource | Requirement        |
|----------|--------------------|
| CPU      | 4 cores            |
| RAM      | 8 GB               |
| Disk     | 20 GB SSD          |
| Network  | 500 Mbps           |
| Cache    | Redis (256 MB)     |

## Known Limitations

1. **No rate limiting** on the API - any client can exhaust the Open-Meteo quota.
2. **No authentication** - all endpoints are public.
3. **Synchronous API calls** - WebClient is used but `.block()` is called, losing reactive benefits.
4. **No health check endpoint** - start.sh checks `/actuator/health` but actuator is not in dependencies.
5. **No HTTPS** - runs on plain HTTP in development.
6. **Single timezone handling** - "auto" timezone relies on Open-Meteo detection.
7. **No request deduplication** - Multiple users requesting the same location simultaneously trigger duplicate API calls.
