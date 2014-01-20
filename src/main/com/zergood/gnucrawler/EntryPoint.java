package com.zergood.gnucrawler;

import com.zergood.gnucrawler.storage.Neo4jStorage;


/**
 *
 */
public class EntryPoint {
    private static final String SERVER_ROOT_URI = "http://localhost:7474/db/data/";

    public static void main(String[] args) throws Exception {
        Crawler crawler = new Crawler(49000,"http://gweb.dwbo.nl/");
        crawler.initHostCache();
        crawler.crawl();
        Thread.sleep(20000);
        Neo4jStorage storage = new Neo4jStorage(crawler.getKnownHostsAsList(), SERVER_ROOT_URI);
        storage.store();
    }
}
