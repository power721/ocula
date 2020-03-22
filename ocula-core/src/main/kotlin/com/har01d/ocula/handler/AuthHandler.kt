package com.har01d.ocula.handler

import com.har01d.ocula.http.HttpMethod
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import java.util.*

abstract class AuthHandler : AbstractPreHandler()

class BasicAuthHandler(private val username: String, private val password: String) : AuthHandler() {
    override fun handle(request: Request) {
        request.headers["Authorization"] = listOf("Basic " + Base64.getEncoder().encode("$username:$password".toByteArray()))
    }
}

class TokenAuthHandler(private val token: String, private val header: String = "Authorization") : AuthHandler() {
    override fun handle(request: Request) {
        request.headers[header] = listOf(token)
    }
}

val sessionHandler = fun(request: Request, response: Response) {
    response.cookies.find { it.name == "session" }
            ?.let {
                request.cookies += it
            }
}

class FormAuthHandler(private val actionUrl: String, private val parameters: Parameters, val block: (request: Request, response: Response) -> Unit = sessionHandler) : AuthHandler() {
    override fun handle(request: Request) {
        val formRequest = Request(actionUrl, HttpMethod.POST, parameters)
        val response = spider.dispatch(formRequest)
        block(request, response)
    }
}
