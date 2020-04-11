package com.har01d.ocula.examples.auth

import com.har01d.ocula.SimpleSpider
import com.har01d.ocula.handler.AuthHandler
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.RequestBody
import com.har01d.ocula.http.post


fun main() {
    SimpleSpider("https://movie.douban.com/subject/1307528/") { _, res ->
        res.select("h1").text()
    }.apply {
        //cookieAuth("dbcl2", "19980731:YCYCCYY")
        config.authHandler = LoginHandler("username", "password")
    }.run()
}

class LoginHandler(private val username: String, private val password: String) : AuthHandler() {
    override fun handle(request: Request) {
        val body = RequestBody.form(
            "name" to username,
            "password" to password,
            "remember" to "true"
        )
        dispatch("https://accounts.douban.com/j/mobile/login/basic".post(body))
    }
}
