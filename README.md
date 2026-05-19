# 🟥 Learn Spring Redis

A hands-on learning project for integrating **Redis** with **Spring Boot** using Spring Data Redis, Spring Web MVC, and Spring Actuator.

![Java](https://img.shields.io/badge/Java-21-007396?logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-6DB33F?logo=springboot&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-latest-DC382D?logo=redis&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-Wrapper-C71A36?logo=apachemaven&logoColor=white)
![Lombok](https://img.shields.io/badge/Lombok-enabled-pink)

---

## Table of Contents

- [About](#about)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [Redis Configuration](#redis-configuration)
- [What You Will Learn](#what-you-will-learn)
- [API Endpoints](#api-endpoints)
- [Running Tests](#running-tests)
- [Docker Setup](#docker-setup)
- [References](#references)

---

## About

This project is a learning repository that demonstrates how to integrate **Redis** into a **Spring Boot** application. It covers the essential patterns for working with Redis as a data store and cache layer, monitored through Spring Actuator.

**Learning goals:**
- Connect Spring Boot to Redis using Spring Data Redis
- Perform read and write operations on Redis
- Use Redis as a caching layer for REST APIs
- Monitor application and Redis health via Spring Actuator

---

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Primary language |
| Spring Boot | 4.0.6 | Application framework |
| Spring Data Redis | via Boot | Redis data access abstraction |
| Spring Web MVC | via Boot | REST API layer |
| Spring Actuator | via Boot | Monitoring & health checks |
| Lombok | latest | Boilerplate reduction |
| Maven Wrapper | — | Build tool (no local Maven required) |

---

## Prerequisites

Make sure the following are installed on your machine:

- **JDK 21+**
  ```bash
  java -version
  ```

- **Redis Server** running on port `6379` (default)

  Run via Docker (recommended):
  ```bash
  docker run -d --name redis -p 6379:6379 redis:latest
  ```

  Or install directly: [https://redis.io/docs/getting-started/](https://redis.io/docs/getting-started/)

- **Git**
  ```bash
  git --version
  ```

---

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/programmerShinobi/learn-spring-redis.git
cd learn-spring-redis
```

### 2. Configure Redis Connection

Edit `src/main/resources/application.properties`:

```properties
# Server
server.port=8080

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379
# spring.data.redis.password=your_password
spring.data.redis.timeout=60000

# Actuator
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=always
```

Or if you prefer `application.yml`:

```yaml
server:
  port: 8080

spring:
  data:
    redis:
      host: localhost
      port: 6379
      # password: your_password
      timeout: 60000

management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics
  endpoint:
    health:
      show-details: always
```

### 3. Run the Application

**Linux / macOS:**
```bash
./mvnw spring-boot:run
```

**Windows:**
```cmd
mvnw.cmd spring-boot:run
```

**Or build and run as a JAR:**
```bash
./mvnw clean package -DskipTests
java -jar target/belajar-spring-redis-0.0.1-SNAPSHOT.jar
```

The application will start at: **http://localhost:8080**

---

## Project Structure

```
learn-spring-redis/
├── .mvn/
│   └── wrapper/
│       └── maven-wrapper.properties
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── programmer/shinobi/
│   │   │       ├── BelajarSpringRedisApplication.java   # Main entry point
│   │   │       ├── config/
│   │   │       │   └── RedisConfig.java                 # RedisTemplate configuration
│   │   │       ├── controller/
│   │   │       │   └── RedisController.java             # REST controllers
│   │   │       ├── service/
│   │   │       │   └── RedisService.java                # Business logic
│   │   │       └── model/
│   │   │           └── ...                              # Data models
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/
│           └── programmer/shinobi/
│               └── BelajarSpringRedisApplicationTests.java
├── .gitattributes
├── .gitignore
├── mvnw
├── mvnw.cmd
└── pom.xml
```

---

## Redis Configuration

This project uses **Spring Data Redis** with the **Lettuce** client (default). Below is a typical `RedisTemplate` setup:

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Serialize keys as plain strings
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Serialize values as JSON
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }
}
```

---

## What You Will Learn

### Basic CRUD with RedisTemplate

```java
// Store a value
redisTemplate.opsForValue().set("key", "value");

// Retrieve a value
String value = (String) redisTemplate.opsForValue().get("key");

// Store with expiration (TTL)
redisTemplate.opsForValue().set("key", "value", Duration.ofMinutes(10));

// Delete a key
redisTemplate.delete("key");
```

### Redis Data Structures

| Structure | Operation |
|---|---|
| String | `opsForValue()` |
| Hash | `opsForHash()` |
| List | `opsForList()` |
| Set | `opsForSet()` |
| Sorted Set | `opsForZSet()` |

### Caching with Spring Cache + Redis

```java
@Service
public class ProductService {

    @Cacheable(value = "products", key = "#id")
    public Product findById(Long id) {
        // Called only when the data is not in cache
        return productRepository.findById(id).orElseThrow();
    }

    @CachePut(value = "products", key = "#product.id")
    public Product update(Product product) {
        return productRepository.save(product);
    }

    @CacheEvict(value = "products", key = "#id")
    public void deleteById(Long id) {
        productRepository.deleteById(id);
    }
}
```

### Health Monitoring with Spring Actuator

```
GET http://localhost:8080/actuator/health
```

Response example:
```json
{
  "status": "UP",
  "components": {
    "redis": {
      "status": "UP",
      "details": {
        "version": "7.x.x"
      }
    }
  }
}
```

---

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/actuator/health` | Application & Redis health status |
| `GET` | `/actuator/info` | Application info |
| `GET` | `/actuator/metrics` | Application metrics |
| `GET` | `/api/redis/{key}` | Get a value from Redis by key |
| `POST` | `/api/redis` | Store a value in Redis |
| `DELETE` | `/api/redis/{key}` | Delete a key from Redis |

> Endpoints may vary depending on the actual implementation in the source code.

---

## Running Tests

```bash
# Run all tests
./mvnw test

# Run with full build and report
./mvnw verify
```

> Ensure Redis is running before executing integration tests, as they require an active Redis connection.

---

## Docker Setup

Run both Redis and the application using Docker Compose. Create a `docker-compose.yml` file:

```yaml
version: '3.8'

services:
  redis:
    image: redis:latest
    container_name: redis-server
    ports:
      - "6379:6379"
    restart: unless-stopped

  app:
    build: .
    container_name: belajar-spring-redis
    ports:
      - "8080:8080"
    environment:
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PORT: 6379
    depends_on:
      - redis
    restart: unless-stopped
```

Start all services:
```bash
docker-compose up -d
```

Stop all services:
```bash
docker-compose down
```

---

## References

- [Spring Data Redis Documentation](https://docs.spring.io/spring-data/redis/docs/current/reference/html/)
- [Spring Boot Reference Guide](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Redis Official Documentation](https://redis.io/docs/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Project Lombok](https://projectlombok.org/)

---

## Author

**programmerShinobi** — [@programmerShinobi](https://github.com/programmerShinobi)

---

> Built with ❤️ using Spring Boot & Redis