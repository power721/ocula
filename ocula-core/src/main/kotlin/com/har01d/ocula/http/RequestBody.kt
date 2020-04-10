package com.har01d.ocula.http

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import java.io.File

@JsonDeserialize(using = RequestBodyDeserializer::class)
sealed class RequestBody {
    companion object {
        fun text(text: String) = TextRequestBody(text)
        fun json(json: String) = JsonRequestBody(json)
        fun bytes(bytes: ByteArray) = BytesRequestBody(bytes)
        fun file(file: File) = FileRequestBody(file)
    }
}

class TextRequestBody(val text: String) : RequestBody()
class JsonRequestBody(val json: String) : RequestBody()
class BytesRequestBody(val bytes: ByteArray) : RequestBody()
class FileRequestBody(val file: File) : RequestBody()

class RequestBodyDeserializer : StdDeserializer<RequestBody>(RequestBody::class.java) {
    override fun deserialize(p: JsonParser, ctx: DeserializationContext): RequestBody {
        val node: JsonNode = p.codec.readTree(p)
        if (node.get("text") != null) {
            return TextRequestBody(node.get("text").textValue())
        }
        if (node.get("json") != null) {
            return JsonRequestBody(node.get("json").textValue())
        }
        if (node.get("bytes") != null) {
            return BytesRequestBody(node.get("bytes").binaryValue())
        }
        if (node.get("file") != null) {
            return FileRequestBody(File(node.get("file").textValue()))
        }
        throw IllegalStateException("Unsupported RequestBody")
    }
}
