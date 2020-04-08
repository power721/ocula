package com.har01d.ocula

import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response

interface Context {
    var name: String
    fun crawl(refer: String, vararg urls: String): Boolean
    fun crawl(refer: String, vararg requests: Request): Boolean
    fun follow(refer: String, vararg urls: String): Boolean
    fun follow(refer: String, vararg requests: Request): Boolean
    fun dispatch(request: Request): Response
    fun stop()
    fun abort()
    fun finish()
}
