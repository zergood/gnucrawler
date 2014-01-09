package com.zergood.gnucrawler;

import java.net.MalformedURLException;

/**
 * Created with IntelliJ IDEA.
 * User: zergood
 * Date: 10.01.14
 * Time: 0:06
 * To change this template use File | Settings | File Templates.
 */
public class EntryPoint {
    public static void main(String[] args) throws Exception {
        Crawler crawler = new Crawler(49000,"http://gweb.dwbo.nl/");
        crawler.initHostCache();
        crawler.crawl();
        Thread.sleep(40000);
        Parser parser = new Parser(crawler.getKnownHostsAsList());
        System.out.println(parser.collectRoutes());
    }
}
