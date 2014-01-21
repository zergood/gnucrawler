package com.zergood.gnucrawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;

public class Crawler {
    final int LOCAL_PORT;
    private boolean forceEnd = false;
    private boolean crawlingFlag = false;
    private final String url;
    private List<Host>  hosts;
    private long CRAWL_TIME_INTERVAL = 30000;

    public Crawler(int localPort, String url) {
        this.LOCAL_PORT = localPort;
        this.url = url;
        this.hosts = new ArrayList<Host>();
    }

    public void initHostCache() throws Exception {
        URL fullHostUrl;
        HttpURLConnection connection;
        BufferedReader incomingData;
        Boolean error = false;
        try{
            System.out.println("enter");
            fullHostUrl = new URL(url + "?client=JTEL&version=0.8&hostfile=1");
            System.out.println(fullHostUrl);
            connection = (HttpURLConnection)fullHostUrl.openConnection();
            connection.setRequestProperty("Connection", "close");
            incomingData = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String host = incomingData.readLine();
            System.out.println(host);
            if (host != null) {
                System.out.println("host not null!");
                if (host.toLowerCase().startsWith("error")) {
                    System.out.println("host error!");
                    incomingData.close();
                } else {
                    System.out.println("host recieved!");
                    while ((host != null) && (!error)) {
                        addHost(host);
                        try{
                            host = incomingData.readLine();
                        }
                        catch (IOException ioe2) {
                            error = true;
                            try {
                                incomingData.close();
                            }
                            catch (IOException ioe3) {}
                            connection.disconnect();
                        }
                    }
                }
            }
        } catch (MalformedURLException e) {
            System.out.println("wrong url!");
        } catch (IOException e) {
            System.out.println("no connection!");
        }
        catch (Exception e){
            System.out.println(e);
        }
    }

    private void addHost(String hostDetails) {
        String ipAddress = "";
        int port = 0;

        if (hostDetails.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{1,5}")) {
            String[] hostParts = hostDetails.split(":", 2);

            try {
                ipAddress = hostParts[0];
                port = Integer.parseInt(hostParts[1]);
            }
            catch (Exception e) {
                return;
            }
            Host host = new Host(ipAddress, port, 0, 0);
            hosts.add(host);
        }
    }

    public String getKnownHostsAsString(){
        String result = "";
        for (int i = 0; i < hosts.size(); i++) {
            result += hosts.get(i).getIpAddress();
        }
        return result;
    }

    public List<Host> getKnownHostsAsList(){
        return hosts;
    }

    public void crawl() throws IOException {
        ChannelManager channelManager = new ChannelManager(Selector.open(), hosts);
        crawlingFlag = true;
        long startedAt = System.currentTimeMillis();
//        while((System.currentTimeMillis() - startedAt) < CRAWL_TIME_INTERVAL)
        while(channelManager.getCrawledHost().size() < 100){
            System.out.println("NODE CRAWLED:" + String.valueOf(channelManager.getCrawledHost().size()));
            int readyChannels = channelManager.getReadyChannels();
            if (readyChannels == 0) continue;
            Set<SelectionKey> selectedKeys = channelManager.getSelectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                if (key.isValid() && key.isConnectable()) {
                    SocketChannel sChannel = (SocketChannel)key.channel();
                    try {
                        boolean success = sChannel.finishConnect();
                        if (!success) {
                            key.cancel();
                        }
                    } catch (IOException ex) {
                        key.cancel();
                    }
                }
                if (key.isValid() && key.isWritable()) {
                    channelManager.write((SocketChannel) key.channel(), (Host) key.attachment());
                }
                if (key.isValid() && key.isReadable()) {
                    channelManager.read((SocketChannel) key.channel(), (Host) key.attachment());
                }
                keyIterator.remove();
            }
        }
    }
}
