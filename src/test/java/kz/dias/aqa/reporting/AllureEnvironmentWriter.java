package kz.dias.aqa.reporting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Properties;

/**
 * Fills the "Environment" widget and failure "Categories" of the Allure report.
 * Both are plain files that Allure picks up from the results directory.
 */
public final class AllureEnvironmentWriter {

    private static final Logger log = LoggerFactory.getLogger(AllureEnvironmentWriter.class);
    private static final String RESULTS_DIR_PROPERTY = "allure.results.directory";
    private static final String DEFAULT_RESULTS_DIR = "target/allure-results";

    private AllureEnvironmentWriter() {
    }

    public static void write(Map<String, String> environment) {
        Path resultsDir = Path.of(System.getProperty(RESULTS_DIR_PROPERTY, DEFAULT_RESULTS_DIR));
        try {
            Files.createDirectories(resultsDir);
            writeEnvironment(resultsDir.resolve("environment.properties"), environment);
            copyFromClasspath("categories.json", resultsDir.resolve("categories.json"));
        } catch (IOException e) {
            // the report is nice-to-have: never fail the test run because of it
            log.warn("Cannot write Allure environment info: {}", e.getMessage());
        }
    }

    private static void writeEnvironment(Path file, Map<String, String> environment) throws IOException {
        Properties properties = new Properties();
        properties.putAll(environment);
        try (OutputStream out = Files.newOutputStream(file)) {
            properties.store(out, null);
        }
    }

    private static void copyFromClasspath(String resource, Path target) throws IOException {
        try (InputStream in = AllureEnvironmentWriter.class.getClassLoader().getResourceAsStream(resource)) {
            if (in != null) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
