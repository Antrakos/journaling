package io.antrakos.web

/**
 * @author Taras Zubrei
 */
class JacksonCodec<T>(val objectMapper: com.fasterxml.jackson.databind.ObjectMapper, val type: Class<T>) : org.bson.codecs.CollectibleCodec<T> where T : io.antrakos.Entity {
    val documentCodec = org.bson.codecs.DocumentCodec()

    override fun generateIdIfAbsentFromDocument(document: T): T {
        if (!documentHasId(document))
            document._id = org.bson.types.ObjectId().toString()
        return document
    }

    override fun getDocumentId(document: T): org.bson.BsonValue {
        return org.bson.BsonString(document._id);
    }

    override fun getEncoderClass(): Class<T> {
        return type;
    }

    override fun documentHasId(document: T): Boolean {
        return java.util.Objects.nonNull(document._id)
    }

    override fun decode(reader: org.bson.BsonReader?, decoderContext: org.bson.codecs.DecoderContext?): T {
        return objectMapper.convertValue(documentCodec.decode(reader, decoderContext), type)
    }

    override fun encode(writer: org.bson.BsonWriter?, value: T, encoderContext: org.bson.codecs.EncoderContext?) {
        documentCodec.encode(writer, objectMapper.convertValue(value, org.bson.Document::class.java), encoderContext)
    }
}