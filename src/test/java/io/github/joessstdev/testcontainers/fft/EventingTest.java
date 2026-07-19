package io.github.joessstdev.testcontainers.fft;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Exercises eventing end to end: a create on a stateful collection auto-publishes a
 * lifecycle event, which lands on the Pub/Sub emulator and is pulled back. The
 * container-native form of the emulator guide's walkthrough.
 */
@Testcontainers
class EventingTest {

    static final Network NET = Network.newNetwork();

    @Container
    static final PubSubEmulatorContainer PUBSUB =
            new PubSubEmulatorContainer().withNetwork(NET).withNetworkAliases("pubsub");

    @Container
    static final FftEmulatorContainer FFT =
            new FftEmulatorContainer().withNetwork(NET).withPubSubHost("pubsub:8085");

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void createdOrderPublishesToPubSub() throws Exception {
        // given a topic, a pull subscription, and an fft subscription for ORDER_CREATED.
        // The Pub/Sub REST API creates the topic and subscription with PUT.
        String base = FFT.getBaseUrl();
        String ps = "http://" + PUBSUB.getEmulatorEndpoint();
        put(ps + "/v1/projects/local/topics/orders", "");
        put(ps + "/v1/projects/local/subscriptions/reader",
                "{\"topic\":\"projects/local/topics/orders\"}");
        post(base + "/api/subscriptions", """
                {"name":"orders","event":"ORDER_CREATED",
                 "target":{"type":"GOOGLE_CLOUD_PUB_SUB","projectId":"local","topicId":"orders"}}""");

        // when an order is created, which auto-emits ORDER_CREATED
        post(base + "/api/orders", "{\"tenantOrderId\":\"order-1\"}");

        // then the event is delivered to the topic and can be pulled back
        HttpResponse<String> pull = post(ps + "/v1/projects/local/subscriptions/reader:pull",
                "{\"maxMessages\":10,\"returnImmediately\":true}");
        assertThat(pull.statusCode()).isEqualTo(200);
        assertThat(pull.body())
                .contains("receivedMessages")
                .contains("ORDER_CREATED");
    }

    private void put(String url, String body) throws Exception {
        send("PUT", url, body);
    }

    private HttpResponse<String> post(String url, String body) throws Exception {
        return send("POST", url, body);
    }

    private HttpResponse<String> send(String method, String url, String body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        assertThat(res.statusCode())
                .as("%s %s -> %s", method, url, res.body())
                .isLessThan(300);
        return res;
    }
}
