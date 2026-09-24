package kz.dias.aqa.tests;

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
@Tag(Tags.KNOWN_BUG)
@DisplayName("[BUG-4] Некорректные HTTP-запросы")
class ProtocolTest extends BaseApiTest {

    @Test
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
