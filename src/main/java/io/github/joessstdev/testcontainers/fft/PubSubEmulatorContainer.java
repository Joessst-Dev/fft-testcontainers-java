package io.github.joessstdev.testcontainers.fft;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * A Google Cloud Pub/Sub emulator, for tests that exercise the fft emulator's eventing.
 * Put it on a shared {@link org.testcontainers.containers.Network} with a
 * {@link FftEmulatorContainer}, give it a network alias, and point the fft emulator at it
 * with {@link FftEmulatorContainer#withPubSubHost(String)}:
 *
 * <pre>{@code
 * Network net = Network.newNetwork();
 * PubSubEmulatorContainer pubsub = new PubSubEmulatorContainer()
 *         .withNetwork(net).withNetworkAliases("pubsub");
 * FftEmulatorContainer fft = new FftEmulatorContainer()
 *         .withNetwork(net).withPubSubHost("pubsub:8085");
 * }</pre>
 */
public class PubSubEmulatorContainer extends GenericContainer<PubSubEmulatorContainer> {

    /** The Google Cloud SDK image carrying the Pub/Sub emulator. */
    public static final DockerImageName DEFAULT_IMAGE =
            DockerImageName.parse("gcr.io/google.com/cloudsdktool/cloud-sdk:emulators");

    private static final int PORT = 8085;

    /** Creates a Pub/Sub emulator on the default image, project {@code local}. */
    public PubSubEmulatorContainer() {
        this(DEFAULT_IMAGE);
    }

    /** Creates a Pub/Sub emulator on the given image. */
    public PubSubEmulatorContainer(DockerImageName image) {
        super(image);
        withExposedPorts(PORT);
        withCommand("gcloud", "beta", "emulators", "pubsub", "start",
                "--host-port=0.0.0.0:" + PORT, "--project=local");
        waitingFor(Wait.forLogMessage(".*Server started, listening on " + PORT + ".*", 1));
    }

    /** The emulator's endpoint from the host, {@code host:port} — the value for {@code PUBSUB_EMULATOR_HOST}. */
    public String getEmulatorEndpoint() {
        return getHost() + ":" + getMappedPort(PORT);
    }
}
