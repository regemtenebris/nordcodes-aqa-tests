package kz.dias.aqa.extensions;

import kz.dias.aqa.infrastructure.TestEnvironment;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * JUnit 5 extension that starts {@link TestEnvironment} ONCE for the whole test run
 * and stops it after the last test.
 *
 * <p>How it works: the environment is kept in the <b>root</b> {@link ExtensionContext.Store}.
 * The root context lives for the entire run, so every test class gets the same instance.
 * When the run finishes JUnit closes the store and calls {@link TestEnvironment#close()}
 * because it implements {@link AutoCloseable}.
 *
 * <p>It is also a {@link ParameterResolver}: any test/lifecycle method can simply declare
 * a {@code TestEnvironment} parameter and JUnit injects it.
 */
public class TestEnvironmentExtension implements BeforeAllCallback, ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(TestEnvironmentExtension.class);
    private static final String KEY = "test-environment";

    @Override
    public void beforeAll(ExtensionContext context) {
        getOrStart(context);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType() == TestEnvironment.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return getOrStart(extensionContext);
    }

    /** The environment if it has already been started in this run, otherwise {@code null}. */
    public static TestEnvironment findRunning(ExtensionContext context) {
        return context.getRoot().getStore(NAMESPACE).get(KEY, TestEnvironment.class);
    }

    private static TestEnvironment getOrStart(ExtensionContext context) {
        return context.getRoot()
                .getStore(NAMESPACE)
                .getOrComputeIfAbsent(KEY, key -> TestEnvironment.start(), TestEnvironment.class);
    }
}
