package io.antrakos.exception

import ratpack.error.ClientErrorHandler
import ratpack.error.ServerErrorHandler
import ratpack.handling.Context
import ratpack.jackson.Jackson.json
import java.util.*

/**
 * @author Taras Zubrei
 */
class ClientErrorHandler(val resourceBundle: ResourceBundle) : ClientErrorHandler {
    override fun error(context: Context, statusCode: Int) {
        val error = when (statusCode) {
            in (1..Error.values().size) -> Error.values()[statusCode]
            401 -> Error.UNAUTHORIZED
            403 -> Error.FORBIDDEN
            else -> Error.DEFAULT
        }
        context.response.apply {
            status(error.code)
            send(resourceBundle.getString(error.message))
        }
    }

}

class ServerErrorHandler : ServerErrorHandler {
    override fun error(context: Context, throwable: Throwable) {
        context.render(json(mapOf("name" to throwable.javaClass.simpleName, "message" to throwable.message)))
    }

}