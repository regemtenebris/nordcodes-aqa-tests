package kz.dias.aqa.tests;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import kz.dias.aqa.data.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static kz.dias.aqa.steps.ResponseAssertions.assertError;
import static kz.dias.aqa.steps.ResponseAssertions.assertSuccess;

@Epic("Сервис авторизации пользователей (/endpoint)")
@Feature("Выход (LOGOUT)")
@Severity(SeverityLevel.CRITICAL)
@DisplayName("LOGOUT: выход пользователя")
class LogoutTest extends BaseApiTest {

    @Test
    @DisplayName("Успешный выход завершает сессию, внешний сервис не вызывается")
    void successfulLogout() {
        String token = TestData.validToken();
        assertSuccess(api.login(token));

        assertSuccess(api.logout(token));

        externalService.verifyDoActionNotCalled();
        externalService.verifyAuthCalledOnceWith(token);   // only the call made by LOGIN
        session.assertAbsent(token);
    }

    @Test
    @DisplayName("Выход без входа отклоняется")
    void logoutWithoutLoginIsRejected() {
        assertError(api.logout(TestData.validToken()), 403, "not found");

        externalService.verifyNoCalls();
    }

    @Test
    @DisplayName("Повторный выход отклоняется")
    void repeatedLogoutIsRejected() {
        String token = TestData.validToken();
        assertSuccess(api.login(token));
        assertSuccess(api.logout(token));

        assertError(api.logout(token), 403, "not found");
    }

    @Test
    @DisplayName("После выхода можно снова войти с тем же токеном")
    void loginAgainAfterLogout() {
        String token = TestData.validToken();
        assertSuccess(api.login(token));
        assertSuccess(api.logout(token));

        assertSuccess(api.login(token));

        session.assertActive(token);
    }

    @Test
    @DisplayName("Выход работает, даже если внешний сервис недоступен")
    void logoutWorksWhenExternalServiceIsDown() {
        String token = TestData.validToken();
        assertSuccess(api.login(token));
        externalService.authIsUnavailable();
        externalService.doActionIsUnavailable();

        assertSuccess(api.logout(token));
    }
}
