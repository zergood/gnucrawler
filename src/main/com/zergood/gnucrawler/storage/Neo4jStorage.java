package com.zergood.gnucrawler.storage;

import com.zergood.gnucrawler.Host;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.rest.graphdb.RestGraphDatabase;

import java.util.List;

/**
 *
 */
public class Neo4jStorage {
    private List<Host> hosts;
    private GraphDatabaseService gds;
    enum relTypes implements org.neo4j.graphdb.RelationshipType{
        KNOWS
    }

    public Neo4jStorage(List<Host> hosts, String dbURL) {
        this.hosts = hosts;
        this.gds = new RestGraphDatabase(dbURL);
    }

    public void store(){
        Node self = gds.createNode();
        for (Host host : hosts) {
            Node node = gds.createNode();
            node.setProperty("ip", host.getIPAddress());
            self.createRelationshipTo(node, relTypes.KNOWS);
            if (host.getResponse().length() > 2) {
                storeUpeers(node, host);
                storeLeaves(node, host);
            }
        }
    }

    private void storeUpeers(Node root, Host host){
        String response = host.getResponse();
        int beginIndex;
        int endIndex;
        String upeers;
        beginIndex = response.indexOf("Peers:");
        endIndex = response.indexOf("\n", beginIndex);
        upeers = response.substring(beginIndex + 6, endIndex);
        upeers = upeers.trim();
        String[] upeersIPs = upeers.split(",");
        for (String upeersIP : upeersIPs) {
            Node upeer = gds.createNode();
            upeer.setProperty("ip", upeersIP);
            root.createRelationshipTo(upeer, relTypes.KNOWS);
        }
    }

    private void storeLeaves(Node root, Host host){
        String response = host.getResponse();
        int beginIndex;
        int endIndex;
        String leaves;
        beginIndex = response.indexOf("Leaves:");
        endIndex = response.indexOf("\n",beginIndex);
        leaves = response.substring(beginIndex+7,endIndex);
        leaves = leaves.trim();
        String[] leaveIPs = leaves.split(",");
        for (String leaveIP : leaveIPs) {
            Node leave = gds.createNode();
            leave.setProperty("ip", leaveIP);
            root.createRelationshipTo(leave, relTypes.KNOWS);
        }
    }
}
