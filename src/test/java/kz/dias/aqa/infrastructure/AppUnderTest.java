package kz.dias.aqa.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Starts the application jar as a separate OS process and waits until it accepts connections.
 * Everything the application prints goes to {@link #logFile()}.
 */
public final class AppUnderTest implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(AppUnderTest.class);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(250);
    private static final Duration STOP_TIMEOUT = Duration.ofSeconds(10);
    private static final int LOG_TAIL_LINES = 40;

    private final Path jar;
    private final String host;
    private final int port;
    private final String secret;
    private final String mockUrl;
    private final Duration startupTimeout;
    private final Path logFile;

    private Process process;

    public AppUnderTest(Path jar, String host, int port, String secret, String mockUrl,
                        Duration startupTimeout, Path logFile) {
        this.jar = jar;
        this.host = host;
        this.port = port;
        this.secret = secret;
        this.mockUrl = mockUrl;
        this.startupTimeout = startupTimeout;
        this.logFile = logFile;
    }

    public void start() {
        if (!Files.isRegularFile(jar)) {
            throw new IllegalStateException("Application jar not found: " + jar.toAbsolutePath()
                    + ". Put it there or override -Dapp.jar.path=...");
        }
        List<String> command = List.of(
                javaExecutable(),
                "-Dsecret=" + secret,
                "-Dmock=" + mockUrl,
                "-Dserver.port=" + port,       // standard Spring Boot property
                "-jar", jar.toString());

        log.info("Starting application: {}", command);
        try {
            Files.createDirectories(logFile.toAbsolutePath().getParent());
            process = new ProcessBuilder(command)
                    .redirectErrorStream(true)                 // stderr -> stdout
                    .redirectOutput(logFile.toFile())          // stdout -> log file
                    .start();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot start application process", e);
        }
        // Safety net: kill the app even if the test JVM is stopped abnormally (Ctrl+C, crash)
        Runtime.getRuntime().addShutdownHook(new Thread(this::close, "app-under-test-shutdown"));
        waitUntilReady();
        log.info("Application is up at {}", baseUrl());
    }

    /** Polls the port until the app accepts connections, the process dies, or the timeout expires. */
    private void waitUntilReady() {
        Instant deadline = Instant.now().plus(startupTimeout);
        while (Instant.now().isBefore(deadline)) {
            if (!process.isAlive()) {
                throw new IllegalStateException("Application exited during startup with code "
                        + process.exitValue() + ". Last log lines:\n" + logTail());
            }
            if (NetworkUtils.isPortOpen(host, port)) {
                return;
            }
            sleep(POLL_INTERVAL);
        }
        close();
        throw new IllegalStateException("Application did not start within " + startupTimeout
                + ". Last log lines:\n" + logTail());
    }

    public String baseUrl() {
        return "http://%s:%d".formatted(host, port);
    }

    public Path logFile() {
        return logFile;
    }

    @Override
    public void close() {
        if (process == null || !process.isAlive()) {
            return;
        }
        process.destroy();                                  // graceful: SIGTERM
        try {
            if (!process.waitFor(STOP_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
                process.destroyForcibly();                  // last resort: SIGKILL
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
        log.info("Application stopped");
    }

    /** Use the same JDK that runs the tests, not whatever "java" happens to be on PATH. */
    private static String javaExecutable() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }

    private String logTail() {
        try {
            List<String> lines = Files.readAllLines(logFile);
            return String.join("\n", lines.subList(Math.max(0, lines.size() - LOG_TAIL_LINES), lines.size()));
        } catch (IOException e) {
            return "<log unavailable: " + e.getMessage() + ">";
        }
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the application", e);
        }
    }
}
