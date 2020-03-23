package com.har01d.ocula.http

interface ProxyProvider {
    fun select(): HttpProxy
    fun hasAny(): Boolean
}

class RandomProxyProvider(private val httpProxies: List<HttpProxy>) : ProxyProvider {
    override fun select() = httpProxies.random()
    override fun hasAny() = httpProxies.isNotEmpty()
}

interface UserAgentProvider {
    fun select(): String
    fun hasAny(): Boolean
}

class RandomUserAgentProvider(private val userAgents: List<String>) : UserAgentProvider {
    override fun select() = userAgents.random()
    override fun hasAny() = userAgents.isNotEmpty()
}
