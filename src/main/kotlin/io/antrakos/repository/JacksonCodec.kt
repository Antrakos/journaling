package io.antrakos.repository

import com.fasterxml.jackson.databind.ObjectMapper
import io.antrakos.Entity
import org.bson.*
import org.bson.codecs.CollectibleCodec
import org.bson.codecs.DecoderContext
import org.bson.codecs.DocumentCodec
import org.bson.codecs.EncoderContext
import org.bson.types.ObjectId
import java.util.*

/**
 * @author Taras Zubrei
 */
class JacksonCodec<T>(val objectMapper: ObjectMapper, val type: Class<T>) : CollectibleCodec<T> where T : Entity {
    val documentCodec = DocumentCodec()

    override fun generateIdIfAbsentFromDocument(document: T): T {
        if (!documentHasId(document))
            document._id = ObjectId().toString()
        return document
    }

    override fun getDocumentId(document: T): BsonValue {
        return BsonString(document._id);
    }

    override fun getEncoderClass(): Class<T> {
        return type;
    }

    override fun documentHasId(document: T): Boolean {
        return Objects.nonNull(document._id)
    }

    override fun decode(reader: BsonReader?, decoderContext: DecoderContext?): T {
        return objectMapper.convertValue(documentCodec.decode(reader, decoderContext), type)
    }

    override fun encode(writer: BsonWriter?, value: T, encoderContext: EncoderContext?) {
        documentCodec.encode(writer, objectMapper.convertValue(value, Document::class.java), encoderContext)
    }
}