package kz.dias.aqa.steps;

import io.qameta.allure.Step;
import io.restassured.response.Response;
import kz.dias.aqa.model.ApiResponse;
import kz.dias.aqa.model.Result;
import org.assertj.core.api.SoftAssertions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reusable checks of {@code /endpoint} responses.
 *
 * <p>Soft assertions: all mismatches (status, content type, body) are collected and reported
 * together, instead of stopping at the first one.
 */
public final class ResponseAssertions {

    private static final int HTTP_OK = 200;

    private ResponseAssertions() {
    }

    @Step("Проверить: запрос выполнен успешно (HTTP 200, result = OK)")
    public static void assertSuccess(Response response) {
        ApiResponse body = parse(response);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(response.statusCode()).as("HTTP-статус").isEqualTo(HTTP_OK);
            soft.assertThat(response.contentType()).as("Content-Type").startsWith("application/json");
            soft.assertThat(body.result()).as("Поле result").isEqualTo(Result.OK);
            soft.assertThat(body.message()).as("Поле message").isNull();
        });
    }

    @Step("Проверить: запрос отклонён (HTTP {expectedStatus}, result = ERROR)")
    public static ApiResponse assertError(Response response, int expectedStatus) {
        ApiResponse body = parse(response);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(response.statusCode()).as("HTTP-статус").isEqualTo(expectedStatus);
            soft.assertThat(response.contentType()).as("Content-Type").startsWith("application/json");
            soft.assertThat(body.result()).as("Поле result").isEqualTo(Result.ERROR);
            soft.assertThat(body.message()).as("Поле message").isNotBlank();
        });
        return body;
    }

    @Step("Проверить: запрос отклонён (HTTP {expectedStatus}, result = ERROR, сообщение содержит \"{expectedMessagePart}\")")
    public static void assertError(Response response, int expectedStatus, String expectedMessagePart) {
        ApiResponse body = assertError(response, expectedStatus);
        assertThat(body.message()).as("Текст ошибки").contains(expectedMessagePart);
    }

    private static ApiResponse parse(Response response) {
        try {
            return response.as(ApiResponse.class);
        } catch (RuntimeException e) {
            throw new AssertionError("Тело ответа не соответствует контракту {result, message}: "
                    + response.asString(), e);
        }
    }
}
