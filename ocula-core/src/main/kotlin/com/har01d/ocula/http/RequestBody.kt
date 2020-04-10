package com.har01d.ocula.http

import java.io.File

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
