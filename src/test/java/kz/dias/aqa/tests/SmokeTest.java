package kz.dias.aqa.tests;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import kz.dias.aqa.data.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static kz.dias.aqa.steps.ResponseAssertions.assertSuccess;

@Epic("Сервис авторизации пользователей (/endpoint)")
@Feature("Основной сценарий")
@Severity(SeverityLevel.BLOCKER)
@Tag(Tags.SMOKE)
@DisplayName("Smoke: основной пользовательский сценарий")
class SmokeTest extends BaseApiTest {

    @Test
    @DisplayName("Пользователь входит, выполняет действие и выходит")
    void fullUserFlow() {
        String token = TestData.validToken();

        assertSuccess(api.login(token));
        externalService.verifyAuthCalledOnceWith(token);

        assertSuccess(api.action(token));
        externalService.verifyDoActionCalledOnceWith(token);

        assertSuccess(api.logout(token));
    }
}
