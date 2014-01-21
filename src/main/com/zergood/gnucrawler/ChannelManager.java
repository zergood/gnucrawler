package com.zergood.gnucrawler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;

public class ChannelManager {

    Selector selector;
    List<Host> hosts;
    List<SocketChannel> socketChannels = new ArrayList<SocketChannel>();
    Boolean forceEnd = false;

    public ChannelManager(Selector selector, List<Host> hosts) throws IOException {
        this.selector = selector;
        for (int i = 0; i < hosts.size(); i++) {
            SocketChannel sChannel = SocketChannel.open();
            sChannel.configureBlocking(false);
            sChannel.connect(new InetSocketAddress(hosts.get(i).getIpAddress(), hosts.get(i).getPort()));
            sChannel.register(selector, sChannel.validOps(), hosts.get(i));
            socketChannels.add(sChannel);
        }
    }

    public int getReadyChannels() throws IOException {
        return selector.select();
    }

    public Set<SelectionKey> getSelectedKeys(){
        return selector.selectedKeys();
    }

    public void write (SocketChannel sChannel, Host host){
        System.out.println("write!");
        if(!host.isWriteFlag()) {
            System.out.println(host.getIpAddress());
            ByteBuffer buf = ByteBuffer.allocateDirect(4444);
            String GNodetName = sChannel.socket().getInetAddress().getHostName();
            System.out.println("Host name is : " + GNodetName);
            StringBuffer request = new StringBuffer();
            request.append("GNUTELLA CONNECT/0.6\r\n" +
                    "User-Agent: UBCECE (carwl)\r\n" +
                    "Query-Routing: 0.2\r\n" +
                    "X-Ultrapeer: False\r\n" +
                    "Crawler: 0.1\r\n" +
                    "\r\n");

            byte[] bytes = request.toString().getBytes();

            try {
                buf.put(bytes);
                buf.flip();
                sChannel.write(buf);
                host.setWriteFlag(true);
            } catch (IOException ex) {
                System.out.println("Error: Failed to send the crawl message to " + sChannel.socket().getInetAddress() + ":" + sChannel.socket().getPort());
            }
        }
    }

    public void read(SocketChannel sChannel, Host host) throws IOException {
        long startedAt = System.currentTimeMillis();
        long READ_TIME_INTERVAL = 4000;
        System.out.println("read!");
        String result = "";
        String responseLine = "";
        while ((System.currentTimeMillis() - startedAt) < READ_TIME_INTERVAL) {
            Integer done = new Integer(0);
            try {
                responseLine = ByteOrder.readLine(sChannel, done);
            } catch (IOException ex) {
                System.out.println("Error: Failed to recieve the responce from the node " + sChannel.socket().getInetAddress() + ":" + sChannel.socket().getPort());
                sChannel.close();
                break;
            }
            if (responseLine.length() < 2 || done == 1) {
                try {
                    sChannel.close();
                    break;
                } catch (IOException e) {
                    break;
                }
            }
            result += responseLine;
            result += "\n";
            host.setResponse(result);
        }
    }
}
