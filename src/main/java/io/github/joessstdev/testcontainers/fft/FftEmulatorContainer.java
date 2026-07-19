package io.github.joessstdev.testcontainers.fft;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * A Testcontainers container for the fulfillmenttools API emulator — the offline,
 * in-memory API server that the {@code fft} CLI ships. It gives an integration test a
 * fresh, disposable fulfillmenttools API per run: a random host port, automatic
 * readiness, automatic teardown. No tenant, no credentials, no network to the real
 * platform, and no authentication — a test simply points an HTTP client at
 * {@link #getBaseUrl()}.
 *
 * <p>The emulator remembers the top-level REST collections (facilities, listings,
 * stocks, orders, subscriptions, …) and synthesizes everything else from the spec.
 */
public class FftEmulatorContainer extends GenericContainer<FftEmulatorContainer> {

    /** The emulator image, without a tag. */
    public static final DockerImageName DEFAULT_IMAGE = DockerImageName.parse("ghcr.io/joessst-dev/fft");

    /**
     * The image tag this container pins by default. It tracks the {@code fft} CLI's own
     * semver; note the published tag carries no leading {@code v} (the git tag
     * {@code v0.3.0} publishes {@code ghcr.io/joessst-dev/fft:0.3.0}).
     */
    public static final String DEFAULT_TAG = "0.3.0";

    private static final int PORT = 8080;

    /** Where {@link #withSeed(Path)} places the fixtures inside the container. */
    private static final String CONTAINER_FIXTURES = "/fixtures";

    // The container command, assembled in configure() so the with* options can be
    // chained in any order. --host 0.0.0.0 is mandatory: the emulator defaults to
    // 127.0.0.1, which answers only inside the container, so the mapped port would be dead.
    private final List<String> args = new ArrayList<>(List.of("emulator", "--host", "0.0.0.0"));

    /** Creates a container on the default pinned image ({@link #DEFAULT_IMAGE}:{@link #DEFAULT_TAG}). */
    public FftEmulatorContainer() {
        this(DEFAULT_IMAGE.withTag(DEFAULT_TAG));
    }

    /** Creates a container on the given image reference (e.g. {@code "ghcr.io/joessst-dev/fft:0.3.0"}). */
    public FftEmulatorContainer(String image) {
        this(DockerImageName.parse(image));
    }

    /** Creates a container on the given image, which must be compatible with {@link #DEFAULT_IMAGE}. */
    public FftEmulatorContainer(DockerImageName image) {
        super(image);
        image.assertCompatibleWith(DEFAULT_IMAGE);
        withExposedPorts(PORT);
        // Readiness is a token-free HTTP 200 from /api/status: the emulator answers it the
        // moment it is listening. Only the status code is asserted — the emulator serves a
        // list envelope there, not the live API's {"status":"UP"} body.
        waitingFor(Wait.forHttp("/api/status").forStatusCode(200));
    }

    @Override
    protected void configure() {
        withCommand(args.toArray(new String[0]));
    }

    /**
     * Preloads the emulator from a host directory of {@code <collection>.json} fixtures
     * (facilities.json, orders.json, …), each a single object or an array. A seeded
     * document keeps the {@code id} and {@code version} it carries, so a fixture can pin
     * the exact ids a test asserts on.
     *
     * <p>The directory is copied into the container (tar over the Docker API), not bind
     * mounted, so it works with the distroless image and remote or rootless Docker hosts.
     * Mode {@code 0555} lets the emulator's nonroot user (uid 65532) read the fixtures.
     */
    public FftEmulatorContainer withSeed(Path fixturesDir) {
        withCopyFileToContainer(MountableFile.forHostPath(fixturesDir.toString(), 0555), CONTAINER_FIXTURES);
        args.add("--seed");
        args.add(CONTAINER_FIXTURES);
        return self();
    }

    /** Makes the emulator log one line per request to the container logs. */
    public FftEmulatorContainer withVerbose() {
        args.add("--verbose");
        return self();
    }

    /**
     * Points the emulator at an already-running Pub/Sub emulator ({@code host:port}) and
     * turns eventing on. Use it to wire a {@link PubSubEmulatorContainer} you manage on a
     * shared {@link org.testcontainers.containers.Network}.
     */
    public FftEmulatorContainer withPubSubHost(String hostPort) {
        args.add("--pubsub-emulator-host");
        args.add(hostPort);
        return self();
    }

    /** The host port the emulator's 8080 is published on. */
    public int getMappedPort() {
        return getMappedPort(PORT);
    }

    /** The emulator's base URL from the host, {@code http://host:<mapped-port>}. */
    public String getBaseUrl() {
        return "http://" + getHost() + ":" + getMappedPort(PORT);
    }
}
