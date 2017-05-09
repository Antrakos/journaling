package io.antrakos.exception

/**
 * @author Taras Zubrei
 */
enum class Error(val message: String, val code: Int) {
    DEFAULT("error.client.default", 400),
    WRONG_ID("error.client.wrong_id", 406),
    DUPLICATE_USERNAME("error.client.duplicate", 406),
    NOT_FOUND("error.client.not-found", 404),
    UNAUTHORIZED("error.client.unauthorized", 401),
    FORBIDDEN("error.client.forbidden", 403)
}