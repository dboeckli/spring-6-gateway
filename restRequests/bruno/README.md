# Bruno-Collection (abgeleitet aus `restRequests/`)

Die IntelliJ-HTTP-Requests unter `restRequests/` bleiben der Master (Source of Truth). Diese
Bruno-Collection wird daraus als **OpenCollection YAML** generiert und dient als Alternative zum
IntelliJ HTTP Client sowie zur Automatisierung per CLI.

Aktueller Stand: alle Service-Ordner sind konvertiert — `data-rest` (12 Requests), `reactive`
(26), `reactive-mongo` (22) und `mvc` (26), inkl. OAuth2 für `reactive`/`reactive-mongo`/`mvc`.
Zusätzlich gibt es einen Auth-Server-Smoke-Flow (`auth-server/token-flow`, 4 Requests).
Das Bruno-Format ist YAML (`.yml`); `.bru` ist optional (`--format bru`).

## Struktur

```text
restRequests/bruno/
├── opencollection.yml          # Collection-Root (YAML), inkl. Pre-Request-Script
├── environments/
│   ├── local.yml               # Ports 8080/9000/8081/8082/8083/8084
│   └── k8s.yml                 # NodePorts 30080/30900/...
├── data-rest/                  # 1:1 aus restRequests/data-rest abgeleitet
│   ├── beer-flow-list-update-delete/
│   ├── beer-flow-search/
│   └── beer/
├── reactive/                   # 1:1 aus restRequests/reactive abgeleitet
│   ├── folder.yml              # OAuth2 (Client Credentials) für alle Unterordner
│   └── …
├── reactive-mongo/             # analog, inkl. folder.yml
├── mvc/                        # analog, inkl. folder.yml
├── auth-server/                # Token-/Discovery-/Actuator-Smoke-Flow
│   └── token-flow/
└── tools/
    ├── http-to-bru.mjs         # Generator .http -> .yml (Default), optional .bru
    ├── package.json            # @usebruno/filestore
    └── package-lock.json
```

Jeder `.http`-Block wird zu einer `.yml`-Datei; Ordner und Reihenfolge (`seq`) entsprechen dem
Master. Flow-Dateien landen im gleichnamigen Ordner, Einzelrequest-Dateien in einem Ordner mit dem
Dateinamen (z. B. `beer.http` → `data-rest/beer/`).

## Voraussetzungen

- Bruno CLI `4.2.1` (`@usebruno/cli`), Node.js ≥ 18: `npm i -g @usebruno/cli@4.2.1`
- Bruno Desktop (optional, für GUI-Nutzung), siehe „Öffnen in Bruno Desktop“.
- Für die Generierung: `npm ci` in `restRequests/bruno/tools` (YAML-Renderer).
- Für `data-rest`: `data-rest` (Port 8084) bzw. der Gateway-Stack, z. B.
  `docker compose up -d auth-server data-rest`.
- Für `reactive`: `auth-server` + `reactive` (Port 8082) bzw. der Gateway-Stack.

## Öffnen in Bruno Desktop

1. In Bruno Desktop **„Collection öffnen“** (Ordner-/Plus-Symbol oben links) wählen.
2. Den Ordner mit `opencollection.yml` auswählen — **nicht** den Unterordner `data-rest/`:

   ```text
   C:\development\projects\spring-6-gateway\restRequests\bruno
   ```
3. Oben rechts das Environment `local` (oder `k8s`) auswählen.
4. Die Requests erscheinen unter `data-rest/…`; der Ordner `tools/` wird über die `ignore`-Liste in
   `opencollection.yml` ausgeblendet.

## Ausführen

Vom Collection-Root (`restRequests/bruno`):

```bash
# einzelner Flow
bru run data-rest/beer-flow-search --env local

# gesamter data-rest-Ordner (rekursiv)
bru run data-rest -r --env local

# k8s-Profil (NodePorts)
bru run data-rest -r --env k8s
```

Ohne laufenden Gateway kann direkt gegen `data-rest` getestet werden:

```bash
bru run data-rest -r --env local --env-var gateway-port=8084
```

Test-Report (z. B. für CI): `--reporter-junit ../../target/bru-data-rest.xml`.

Hinweis: `bru run .` über die ganze Collection nur bewusst nutzen — die Flows sind
zustandsbehaftet (Runtime-Variablen, create/delete, Token). Runtime-Variablen (`bru.setVar`)
überleben nur einen CLI-Lauf: Ein späterer Flow-Schritt (z. B. `02-update-the-beer`) kann nicht
allein gestartet werden, sondern nur im Flow-Verbund.

## Konvertierung (IntelliJ → Bruno)

Aus dem Collection-Root `restRequests/bruno`:

```bash
node tools/http-to-bru.mjs --source ../data-rest
node tools/http-to-bru.mjs --source ../data-rest --check   # nur prüfen, Exit 1 bei Abweichung
```

Alternativ vom Repo-Root:

```bash
node restRequests/bruno/tools/http-to-bru.mjs \
  --source restRequests/data-rest \
  --collection restRequests/bruno
```

Optionen:

- `--source <pfad>`: `.http`-Datei oder Ordner (Default `../data-rest`).
- `--collection <pfad>`: Ziel-Collection-Root (Default: Elternordner von `tools/`).
- `--format <bru|yml>`: Ausgabeformat (Default `yml`).
- `--check`: schreibt nichts, Exit 1 bei Abweichung.
- `--help`: Hilfe.

Beispiel für einen weiteren Service-Ordner (`reactive`) — PowerShell vom Repo-Root:

```powershell
node .\restRequests\bruno\tools\http-to-bru.mjs --source .\restRequests\reactive
```

Oder aus `restRequests/bruno` (bash/PowerShell):

```bash
node tools/http-to-bru.mjs --source ../reactive
node tools/http-to-bru.mjs --source ../reactive --check
```

Das konvertiert rekursiv alle `.http`-Dateien nach `restRequests/bruno/reactive/…`; andere
Service-Ordner bleiben unberührt. Der IntelliJ-Header `{{$auth.token("auth-id")}}` wird entfernt
und durch OAuth2 auf Folder-Ebene ersetzt (siehe unten).

Der Generator übersetzt:

|                   IntelliJ                    |                       Bruno                       |
|-----------------------------------------------|---------------------------------------------------|
| `### Name` + Request-Zeile                    | `info.name` + `http`-Block                        |
| Header                                        | `http.headers`                                    |
| JSON-Body                                     | `http.body` (type `json`)                         |
| `< ../../scripts/traceparent.js`              | Collection-Script in `opencollection.yml`         |
| `< {% ... %}`                                 | `request.scripts` (before-request)                |
| `> {% ... %}`                                 | `runtime.scripts` (tests)                         |
| `client.global.set/get`                       | `bru.setVar/getVar`                               |
| `client.test` / `client.assert`               | `test(...)` / `expect(...).to.be.true`            |
| `response.body/status/headers`                | `res.body/res.status/res.getHeader(...)`          |
| `Content-Type: application/json`              | entfällt bei JSON-Body (setzt Bruno selbst)       |
| `Authorization: Bearer {{$auth.token("id")}}` | `folder.yml` mit OAuth2, Requests `auth: inherit` |

## OAuth2 (reactive/mvc)

Der Generator liest die OAuth2-Definitionen aus `restRequests/http-client.env.json`
(Profil `local`, `Security.Auth`) und erzeugt pro Service-Ordner ein `folder.yml`:

```yaml
request:
  auth:
    type: oauth2
    flow: client_credentials
    accessTokenUrl: "{{tokenUrl}}"
    credentials:
      clientId: "{{clientId}}"
      clientSecret: "{{clientSecret}}"
      placement: basic_auth_header
    scope: message.read message.write
    tokenConfig:
      id: auth-id
      placement:
        header: Bearer
      source: access_token
    settings:
      autoFetchToken: true
      autoRefreshToken: false
```

Die Requests behalten `auth: inherit`. Client-ID/Scope/Token-URL kommen aus dem IntelliJ-Master
(Generierungszeit); der Secret wird **nicht** in die Collection geschrieben.

Secret-Auflösung (Collection-Pre-Request in `opencollection.yml`):

```text
BRUNO_CLIENT_ID     → clientId     (Fallback: clientId aus dem Environment)
BRUNO_CLIENT_SECRET → clientSecret (Fallback: "secret" für die lokale Dev-Umgebung)
```

Lokal reicht der Dev-Default. In CI:

```yaml
env:
  BRUNO_CLIENT_ID: ${{ vars.BRUNO_CLIENT_ID }}
  BRUNO_CLIENT_SECRET: ${{ secrets.BRUNO_CLIENT_SECRET }}
```

Oder pro Lauf: `bru run reactive -r --env local --env-var clientSecret=...`.
Werden die Tokens in Reports nicht benötigt, Reports mit `--reporter-skip-all-headers` erzeugen.

## YAML (Default)

Die Bruno-Collection ist **OpenCollection YAML**. Der Generator erzeugt standardmäßig `.yml`:

```bash
node tools/http-to-bru.mjs --source ../data-rest
node tools/http-to-bru.mjs --source ../data-rest --check
```

Das erzeugt/aktualisiert `opencollection.yml`, `environments/*.yml`, `folder.yml` und `*.yml`.
Einmalig nötig: `npm ci` in `restRequests/bruno/tools` (der YAML-Renderer
`@usebruno/filestore` ist dort gepinnt; `nanoid` wird wegen einer fehlenden Dependency dort
explizit mitinstalliert).

Das alte `.bru`-Format bleibt optional für bestehende `.bru`-Collections:

```bash
node tools/http-to-bru.mjs --source ../data-rest --format bru --collection ../legacy-bru
```

`--format bru` erzeugt nur Requests/Folders; Root- und Environment-Dateien werden für `.bru`
nicht generiert (die bestehende `.bru`-Collection bringt sie mit).

Wichtig:

- Ein Zielverzeichnis darf **nicht** `.bru` und `.yml` mischen; der Generator bricht sonst ab.
- `--check` funktioniert für beide Formate.
- Test: `cd restRequests/bruno && bru run data-rest -r --env local --env-var gateway-port=8084`.
- Der IntelliJ-`.http`-Master bleibt unverändert.

## Auth-Server-Flow

`restRequests/auth-server/token-flow/token-flow.http` (und das Bruno-Pendant unter
`restRequests/bruno/auth-server/token-flow/`) prüft:

1. Client-Credentials-Token über das Gateway holen (`accessToken`)
2. Discovery-Dokument (`/.well-known/openid-configuration`)
3. Auth-Server-Activator über `/oauth2/actuator/info`
4. Geschützten Aufruf `/api/v1/beer/listBeers` mit dem Token

Die Varianten in `restRequests/AuthServerRequest.http` (localhost, host.docker.internal, direkt vs.
Gateway) bleiben als Utility-Sammlung bestehen.

## Pflege-Workflow

1. Änderung am Master in `restRequests/<service>/...http` (und ggf. `.md`).
2. `node tools/http-to-bru.mjs --source ../<service>` ausführen.
3. Diff prüfen, `bru run` gegen den Service ausführen.
4. Generierte `.yml`-Dateien **nicht** manuell bearbeiten — sie tragen den Marker
   `Generated by tools/http-to-bru.mjs` und werden beim nächsten Lauf überschrieben.
   Veraltete generierte Dateien entfernt der Generator selbst, aber nur innerhalb des zur
   `--source` gehörenden Baums (z. B. `data-rest/`); fremde Dateien bleiben unberührt.
   Nicht-generierte Dateien an einem Zielpfad werden nicht überschrieben (Abbruch mit Fehler).
5. `opencollection.yml` ist handgepflegt und spiegelt `restRequests/scripts/traceparent.js` — bei
   Änderungen am Traceparent-Script dort nachziehen.

## Evaluation (Stand: alle Service-Ordner)

|         Kriterium         |                                           Ergebnis                                           |
|---------------------------|----------------------------------------------------------------------------------------------|
| Git-Freundlichkeit        | Sehr gut: YAML ist Plain Text, ein Request pro Datei, gut reviewbar.                         |
| Collection-/Env-Struktur  | Klar: `opencollection.yml`, `environments/*.yml`; ein Env-File pro Zielumgebung.             |
| OAuth2 Client Credentials | Umgesetzt für `reactive` (Folder-Auth, Client Credentials, Secret per Env/CI).               |
| Pre-/Post-Request-Scripts | Voll nutzbar; IntelliJ-API muss übersetzt werden (siehe Tabelle).                            |
| Assertions                | `test(...)` + Chai-`expect`; Ausgabe im CLI-Report, JUnit-Reporter vorhanden.                |
| CLI-/CI-Tauglichkeit      | Sehr gut: `bru run`, `--env`, `--env-var`, `--reporter-junit`, `--bail`, Exit-Code.          |
| Secret-Handling           | `bru.getProcessEnv` (`BRUNO_CLIENT_SECRET`) mit Dev-Fallback; kein Secret in `.yml`-Dateien. |
| Import aus IntelliJ       | Nicht vorhanden (Bruno importiert nur OpenAPI/WSDL/Postman/Insomnia) → eigener Generator.    |

**Entscheid:** Bruno wird als abgeleitete Alternative eingeführt; IntelliJ bleibt Master. Die
Konvertierung erfolgt skriptbasiert (Option 1), damit keine manuelle Doppelpflege entsteht.

## Troubleshooting

**GET funktioniert, POST/PUT/DELETE liefern `403 Forbidden` (Response setzt `JSESSIONID`).**

Das ist CSRF im `spring-6-data-rest`-Service, nicht Bruno oder das Gateway. Der Fix
(`csrf.ignoringRequestMatchers("/h2-console/**", "/api/**")`) ist ab Commit `64e5b86`
(„Feature/data rest csrf api (#122)“) enthalten. Ein alter `0.0.1-snapshot`-Container
verweigert Schreibzugriffe.

```bash
docker compose pull data-rest
docker compose up -d --force-recreate data-rest
```

Prüfen: `curl http://localhost:8084/actuator/info` → `git.commit.id.full` ≥ `64e5b86`.
Danach den Flow im Bruno GUI erneut ausführen (Runtime-Variablen werden pro Lauf neu gesetzt).

## Master-Korrekturen (data-rest)

Beim Lauf gegen den echten `data-rest`-Container wurden die data-rest-Flows korrigiert:

- HAL-Collection heißt `_embedded.beers` (plural), nicht `_embedded.beer`.
- Spring Data REST liefert kein `id`-Feld; die ID kommt aus `_links.self.href`.
- `DELETE /api/v4/beer/{id}` liefert `200 OK` (nicht `204`).

Die Bruno-Dateien wurden danach neu generiert.

## CI (Deploy-Workflow)

`.github/workflows/deploy-and-test-cluster.yml` führt nach Helm-Deploy und `helm test` einen
Bruno-Smoke-Test als Pflicht-Gate aus:

1. `actions/setup-node` + `npm install -g @usebruno/cli@4.2.1`
2. `kubectl port-forward svc/spring-6-gateway 8080:8080`
3. Pro Service ein eigener Lauf (isoliert Runtime-Variablen):
   `bru run <service> -r --env local` für `data-rest`, `reactive`, `reactive-mongo`, `mvc`,
   `auth-server`
4. JUnit-/HTML-Report pro Service als Artifact `bruno-reports` (`--reporter-skip-all-headers`)
5. JUnit-Ergebnisse via `EnricoMi/publish-unit-test-result-action` im **Checks**-Tab
   (`Bruno API smoke tests`), ohne Download des Artifacts
6. HTML-Reports zusätzlich auf **GitHub Pages** (Job `publish-bruno-report`)

`BRUNO_CLIENT_ID` (Repository-Variable) und `BRUNO_CLIENT_SECRET` (Secret) werden in den Bruno-Step
durchgereicht; sie werden für `reactive`, `reactive-mongo` und `mvc` benötigt.

Der Bruno-Step ist ein **Pflicht-Gate**: Schlägt ein Request/Test fehl, schlägt der Deploy-Job fehl
(`fail_on: test failures`, `action_fail: true`). Reports werden trotzdem hochgeladen, als Check
publiziert und auf Pages deployed (`if: always()`). Der Port-Forward wird per `trap` beendet.

### GitHub Pages

Nach dem Deploy wird ein `_site` mit `index.html` und den HTML-Reports aller Services gebaut und
über die offiziellen Pages-Actions veröffentlicht:

<https://dboeckli.github.io/spring-6-gateway/>

Einmalige Voraussetzungen:

- **Settings → Pages → Build and deployment → Source: GitHub Actions**.
- Falls das `github-pages`-Environment Branch-Regeln hat: den deployenden Branch erlauben.

Hinweise:

- Es wird immer der **letzte Lauf** veröffentlicht (vorherige Reports werden überschrieben).
- Die Bruno-HTML-Reports laden Vue/Naive UI von `unpkg.com` — der Browser braucht Internetzugang.
- `--reporter-skip-all-headers` entfernt Auth-Header; Request-/Response-Bodies sind weiterhin im
  Report. Bei öffentlichem Repo sind die Pages damit öffentlich.

## Offene Punkte

- Bruno-Smoke-Test ist im Deploy-Workflow ein Pflicht-Gate; bei Flakiness ggf. wieder lockern oder
  in einen separaten Nightly-Job verschieben.
- `data-rest`/`reactive` sind gegen die echten Services verifiziert; `reactive-mongo`/`mvc` werden
  über den Deploy-Workflow validiert (Host-Stack war beim Konvertieren aus).
- `bru`-Version im Sandbox-Kit versioniert aufnehmen, falls Bruno bleibt.

