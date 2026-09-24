package kz.dias.aqa.tests;

import kz.dias.aqa.client.EndpointClient;
import kz.dias.aqa.extensions.TestEnvironmentExtension;
import kz.dias.aqa.infrastructure.TestEnvironment;
import kz.dias.aqa.steps.ExternalServiceSteps;
import kz.dias.aqa.steps.SessionSteps;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Parent of all API test classes.
 * <ul>
 *   <li>makes sure the environment (mock + app) is running;</li>
 *   <li>resets the mock before every test so tests never influence each other;</li>
 *   <li>by default the external service answers 200 (happy path) - tests override when needed.</li>
 * </ul>
 */
@ExtendWith(TestEnvironmentExtension.class)
public abstract class BaseApiTest {

    protected static TestEnvironment env;

    protected EndpointClient api;
    protected ExternalServiceSteps externalService;
    protected SessionSteps session;

    @BeforeAll
    static void initEnvironment(TestEnvironment environment) {   // injected by the extension
        env = environment;
    }

    @BeforeEach
    void setUp() {
        env.mock().server().resetAll();   // removes stubs AND the recorded request journal

        api = new EndpointClient(env.app().baseUrl());
        externalService = new ExternalServiceSteps(env.mock().server());
        session = new SessionSteps(api);

        externalService.authRespondsWith(200);
        externalService.doActionRespondsWith(200);
    }
}
