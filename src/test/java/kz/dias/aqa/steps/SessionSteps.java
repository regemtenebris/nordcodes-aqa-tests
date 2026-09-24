package kz.dias.aqa.steps;

import io.qameta.allure.Step;
import kz.dias.aqa.client.EndpointClient;

import static kz.dias.aqa.steps.ResponseAssertions.assertError;
import static kz.dias.aqa.steps.ResponseAssertions.assertSuccess;

/**
 * Checks whether the application keeps a session for a token.
 * The application has no "get session" endpoint, so we probe it with ACTION:
 * ACTION succeeds only for tokens that are logged in.
 */
public class SessionSteps {

    private final EndpointClient api;

    public SessionSteps(EndpointClient api) {
        this.api = api;
    }

    @Step("Проверить: сессия для токена {token} активна (ACTION выполняется)")
    public void assertActive(String token) {
        assertSuccess(api.action(token));
    }

    @Step("Проверить: сессии для токена {token} нет (ACTION отклоняется)")
    public void assertAbsent(String token) {
        assertError(api.action(token), 403, "not found");
    }
}
