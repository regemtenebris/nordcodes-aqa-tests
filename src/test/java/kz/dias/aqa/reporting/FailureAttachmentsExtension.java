package kz.dias.aqa.reporting;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.qameta.allure.Allure;
import kz.dias.aqa.extensions.TestEnvironmentExtension;
import kz.dias.aqa.infrastructure.TestEnvironment;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * When a test fails, attaches to the Allure report everything needed to investigate it
 * without re-running: what the application sent to the external service, and the application log.
 */
public class FailureAttachmentsExtension implements AfterTestExecutionCallback {

    private static final int LOG_TAIL_LINES = 100;

    @Override
    public void afterTestExecution(ExtensionContext context) {
        TestEnvironment environment = TestEnvironmentExtension.findRunning(context);
        if (context.getExecutionException().isEmpty() || environment == null) {
            return;
        }
        attachExternalServiceRequests(environment.mock().server());
        attachAppLogTail(environment.app().logFile());
    }

    private static void attachExternalServiceRequests(WireMockServer server) {
        List<LoggedRequest> requests = server.findAll(
                com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor(
                        com.github.tomakehurst.wiremock.client.WireMock.anyUrl()));
        String text = requests.isEmpty()
                ? "Приложение не отправляло запросов во внешний сервис"
                : requests.stream()
                        .map(r -> "%s %s%nContent-Type: %s%nAccept: %s%n%n%s".formatted(
                                r.getMethod(), r.getUrl(),
                                r.getHeader("Content-Type"), r.getHeader("Accept"),
                                r.getBodyAsString()))
                        .collect(Collectors.joining("\n\n----------\n\n"));
        Allure.addAttachment("Запросы приложения во внешний сервис", "text/plain", text, ".txt");
    }

    private static void attachAppLogTail(Path logFile) {
        try {
            List<String> lines = Files.readAllLines(logFile);
            String tail = String.join("\n", lines.subList(Math.max(0, lines.size() - LOG_TAIL_LINES), lines.size()));
            Allure.addAttachment("Лог приложения (последние " + LOG_TAIL_LINES + " строк)", "text/plain", tail, ".log");
        } catch (IOException e) {
            Allure.addAttachment("Лог приложения", "Недоступен: " + e.getMessage());
        }
    }
}
