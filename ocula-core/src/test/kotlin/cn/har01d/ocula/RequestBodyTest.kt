package cn.har01d.ocula

import cn.har01d.ocula.http.BytesRequestBody
import cn.har01d.ocula.http.RequestBody
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class RequestBodyTest {
    private val mapper = jacksonObjectMapper()

    @Test
    fun testTextRequestBody() {
        val body = RequestBody.text("Hello!")
        val json = mapper.writeValueAsString(body)
        assertEquals(body, mapper.readValue(json, RequestBody::class.java))
    }

    @Test
    fun testJsonRequestBody() {
        val body = RequestBody.json("123")
        val json = mapper.writeValueAsString(body)
        assertEquals(body, mapper.readValue(json, RequestBody::class.java))
    }

    @Test
    fun testBytesRequestBody() {
        val body = RequestBody.bytes("Hello!".toByteArray())
        val json = mapper.writeValueAsString(body)
        val obj = mapper.readValue(json, BytesRequestBody::class.java)
        assertEquals(body, obj)
        assertEquals("Hello!", obj.bytes.toString(Charsets.UTF_8))
    }

    @Test
    fun testFileRequestBody() {
        val body = RequestBody.file(File("/pom.xml"))
        val json = mapper.writeValueAsString(body)
        assertEquals(body, mapper.readValue(json, RequestBody::class.java))
    }

    @Test
    fun testFormRequestBody() {
        val body = RequestBody.form("name" to "Harold")
        val json = mapper.writeValueAsString(body)
        assertEquals(body, mapper.readValue(json, RequestBody::class.java))
    }
}
