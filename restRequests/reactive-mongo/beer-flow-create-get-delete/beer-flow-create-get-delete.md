# beer Flow: createBeer → getBeer → deleteBeer

Datei: `restRequests/reactive-mongo/beer-flow-create-get-delete/beer-flow-create-get-delete.http`

## Ziel

Ein neues Bier anlegen, die erzeugte Ressource per GET prüfen und anschließend wieder löschen.

## Ablauf (Services)

```mermaid
sequenceDiagram
    actor C as HTTP Client (IntelliJ)
    participant GW as spring-6-gateway
    participant API as spring-6-reactive-mongo
    participant DB as MongoDB (Reactive)

    C->>GW: POST /api/v3/beer
    GW->>API: POST /api/v3/beer
    API->>DB: insert beer
    API-->>GW: 201 (Location: /api/v3/beer/{beerId})
    GW-->>C: 201 (Location: /api/v3/beer/{beerId})

    C->>GW: GET /api/v3/beer/{beerId}
    GW->>API: GET /api/v3/beer/{beerId}
    API->>DB: find beer
    API-->>GW: 200 (created beer)
    GW-->>C: 200 (created beer)

    C->>GW: DELETE /api/v3/beer/{beerId}
    GW->>API: DELETE /api/v3/beer/{beerId}
    API->>DB: delete beer
    API-->>GW: 204 (No Content)
    GW-->>C: 204 (No Content)
```

## Requests (in dieser Reihenfolge)

1. `createBeer` – legt das Bier an und liest `beerId` aus dem `Location`-Header.
2. `getBeerById` – prüft, dass das Bier existiert und `beerName`/`beerStyle` übereinstimmen.
3. `deleteBeer` – löscht das zuvor erzeugte Bier (hinterlässt keine Testdaten).

## Benötigte Globals

|       Global        |          gesetzt von           |         genutzt von         |
|---------------------|--------------------------------|-----------------------------|
| `beerId`            | `createBeer`                   | `getBeerById`, `deleteBeer` |
| `traceId`, `spanId` | `../../scripts/traceparent.js` | alle Requests               |

## Asserts

- `createBeer` → `201` mit `Location`-Header.
- `getBeerById` → `200`, `beerName` = `Flow Beer`, `beerStyle` = `LAGER`.
- `deleteBeer` → `204`.

## Hinweise

- Basis-URL ist `{{gateway-port}}` (Profil `local` = 8080, `k8s` = 30080); der Gateway routet `/api/v3/**` an
  `spring-6-reactive-mongo` (Port 8083/30083).
- Der `Location`-Header wird als relativer Pfad `/api/v3/beer/{id}` erzeugt; die ID wird über das letzte Pfadsegment
  extrahiert.
- `beerName` ist Pflicht (`@NotBlank`, 3–255 Zeichen); `beerStyle` ist optional (1–255 Zeichen). Die Mongo-`_id`
  wird als String (ObjectId) zurückgegeben.
- Der Flow ist wiederholbar: Das angelegte Bier wird am Ende gelöscht.
- Alle Requests benötigen ein Bearer-Token (`auth-id` aus `http-client.env.json`); dafür muss der Auth-Server auf
  Port `9000` laufen (siehe `docker compose up`).

