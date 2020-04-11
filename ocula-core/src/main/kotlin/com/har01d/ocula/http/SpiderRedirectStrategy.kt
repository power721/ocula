package com.har01d.ocula.http

import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.impl.client.LaxRedirectStrategy
import org.apache.http.protocol.HttpContext
import java.net.URI

object SpiderRedirectStrategy : LaxRedirectStrategy() {
    override fun getLocationURI(request: HttpRequest, response: HttpResponse, context: HttpContext): URI {
        val uri = super.getLocationURI(request, response, context)
        context.setAttribute("uri", uri.toString())
        return uri
    }
}
