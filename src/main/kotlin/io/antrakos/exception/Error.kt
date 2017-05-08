package io.antrakos.exception

/**
 * @author Taras Zubrei
 */
enum class Error(val message: String, val code: Int) {
    DEFAULT("error.client.default", 400),
    WRONG_ID("error.client.wrong_id", 400),
    UNAUTHORIZED("error.client.unauthorized", 401),
    FORBIDDEN("error.client.forbidden", 403)
}