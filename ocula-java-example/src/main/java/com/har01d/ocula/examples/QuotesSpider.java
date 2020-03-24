package com.har01d.ocula.examples;

import com.har01d.ocula.Spider;
import com.har01d.ocula.handler.ConsoleLogResultHandler;
import com.har01d.ocula.handler.TextFileResultHandler;
import com.har01d.ocula.http.Request;
import com.har01d.ocula.http.Response;
import com.har01d.ocula.listener.LogListener;
import com.har01d.ocula.parser.AbstractParser;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;

public class QuotesSpider {
    public static void main(String[] args) {
        Spider spider = new Spider<List<Quote>>(new QuotesParser(), "http://quotes.toscrape.com/tag/humor/");
        spider.getListeners().add(LogListener.INSTANCE);
        spider.getResultHandlers().add(ConsoleLogResultHandler.INSTANCE);
        spider.getResultHandlers().add(new TextFileResultHandler(System.getProperty("java.io.tmpdir") + "/quotes/text"));
        spider.run();
    }
}

class QuotesParser extends AbstractParser<List<Quote>> {
    public List<Quote> parse(Request request, Response response) {
        List<Quote> quotes = new ArrayList<Quote>();
        for (Element quote : response.select("div.quote")) {
            String author = quote.select("span small").text();
            String text = quote.select("span.text").text();
            quotes.add(new Quote(author, text));
        }

        String next = response.select("li.next a").attr("href");
        if (!spider.follow(request.getUrl(), next)) {
            spider.finish();
        }
        return quotes;
    }
}

class Quote {
    private final String author;
    private final String text;

    public Quote(String author, String text) {
        this.author = author;
        this.text = text;
    }

    public String getAuthor() {
        return author;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return "Quote{" +
                "author='" + author + '\'' +
                ", text='" + text + '\'' +
                '}';
    }
}
