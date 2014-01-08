package com.zergood.gnucrawler;

import java.util.TimerTask;

public class Interrupt2 extends TimerTask{
    public void run(){
        System.out.println("read interrupted!");
        ByteOrder.setKeepReading();
    }
}
