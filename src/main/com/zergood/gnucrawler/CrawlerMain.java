package com.zergood.gnucrawler;

import com.kenmccrary.jtella.ConnectionData;
import com.kenmccrary.jtella.GNUTellaConnection;
import com.kenmccrary.jtella.Host;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;

/**
 * Created with IntelliJ IDEA.
 * User: zergood
 * Date: 07.01.14
 * Time: 11:45
 * To change this template use File | Settings | File Templates.
 */
public class CrawlerMain {
    final static int LOCAL_PORT = 49000;
    private static GNUTellaConnection c;
    public static final String LOGGER = "crawler";
    private static Logger LOG = Logger.getLogger(LOGGER);
    private static final String LOGFILE = "crawler.log4j.properties";
    private String result;
    private static boolean keepReading;
    private static boolean forceEnd = false;
    private static boolean crawlingFlag = false;
    private static final String[] urls =new String[] {"http://gweb.dwbo.nl/"};
    public static void setCrawlingFlag(boolean flag) {
        crawlingFlag = flag;
    }



    public static void main(String[] args) throws IOException, InterruptedException {
        PropertyConfigurator.configure(LOGFILE);
        ConnectionData connData = new ConnectionData();
        connData.setIncommingConnectionCount(10);
        connData.setOutgoingConnectionCount(10);
        connData.setUltrapeer(true);
        connData.setIncomingPort(Integer.valueOf(LOCAL_PORT).intValue());
        connData.setAgentHeader("up2p");

        Selector selector = Selector.open();
        c = new GNUTellaConnection(connData);
        c.start();
        System.out.println("JTellaAdapter:: init: GnutellaConnection started");
        Thread.sleep(4000);
        List<Host> t = c.getHostCache().getKnownHosts();
        for (int i = 0; i < t.size(); i++) {
            Host host = t.get(i);
            SocketChannel sChannel = SocketChannel.open();
            sChannel.configureBlocking(false);
            sChannel.connect(new InetSocketAddress(t.get(i).getIPAddress(), t.get(i).getPort()));
            sChannel.register(selector, sChannel.validOps(), t.get(i));
            System.out.println(t.get(i).getIPAddress());
        }
        crawlingFlag = true;
        Timer timer = new Timer();
        timer.schedule(new CrawlerInterrupt(), 30000);
        while(crawlingFlag){
            int readyChannels = selector.select();
            if (readyChannels == 0) continue;
            System.out.println(readyChannels);
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
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
                    write((SocketChannel) key.channel(), (Host)key.attachment());
                }
                if (key.isValid() && key.isReadable()) {
                    read((SocketChannel) key.channel(), (Host)key.attachment());
                }
                keyIterator.remove();
            }
        }
        System.out.println("Crawler stopped!");
        System.out.println(collectRoutes(t));
    }

    private static void write (SocketChannel sChannel, Host host){
        System.out.println("write!");
        if(!host.isWriteFlag()) {
            System.out.println(host.getIPAddress());
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

    private static void read(SocketChannel sChannel, Host host) throws IOException {
        System.out.println("read!");
        String result = "";
        String responseLine = "";
        Timer interrupt = new Timer();
        interrupt.schedule(new Interrupt2(), 5000);
        while (!forceEnd) {
            Integer done = new Integer(0);
            try {
                responseLine = ByteOrder.readLine(sChannel, done);
            } catch (IOException ex) {
                System.out.println("Error: Failed to recieve the responce from the node " + sChannel.socket().getInetAddress() + ":" + sChannel.socket().getPort());
                sChannel.close();
                break;
            }

            // reached the end of the responce
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
            interrupt.cancel();
            forceEnd = false;
        }
    }

    public static void setForceEnd(){
        forceEnd = true;
    }

    private static String collectRoutes(List<Host> t){
        StringBuilder resultBuilder = new StringBuilder();
        for (int i = 0; i < t.size(); i++) {
            resultBuilder.append("\n===IP===\n");
            resultBuilder.append(t.get(i).getIPAddress());
            resultBuilder.append("\n");
            if(t.get(i).getResponse().length() > 2){
                resultBuilder.append("===Routes===\n");
                resultBuilder.append(parseLeaves(t.get(i).getResponse()));
                resultBuilder.append("\n");
                resultBuilder.append(parseUpeers(t.get(i).getResponse()));
            }
        }
        return resultBuilder.toString();
    }

    private static String parseUpeers(String response){
        int beginIndex;
        int endIndex;
        String upeers = new String();
        beginIndex= response.indexOf("Peers:");
        endIndex= response.indexOf("\n",beginIndex);
        upeers= response.substring(beginIndex + 6, endIndex);
        upeers= upeers.trim();
        return upeers;
    }

    private static String parseLeaves(String response){
        int beginIndex;
        int endIndex;
        String leaves=new String();

        beginIndex= response.indexOf("Leaves:");
        endIndex= response.indexOf("\n",beginIndex);
        leaves= response.substring(beginIndex+7,endIndex);
        leaves=leaves.trim();
        return leaves;
    }
}
