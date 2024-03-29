package cn.har01d.ocula.http

import java.util.concurrent.atomic.AtomicInteger

interface Provider<T> {
    fun select(): T
    fun hasAny(): Boolean
}

abstract class RandomProvider<T>(private val collection: List<T>) {
    fun select() = collection.random()
    fun hasAny() = collection.isNotEmpty()
}

abstract class RoundRobinProvider<T>(private val collection: List<T>) {
    private val id = AtomicInteger()
    fun select() = collection[index()]
    fun hasAny() = collection.isNotEmpty()

    private fun index(): Int {
        var index = id.getAndIncrement()
        val size = collection.size
        if (index < size) {
            return index
        }
        while (!id.compareAndSet(index, index % size)) {
            index = id.get()
        }
        return index % size
    }
}

interface ProxyProvider : Provider<HttpProxy>
object EmptyProxyProvider : ProxyProvider {
    override fun select(): HttpProxy {
        throw NotImplementedError()
    }

    override fun hasAny() = false
}

class SimpleProxyProvider(httpProxy: HttpProxy) : ProxyProvider, RandomProvider<HttpProxy>(listOf(httpProxy))
class RandomProxyProvider(httpProxies: List<HttpProxy>) : ProxyProvider, RandomProvider<HttpProxy>(httpProxies)
class RoundRobinProxyProvider(httpProxies: List<HttpProxy>) : ProxyProvider, RoundRobinProvider<HttpProxy>(httpProxies)

interface UserAgentProvider : Provider<String>
object EmptyUserAgentProvider : UserAgentProvider {
    override fun select(): String {
        throw NotImplementedError()
    }

    override fun hasAny() = false
}

class SimpleUserAgentProvider(userAgent: String) : UserAgentProvider, RandomProvider<String>(listOf(userAgent))
class RandomUserAgentProvider(userAgents: List<String>) : UserAgentProvider, RandomProvider<String>(userAgents)
class RoundRobinUserAgentProvider(userAgents: List<String>) : UserAgentProvider, RoundRobinProvider<String>(userAgents)
