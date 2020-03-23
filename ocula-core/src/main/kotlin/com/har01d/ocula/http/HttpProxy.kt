package com.har01d.ocula.http

import java.net.InetSocketAddress
import java.net.Proxy

data class HttpProxy(val hostname: String, val port: Int) {
    fun toProxy(): Proxy {
        return Proxy(Proxy.Type.HTTP, InetSocketAddress(hostname, port))
    }
}
