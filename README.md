# Asher Zaidi, Sri Juluru and Varun Sharma

# Auction App — Run Guide (macOS & Windows)

Java 17 + Maven are required on both macOS and Windows.

## Prerequisites
- Java 17 (Temurin/Zulu/Oracle JDK)
- Maven 3.9+

Verify tools:
- `java -version`
- `mvn -version`

SQLite is embedded via JDBC; no separate DB install needed.

Tips when using curl (zsh/bash):
- Do NOT wrap commands in shell backticks; run them as-is.
- Wrap JSON in single quotes; passwords with `!` can break if unquoted.
- Optional: install `jq` for easy JSON parsing (used below); otherwise copy IDs from responses.

## Option A — Run the Monolith UI (port 2020)
This starts the complete app with HTML/Thymeleaf UI.

- Start (macOS/Windows/Linux):
  - `mvn -DskipTests spring-boot:run`
- Or build a jar and run:
  - `mvn -DskipTests package`
  - `java -jar target/auth-app-0.0.1-SNAPSHOT.jar`
- Open in browser:
  - http://localhost:2020/auth

Notes
- Common routes
  - Sign-In/Sign-Up: `http://localhost:2020/auth`
  - Catalogue: `http://localhost:2020/catalog`
  - Sell: `http://localhost:2020/sell`
  - Manage Sales: `http://localhost:2020/sell/manage`

## Option B — Run Modular Services (microservices)
Run each service in its own terminal. Default ports:
- controller-service: 8080 (facade/orchestrator; API only)
- identity-service: 8086 (login/register; JSON API)
- catalogue-service: 8082 (items; JSON API)
- auction-service: 8083 (bids/buy-now; JSON API)
- payment-service: 8084 (payments; JSON API)

Important: identity-service runs on 8086 (changed from 8081). Update dependent services’ base URL or pass it at runtime as shown below.

### 1) identity-service (8086)

Run in a new terminal:

```bash
cd identity-service
mvn -DskipTests spring-boot:run
```

Tests (copy-paste):

```bash
# Signup a new user
curl -sS -X POST http://localhost:8086/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"Password123!","firstName":"Alice","lastName":"Smith","streetName":"King","streetNumber":"100","city":"Toronto","country":"CA","postalCode":"A1A1A1"}'

# Login (expect userId/username)
curl -sS -X POST http://localhost:8086/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"Password123!"}'

### 2) catalogue-service (8082)

Run in a new terminal:

```bash
cd catalogue-service
mvn -DskipTests spring-boot:run
```

Tests:

```bash
# List items
curl -sS http://localhost:8082/api/items

# Create DUTCH item
curl -sS -X POST http://localhost:8082/api/items \
  -H 'Content-Type: application/json' \
  -d '{"name":"Demo Lamp","description":"desk lamp","auctionType":"DUTCH","startingPrice":45,"currentPrice":45,"shippingPrice":5,"shippingDays":3,"sellerUserId":1}'

# Create FORWARD item (1 minute duration required)
curl -sS -X POST 'http://localhost:8082/api/items?duration=1&unit=minutes' \
  -H 'Content-Type: application/json' \
  -d '{"name":"Forward Demo","description":"1 min","auctionType":"FORWARD","startingPrice":50,"shippingPrice":10,"sellerUserId":1}'

### 3) auction-service (8083)

Run in a new terminal:

```bash
cd auction-service
mvn -DskipTests spring-boot:run
```

Tests:

```bash
# Create a FORWARD item in catalogue (1 minute) and capture its id
curl -sS -X POST 'http://localhost:8082/api/items?duration=1&unit=minutes' \
  -H 'Content-Type: application/json' \
  -d '{"name":"Auction Forward","description":"demo","auctionType":"FORWARD","startingPrice":50,"shippingPrice":5,"sellerUserId":1}'
FID=$(curl -sS 'http://localhost:8082/api/items?q=Auction%20Forward' | jq -r '.[0].id'); echo "FID=$FID"

# Place bids (<= current -> 400; > current -> 200)
curl -sS -i -X POST "http://localhost:8083/api/auctions/$FID/bids" \
  -H 'Content-Type: application/json' -d '{"userId":2,"amount":50}'
curl -sS -i -X POST "http://localhost:8083/api/auctions/$FID/bids" \
  -H 'Content-Type: application/json' -d '{"userId":2,"amount":60}'

# Top bid for the same id
curl -sS "http://localhost:8083/api/auctions/$FID/top"

# Close expired forward auctions (wait ~70s after creation)
curl -sS -X POST http://localhost:8083/api/auctions/close-expired

# DUTCH buy-now flow
curl -sS -X POST http://localhost:8082/api/items \
  -H 'Content-Type: application/json' \
  -d '{"name":"Auction Dutch","description":"demo","auctionType":"DUTCH","startingPrice":300,"currentPrice":300,"shippingPrice":10,"sellerUserId":1}'
DID=$(curl -sS 'http://localhost:8082/api/items?q=Auction%20Dutch' | jq -r '.[0].id'); echo "DID=$DID"
curl -sS -i -X POST "http://localhost:8083/api/auctions/$DID/buy-now" \
  -H 'Content-Type: application/json' -d '{"userId":3}'
```

### 4) payment-service (8084)

Run in a new terminal (passing identity base URL is safe even if not used):

```bash
cd payment-service
mvn -DskipTests spring-boot:run 
```

Tests:

```bash
# Prepare a FORWARD item with shipping/expedite, and bid on it
curl -sS -X POST 'http://localhost:8082/api/items?duration=5&unit=minutes' \
  -H 'Content-Type: application/json' \
  -d '{"name":"Pay Forward","description":"demo","auctionType":"FORWARD","startingPrice":100,"shippingPrice":15,"expeditePrice":30,"sellerUserId":1}'
PID=$(curl -sS 'http://localhost:8082/api/items?q=Pay%20Forward' | jq -r '.[0].id'); echo "PID=$PID"
curl -sS -X POST "http://localhost:8083/api/auctions/$PID/bids" \
  -H 'Content-Type: application/json' -d '{"userId":2,"amount":120}'

# Pay (no expedite)
curl -sS -X POST http://localhost:8084/api/payments \
  -H 'Content-Type: application/json' \
  -d "{\"itemId\":$PID,\"userId\":2,\"expedite\":false}"

# Pay with expedite (different total)
curl -sS -X POST http://localhost:8084/api/payments \
  -H 'Content-Type: application/json' \
  -d "{\"itemId\":$PID,\"userId\":2,\"expedite\":true}"

# Verify item removed from catalogue
curl -sS -i "http://localhost:8082/api/items/$PID"
```

Notes:
- If no top bid exists, payment returns `400` with `{ "error": "no winner" }`.
- If `userId` doesn’t match the winner, returns `{ "error": "user is not winner" }`.

### 5) controller-service (8080)

Run in a new terminal:

```bash
cd controller-service
mvn -DskipTests spring-boot:run 
```

Tests (end-to-end facade):

```bash
# Register + Login (capture token)
curl -sS -X POST http://localhost:8080/api/controller/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"bob","password":"Password123!","firstName":"Bob","lastName":"B","city":"Toronto","country":"CA"}'
TOK=$(curl -sS -X POST http://localhost:8080/api/controller/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"bob","password":"Password123!"}' | jq -r '.token'); echo "TOK=$TOK"

# Sell a DUTCH item via controller (header token)
curl -sS -X POST 'http://localhost:8080/api/controller/sell' \
  -H 'Content-Type: application/json' -H "X-Session-Token: $TOK" \
  -d '{"name":"Ctl Dutch","description":"via controller","auctionType":"DUTCH","startingPrice":200,"currentPrice":200,"shippingPrice":10,"sellerUserId":1}'

# List seller DUTCH items
curl -sS -H "X-Session-Token: $TOK" http://localhost:8080/api/controller/sell/manage

# Sell a FORWARD item (1 minute)
curl -sS -X POST 'http://localhost:8080/api/controller/sell?duration=1&unit=minutes' \
  -H 'Content-Type: application/json' -H "X-Session-Token: $TOK" \
  -d '{"name":"Ctl Forward","description":"via controller","auctionType":"FORWARD","startingPrice":60,"shippingPrice":5,"sellerUserId":1}'

# Forward bid through controller (replace FID with the created id)
curl -sS -X POST http://localhost:8080/api/controller/forward-bid \
  -H 'Content-Type: application/json' \
  -d "{\"token\":\"$TOK\",\"itemId\":FID,\"amount\":70}"

# Aggregate item view & catalog search
curl -sS http://localhost:8080/api/controller/item/FID
curl -sS 'http://localhost:8080/api/controller/catalog?q=Ctl'

# Pay via controller
curl -sS -X POST http://localhost:8080/api/controller/pay \
  -H 'Content-Type: application/json' \
  -d "{\"token\":\"$TOK\",\"itemId\":FID,\"expedite\":false}"

# Current user
curl -sS -H "X-Session-Token: $TOK" http://localhost:8080/api/controller/me
```

### 6) application (2020)

Run in a new terminal:

```bash
mvn spring-boot:run
```

Notes
- You can permanently change base URLs by editing:
  - `controller-service/src/main/resources/application.properties`
  - `payment-service/src/main/resources/application.properties`
- The microservices are API-only. A 404 at `http://localhost:8080/` is normal; use `/api/controller/...` routes.

## Windows Tips
- Use PowerShell or Command Prompt; same `mvn` commands work if Maven is on PATH

## Directory Map
- `identity-service`: Auth and user profile API
- `catalogue-service`: Items catalogue API
- `auction-service`: Bids and buy-now API
- `payment-service`: Payments/receipts API
- `controller-service`: Facade/orchestrator API
- Monolith UI (root project): Thymeleaf UI on port 2020

## Clean Shutdown
- Press Ctrl+C in each terminal to stop services

