# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2026-07-19

### Added

- `FftEmulatorContainer` — start the fulfillmenttools emulator, waiting on a token-free
  `GET /api/status` `200`; `getBaseUrl()` / `getMappedPort()` accessors.
- Fluent options: `withSeed(Path)`, `withVerbose()`, `withPubSubHost(String)`.
- `PubSubEmulatorContainer` sidecar for eventing tests.
- Project scaffolding: Maven build, CI (JDK 17/21), release pipeline (Maven Central via
  the central-publishing plugin with GPG signing).

### Notes

- The Docker Engine API version the Testcontainers client speaks is pinned via the
  `docker.api.version` property (default `1.44`), because the bundled docker-java does not
  negotiate against a very new Docker daemon (e.g. Docker Desktop 29, min API 1.44).
  Override with `-Ddocker.api.version=...`.
