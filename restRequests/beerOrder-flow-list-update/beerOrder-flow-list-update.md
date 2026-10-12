# beerOrder Flow: listBeerOrders → updateBeerOrder

Datei: `restRequests/beerOrder-flow-list-update/beerOrder-flow-list-update.http`

## Ziel

Eine bestehende Bestellung **mit Line** aus der Liste auswählen und aktualisieren.

## Ablauf (Services)

```mermaid
sequenceDiagram
    actor C as HTTP Client (IntelliJ)
    participant GW as spring-6-gateway
    participant AS as spring-6-auth-server
    participant MVC as spring-6-rest-mvc
    participant DB as MySQL
    participant K as Kafka (order.placed)
    participant D as cold/cool/icecold

    C->>GW: GET listCustomer (Bearer)
    GW->>AS: JWKS / OIDC discovery
    GW->>MVC: GET /api/v1/customer/listCustomer
    MVC->>DB: SELECT customer
    MVC-->>C: 200 (customers)

    C->>GW: GET listBeers
    GW->>MVC: GET /api/v1/beer/listBeers
    MVC->>DB: SELECT beer
    MVC-->>C: 200 (beers)

    C->>GW: GET listBeerOrders
    GW->>MVC: GET /api/v1/beerOrder/listBeerOrders
    MVC->>DB: SELECT beer_order (+ lines)
    MVC-->>C: 200 (orders)

    C->>GW: PUT updateBeerOrder/{beerOrderId}
    GW->>MVC: PUT /api/v1/beerOrder/updateBeerOrder/{id}
    MVC->>DB: UPDATE beer_order + line
    MVC->>K: OrderPlacedEvent (paymentAmount != null)
    K-->>D: drink.request.icecold / cold / cool
    MVC-->>C: 200
```

## Requests (in dieser Reihenfolge)

1. `listCustomer` – speichert `customerId` (erster Kunde aus der Liste).
   - alternativ: `createCustomer` – speichert `customerId` (neu angelegter Kunde).
2. `listBeers` – speichert `beerId` (erstes Bier aus der Liste).
3. `listBeerOrders` – wählt die **erste Order, die eine Line hat**, und speichert `beerOrderId` **und** `beerOrderLineId`.
4. `updateBeerOrder` – aktualisiert diese bestehende Order/Line.

## Benötigte Globals

|       Global        |            gesetzt von            |            genutzt von            |
|---------------------|-----------------------------------|-----------------------------------|
| `customerId`        | `listCustomer` / `createCustomer` | `updateBeerOrder`                 |
| `beerId`            | `listBeers`                       | `updateBeerOrder`                 |
| `beerOrderId`       | `listBeerOrders`                  | `updateBeerOrder`                 |
| `beerOrderLineId`   | `listBeerOrders`                  | `updateBeerOrder`                 |
| `traceId`, `spanId` | `../scripts/traceparent.js`       | alle Requests (Trace-Korrelation) |

## Asserts

- `updateBeerOrder` → `200`, `id` entspricht der `{{beerOrderId}}`.

## Hinweise

- `listBeerOrders` speichert nur Orders **mit** Line. Gibt es keine, wird `no beer order with a line found` geloggt und
  `beerOrderId`/`beerOrderLineId` bleiben ungesetzt.
- `updateBeerOrder` mit gesetztem `paymentAmount` publiziert ein `OrderPlacedEvent` → Kafka-Topic `order.placed`
  (danach `drink.request.*` durch den `DrinkSplitterRouter`).
- Die Line im Update muss die gespeicherte `beerOrderLineId` mitschicken, sonst 404 (die App sucht die Line per `id`).

