# beer Flow: listBeers → patchBeer → verify → restoreBeer

Datei: `restRequests/reactive-mongo/beer-flow-patch/beer-flow-patch.http`

## Ziel

Ein bestehendes Bier partiell aktualisieren (nur `beerName`), das Teilergebnis per GET prüfen und den ursprünglichen
Namen anschließend wiederherstellen – ohne das Bier zu löschen.

## Ablauf (Services)

```mermaid
sequenceDiagram
    actor C as HTTP Client (IntelliJ)
    participant GW as spring-6-gateway
    participant API as spring-6-reactive-mongo
    participant DB as MongoDB (Reactive)

    C->>GW: GET /api/v3/beer
    GW->>API: GET /api/v3/beer
    API->>DB: find beer
    API-->>GW: 200 (JSON array)
    GW-->>C: 200 (JSON array)

    C->>GW: PATCH /api/v3/beer/{beerId} (beerName)
    GW->>API: PATCH /api/v3/beer/{beerId} (beerName)
    API->>DB: update beer (partial)
    API-->>GW: 204 (No Content)
    GW-->>C: 204 (No Content)

    C->>GW: GET /api/v3/beer/{beerId}
    GW->>API: GET /api/v3/beer/{beerId}
    API->>DB: find beer
    API-->>GW: 200 (patched beer)
    GW-->>C: 200 (patched beer)

    C->>GW: PATCH /api/v3/beer/{beerId} (original name)
    GW->>API: PATCH /api/v3/beer/{beerId} (original name)
    API->>DB: update beer (partial)
    API-->>GW: 204 (No Content)
    GW-->>C: 204 (No Content)
```

## Requests (in dieser Reihenfolge)

1. `listBeers` – speichert `beerId`, `originalBeerName` und `originalBeerStyle` (erstes Bier aus dem JSON-Array).
2. `patchBeer` – partielles Update per PATCH; nur `beerName` wird gesendet.
3. `getBeerById` – prüft, dass `beerName` geändert und `beerStyle` unverändert ist.
4. `restoreBeer` – setzt den ursprünglichen `beerName` per PATCH zurück.

## Benötigte Globals

|                 Global                  |          gesetzt von           |         genutzt von          |
|-----------------------------------------|--------------------------------|------------------------------|
| `beerId`                                | `listBeers`                    | alle weiteren Requests       |
| `originalBeerName`, `originalBeerStyle` | `listBeers`                    | `getBeerById`, `restoreBeer` |
| `traceId`, `spanId`                     | `../../scripts/traceparent.js` | alle Requests                |

## Asserts

- `listBeers` → `200`, mindestens ein Bier.
- `patchBeer` → `204`.
- `getBeerById` → `200`, `beerName` = `Blessed Patched`, `beerStyle` unverändert.
- `restoreBeer` → `204`.

## Hinweise

- Basis-URL ist `{{gateway-port}}` (Profil `local` = 8080, `k8s` = 30080); der Gateway routet `/api/v3/**` an
  `spring-6-reactive-mongo` (Port 8083/30083).
- PATCH ist ein partielles Update: nur nicht-leere Felder (`beerName`, `beerStyle`, `upc`, `price`, `quantityOnHand`)
  werden übernommen; das Response-Body ist leer.
- Die Collection wird als JSON-Array geliefert (kein HAL, kein `_embedded`).
- Der Flow löscht kein Bier; Schritt 4 stellt den Ausgangszustand wieder her. Ohne Schritt 4 bleibt `Blessed Patched`
  gespeichert.
- Alle Requests benötigen ein Bearer-Token (`auth-id` aus `http-client.env.json`); dafür muss der Auth-Server auf
  Port `9000` laufen (siehe `docker compose up`).

