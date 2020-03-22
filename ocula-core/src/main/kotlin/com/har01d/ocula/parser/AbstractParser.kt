package com.har01d.ocula.parser

import com.har01d.ocula.Spider

abstract class AbstractParser<T> : Parser<T> {
    override lateinit var spider: Spider<*>
}
