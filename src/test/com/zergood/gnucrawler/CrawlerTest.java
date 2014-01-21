package com.zergood.gnucrawler;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: zergood
 * Date: 10.01.14
 * Time: 0:27
 * To change this template use File | Settings | File Templates.
 */
public class CrawlerTest {
    @Test
    public void testInitHostCache() throws Exception {
        Crawler crawler = new Crawler(49000,"http://gweb.dwbo.nl/");
        crawler.initHostCache();
        assertTrue(true);
    }

    @Test
    public void testGetKnownHostsAsString() throws Exception {
        Crawler crawler = new Crawler(49000,"http://gweb.dwbo.nl/");
        crawler.initHostCache();
        assertTrue(crawler.getKnownHostsAsString().length() > 0);
    }

    @Test
    public void testGetKnownHostsAsList() throws Exception {
        Crawler crawler = new Crawler(49000,"http://gweb.dwbo.nl/");
        crawler.initHostCache();
        assertTrue(crawler.getKnownHostsAsList().get(0).getIpAddress().length() > 0);
    }

    @Test
    public void testCrawl() throws Exception {
        Crawler crawler = new Crawler(49000,"http://gweb.dwbo.nl/");
        crawler.initHostCache();
        crawler.crawl();
        Thread.sleep(30000);
        assertTrue(true);
    }

}
