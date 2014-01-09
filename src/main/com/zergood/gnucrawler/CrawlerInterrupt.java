package com.zergood.gnucrawler;

import java.util.TimerTask;

public class CrawlerInterrupt extends TimerTask{
    public void run(){
        System.out.println("crawler interrupted!");
        CrawlerMain.setCrawlingFlag(false);
    }
}