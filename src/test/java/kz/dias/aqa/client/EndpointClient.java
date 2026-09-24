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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
    /** Same as {@link #spec} but without the Allure filter - for requests sent from background threads. */
    private final RequestSpecification quietSpec;

    public EndpointClient(String baseUrl) {
        this.quietSpec = baseSpec(baseUrl).build();
        this.spec = baseSpec(baseUrl)
                // attaches every request/response to the Allure report
                .addFilter(new AllureRestAssured())
                .build();
    }

    private static RequestSpecBuilder baseSpec(String baseUrl) {
        return new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .setBasePath(TestConfig.endpointPath())
                .setContentType(ContentType.URLENC)
                .setAccept(ContentType.JSON)
                .setConfig(RestAssuredConfig.config().encoderConfig(EncoderConfig.encoderConfig()
                        // send exactly "application/x-www-form-urlencoded", like the spec says (no "; charset=...")
                        .appendDefaultContentCharsetToContentTypeIfUndefined(false)
                        // encode non-ASCII form values (e.g. Cyrillic) as UTF-8
                        .defaultCharsetForContentType("UTF-8", ContentType.URLENC)));
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

    /**
     * Sends {@code count} identical requests at the same moment and returns their HTTP status codes.
     * A latch releases all threads simultaneously to maximise the chance of a race condition.
     */
    @Step("Отправить {count} одновременных запросов {action} с токеном {token}")
    public List<Integer> sendConcurrently(String token, Action action, int count) {
        ExecutorService pool = Executors.newFixedThreadPool(count);
        CountDownLatch startGate = new CountDownLatch(1);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                futures.add(pool.submit(() -> {
                    startGate.await();
                    return given().spec(quietSpec)
                            .header(API_KEY_HEADER, TestConfig.apiKey())
                            .formParam(TOKEN_PARAM, token)
                            .formParam(ACTION_PARAM, action.name())
                            .post()
                            .statusCode();
                }));
            }
            startGate.countDown();   // "На старт, внимание, марш!"
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get());
            }
            return statuses;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while sending concurrent requests", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Concurrent request failed", e.getCause());
        } finally {
            pool.shutdownNow();
        }
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
