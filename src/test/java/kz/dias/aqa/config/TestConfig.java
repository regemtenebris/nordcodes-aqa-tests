package kz.dias.aqa.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Properties;

/**
 * Single entry point to all test settings.
 *
 * <p>Values are resolved in this order (first non-empty wins):
 * <ol>
 *   <li>JVM system property ({@code -Dapp.port=9090})</li>
 *   <li>environment variable ({@code APP_PORT=9090})</li>
 *   <li>{@code src/test/resources/test.properties}</li>
 * </ol>
 * This lets CI or a developer change any setting without touching the code.
 */
public final class TestConfig {

    private static final String PROPERTIES_FILE = "test.properties";
    private static final Properties FILE_PROPERTIES = loadFile();

    private TestConfig() {
        // utility class: no instances
    }

    // ------------------------------------------------------------------
    // Application under test
    // ------------------------------------------------------------------

    public static String appJarPath() {
        return get("app.jar.path");
    }

    public static String appHost() {
        return get("app.host");
    }

    /** Configured port; {@code 0} means "choose a free port at runtime". */
    public static int appPort() {
        return getInt("app.port");
    }

    public static String endpointPath() {
        return get("app.endpoint.path");
    }

    public static String apiKey() {
        return get("app.api.key");
    }

    public static Duration appStartupTimeout() {
        return Duration.ofSeconds(getInt("app.startup.timeout.seconds"));
    }

    // ------------------------------------------------------------------
    // External service mock
    // ------------------------------------------------------------------

    /** Configured port; {@code 0} means "choose a free port at runtime". */
    public static int mockPort() {
        return getInt("mock.port");
    }

    // ------------------------------------------------------------------
    // Resolution logic
    // ------------------------------------------------------------------

    static String get(String key) {
        String value = System.getProperty(key);
        if (isBlank(value)) {
            value = System.getenv(toEnvName(key));
        }
        if (isBlank(value)) {
            value = FILE_PROPERTIES.getProperty(key);
        }
        if (isBlank(value)) {
            throw new IllegalStateException(
                    "Config key '%s' is not set (checked -D%s, env %s, %s)"
                            .formatted(key, key, toEnvName(key), PROPERTIES_FILE));
        }
        return value.trim();
    }

    static int getInt(String key) {
        String value = get(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "Config key '%s' must be an integer, but was '%s'".formatted(key, value), e);
        }
    }

    /** {@code app.api.key} -> {@code APP_API_KEY}. */
    static String toEnvName(String key) {
        return key.toUpperCase().replace('.', '_');
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static Properties loadFile() {
        Properties properties = new Properties();
        try (InputStream in = TestConfig.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (in == null) {
                throw new IllegalStateException(PROPERTIES_FILE + " not found on the test classpath");
            }
            properties.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + PROPERTIES_FILE, e);
        }
        return properties;
    }
}
