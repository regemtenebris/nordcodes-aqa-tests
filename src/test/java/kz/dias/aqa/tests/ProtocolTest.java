package kz.dias.aqa.tests;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.http.ContentType;
import kz.dias.aqa.data.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static kz.dias.aqa.steps.ResponseAssertions.assertError;

/**
 * Requests that do not follow the protocol. Expected statuses follow HTTP semantics (RFC 9110),
 * because the task documentation does not describe these cases.
 */
@Epic("Сервис авторизации пользователей (/endpoint)")
@Feature("Протокол HTTP")
@Story("Некорректные запросы")
@Severity(SeverityLevel.MINOR)
@Tag(Tags.KNOWN_BUG)
@Issue("4")
@DisplayName("[BUG-4] Некорректные HTTP-запросы")
class ProtocolTest extends BaseApiTest {

    @Test
    @Description("Эндпоинт принимает только POST. На запрос методом GET приложение должно ответить «405 Method Not Allowed», а сейчас отвечает «500 Internal Server Error» — клиенту непонятно, что он сделал не так.")
    @DisplayName("Метод GET вместо POST отклоняется с кодом 405 Method Not Allowed")
    void getMethodIsNotAllowed() {
        var response = api.request()
                .queryParam("token", TestData.validToken())
                .queryParam("action", "LOGIN")
                .get();

        assertError(response, 405);
        externalService.verifyNoCalls();
    }

    @Test
    @Description("Эндпоинт принимает данные только в формате формы (x-www-form-urlencoded). На JSON приложение должно ответить «415 Unsupported Media Type», а сейчас отвечает «500 Internal Server Error».")
    @DisplayName("Тело в формате JSON отклоняется с кодом 415 Unsupported Media Type")
    void jsonBodyIsUnsupported() {
        var response = api.request()
                .contentType(ContentType.JSON)
                .body(Map.of("token", TestData.validToken(), "action", "LOGIN"))
                .post();

        assertError(response, 415);
        externalService.verifyNoCalls();
    }

    @Test
    @Description("На запрос к несуществующему адресу приложение должно ответить «404 Not Found», а сейчас отвечает «500 Internal Server Error».")
    @DisplayName("Запрос на несуществующий адрес отклоняется с кодом 404 Not Found")
    void unknownPathIsNotFound() {
        var response = api.request()
                .basePath("/unknown")
                .formParam("token", TestData.validToken())
                .formParam("action", "LOGIN")
                .post();

        assertError(response, 404);
        externalService.verifyNoCalls();
    }
}
