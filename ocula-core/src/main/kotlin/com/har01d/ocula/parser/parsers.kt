package com.har01d.ocula.parser

import com.har01d.ocula.Context
import com.har01d.ocula.http.HttpClient
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import com.har01d.ocula.queue.RequestQueue

interface Parser<out T> {
    var context: Context
    var httpClient: HttpClient?
    var queue: RequestQueue?
    fun parse(request: Request, response: Response): T
}

abstract class AbstractParser<T> : Parser<T> {
    override lateinit var context: Context
    override var httpClient: HttpClient? = null
    override var queue: RequestQueue? = null

    fun follow(response: Response, vararg urls: String): Boolean = context.follow(response.url, *urls)
    fun follow(response: Response, vararg requests: Request): Boolean = context.follow(response.url, *requests)
    fun finish() = context.finish()
}

class NoopParser : AbstractParser<String>() {
    override fun parse(request: Request, response: Response): String {
        context.finish()
        return ""
    }
}

class SimpleParser<T>(val block: (request: Request, response: Response) -> T) : AbstractParser<T>() {
    override fun parse(request: Request, response: Response) = block(request, response).also { context.finish() }
}
