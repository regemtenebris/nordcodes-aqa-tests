package kz.dias.aqa.tests;

/**
 * JUnit 5 tags. Used to select/skip groups of tests:
 * <pre>
 *   ./mvnw test -Dgroups=smoke                 # only smoke tests
 *   ./mvnw test -DexcludedGroups=known-bug     # everything except tests for already reported bugs
 * </pre>
 */
public final class Tags {

    /** Test that reproduces a known, reported defect. Expected to fail until the defect is fixed. */
    public static final String KNOWN_BUG = "known-bug";
    public static final String SMOKE = "smoke";

    private Tags() {
    }
}
