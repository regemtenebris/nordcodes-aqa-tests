package kz.dias.aqa.data;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Test data generation.
 *
 * <p>Every test uses its own random token, so tests never share state inside the application
 * and can run in any order without restarting it.
 */
public final class TestData {

    public static final int TOKEN_LENGTH = 32;

    public static final String DIGITS = "0123456789";
    public static final String HEX_LETTERS = "ABCDEF";
    /** Letters that the documentation allows (A-Z) but the application rejects (see BUG-1). */
    public static final String NON_HEX_LETTERS = "GHIJKLMNOPQRSTUVWXYZ";

    private TestData() {
    }

    /** A token the application accepts: 32 chars of [0-9A-F]. */
    public static String validToken() {
        return randomString(DIGITS + HEX_LETTERS, TOKEN_LENGTH);
    }

    public static String randomString(String alphabet, int length) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}
