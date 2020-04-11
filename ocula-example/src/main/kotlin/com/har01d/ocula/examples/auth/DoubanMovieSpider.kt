package com.har01d.ocula.examples.auth

import com.har01d.ocula.SimpleSpider
import com.har01d.ocula.handler.FormAuthHandler
import com.har01d.ocula.http.RequestBody


fun main() {
    SimpleSpider("https://movie.douban.com/subject/1307528/") { _, res ->
        res.select("h1").text()
    }.apply {
        //cookieAuth("dbcl2", "19980731:YCYCCYY")
        val body = RequestBody.form(
            "name" to "username",
            "password" to "password",
            "remember" to "true"
        )
        config.authHandler = FormAuthHandler("https://accounts.douban.com/j/mobile/login/basic", body)
    }.run()
}
