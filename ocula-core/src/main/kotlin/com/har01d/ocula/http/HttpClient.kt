package com.har01d.ocula.http

import java.nio.charset.Charset

interface HttpClient : AutoCloseable {
    var userAgentProvider: UserAgentProvider
    var proxyProvider: ProxyProvider
    var charset: Charset
    var timeout: Int
    var timeoutRead: Int
    fun dispatch(request: Request): Response
    fun dispatch(request: Request, handler: (result: Result<Response>) -> Unit)
    override fun close()
}

abstract class AbstractHttpClient : HttpClient {
    override var userAgentProvider: UserAgentProvider = EmptyUserAgentProvider
    override var proxyProvider: ProxyProvider = EmptyProxyProvider
    override var charset: Charset = Charsets.UTF_8
    override var timeout: Int = 15000
    override var timeoutRead: Int = 15000
}
