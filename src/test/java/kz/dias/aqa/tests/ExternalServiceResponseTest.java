package kz.dias.aqa.tests;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
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
@Epic("Сервис авторизации пользователей (/endpoint)")
@Feature("Интеграция с внешним сервисом")
@Story("Нестандартные ответы внешнего сервиса")
@Severity(SeverityLevel.NORMAL)
@Tag(Tags.KNOWN_BUG)
@DisplayName("Обработка ответов внешнего сервиса")
class ExternalServiceResponseTest extends BaseApiTest {

    @ParameterizedTest(name = "Внешний сервис ответил HTTP {0}")
    @ValueSource(ints = {301, 302, 307})
    @Issue("2")
    @Description("""
            Ответ-перенаправление (3xx) от внешнего сервиса не означает, что вход подтверждён.
            Сейчас приложение считает такой ответ успехом и создаёт сессию — пользователь
            получает доступ без реального подтверждения.""")
    @DisplayName("[BUG-2] Перенаправление (3xx) от /auth не считается успешным входом")
    void redirectFromAuthIsNotSuccess(int status) {
        String token = TestData.validToken();
        externalService.authRespondsWith(status);

        assertRejected(api.login(token));

        session.assertAbsent(token);
    }

    @ParameterizedTest(name = "Внешний сервис ответил HTTP {0}")
    @ValueSource(ints = {301, 302, 307})
    @Issue("2")
    @Description("""
            Ответ-перенаправление (3xx) от внешнего сервиса не означает, что действие выполнено.
            Сейчас приложение сообщает пользователю об успехе.""")
    @DisplayName("[BUG-2] Перенаправление (3xx) от /doAction не считается успешным действием")
    void redirectFromDoActionIsNotSuccess(int status) {
        String token = TestData.validToken();
        assertSuccess(api.login(token));
        externalService.doActionRespondsWith(status);

        assertRejected(api.action(token));
    }

    @Test
    @Issue("3")
    @Description("""
            Когда внешний сервис отвечает ошибкой, приложение возвращает «500 Internal Server Error» —
            как будто сломалось оно само. Правильнее «502 Bad Gateway»: это помогает быстро понять,
            что проблема во внешней системе. Документация этот случай не описывает, ожидание основано
            на стандарте HTTP (RFC 9110).""")
    @DisplayName("[BUG-3] Сбой внешнего сервиса возвращается как 502 Bad Gateway, а не как внутренняя ошибка 500")
    void externalFailureIsReportedAsBadGateway() {
        externalService.authRespondsWith(500);

        assertError(api.login(TestData.validToken()), 502);
    }
}
