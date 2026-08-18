# 📚 URL Shortener: Comprehensive Architecture & Spring Boot Guide

Welcome to the definitive guide for the URL Shortener backend. This document is designed not just to explain *what* the application does, but exactly *how* it does it using Spring Boot. If you were to start from an empty folder, this document will explain every piece of technology required to rebuild the backend from scratch.

---

## 🛠 1. The Technology Stack Explained

We are using **Java 21** alongside **Spring Boot 3**. Spring Boot is an opinionated framework built on top of the standard Spring framework; it provides auto-configuration, meaning you don't have to write hundreds of lines of XML or Java configuration to start a web server or connect to a database.

### Core Dependencies Used
- **Spring Web (`spring-boot-starter-web`)**: This brings in Apache Tomcat (a lightweight embedded web server) and Spring MVC. It allows us to create REST APIs using annotations like `@RestController` and `@GetMapping`.
- **Spring Data JPA (`spring-boot-starter-data-jpa`)**: This is the Data Access Layer. It uses Hibernate under the hood to map Java Objects to Relational Database Tables (ORM - Object-Relational Mapping). We use this to talk to PostgreSQL without writing raw SQL.
- **Spring Data Redis (`spring-boot-starter-data-redis`)**: Provides the `StringRedisTemplate` class to interact with Redis. Redis is an in-memory key-value store used to cache URL mappings for instant redirects.
- **PostgreSQL Driver (`postgresql`)**: The JDBC driver required for Java to physically communicate with a Postgres database over the network.
- **Spring Validation (`spring-boot-starter-validation`)**: Uses Hibernate Validator to enforce constraints on incoming HTTP requests (e.g., ensuring a string is a valid URL format using `@Pattern` and `@NotBlank`).
- **Lombok (`lombok`)**: A compile-time library that generates boilerplate code (Getters, Setters, Constructors) via annotations like `@Data`, keeping our classes incredibly clean.

---

## 🧩 2. Building the System From Scratch: Layer by Layer

To rebuild this system, you follow a classic "N-Tier" or "Layered" Architecture. The flow of data goes:
`Client -> Controller -> Service -> Repository -> Database`

### Layer 1: The Database Configuration (application.yml)
Spring Boot needs to know where your databases live. By placing an `application.yml` file in `src/main/resources`, Spring Boot's auto-configuration will automatically read it and instantiate a `DataSource` (Postgres connection pool) and a `RedisConnectionFactory`. 
* **Note:** We run Postgres and Redis locally using Docker (`docker-compose.yml`) so we don't have to install them directly on our machines.

### Layer 2: Entities & Repositories (The Data Layer)
You create Java classes annotated with `@Entity` (e.g., `UrlEntity`, `UrlAnalyticsEntity`). Spring Data JPA scans these classes and generates the actual database tables automatically (because we set `ddl-auto: update` in our config). 

Next, you create an interface extending `JpaRepository`.
```java
public interface UrlRepository extends JpaRepository<UrlEntity, Long> {
    Optional<UrlEntity> findByShortCode(String shortCode);
}
```
* **How it works:** Spring Boot dynamically generates the implementation of this interface at runtime. You instantly get methods like `save()`, `findById()`, and `delete()`. If you declare `findByShortCode`, Spring parses the method name and automatically writes the SQL query: `SELECT * FROM urls WHERE short_code = ?`.

### Layer 3: Services (The Business Logic Layer)
This is where the heavy lifting happens. Classes are annotated with `@Service` so Spring creates singletons of them in its Application Context. 

1. **URL Generation (`Base62Service`)**: Instead of guessing random strings, we save a URL to PostgreSQL, get its auto-incremented numeric ID (e.g., `10024`), and convert that base-10 number into a base-62 string (using characters a-z, A-Z, 0-9). This guarantees no collisions.
2. **The Cache-Aside Pipeline (`UrlService`)**: 
   - When a user tries to resolve a short link, `UrlService` queries Redis *first*. 
   - Since Redis holds data in RAM, this takes less than a millisecond. 
   - If Redis doesn't have it (Cache Miss), it fetches from PostgreSQL, puts it into Redis with a 24-hour TTL, and returns it.

### Layer 4: Events & Async Telemetry
We want to track clicks (analytics) when a user visits a link. But writing to the database takes time. If we do this synchronously, the user has to wait for the database write to finish before they get redirected to their destination.

* **The Solution:** Spring's `@Async` and Application Events. 
* In `RedirectController`, we publish a `UrlClickEvent`. 
* A separate class, `UrlClickEventListener`, is annotated with `@EventListener` and `@Async`. 
* **How it works:** Spring Boot intercepts the event and passes it to a separate background thread (configured in our ThreadPoolTaskExecutor in `application.yml`). The background thread does the heavy database insert into `url_analytics`, while the main web thread immediately redirects the user. This ensures blazing fast performance.

### Layer 5: Controllers (The API Layer)
Controllers map HTTP requests to our Java methods. We use two controllers:
1. `UrlController`: Handles JSON APIs (`/api/v1/urls`) for creating links and viewing stats. It consumes and produces JSON.
2. `RedirectController`: Handles the root path (`/{shortCode}`). It does *not* return JSON. Instead, it returns an HTTP `302 FOUND` status code with a `Location: <destination>` header, which forces the user's browser to redirect.

### Layer 6: Exception Handling
Instead of returning ugly Java stack traces to the frontend, we use a class annotated with `@RestControllerAdvice`. This acts as a global net. If a `UrlNotFoundException` is thrown anywhere in the application, this class catches it and transforms it into a clean HTTP 404 JSON response.

---

## 🌐 3. Detailed API Specification

### 1. Create a Short URL
* **Endpoint:** `POST /api/v1/urls`
* **Purpose:** Takes a long URL, generates a Base62 short code, saves it to PostgreSQL, and returns the short link.
* **Request Body:**
```json
{
  "originalUrl": "https://www.google.com/search?q=spring+boot+tutorial"
}
```
* **Validation:** Spring checks if the URL is valid using Regex before it even hits the controller. If invalid, throws 400 Bad Request.
* **Response (201 Created):**
```json
{
  "shortCode": "bT4",
  "shortUrl": "http://localhost:8080/bT4"
}
```

### 2. Redirect to Target URL
* **Endpoint:** `GET /{shortCode}`
* **Purpose:** Intercepts the short link, logs the telemetry data asynchronously, checks Redis for the target, and issues a 302 redirect.
* **Response:** HTTP 302 Redirect. (Browser navigates away).

### 3. Get Analytics
* **Endpoint:** `GET /api/v1/urls/{shortCode}/stats`
* **Purpose:** Aggregates telemetry data for the last 7 days and returns the 10 most recent clicks.
* **Response (200 OK):**
```json
{
  "totalClicks": 1,
  "lastClickedAt": "2023-10-27T10:00:00Z",
  "recentClicks": [
    {
      "clickTime": "2023-10-27T10:00:00Z",
      "browserOs": "Mozilla/5.0...",
      "referer": "https://twitter.com"
    }
  ]
}
```

### 4. Delete URL
* **Endpoint:** `DELETE /api/v1/urls/{shortCode}`
* **Purpose:** Soft-deletes the URL by setting `is_active = false` in the database, and immediately invalidates (deletes) the key from the Redis cache.

---

## 🎨 4. Frontend Integration
The frontend is intentionally minimal, utilizing pure Vanilla JavaScript (`fetch` API), HTML5, and CSS3. 
- It communicates directly with the JSON endpoints described above.
- It uses `Chart.js` loaded via a CDN to visualize the analytics array returned from the `/stats` endpoint. 
- By placing these files in `src/main/resources/static`, Spring Boot automatically serves them as static content on the root URL (`http://localhost:8080/`).

With this architecture, you have a highly scalable, enterprise-grade system capable of handling millions of URL shortens and redirects efficiently using caching and asynchronous threading.
