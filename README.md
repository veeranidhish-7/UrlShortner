# ⚡️ High-Performance URL Shortener

A highly scalable URL Shortener built with Java 21, Spring Boot 3, PostgreSQL, and Redis. Features an ultra-fast cache-aside redirect pipeline and asynchronous click-stream telemetry.

## 📖 Documentation
- **[Architecture & Spring Boot Guide](docs/ARCHITECTURE.md)**: Understand the internals of how the system works, the technologies used, and how to build it from scratch.
- **[User Guide & Case Study](docs/USER_GUIDE.md)**: A step-by-step walkthrough of how to use the UI and a real-world marketing case study.

## 🚀 Quickstart Guide

### Prerequisites
- Docker & Docker Compose
- Java 21
- Maven

### 1. Start the Infrastructure (Database & Cache)
Navigate to the root directory and spin up PostgreSQL and Redis using Docker:
```bash
docker-compose up -d
```

### 2. Run the Application
You can run the Spring Boot application using Maven:
```bash
./mvnw spring-boot:run
```
*(If you are on Windows, use `mvnw.cmd spring-boot:run`)*

### 3. Access the Dashboard
Once the application starts, open your browser and navigate to:
```
http://localhost:8080
```
Here you will see the minimal Vanilla JS dashboard where you can create short URLs and view the analytics dashboard with Chart.js integration.

### 4. Stop the Infrastructure
When you are done, you can stop the Docker containers:
```bash
docker-compose down
```

## 🏗 System Design Overview
- **Storage**: PostgreSQL (Relational persistence)
- **Caching**: Redis (Sub-millisecond lookup for fast HTTP 302 redirects)
- **ID Generation**: Base62 conversion of PostgreSQL's BigSerial sequence to ensure mathematically collision-free short codes.
- **Analytics**: Spring `@Async` Event Listeners ensure telemetry tracking never blocks the user's redirect request.
- **Frontend**: Clean, dark-mode single-page layout utilizing CSS Grid and Vanilla JavaScript fetch APIs.
