package com.zergood.gnucrawler.interruptors;

import com.zergood.gnucrawler.ByteOrder;

import java.util.TimerTask;

public class ByteOrderInterrupt extends TimerTask{
    public void run(){
        System.out.println("read interrupted!");
        ByteOrder.setKeepReading();
    }
}
