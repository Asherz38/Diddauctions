# ITEC 4020 Auction Backend (Deliverable 1)

Multi-module Spring Boot project targeting Java 21 that implements a simple auction platform per the project description/PDF.

## Modules and Ports

- common-lib: Shared DTOs
- catalogue-service (8081): Create/search/get items (H2 + JPA)
- auction-service (8082): Start auctions, bid/withdraw, Dutch price decrease, buy-now (H2 + JPA)
- payment-service (8083): Mock payment endpoint returning a receipt
- user-service (8084): Sign-up/sign-in and token issuance; `GET /users/me` for token validation
- controller-app (8080): Gateway facade that proxies requests and enforces winner-only payment

## Build

- Java: 21
- Spring Boot: 3.3.5
- Build tool: Maven

Build everything:

```
mvn -DskipTests package
```

Run unit tests (example):

```
mvn -pl auction-service -am test
```

## Run locally

Open six terminals and start each service:

```
# Terminal 1
mvn -pl user-service spring-boot:run

# Terminal 2
mvn -pl catalogue-service spring-boot:run

# Terminal 3
mvn -pl auction-service spring-boot:run

# Terminal 4
mvn -pl payment-service spring-boot:run

# Terminal 5
mvn -pl controller-app spring-boot:run
```

H2 consoles (dev only):

- Auction DB: http://localhost:8082/h2-console
- Catalogue DB: http://localhost:8081/h2-console

## Happy-path flow (sample)

1) Sign up and sign in (user-service)

```
POST http://localhost:8084/users/sign-up
{
  "username":"alice","password":"pw",
  "firstName":"Alice","lastName":"A"
}

POST http://localhost:8084/users/sign-in
{
  "username":"alice","password":"pw"
}
# => {"token":"...","userId":1}
```

2) Create an item (controller-app proxies to catalogue):

```
POST http://localhost:8080/api/items
{
  "title":"Phone","description":"Good","condition":"USED",
  "auctionType":"FORWARD","startingPrice":100
}
# => {"id":123,...}
```

3) Start an auction (controller-app proxies to auction):

```
POST http://localhost:8080/api/auctions/start
{
  "itemId":123, "type":"FORWARD", "startingPrice":100, "durationSeconds":120
}
```

4) Bid and (optionally) withdraw within 30 seconds:

```
POST http://localhost:8080/api/auctions/123/bids
{"bidderId":1, "amount":120}

POST http://localhost:8080/api/auctions/123/withdraw
{"bidderId":1}
```

5) Close auction (let time elapse or for DUTCH use buy-now), then pay as the winner via controller-app. Winner-only is enforced and the payment amount must equal the winning price. Include the Bearer token from sign-in:

```
POST http://localhost:8080/api/payments
Authorization: Bearer <token>
{
  "itemId":123,
  "userId":999,        # will be overridden to the caller's userId
  "amount":120,        # must equal winning price
  "expeditedShipping":false,
  "addressStreet":"1 Main St","addressCity":"Toronto","addressCountry":"CA","postalCode":"M1M1M1",
  "cardNumber":"4111111111111111","cardName":"ALICE A","cardExpiry":"12/29","cardCvv":"123"
}
```

If the caller is not the auction winner, the auction is not CLOSED, or the amount mismatches the winning price, the gateway returns an error and will not forward to the payment service.

## Notes

- Persistence uses in-memory H2; schema is managed by JPA (ddl-auto update). Data resets on restart.
- Simple in-memory token service in user-service; tokens expire after 30 minutes.
- Dutch auction: sellers can decrease price with a reserve floor; buyers can buy-now at current price.
- Bid withdrawal (forward auctions) is allowed only for the last bid within 30 seconds; the price and winner are recomputed from bid history.

## Tests

- `auction-service`: includes a unit test for bid withdrawal within the allowed window.
- More tests can be added for Dutch price decrease and gateway payment enforcement.
