package kz.dias.aqa.tests;

import kz.dias.aqa.data.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static kz.dias.aqa.data.TestData.DIGITS;
import static kz.dias.aqa.data.TestData.HEX_LETTERS;
import static kz.dias.aqa.data.TestData.NON_HEX_LETTERS;
import static kz.dias.aqa.data.TestData.TOKEN_LENGTH;
import static kz.dias.aqa.data.TestData.randomString;
import static kz.dias.aqa.steps.ResponseAssertions.assertError;
import static kz.dias.aqa.steps.ResponseAssertions.assertSuccess;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Validation of the {@code token} parameter.
 * Technique: equivalence partitioning + boundary values (length 31 / 32 / 33).
 */
@DisplayName("Валидация токена")
class TokenValidationTest extends BaseApiTest {

    static Stream<Arguments> validTokens() {
        return Stream.of(
                arguments(Named.of("Только цифры", randomString(DIGITS, TOKEN_LENGTH))),
                arguments(Named.of("Только буквы A–F", randomString(HEX_LETTERS, TOKEN_LENGTH))),
                arguments(Named.of("Цифры и буквы", TestData.validToken())));
    }

    static Stream<Arguments> invalidTokens() {
        String valid = TestData.validToken();
        return Stream.of(
                arguments(Named.of("Пустая строка", "")),
                arguments(Named.of("1 символ", "A")),
                arguments(Named.of("31 символ (на 1 меньше нормы)", valid.substring(1))),
                arguments(Named.of("33 символа (на 1 больше нормы)", valid + "A")),
                arguments(Named.of("64 символа", valid + valid)),
                arguments(Named.of("Строчные буквы", randomString("0123456789abcdef", TOKEN_LENGTH - 1) + "a")),
                arguments(Named.of("Спецсимвол", valid.substring(1) + "_")),
                arguments(Named.of("Пробел внутри", valid.substring(0, 16) + " " + valid.substring(17))),
                arguments(Named.of("Кириллица", valid.substring(1) + "Ж")),
                arguments(Named.of("SQL-инъекция", "' OR '1'='1' --" + valid.substring(15))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validTokens")
    @DisplayName("Корректный токен (32 символа 0–9, A–F) принимается")
    void validTokenIsAccepted(String token) {
        assertSuccess(api.login(token));

        externalService.verifyAuthCalledOnceWith(token);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidTokens")
    @DisplayName("Некорректный токен отклоняется, внешний сервис не вызывается")
    void invalidTokenIsRejected(String token) {
        assertError(api.login(token), 400, "token");

        externalService.verifyNoCalls();
    }

    @Test
    @DisplayName("Запрос без токена отклоняется")
    void missingTokenIsRejected() {
        assertError(api.send(null, "LOGIN"), 400, "token");

        externalService.verifyNoCalls();
    }

    @Test
    @Tag(Tags.KNOWN_BUG)
    @DisplayName("[BUG-1] Токен с буквами G–Z принимается (по документации алфавит A–Z0–9)")
    void tokenWithAnyLatinLetterIsAccepted() {
        String token = randomString(NON_HEX_LETTERS, TOKEN_LENGTH);

        assertSuccess(api.login(token));
    }
}
