package com.har01d.ocula

import com.har01d.ocula.util.generateId
import com.har01d.ocula.util.md5
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

    @Test
    fun testMd5() {
        assertEquals("098f6bcd4621d373cade4e832627b4f6", "test".md5())
        assertEquals("202cb962ac59075b964b07152d234b70", "123".md5())
        assertEquals("a7bac2239fcdcb3a067903d8077c4a07", "中文".md5())
    }

    @Test
    fun testGenerateId() {
        assertEquals(6, generateId(6).length)
        assertEquals(10, generateId(10).length)
    }
}
