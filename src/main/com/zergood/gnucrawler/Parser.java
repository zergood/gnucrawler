package com.zergood.gnucrawler;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: zergood
 * Date: 10.01.14
 * Time: 1:27
 * To change this template use File | Settings | File Templates.
 */
public class Parser {
    List<Host> hosts;

    public Parser(List<Host> hosts) {
        this.hosts = hosts;
    }

    public String collectRoutes(){
        StringBuilder resultBuilder = new StringBuilder();
        for (int i = 0; i < hosts.size(); i++) {
            resultBuilder.append("\n===IP===\n");
            resultBuilder.append(hosts.get(i).getIPAddress());
            resultBuilder.append("\n");
            if(hosts.get(i).getResponse().length() > 2){
                resultBuilder.append("===Routes===\n");
                resultBuilder.append(parseLeaves(hosts.get(i).getResponse()));
                resultBuilder.append("\n");
                resultBuilder.append(parseUpeers(hosts.get(i).getResponse()));
            }
        }
        return resultBuilder.toString();
    }

    private String parseUpeers(String response){
        int beginIndex;
        int endIndex;
        String upeers = new String();
        beginIndex= response.indexOf("Peers:");
        endIndex= response.indexOf("\n",beginIndex);
        upeers= response.substring(beginIndex + 6, endIndex);
        upeers= upeers.trim();
        return upeers;
    }

    private String parseLeaves(String response){
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
