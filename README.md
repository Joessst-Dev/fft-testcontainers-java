# fft-testcontainers-java

A [Testcontainers for Java](https://java.testcontainers.org) module for the
[fulfillmenttools](https://fulfillmenttools.com) API **emulator** — the offline, in-memory
API server that the [`fft` CLI](https://github.com/Joessst-Dev/fft-cli) ships. It gives a
JUnit test a fresh, disposable fulfillmenttools API per run: a random host port, automatic
readiness, automatic teardown. No tenant, no credentials, no network to the real platform. It mirrors the [Go module](https://github.com/Joessst-Dev/testcontainers-fft).

> **Disclaimer:** This is an independent, community-maintained project and is **not** an
> official fulfillmenttools product. It is not affiliated with, endorsed by, or supported
> by fulfillmenttools GmbH. "fulfillmenttools" is a trademark of its respective owner.

## Usage

```java
@Testcontainers
class OrderFlowTest {

    @Container
    static final FftEmulatorContainer FFT =
            new FftEmulatorContainer().withSeed(Path.of("src/test/resources/fixtures"));

    @Test
    void listsSeededFacilities() throws Exception {
        HttpClient http = HttpClient.newHttpClient();
        HttpResponse<String> res = http.send(
                HttpRequest.newBuilder(URI.create(FFT.getBaseUrl() + "/api/facilities")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("Berlin Warehouse"));
    }
}
```

## Coordinates

```xml
<dependency>
  <groupId>io.github.joessst-dev</groupId>
  <artifactId>fft-testcontainers</artifactId>
  <version>0.1.0</version>
  <scope>test</scope>
</dependency>
```

Requires Java 17+ and a Docker daemon reachable by Testcontainers.

## What it does

`FftEmulatorContainer` starts `ghcr.io/joessst-dev/fft` with `emulator --host 0.0.0.0` and
waits until the emulator is listening. Readiness is a token-free `GET /api/status` returning
`200` (the emulator needs no auth at all). The image tag is pinned to a tested emulator
release; pass a `DockerImageName` to the constructor to override it.

The emulator remembers the top-level REST collections (facilities, listings, stocks, orders,
subscriptions, …): a create is stored, a get reflects it, versions and both pagination models
work, and optimistic-locking `409`s are real. Everything else is answered from a
spec-synthesized response. See the
[emulator guide](https://github.com/Joessst-Dev/fft-cli/blob/main/docs/guide/emulator.md) for
the full model.

## API

| Member | Purpose |
| --- | --- |
| `new FftEmulatorContainer()` | default pinned image |
| `new FftEmulatorContainer(DockerImageName)` | override the image/tag |
| `withSeed(Path)` | preload `<collection>.json` fixtures (copied in, then `--seed`) |
| `withVerbose()` | one emulator log line per request |
| `withPubSubHost(String)` | point eventing at a Pub/Sub emulator you manage |
| `getBaseUrl()` | `http://host:<mapped-port>` |
| `getMappedPort()` | the mapped host port |

`FftEmulatorContainer` extends `GenericContainer`, so its full API is available too. For
eventing, a `PubSubEmulatorContainer` sidecar can be wired on a shared `Network`.

## Versioning

The default image tag tracks the [`fft` CLI](https://github.com/Joessst-Dev/fft-cli/releases)'s
own semver. This module is versioned independently and published to Maven Central.

## License

MIT — see [LICENSE](./LICENSE).
