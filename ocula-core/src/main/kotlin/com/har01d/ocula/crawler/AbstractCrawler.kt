package com.har01d.ocula.crawler

import com.har01d.ocula.Spider

abstract class AbstractCrawler : Crawler {
    override lateinit var spider: Spider<*>
}
