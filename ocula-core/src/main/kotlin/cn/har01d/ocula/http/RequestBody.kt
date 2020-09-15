package cn.har01d.ocula.http

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode
import java.io.File

@JsonDeserialize(using = RequestBodyDeserializer::class)
sealed class RequestBody {
    companion object {
        fun text(text: String) = TextRequestBody(text)
        fun json(json: String) = JsonRequestBody(json)
        fun bytes(bytes: ByteArray) = BytesRequestBody(bytes)
        fun file(file: File) = FileRequestBody(file)
        fun form(vararg form: Pair<String, String>) = FormRequestBody(form.toMap())
    }
}

data class TextRequestBody(val text: String) : RequestBody()
data class JsonRequestBody(val json: String) : RequestBody()
data class FileRequestBody(val file: File) : RequestBody()
data class FormRequestBody(val form: Map<String, String>) : RequestBody()
data class BytesRequestBody(val bytes: ByteArray) : RequestBody() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return bytes.contentEquals((other as BytesRequestBody).bytes)
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }
}

class RequestBodyDeserializer : StdDeserializer<RequestBody>(RequestBody::class.java) {
    override fun deserialize(parser: JsonParser, ctx: DeserializationContext): RequestBody {
        val node: JsonNode = parser.codec.readTree(parser)
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
        if (node.get("form") != null) {
            return FormRequestBody(toMap(node.get("form") as ObjectNode))
        }
        throw IllegalStateException("Unsupported RequestBody")
    }

    private fun toMap(node: ObjectNode): Map<String, String> {
        val map = mutableMapOf<String, String>()
        node.fields().forEach {
            map[it.key] = it.value.textValue()
        }
        return map
    }
}
