package kz.dias.aqa.tests;

import kz.dias.aqa.data.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static kz.dias.aqa.steps.ResponseAssertions.assertError;
import static kz.dias.aqa.steps.ResponseAssertions.assertRejected;
import static kz.dias.aqa.steps.ResponseAssertions.assertSuccess;

@DisplayName("ACTION: действие пользователя")
class ActionTest extends BaseApiTest {

    @Test
    @DisplayName("Вошедший пользователь выполняет действие, приложение передаёт его во внешний сервис")
    void actionAfterLogin() {
        String token = TestData.validToken();
        assertSuccess(api.login(token));

        assertSuccess(api.action(token));

        externalService.verifyDoActionCalledOnceWith(token);
    }

    @Test
    @DisplayName("Вошедший пользователь может выполнить действие несколько раз подряд")
    void actionCanBeRepeated() {
        String token = TestData.validToken();
        assertSuccess(api.login(token));

        assertSuccess(api.action(token));
        assertSuccess(api.action(token));
        assertSuccess(api.action(token));

        externalService.verifyDoActionCalledTimes(3);
    }

    @Test
    @DisplayName("Действие без входа запрещено, внешний сервис не вызывается")
    void actionWithoutLoginIsForbidden() {
        String token = TestData.validToken();

        assertError(api.action(token), 403, "not found");

        externalService.verifyNoCalls();
    }

    @Test
    @DisplayName("Действие после выхода запрещено")
    void actionAfterLogoutIsForbidden() {
        String token = TestData.validToken();
        assertSuccess(api.login(token));
        assertSuccess(api.logout(token));

        assertError(api.action(token), 403, "not found");

        externalService.verifyDoActionNotCalled();
    }

    @Test
    @DisplayName("Вход одного пользователя не даёт права на действия другому")
    void sessionsAreIsolatedBetweenTokens() {
        String loggedIn = TestData.validToken();
        String stranger = TestData.validToken();
        assertSuccess(api.login(loggedIn));

        assertError(api.action(stranger), 403, "not found");

        externalService.verifyDoActionNotCalled();
    }

    @ParameterizedTest(name = "Внешний сервис ответил HTTP {0}")
    @ValueSource(ints = {400, 404, 500, 503})
    @DisplayName("Ошибка внешнего сервиса при действии не завершает сессию пользователя")
    void failedActionKeepsSession(int doActionStatus) {
        String token = TestData.validToken();
        assertSuccess(api.login(token));
        externalService.doActionRespondsWith(doActionStatus);

        assertRejected(api.action(token));

        externalService.doActionRespondsWith(200);
        session.assertActive(token);
    }

    @Test
    @DisplayName("Действие не выполняется, если внешний сервис недоступен")
    void actionFailsWhenDoActionIsUnavailable() {
        String token = TestData.validToken();
        assertSuccess(api.login(token));
        externalService.doActionIsUnavailable();

        assertRejected(api.action(token));
    }
}
