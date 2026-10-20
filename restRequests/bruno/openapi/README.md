# OpenAPI-derived Bruno requests

The collections under `openapi/<service>/` are generated from the OpenAPI specs produced by the
`spring-6-gateway` Maven build. They are **not** hand-maintained; regenerate them instead.

|      Folder      |        Spec (`target/openapi/`)        |     Maven classifier     |      Auth       |
|------------------|----------------------------------------|--------------------------|-----------------|
| `rest-mvc`       | `openapi-spring-6-rest-mvc.json`       | `openapi-rest-mvc`       | OAuth2 (folder) |
| `reactive`       | `openapi-spring-6-reactive.json`       | `openapi-reactive`       | OAuth2 (folder) |
| `reactive-mongo` | `openapi-spring-6-reactive-mongo.json` | `openapi-reactive-mongo` | OAuth2 (folder) |
| `data-rest`      | `openapi-spring-6-data-rest.json`      | `openapi-data-rest`      | `inherit`       |
| `auth-server`    | `openapi-spring-6-auth-server.json`    | `openapi-auth-server`    | none            |

## 1. Generate the specs

Fastest variant (skips tests, but still starts the app and Docker Compose in
`pre-integration-test` so springdoc can fetch the docs):

```bash
./mvnw clean verify -DskipTests
```

The files are written to `target/openapi/`. Alternatively resolve a published spec via Maven:

```bash
mvn dependency:copy \
  -Dartifact=guru.springframework:spring-6-gateway:<version>:json:openapi-data-rest \
  -DoutputDirectory=target/openapi
```

## 2. Import into Bruno

For `rest-mvc`, `reactive`, `reactive-mongo` and `data-rest` the URL-path grouping produces an
`api/` tree that is copied into the collection:

```bash
service=data-rest
bru import openapi \
  --source "target/openapi/openapi-spring-6-$service.json" \
  --output "/tmp/bru-openapi-$service" \
  --collection-name "$service" \
  --collection-format opencollection \
  --group-by path

cp -r "/tmp/bru-openapi-$service/api" "restRequests/bruno/openapi/$service/"
```

`auth-server` has no `api/` prefix and its `/oauth2/actuator/**` paths are filtered out (they would
add e.g. `actuator/shutdown` and `heapdump` to the collection):

```bash
python3 - <<'PY'
import json
d = json.load(open('target/openapi/openapi-spring-6-auth-server.json'))
for p in [p for p in d['paths'] if '/actuator' in p]:
    del d['paths'][p]
json.dump(d, open('/tmp/openapi-auth-server.json', 'w'), indent=2)
PY

bru import openapi \
  --source /tmp/openapi-auth-server.json \
  --output /tmp/bru-openapi-auth-server \
  --collection-name "auth-server" \
  --collection-format opencollection \
  --group-by path

cp -r /tmp/bru-openapi-auth-server/oauth2 \
      /tmp/bru-openapi-auth-server/userinfo \
      /tmp/bru-openapi-auth-server/well-known \
      "/tmp/bru-openapi-auth-server/{subpath}" \
      restRequests/bruno/openapi/auth-server/
```

## 3. Map the server URL to the environments

The imported requests use `{{baseUrl}}`; rewrite it to the existing `gateway-port` variable:

```bash
grep -rl '{{baseUrl}}' restRequests/bruno/openapi | while read -r f; do
  sed -i 's|{{baseUrl}}|http://localhost:{{gateway-port}}|g' "$f"
done
```

## Notes

- Requests use `auth: inherit`; `rest-mvc`, `reactive` and `reactive-mongo` therefore get OAuth2
  client credentials from their folder-level `folder.yml`.
- The Bruno import creates its own `opencollection.yml` and environment. Those are intentionally
  **not** copied: the generated requests are part of the parent collection.
- The spec embeds the project version at generation time. Regenerate after version changes to keep
  the embedded `info.version` current.

