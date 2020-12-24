package cn.har01d.ocula.handler

import cn.har01d.ocula.http.*
import java.util.*

typealias AuthConfigure = (request: Request, response: Response) -> Unit

abstract class AuthHandler : AbstractPreHandler() {
    fun dispatch(request: Request) = context.dispatch(request)
}

class BasicAuthHandler(private val username: String, private val password: String) : AuthHandler() {
    override fun handle(request: Request) {
        request.headers["Authorization"] = listOf("Basic " + Base64.getEncoder().encode("$username:$password".toByteArray()))
    }
}

class CookieAuthHandler(private val name: String, private val value: String) : AuthHandler() {
    override fun handle(request: Request) {
        request.cookies += HttpCookie(name, value)
    }
}

class TokenAuthHandler(private val token: String, private val header: String = "Authorization") : AuthHandler() {
    override fun handle(request: Request) {
        request.headers[header] = listOf(token)
    }
}

class FormAuthHandler(
    private val actionUrl: String,
    private val body: FormRequestBody,
    val block: AuthConfigure = { _, _ -> }
) : AuthHandler() {
    override fun handle(request: Request) {
        val formRequest = Request(actionUrl, HttpMethod.POST, body)
        val response = dispatch(formRequest)
        block(request, response)
    }
}
