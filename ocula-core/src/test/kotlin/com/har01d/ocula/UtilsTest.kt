package com.har01d.ocula

import com.har01d.ocula.util.normalizeUrl
import org.junit.Test
import kotlin.test.assertEquals

class UtilsTest {
    @Test
    fun testNormalizeUrl() {
        assertEquals("http://example.com/test.html", normalizeUrl("http://example.com", "test.html"))
        assertEquals("http://example.com/test.html", normalizeUrl("http://example.com/", "test.html"))
        assertEquals("http://example.com/test.html", normalizeUrl("http://example.com?q=1", "test.html"))
        assertEquals("http://example.com/test.html", normalizeUrl("http://example.com/?q=1", "test.html"))
        assertEquals("http://example.com/test.html", normalizeUrl("http://example.com", "/test.html"))
        assertEquals("http://example.com/test.html", normalizeUrl("http://example.com/", "/test.html"))
        assertEquals("http://example.com/test.html", normalizeUrl("http://example.com?q=1", "/test.html"))
        assertEquals("http://example.com/test.html", normalizeUrl("http://example.com/?q=1", "/test.html"))
        assertEquals("http://example.com/?q=image", normalizeUrl("http://example.com", "?q=image"))
        assertEquals("http://example.com/?q=image", normalizeUrl("http://example.com/", "?q=image"))
        assertEquals("http://example.com/?q=image", normalizeUrl("http://example.com?s=1", "?q=image"))
        assertEquals("http://example.com/?q=image", normalizeUrl("http://example.com/?s=1", "?q=image"))
        assertEquals("http://image.com/test.png", normalizeUrl("http://example.com", "//image.com/test.png"))
        assertEquals("http://image.com/test.png", normalizeUrl("http://example.com?q=", "//image.com/test.png"))
        assertEquals("http://image.com/test.png", normalizeUrl("http://example.com/?q=", "//image.com/test.png"))

        assertEquals("https://example.com/test.html", normalizeUrl("https://example.com", "test.html"))
        assertEquals("https://example.com/news/test.html", normalizeUrl("https://example.com/news/", "test.html"))
        assertEquals("https://example.com/test.html", normalizeUrl("https://example.com/news/", "/test.html"))
        assertEquals("https://example.com/test.html", normalizeUrl("https://example.com/news/?q=1", "/test.html"))
        assertEquals("https://example.com/test.html?q=2", normalizeUrl("https://example.com/news/?q=1", "/test.html?q=2"))
        assertEquals("https://example.com/test.html", normalizeUrl("https://example.com", "/test.html"))
        assertEquals("https://example.com/?q=image", normalizeUrl("https://example.com", "?q=image"))
        assertEquals("https://image.com/test.png", normalizeUrl("https://example.com", "//image.com/test.png"))
    }
}
