# ParkEase Backend

This folder contains the Spring Boot microservices for ParkEase.

## Services

| Service | Port | Description |
| --- | ---: | --- |
| `eureka-service` | 8761 | Service discovery |
| `api-gateway` | 8080 | Gateway and route forwarding |
| `auth-service` | 8081 | Authentication, users, JWT |
| `vehicle-service` | 8082 | Vehicle CRUD and lookup |
| `parkinglot-service` | 8083 | Parking lot CRUD, nearby search, manager views |
| `parkingspot-service` | 8084 | Parking spot CRUD, availability, reservation status |
| `booking-service` | 8085 | Booking lifecycle and billing calculation |

## Required Environment Variables

```powershell
$env:DATABASE="jdbc:mysql://localhost:3306/parkease"
$env:USERNAME="your_mysql_username"
$env:PASSWORD="your_mysql_password"
$env:jwt-secret-key="your_jwt_secret"
```

## Startup Order

Run each command in a separate terminal:

```powershell
cd eureka-service
.\mvnw spring-boot:run

cd auth-service
.\mvnw spring-boot:run

cd vehicle-service
.\mvnw spring-boot:run

cd parkinglot-service
.\mvnw spring-boot:run

cd parkingspot-service
.\mvnw spring-boot:run

cd booking-service
.\mvnw spring-boot:run

cd api-gateway
.\mvnw spring-boot:run
```

Eureka dashboard: `http://localhost:8761`

Gateway base URL: `http://localhost:8080/api`

## Testing

Run tests for any service from that service directory:

```powershell
.\mvnw test
```

## Gateway Routes

- `/api/auth/**`
- `/api/vehicle/**`
- `/api/parking-lots/**`
- `/api/parking-spots/**`
- `/api/bookings/**`
