package kz.dias.aqa.tests;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import kz.dias.aqa.data.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static kz.dias.aqa.steps.ResponseAssertions.assertError;
import static kz.dias.aqa.steps.ResponseAssertions.assertRejected;
import static kz.dias.aqa.steps.ResponseAssertions.assertSuccess;

@Epic("Сервис авторизации пользователей (/endpoint)")
@Feature("Вход (LOGIN)")
@Severity(SeverityLevel.CRITICAL)
@DisplayName("LOGIN: вход пользователя")
class LoginTest extends BaseApiTest {

    @Test
    @DisplayName("Успешный вход: приложение подтверждает вход у внешнего сервиса и создаёт сессию")
    void successfulLogin() {
        String token = TestData.validToken();

        assertSuccess(api.login(token));

        externalService.verifyAuthCalledOnceWith(token);
        externalService.verifyDoActionNotCalled();
        session.assertActive(token);
    }

    @Test
    @DisplayName("Повторный вход с тем же токеном отклоняется, внешний сервис повторно не вызывается")
    void repeatedLoginIsRejected() {
        String token = TestData.validToken();
        assertSuccess(api.login(token));

        assertError(api.login(token), 409, "already exists");

        externalService.verifyAuthCalledOnceWith(token);
        session.assertActive(token);
    }

    @ParameterizedTest(name = "Внешний сервис ответил HTTP {0}")
    @ValueSource(ints = {400, 401, 403, 404, 500, 503})
    @DisplayName("Вход не выполняется, если внешний сервис вернул ошибку")
    void loginFailsWhenAuthReturnsError(int authStatus) {
        String token = TestData.validToken();
        externalService.authRespondsWith(authStatus);

        assertRejected(api.login(token));

        externalService.verifyAuthCalledOnceWith(token);
        session.assertAbsent(token);
    }

    @Test
    @DisplayName("Вход не выполняется, если внешний сервис недоступен")
    void loginFailsWhenAuthIsUnavailable() {
        String token = TestData.validToken();
        externalService.authIsUnavailable();

        assertRejected(api.login(token));

        session.assertAbsent(token);
    }

    @Test
    @DisplayName("После неудачной попытки входа можно войти снова")
    void loginRetryAfterFailureSucceeds() {
        String token = TestData.validToken();
        externalService.authRespondsWith(500);
        assertRejected(api.login(token));

        externalService.authRespondsWith(200);
        assertSuccess(api.login(token));

        session.assertActive(token);
    }
}
