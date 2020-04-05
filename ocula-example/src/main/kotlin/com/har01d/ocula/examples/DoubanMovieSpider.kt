package com.har01d.ocula.examples

import com.har01d.ocula.SimpleSpider
import com.har01d.ocula.handler.AuthHandler
import com.har01d.ocula.http.HttpMethod
import com.har01d.ocula.http.Request


fun main() {
    SimpleSpider("https://movie.douban.com/subject/1307528/") { _, res ->
        res.select("h1").text()
    }.apply {
        //cookieAuth("dbcl2", "19980731:YCYCCYY")
        authHandler = LoginHandler("username", "password")
    }.run()
}

class LoginHandler(private val username: String, private val password: String) : AuthHandler() {
    override fun handle(request: Request) {
        val params = listOf(
                "name" to username,
                "password" to password,
                "remember" to "true"
        )
        val req = Request("https://accounts.douban.com/j/mobile/login/basic", HttpMethod.POST, params)
        val res = dispatch(req)
        res.cookies.find { it.name == "dbcl2" }?.let {
            request.cookies += it
        }
    }
}
