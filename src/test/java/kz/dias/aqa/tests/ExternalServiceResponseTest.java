package kz.dias.aqa.tests;

import kz.dias.aqa.data.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static kz.dias.aqa.steps.ResponseAssertions.assertError;
import static kz.dias.aqa.steps.ResponseAssertions.assertRejected;
import static kz.dias.aqa.steps.ResponseAssertions.assertSuccess;

/**
 * How the application interprets unusual answers of the external service.
 * These tests reproduce defects found during exploratory testing (see docs/test-plan.md).
 */
@Tag(Tags.KNOWN_BUG)
@DisplayName("Обработка ответов внешнего сервиса")
class ExternalServiceResponseTest extends BaseApiTest {

    @ParameterizedTest(name = "Внешний сервис ответил HTTP {0}")
    @ValueSource(ints = {301, 302, 307})
    @DisplayName("[BUG-2] Перенаправление (3xx) от /auth не считается успешным входом")
    void redirectFromAuthIsNotSuccess(int status) {
        String token = TestData.validToken();
        externalService.authRespondsWith(status);

        assertRejected(api.login(token));

        session.assertAbsent(token);
    }

    @ParameterizedTest(name = "Внешний сервис ответил HTTP {0}")
    @ValueSource(ints = {301, 302, 307})
    @DisplayName("[BUG-2] Перенаправление (3xx) от /doAction не считается успешным действием")
    void redirectFromDoActionIsNotSuccess(int status) {
        String token = TestData.validToken();
        assertSuccess(api.login(token));
        externalService.doActionRespondsWith(status);

        assertRejected(api.action(token));
    }

    @Test
    @DisplayName("[BUG-3] Сбой внешнего сервиса возвращается как 502 Bad Gateway, а не как внутренняя ошибка 500")
    void externalFailureIsReportedAsBadGateway() {
        externalService.authRespondsWith(500);

        assertError(api.login(TestData.validToken()), 502);
    }
}
