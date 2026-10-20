> **Production:** [Vercel frontend + Docker backend + Cloudflare Tunnel](DEPLOYMENT.md). Các hướng dẫn EC2/Watchtower cũ bên dưới không còn áp dụng.

# Internal Room Booking System

A robust, enterprise-grade Room Booking System built with Java and Spring Boot. This system powers the internal room booking logic for students and administrators, featuring strict concurrency controls, real-time notifications, and robust scheduling workflows.

## 🚀 Tech Stack

- **Backend Framework**: Spring Boot
- **Language**: Java 25
- **Database**: PostgreSQL
- **Migrations**: Flyway
- **Messaging / WebSockets**: RabbitMQ (Message Broker) + Spring WebSockets
- **Caching**: Redis
- **Authentication**: Spring Security (OAuth2 / JWT)
- **Emails**: Mailjet API
- **Deployment**: Docker, Docker Compose, Nginx (Reverse Proxy)

## ✨ Core Features

- **Multi-slot Booking**: Book multiple consecutive time slots in a single transaction.
- **Strict Availability Grid**: Real-time slot statuses (`AVAILABLE`, `PENDING`, `OCCUPIED`, `IN_USE`).
- **Atomic Operations**: Database-level locks and atomic updates prevent race conditions and double-booking.
- **Timezone Safety**: The entire system strictly operates on System UTC internally to handle server offsets, adapting to GMT+7 (Vietnam Time) natively at the presentation layer.
- **Auto-Scheduler**: Automated background jobs (`ShedLock` protected) to automatically cancel or reject bookings when users fail to check in or administrators do not approve them in time.
- **Real-time Notifications**: Triggered by booking status changes and sent directly to the UI via WebSockets.
- **i18n Translation**: Multi-language support (English / Vietnamese) stored dynamically and loaded per user preference.

## 📦 Local Development

This project utilizes `Makefile` to simplify Docker workflows.

1.  **Start Infrastructure Only (Recommended for IDE Development)**
    Start PostgreSQL, Redis, RabbitMQ, and MailDev locally.
    ```bash
    make infra
    ```
    Then, run the actual Spring Boot Application from IntelliJ IDEA with the `dev` active profile!

2.  **Start Full Docker Dev Stack**
    ```bash
    make dev
    ```

3.  **Stop Dev Stack**
    ```bash
    make dev-down
    ```

### Services Available on Dev:
- Application: `http://localhost:8080`
- MailDev (Local SMTP UI): `http://localhost:1080`
- RabbitMQ UI: `http://localhost:15672` (guest / guest)
- Postgres: `localhost:5432`
- Redis: `localhost:6379`

## 🛳 Production Deployment (AWS EC2)

The CI/CD pipeline is handled automatically via GitHub Actions upon pushing to the `main` branch. 

Production overrides are stored securely in `.env.prod`. If you need to manually intervene on the VPS:

- **Pull latest container images:** `make prod-pull`
- **Restart production application:** `make prod-restart`
- **Follow logs:** `make prod-logs`
- **Take down production:** `make prod-down`

*Note: All HTTP traffic on production is masked by the Nginx reverse proxy which handles SSL termination via Let's Encrypt.*
