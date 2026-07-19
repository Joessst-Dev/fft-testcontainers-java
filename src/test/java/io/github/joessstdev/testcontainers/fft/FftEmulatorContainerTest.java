package io.github.joessstdev.testcontainers.fft;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class FftEmulatorContainerTest {

    @Container
    static final FftEmulatorContainer FFT =
            new FftEmulatorContainer().withSeed(Path.of("src/test/resources/fixtures"));

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void readinessEndpointAnswersWithoutAToken() throws Exception {
        // given a running emulator (the @Container)

        // when the status endpoint is queried without any token
        HttpResponse<String> res = get("/api/status");

        // then it answers 200
        assertThat(res.statusCode()).isEqualTo(200);
    }

    @Test
    void seededFacilityIsRetrievable() throws Exception {
        // given the emulator seeded from src/test/resources/fixtures (the @Container)

        // when the seeded collection is listed
        HttpResponse<String> res = get("/api/facilities");

        // then the pinned fixture id and name survive seeding and are present
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.body())
                .contains("11111111-1111-4111-8111-111111111111")
                .contains("Berlin Warehouse");
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(FFT.getBaseUrl() + path)).GET().build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }
}
