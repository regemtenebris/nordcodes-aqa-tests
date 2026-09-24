package kz.dias.aqa.infrastructure;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * WireMock server that plays the role of the external service
 * ({@code /auth}, {@code /doAction}) the application calls.
 */
public final class ExternalServiceMock implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ExternalServiceMock.class);

    private final WireMockServer server;

    public ExternalServiceMock(int port) {
        this.server = new WireMockServer(options()
                .port(port)
                .notifier(new Slf4jNotifier(false)));
    }

    public void start() {
        server.start();
        log.info("External service mock started on port {}", server.port());
    }

    public int port() {
        return server.port();
    }

    /** Value for the application's {@code -Dmock=...} parameter. */
    public String baseUrl() {
        return "http://localhost:%d/".formatted(port());
    }

    public WireMockServer server() {
        return server;
    }

    @Override
    public void close() {
        if (server.isRunning()) {
            server.stop();
            log.info("External service mock stopped");
        }
    }
}
