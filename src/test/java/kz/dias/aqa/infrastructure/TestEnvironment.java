package kz.dias.aqa.infrastructure;

import kz.dias.aqa.config.TestConfig;
import kz.dias.aqa.reporting.AllureEnvironmentWriter;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Everything the tests need running: the external service mock and the application
 * wired to it. Started once per test run, shared by all test classes.
 */
public final class TestEnvironment implements AutoCloseable {

    private static final Path APP_LOG = Path.of("target", "app-under-test.log");

    private final ExternalServiceMock mock;
    private final AppUnderTest app;

    private TestEnvironment(ExternalServiceMock mock, AppUnderTest app) {
        this.mock = mock;
        this.app = app;
    }

    /** Order matters: the mock must be up before the app, because the app gets the mock URL at startup. */
    public static TestEnvironment start() {
        ExternalServiceMock mock = new ExternalServiceMock(NetworkUtils.resolvePort(TestConfig.mockPort()));
        mock.start();

        AppUnderTest app = new AppUnderTest(
                Path.of(TestConfig.appJarPath()),
                TestConfig.appHost(),
                NetworkUtils.resolvePort(TestConfig.appPort()),
                TestConfig.apiKey(),
                mock.baseUrl(),
                TestConfig.appStartupTimeout(),
                APP_LOG);
        try {
            app.start();
        } catch (RuntimeException e) {
            mock.close();   // don't leave the mock running if the app failed to start
            throw e;
        }
        AllureEnvironmentWriter.write(describe(mock, app));
        return new TestEnvironment(mock, app);
    }

    private static Map<String, String> describe(ExternalServiceMock mock, AppUnderTest app) {
        Map<String, String> info = new LinkedHashMap<>();
        info.put("Тестируемое приложение", Path.of(TestConfig.appJarPath()).getFileName().toString());
        info.put("Адрес приложения", app.baseUrl() + TestConfig.endpointPath());
        info.put("Внешний сервис (заглушка WireMock)", mock.baseUrl());
        info.put("Java", System.getProperty("java.version"));
        info.put("ОС", System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        return info;
    }

    public ExternalServiceMock mock() {
        return mock;
    }

    public AppUnderTest app() {
        return app;
    }

    /** Reverse order of start: app first, then its dependency. */
    @Override
    public void close() {
        app.close();
        mock.close();
    }
}
