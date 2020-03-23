package com.har01d.ocula.http

import java.util.concurrent.atomic.AtomicInteger

interface ProxyProvider {
    fun select(): HttpProxy
    fun hasAny(): Boolean
}

class RandomProxyProvider(private val httpProxies: List<HttpProxy>) : ProxyProvider {
    override fun select() = httpProxies.random()
    override fun hasAny() = httpProxies.isNotEmpty()
}

class RoundRobinProxyProvider(private val httpProxies: List<HttpProxy>) : ProxyProvider {
    private val id = AtomicInteger()
    override fun select() = httpProxies[id.getAndIncrement() % httpProxies.size]
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

class RoundRobinUserAgentProvider(private val userAgents: List<String>) : UserAgentProvider {
    private val id = AtomicInteger()
    override fun select() = userAgents[id.getAndIncrement() % userAgents.size]
    override fun hasAny() = userAgents.isNotEmpty()
}
