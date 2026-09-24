package kz.dias.aqa.tests;

import kz.dias.aqa.data.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static kz.dias.aqa.steps.ResponseAssertions.assertError;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Валидация действия (action)")
class ActionValidationTest extends BaseApiTest {

    static Stream<Arguments> invalidActions() {
        return Stream.of(
                arguments(Named.of("Неизвестное действие", "DELETE")),
                arguments(Named.of("Нижний регистр", "login")),
                arguments(Named.of("Смешанный регистр", "Login")),
                arguments(Named.of("Пустая строка", "")),
                arguments(Named.of("Параметр отсутствует", null)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidActions")
    @DisplayName("Некорректное действие отклоняется, внешний сервис не вызывается")
    void invalidActionIsRejected(String action) {
        assertError(api.send(TestData.validToken(), action), 400, "action");

        externalService.verifyNoCalls();
    }
}
