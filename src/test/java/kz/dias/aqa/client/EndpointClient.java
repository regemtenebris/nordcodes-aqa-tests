package kz.dias.aqa.client;

import io.qameta.allure.Step;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.EncoderConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import kz.dias.aqa.config.TestConfig;
import kz.dias.aqa.model.Action;

import static io.restassured.RestAssured.given;

/**
 * API client for {@code POST /endpoint}.
 *
 * <p>Tests talk to the application ONLY through this class. If the API changes
 * (header name, path, parameter names) - we fix it here, not in 50 tests.
 *
 * <p>Methods return the raw {@link Response}: the client sends requests, it does not assert.
 * Checking is the job of the test (see {@code ResponseAssertions}).
 */
public class EndpointClient {

    public static final String API_KEY_HEADER = "X-Api-Key";
    public static final String TOKEN_PARAM = "token";
    public static final String ACTION_PARAM = "action";

    private final RequestSpecification spec;

    public EndpointClient(String baseUrl) {
        this.spec = new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .setBasePath(TestConfig.endpointPath())
                .setContentType(ContentType.URLENC)
                .setAccept(ContentType.JSON)
                // send exactly "application/x-www-form-urlencoded", like the spec says (no "; charset=...")
                .setConfig(RestAssuredConfig.config().encoderConfig(
                        EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
                // attaches every request/response to the Allure report
                .addFilter(new AllureRestAssured())
                .build();
    }

    // ------------------------------------------------------------------
    // Business-level methods (valid API key)
    // ------------------------------------------------------------------

    @Step("Отправить LOGIN с токеном {token}")
    public Response login(String token) {
        return doSend(TestConfig.apiKey(), token, Action.LOGIN.name());
    }

    @Step("Отправить ACTION с токеном {token}")
    public Response action(String token) {
        return doSend(TestConfig.apiKey(), token, Action.ACTION.name());
    }

    @Step("Отправить LOGOUT с токеном {token}")
    public Response logout(String token) {
        return doSend(TestConfig.apiKey(), token, Action.LOGOUT.name());
    }

    // ------------------------------------------------------------------
    // Low-level methods for negative tests
    // ------------------------------------------------------------------

    /** Any token/action; {@code null} means "do not send this parameter at all". */
    @Step("Отправить запрос: token = {token}, action = {action}")
    public Response send(String token, String action) {
        return doSend(TestConfig.apiKey(), token, action);
    }

    /** Custom API key; {@code null} means "do not send the X-Api-Key header". */
    @Step("Отправить запрос с API-ключом \"{apiKey}\": token = {token}, action = {action}")
    public Response sendWithApiKey(String apiKey, String token, String action) {
        return doSend(apiKey, token, action);
    }

    /** Pre-configured request with a valid key, for protocol-level checks (other methods, content types). */
    public RequestSpecification request() {
        return given().spec(spec).header(API_KEY_HEADER, TestConfig.apiKey());
    }

    private Response doSend(String apiKey, String token, String action) {
        RequestSpecification request = given().spec(spec);
        if (apiKey != null) {
            request.header(API_KEY_HEADER, apiKey);
        }
        if (token != null) {
            request.formParam(TOKEN_PARAM, token);
        }
        if (action != null) {
            request.formParam(ACTION_PARAM, action);
        }
        return request.post();
    }
}
