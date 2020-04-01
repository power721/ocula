package com.har01d.ocula.parser

import com.har01d.ocula.Context
import com.har01d.ocula.http.HttpClient
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response

interface Parser<out T> {
    var context: Context
    var httpClient: HttpClient?
    val candidates: List<Request>
    fun parse(request: Request, response: Response): T
}

abstract class AbstractParser<T> : Parser<T> {
    override lateinit var context: Context
    override var httpClient: HttpClient? = null
    override val candidates: MutableList<Request> = mutableListOf()

    fun follow(next: String) {
        candidates += Request(next)
    }

    fun follow(next: Request) {
        candidates += next
    }
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
