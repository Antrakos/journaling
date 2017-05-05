package io.antrakos.repository

import com.fasterxml.jackson.databind.ObjectMapper
import io.antrakos.Entity
import org.bson.codecs.Codec
import org.bson.codecs.configuration.CodecProvider
import org.bson.codecs.configuration.CodecRegistry

/**
 * @author Taras Zubrei
 */
class JacksonCodecProvider(val objectMapper: ObjectMapper) : CodecProvider {
    override fun <T : Any?> get(clazz: Class<T>, registry: CodecRegistry?): Codec<T> {
        return JacksonCodec<Entity>(objectMapper, clazz as Class<Entity>) as Codec<T>
    }

}