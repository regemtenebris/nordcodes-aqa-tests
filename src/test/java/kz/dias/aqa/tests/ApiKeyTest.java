package kz.dias.aqa.tests;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import kz.dias.aqa.config.TestConfig;
import kz.dias.aqa.data.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static kz.dias.aqa.steps.ResponseAssertions.assertError;
import static kz.dias.aqa.steps.ResponseAssertions.assertSuccess;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@Epic("Сервис авторизации пользователей (/endpoint)")
@Feature("Безопасность")
@Story("API-ключ")
@Severity(SeverityLevel.CRITICAL)
@DisplayName("Безопасность: API-ключ")
class ApiKeyTest extends BaseApiTest {

    private static final String UNAUTHORIZED_MESSAGE = "Missing or invalid API Key";

    static Stream<Arguments> invalidApiKeys() {
        String key = TestConfig.apiKey();
        return Stream.of(
                arguments(Named.of("Заголовок отсутствует", null)),
                arguments(Named.of("Пустое значение", "")),
                arguments(Named.of("Неверный ключ", "wrongKey123")),
                arguments(Named.of("Ключ в другом регистре", key.toUpperCase())),
                arguments(Named.of("Ключ без последнего символа", key.substring(0, key.length() - 1))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidApiKeys")
    @DisplayName("Вход без корректного API-ключа запрещён: сессия не создаётся, внешний сервис не вызывается")
    void loginWithoutValidApiKeyIsUnauthorized(String apiKey) {
        String token = TestData.validToken();

        assertError(api.sendWithApiKey(apiKey, token, "LOGIN"), 401, UNAUTHORIZED_MESSAGE);

        externalService.verifyNoCalls();
        session.assertAbsent(token);
    }

    @Test
    @DisplayName("Действие с неверным API-ключом запрещено даже для вошедшего пользователя")
    void actionWithWrongApiKeyIsUnauthorized() {
        String token = TestData.validToken();
        assertSuccess(api.login(token));

        assertError(api.sendWithApiKey("wrongKey123", token, "ACTION"), 401, UNAUTHORIZED_MESSAGE);

        externalService.verifyDoActionNotCalled();
    }

    @Test
    @DisplayName("Выход с неверным API-ключом запрещён, сессия сохраняется")
    void logoutWithWrongApiKeyIsUnauthorized() {
        String token = TestData.validToken();
        assertSuccess(api.login(token));

        assertError(api.sendWithApiKey("wrongKey123", token, "LOGOUT"), 401, UNAUTHORIZED_MESSAGE);

        session.assertActive(token);
    }
}
