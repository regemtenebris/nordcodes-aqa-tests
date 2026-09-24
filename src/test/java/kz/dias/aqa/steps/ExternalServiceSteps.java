package kz.dias.aqa.steps;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import io.qameta.allure.Step;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

/**
 * Controls and inspects the external service mock in business terms.
 * <ul>
 *   <li><b>Stubbing</b> - "the external service answers X";</li>
 *   <li><b>Verification</b> - "the application sent exactly this request".</li>
 * </ul>
 */
public class ExternalServiceSteps {

    public static final String AUTH = "/auth";
    public static final String DO_ACTION = "/doAction";

    private final WireMockServer server;

    public ExternalServiceSteps(WireMockServer server) {
        this.server = server;
    }

    // ------------------------------------------------------------------
    // Stubbing (the newest stub for a URL wins)
    // ------------------------------------------------------------------

    @Step("Внешний сервис: /auth отвечает HTTP {status}")
    public void authRespondsWith(int status) {
        stubStatus(AUTH, status);
    }

    @Step("Внешний сервис: /doAction отвечает HTTP {status}")
    public void doActionRespondsWith(int status) {
        stubStatus(DO_ACTION, status);
    }

    @Step("Внешний сервис: /auth недоступен (соединение обрывается)")
    public void authIsUnavailable() {
        stubFault(AUTH);
    }

    @Step("Внешний сервис: /doAction недоступен (соединение обрывается)")
    public void doActionIsUnavailable() {
        stubFault(DO_ACTION);
    }

    private void stubStatus(String path, int status) {
        server.stubFor(post(urlEqualTo(path)).willReturn(aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody("{}")));
    }

    private void stubFault(String path) {
        server.stubFor(post(urlEqualTo(path)).willReturn(aResponse()
                .withFault(Fault.CONNECTION_RESET_BY_PEER)));
    }

    // ------------------------------------------------------------------
    // Verification
    // ------------------------------------------------------------------

    @Step("Проверить: приложение отправило ровно 1 запрос на /auth с токеном {token}")
    public void verifyAuthCalledOnceWith(String token) {
        server.verify(exactly(1), expectedRequest(AUTH, token));
        server.verify(exactly(1), postRequestedFor(urlEqualTo(AUTH)));
    }

    @Step("Проверить: приложение отправило ровно 1 запрос на /doAction с токеном {token}")
    public void verifyDoActionCalledOnceWith(String token) {
        server.verify(exactly(1), expectedRequest(DO_ACTION, token));
        server.verify(exactly(1), postRequestedFor(urlEqualTo(DO_ACTION)));
    }

    @Step("Проверить: приложение отправило {times} запрос(ов) на /doAction")
    public void verifyDoActionCalledTimes(int times) {
        server.verify(exactly(times), postRequestedFor(urlEqualTo(DO_ACTION)));
    }

    @Step("Проверить: приложение НЕ обращалось к /auth")
    public void verifyAuthNotCalled() {
        server.verify(exactly(0), postRequestedFor(urlEqualTo(AUTH)));
    }

    @Step("Проверить: приложение НЕ обращалось к /doAction")
    public void verifyDoActionNotCalled() {
        server.verify(exactly(0), postRequestedFor(urlEqualTo(DO_ACTION)));
    }

    @Step("Проверить: приложение НЕ обращалось к внешнему сервису")
    public void verifyNoCalls() {
        server.verify(exactly(0), anyRequestedFor(anyUrl()));
    }

    /** Full contract of an outgoing request, as described in the task documentation. */
    private static RequestPatternBuilder expectedRequest(String path, String token) {
        return postRequestedFor(urlEqualTo(path))
                .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
                .withHeader("Accept", equalTo("application/json"))
                .withRequestBody(equalTo("token=" + token));
    }
}
