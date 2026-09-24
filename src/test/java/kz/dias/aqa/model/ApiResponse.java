package kz.dias.aqa.model;

/**
 * Body of every {@code /endpoint} response.
 * <pre>
 * {"result": "OK"}
 * {"result": "ERROR", "message": "reason"}
 * </pre>
 * A Java record: immutable, equals/hashCode/toString for free, Jackson maps it out of the box.
 * Unknown JSON fields make parsing fail - that is intentional, it is a contract check.
 */
public record ApiResponse(Result result, String message) {
}
