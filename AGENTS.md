# AGENTS.md

Spring Cloud Gateway (Spring Boot 4 parent 4.1.0) on **Java 25** (enforced by the maven-enforcer
plugin). Single Maven module, package `guru.springframework.spring6gateway`. It is an OAuth2 JWT
resource server that routes to the sibling Spring 6 projects (`spring-6-auth-server`,
`spring-6-rest-mvc`, `spring-6-reactive`, `spring-6-reactive-mongo`, `spring-6-data-rest`),
packaged as Docker image and Helm chart.

## Build & test commands

- Full build: `./mvnw clean verify` — format checks, unit (`*Test`, surefire) + IT (`*IT`, failsafe)
  tests, Helm lint/template. `./mvnw verify` also runs the unit tests.
- Unit tests only: `./mvnw test`. Single test: `./mvnw test -Dtest=RouterConfigTest#methodName`.
- `./mvnw clean install` additionally builds the Docker image and packages the Helm chart into
  `target/helm/repo/`. Skip the Docker build with `-Dskip.docker.build=true` /
  `-Dskip.start.stop.springboot=true`.
- Start locally: `./mvnw spring-boot:run` (app on `:8080`, requires auth-server on `:9000`).

After changing code, always verify: run the relevant Maven goal above and report its output
(evidence, not just "done").

## Sandbox build quirk (background)

This sandbox mounts the repo via filesystem passthrough, which blocks symlinks — Spotless's
`npm install` (prettier) would fail with `EPERM` unless npm skips bin links. The sandbox kit sets
`npm_config_bin_links=false` globally (`spec.yaml` → `environment.variables`), so no manual export
is needed here. On a normal host (Windows/CI) this does not apply either.

## Formatting is enforced (fails the `validate` phase)

- Java: Spring Java Format → fix with `./mvnw spring-javaformat:apply`.
- Everything else (pom.xml, `**/*.md`, json, `src/main/resources/application*.yaml`, `**/*.sh`):
  Spotless → fix with `./mvnw spotless:apply`. Spotless uses shfmt `3.13.1` for shell scripts.
- Spotless excludes `AGENTS.md`/`CLAUDE.md` from flexmark (markdown) formatting.

## External dependency gotcha

- The auth-server (`spring-6-auth-server`) is resolved from the auth-server project's GitHub Packages
  (`maven.pkg.github.com`) / Helm repo (`repo.repsy.io/user08694146/helm-dboeckli`). Without a PAT in
  `~/.m2/settings.xml` (server id `github`) the build cannot resolve the snapshot dependency.
- The Helm chart depends on **8 aliased subcharts** (auth-server from Repsy, `spring-6-rest-mvc` +
  its `-mysql`/`-kafka` charts from Cloudsmith `oci://docker.cloudsmith.io/dboeckli/dboeckli-cloudsmith-repo`,
  the remaining projects from Docker Hub `oci://registry-1.docker.io/domboeckli`). `helm dependency build`
  pulls them during the build; all subcharts get a `fullnameOverride` so their service names are
  release-independent. The Cloudsmith charts are private — CI logs in via
  `helm registry login docker.cloudsmith.io` (`CLOUDSMITH_USERNAME` var, `CLOUDSMITH_API_KEY` secret),
  locally you need a manual `helm registry login` before building.

## Test conventions

- Naming matters: `*Test` = unit (surefire), `*IT` = integration (failsafe). A `*Test` class will
  not run during `verify`'s failsafe phase and vice versa.
- Tests use WireMock (not Testcontainers) to simulate the upstream services — no Docker container
  dependency for the tests themselves.

## Architecture

- Gateway routing via `Spring Cloud Gateway`; routes and upstream FQDNs are configured through Helm
  values / env (see `helm-charts/templates/deployment.yaml`).
- OpenAPI aggregation in `openapi/`, health indicators in `config/health/`.
- The app is an OAuth2 JWT resource server; issuer URI `http://localhost:9000`.

## Helm / Deploy

- Chart in `helm-charts/`, packaged to `target/helm/repo/<artifactId>-chart-<version>.tgz`, release
  name = chart dir name, namespace `spring-6-gateway`.
- CI (`.github/workflows/`): `maven-build.yml` builds + deploys snapshots and triggers
  `deploy-and-test-cluster.yml` (in-cluster, using the auth-server chart from Repsy).
- Dependency updates are managed via `.github/renovate.json`; validate changes with
  `renovate-config-validator`.
