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
    HashSet<Host> crawledHost = new HashSet<Host>();
    List<SocketChannel> socketChannels = new ArrayList<SocketChannel>();
    Boolean forceEnd = false;

    public ChannelManager(Selector selector, List<Host> hosts) throws IOException {
        this.selector = selector;
        this.hosts = hosts;
        for (int i = 0; i < hosts.size(); i++) {
            SocketChannel sChannel = SocketChannel.open();
            sChannel.configureBlocking(false);
            sChannel.connect(new InetSocketAddress(hosts.get(i).getIpAddress(), hosts.get(i).getPort()));
            sChannel.register(selector, sChannel.validOps(), hosts.get(i));
            socketChannels.add(sChannel);
            crawledHost.add(hosts.get(i));
        }
    }

    public HashSet<Host> getCrawledHost() {
        return crawledHost;
    }

    public int getReadyChannels() throws IOException {
        return selector.select();
    }

    public Set<SelectionKey> getSelectedKeys(){
        return selector.selectedKeys();
    }

    public void addHost(Host host) throws IOException {
        System.out.println("HOST ADDED");
        System.out.println("HOST IP:" + host.getIpAddress());
        System.out.println("HOST PORT:" + host.getPort());
        try{
            if(!crawledHost.contains(host)){
                SocketChannel sChannel = SocketChannel.open();
                sChannel.configureBlocking(false);
                sChannel.connect(new InetSocketAddress(host.getIpAddress(), host.getPort()));
                sChannel.register(selector, sChannel.validOps(), host);
                socketChannels.add(sChannel);
                crawledHost.add(host);
            }
        }catch(Exception e){
            System.out.println("########################IO EXEPTION##################################################");
            System.out.println("HOST IP:" + host.getIpAddress());
            System.out.println("HOST PORT:" + host.getPort());
        }
    }

    private void addHost(List<Host> hosts) throws IOException {
        for(Host host : hosts){
            addHost(host);
        }
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
//            crawledHost.add(host);
            addHostFromResult(result);
        }
    }

    private void addHostFromResult(String result) throws IOException {
        if(checkResponseComplete(result)) {
            List<Host> leafHosts = getLeafPeers(result);
            List<Host> ultraHosts = getUltraPeers(result);
            addHost(leafHosts);
            addHost(ultraHosts);
        }
    }

    private List<Host> getLeafPeers(String response){
        List<Host> leafHost = new LinkedList<Host>();
        String[] leafIPs = responseToArray(response);
        for(String leafIP : leafIPs){
            String[] hostString = leafIP.split(":");
            Host host = new Host(hostString[0], Integer.valueOf(hostString[1]), false);
            leafHost.add(host);
        }
        return leafHost;
    }

    private List<Host> getUltraPeers(String response){
        List<Host> ultraHost = new LinkedList<Host>();
        String[] upeersIPs = responseToArray(response);
        for(String upeersIP : upeersIPs){
            System.out.println(upeersIP);
            String[] hostString = upeersIP.split(":");
            Host host = new Host(hostString[0], Integer.valueOf(hostString[1]), true);
            ultraHost.add(host);
        }
        return ultraHost;
    }

    private String[] responseToArray(String response){
        int beginIndex;
        int endIndex;
        String responseBuffer;
        beginIndex = response.indexOf("Peers:");
        endIndex = response.indexOf("\n", beginIndex);
        responseBuffer = response.substring(beginIndex + 6, endIndex);
        responseBuffer = responseBuffer.trim();
        String[] responseArray = responseBuffer.split(",");
        return responseArray;
    }

    private boolean checkResponseComplete(String response){
        boolean result = false;
        if(response.indexOf("Peers:") != -1 && response.indexOf("Leaves:") != -1)
        {
            return true;
        }
        return result;
    }
}
