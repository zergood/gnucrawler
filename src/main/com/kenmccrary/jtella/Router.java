/*
 * Copyright (C) 2000-2001  Ken McCrary
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * Email: jkmccrary@yahoo.com
 */
package com.kenmccrary.jtella;

//import java.util.Collections;

import com.kenmccrary.jtella.util.BoundedQueue;
import com.kenmccrary.jtella.util.Log;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;

//import java.util.Stack;

/**
 *  Routes messages read from the network to appropriate
 *  Connections
 *
 */
public class Router extends Thread {
	// TODO flush dead connections from routing tables
	
	/** Name of Logger used by this adapter. */
    public static final String LOGGER = "com.kenmccrary.jtella";

    /** Logger used by this adapter. */
    private static Logger LOG = Logger.getLogger(LOGGER);

	private static int MAX_ROUTER_TABLE = 5000;
	private static byte MAX_HOPS = (byte)7;
	private static byte MAX_TTL = (byte)50;

	private ConnectionList connectionList;
	private ConnectionData connectionData;
	private HostCache hostCache;
	private RouteTable pingRouteTable;
	private RouteTable queryRouteTable;
	private RouteTable queryHitRouteTable;
	private OriginateTable originateTable;
	private Vector<MessageReceiver> searchReceivers;
	private Vector<MessageReceiver> pushReceivers;
	private BoundedQueue messageQueue;
	private boolean shutDownFlag;

	/**
	 *  Collection of active connections to the network
	 *
	 *  @param the list of connections in the system
	 *  @param cache of available hosts in the system
	 */
    public Router(ConnectionList connectionList, ConnectionData connectionData, HostCache hostCache) {

		super("RouterThread");
		this.connectionList = connectionList;
		this.connectionData = connectionData;
		this.hostCache = hostCache;
		pingRouteTable = new RouteTable(MAX_ROUTER_TABLE);
		queryRouteTable = new RouteTable(MAX_ROUTER_TABLE);
		queryHitRouteTable = new RouteTable(MAX_ROUTER_TABLE);
		originateTable = new OriginateTable();
		messageQueue = new BoundedQueue(1000);
		searchReceivers = new Vector<MessageReceiver>();
		pushReceivers = new Vector<MessageReceiver>();
	}

	/**
	 *  Stops the operation of the router
	 *
	 */
	void shutdown() {
		shutDownFlag = true;
		interrupt();
	}

	/**
	 *  Routes a message, used by Connections
	 *
	 *  @return false if routing failed because of overload
	 */
	boolean route(Message m, NodeConnection connection) {

		if (m.getTTL() < 1) {
			// expired message, no failure signal required
			return true;
		}

		RouteMessage message = new RouteMessage(m, connection);

		boolean result = true;
		synchronized (this) {
			result = messageQueue.enqueue(message);
			LOG.debug("Router::route:new message enqueued");
			// notify in either case, either a new message on the queue or
			// the queue is full
			notifyAll();
		}

		return result;
	}

	/**
	 *  Record a message we originate, so we can route it back
	 *
	 */
	void routeBack(Message m, MessageReceiver receiver) {
		originateTable.put(m.getGUID(), receiver);
	}

	/**
	 *  Removes a message sender's origination data
	 *
	 *  @param messasgeGUIDs the originated message guids
	 */
	void removeMessageSender(List<GUID> messageGUIDs) {
		Iterator<GUID> iterator = messageGUIDs.iterator();

		while (iterator.hasNext()) {
			GUID guid = (GUID)iterator.next();

			originateTable.remove(guid);
		}
	}

	/**
	 *  Adds a search listener
	 *
	 *  @param receiver search receiver
	 */
	void addSearchMessageReceiver(MessageReceiver receiver) {
		searchReceivers.addElement(receiver);
	}

	/**
	 *  Removes a search receiver
	 *
	 *  @param receiver message receiver
	 */
	void removeSearchMessageReceiver(MessageReceiver receiver) {
		searchReceivers.removeElement(receiver);
	}

	/**
	 *  Adds a push listener
	 *
	 *  @param receiver push message receiver
	 */
	void addPushMessageReceiver(MessageReceiver receiver) {
		pushReceivers.addElement(receiver);
	}

	/**
	 *  Removes a push receiver
	 *
	 *  @param receiver message receiver
	 */
	void removePushMessageReceiver(MessageReceiver receiver) {
		pushReceivers.removeElement(receiver);
	}

	/**
	 *  Query the next message to route, blocks if no message are available
	 *
	 *  @return message to route
	 */
	RouteMessage getNextMessage() throws InterruptedException {
		synchronized (this) {
			while (messageQueue.empty()) {
				try {
					//LOG.debug("Router::getNextMsg: queue empty: waiting"); Live connection list start
					wait();
				}
				catch (InterruptedException ie) {
					//ie.printStackTrace();
					if (shutDownFlag) {
						throw new InterruptedException();
					}
				}
			}
			LOG.debug("Router::getNextMsg: I'm awake! getting next message...");
			return (RouteMessage)messageQueue.dequeue();
		}
	}

	/**
	 *  Runs along routing messages
	 *
	 */
	public void run() {

		while (!shutDownFlag) {
			try {
				RouteMessage routeMessage = getNextMessage();

				if (null == routeMessage) {
					LOG.error("Router::Null message in router");
					continue;
				}
				//-----------------------------------------------------------
				// Check if this is a response to a message we generated
				//-----------------------------------------------------------
				if (originateTable.containsGUID(routeMessage.getMessage().getGUID())) {
					LOG.info(
						"Routing response to originated message\r\n"
							+ routeMessage.getMessage().getGUID());

					// Retrieve the message receiver
					Message m = routeMessage.getMessage();

					MessageReceiver receiver = originateTable.get(m.getGUID());

					if (m instanceof SearchReplyMessage) {
						receiver.receiveSearchReply((SearchReplyMessage)m);
					}
					else {
						PongMessage pong =
							new PongMessage(
								m.getGUID(),
								(short)connectionData.getIncomingPort(),
								InetAddress.getLocalHost().getHostAddress(),
								connectionData.getSharedFileCount(),
								connectionData.getSharedFileSize());
						pong.setTTL((byte)m.getTTL());
						m.getOriginatingConnection().send(pong);
						LOG.error("Router::Routeback unknown message");
					}

					continue;
				}

				//-----------------------------------------------------------
				// Don't forward invalid messages
				//-----------------------------------------------------------
				if (!validateMessage(routeMessage.getMessage())) {
					continue;
				}

				//-----------------------------------------------------------
				// Route the network traffic to our connections
				//-----------------------------------------------------------
				switch (routeMessage.getMessage().getType()) {
					case Message.PING :
						{
							LOG.info("Routing ping message");
							routePingMessage(routeMessage);
							break;
						}

					case Message.PONG :
						{
							LOG.info("Routing pong message");
							routePongMessage(routeMessage);
							break;
						}

					case Message.PUSH :
						{
							LOG.info("Routing push message");
							routePushMessage(routeMessage);
							break;
						}

					case Message.QUERY :
						{
							LOG.info("Routing query message");
							routeQueryMessage(routeMessage);
							break;
						}

					case Message.QUERYREPLY :
						{

							LOG.info("Routing query reply message");
							routeQueryReplyMessage(routeMessage);
							break;
						}
				}
			}
			catch (Exception e) {
				// keep running
				LOG.error(e);
			}

		}

	}

	/**
	 *  Get the source of a previously received message query message
	 *
	 *  @param message a search message
	 */
	NodeConnection getQuerySource(SearchMessage message) {
		return queryRouteTable.get(message.getGUID());
	}

	// TODO history for routing

	/**
	 *  Routes PING messages -
	 *  RULES: Route PING messages to all connections
	 *  except originator
	 *
	 */
	void routePingMessage(RouteMessage m) {
		if (m.getConnection().getStatus() != NodeConnection.STATUS_OK) {
			// Originating connection does not exist, so drop the message
			// If we received Pong responses, there would be no connection to
			// route them to
			LOG.debug("Connection NOK, dropping ping message");
			return;
		}

		// Make a local connection list to avoid concurrency issues
		LinkedList<NodeConnection> listcopy = connectionList.getList();
		ListIterator<NodeConnection> iterator = listcopy.listIterator(0);

		prepareMessage(m.getMessage());

		while (iterator.hasNext()) {
			NodeConnection connection = (NodeConnection)iterator.next();

			if (!connection.equals(m.getConnection())
				&& connection.getStatus() == NodeConnection.STATUS_OK) {
				routerSend(connection, m.getMessage());
			}
		}
        System.out.println("send ping!");
        // History of Pings
		pingRouteTable.put(m.getMessage().getGUID(), m.getConnection());
	}

	/**
	 *  Routes PONG messages -
	 *  RULES: Route PONG messages only to the connection
	 *  the PING arrived on
	 *
	 */
	void routePongMessage(RouteMessage m) {
		// Harvest this servant from the pong message
		PongMessage pongMessage = (PongMessage)m.getMessage();
		hostCache.addHost(new Host(pongMessage));

		NodeConnection originator = pingRouteTable.get(m.getMessage().getGUID());
		if (null != originator && originator.getStatus() == NodeConnection.STATUS_OK) {
			prepareMessage(m.getMessage());

			routerSend(originator, m.getMessage());
		}
		else {
			LOG.info("No connection for routing pong");
		}
	}

	/**
	 *  Routes QUERY messages -
	 *  RULES: Route QUERY messages to all connections
	 *  except the originator
	 *
	 */
	void routeQueryMessage(RouteMessage m) {

		if (m.getConnection().getStatus() != NodeConnection.STATUS_OK || queryRouteTable.containsGUID(m.getMessage().getGUID())) {
			// Originating connection does not exist, so drop the message
			// If we received a response to the query, there would be no
			// connection to route it to
			
			// OR ELSE: the message has already been seen. don't forward or modify the routing table!!
			return;
		}

		// Make a local connection list to avoid concurrency L
		LinkedList<NodeConnection> listcopy = connectionList.getList();
		ListIterator<NodeConnection> iterator = listcopy.listIterator(0);

		prepareMessage(m.getMessage());

		while (iterator.hasNext()) {
			NodeConnection connection = (NodeConnection)iterator.next();

			if (!connection.equals(m.getConnection())
				&& connection.getStatus() == NodeConnection.STATUS_OK) {
				routerSend(connection, m.getMessage());

			}
		}

		// History of Queries
		queryRouteTable.put(m.getMessage().getGUID(), m.getConnection());

		// inform any search listener
		if (0 != searchReceivers.size()) {
			fireSearchMessage((SearchMessage)m.getMessage());
		} else {
			LOG.debug("no search receivers to fire the search message!");
		}
	}

	/**
	 *  Routes QUERY REPLY messages -
	 *  Rules: Route Query Replys to the connection
	 *  which had the Query
	 *
	 */
	void routeQueryReplyMessage(RouteMessage m) {
		NodeConnection originator = queryRouteTable.get(m.getMessage().getGUID());

		if (null != originator && originator.getStatus() == NodeConnection.STATUS_OK) {
			prepareMessage(m.getMessage());

			routerSend(originator, m.getMessage());

			// Record query hit route, for push forwarding
			// Push messages are routed by Servant ID
			queryHitRouteTable.put(
				((SearchReplyMessage)m.getMessage()).getClientIdentifier(),
				m.getConnection());
		}
		else {
			LOG.info("No connection for routing query reply");
		}
	}

	/**
	 *  Routes PUSH messages -
	 *  Rules: Route PUSH on the connection that had the QUERYHIT
	 *
	 *
	 */
	void routePushMessage(RouteMessage m) {

		PushMessage pushMessage = (PushMessage)m.getMessage();
		if (0 != pushReceivers.size()
			&& Utilities.getClientGUID().equals(pushMessage.getClientIdentifier())) {
			// this is a push request for the JTella servant
			firePushMessage(pushMessage);
			return;
		}

		NodeConnection originator = queryHitRouteTable.get(pushMessage.getClientIdentifier());

		if (null != originator) {
			prepareMessage(pushMessage);
			routerSend(originator, pushMessage);
		}
		else {
			LOG.info("No connection for routing push");
		}
	}

	/**
	 *  Updates a message for sending
	 *
	 *  @param message message to update
	 */
	void prepareMessage(Message message) {
		message.setTTL((byte) (message.getTTL() - 1));
		message.setHops((byte) (message.getHops() + 1));
	}

	/**
	 *  Utility method for common send
	 *
	 *  @param connection connection
	 *  @param message message
	 */
	boolean routerSend(Connection connection, Message message) {
		try {
			connection.send(message);
		}
		catch (IOException io) {
			Log.getLog().log(io);
		}

		return true; // TODO fix
	}

	/**
	 *  Performs some validation against network traffic
	 *
	 *  @return true if the message is acceptable, false otherwise
	 */
	boolean validateMessage(Message m) {

		//---------------------------------------------------------------
		//  The idea is to limit trafic, making sure hops doesn't exceed
		//  a reasonable amount - 7
		//---------------------------------------------------------------
		if (m.getHops() > MAX_HOPS) {
			LOG.info("Router dropped message exceeding max hops");
			return false;
		}

		if (m.getTTL() > MAX_TTL) {
			LOG.info("Router dropped message exceeding max ttl");
			return false;
		}

		if (m.getTTL() > MAX_HOPS && m.getTTL() < MAX_TTL) {
			LOG.info("Router adjusted message ttl to 7");
			m.setTTL(MAX_HOPS);
		}

		if ((m.getTTL() + m.getHops()) > MAX_HOPS) {
			LOG.info("Router adjusted message ttl to 7");
			m.setTTL((byte) (MAX_HOPS - m.getHops()));
		}

		return true;
	}

	/**
	 *  Sends a search message to listeners
	 *
	 *  @param searchMessage search message to send
	 */
	void fireSearchMessage(SearchMessage searchMessage) {
		Vector<MessageReceiver> v = (Vector<MessageReceiver>)searchReceivers.clone();
		Enumeration<MessageReceiver> e = v.elements();
		
		LOG.debug("Router :: firing search message");

		while (e.hasMoreElements()) {
			LOG.debug("Router :: firing to one receiver...");
			MessageReceiver receiver = (MessageReceiver)e.nextElement();
			receiver.receiveSearch((SearchMessage)searchMessage);
		}

	}

	/**
	 *  Sends a push message to listener(s)
	 *
	 *  @param pushMessage push message to send
	 */
	void firePushMessage(PushMessage pushMessage) {
		Vector<MessageReceiver> v = (Vector<MessageReceiver>)pushReceivers.clone();
		Enumeration<MessageReceiver> e = v.elements();

		while (e.hasMoreElements()) {
			MessageReceiver receiver = (MessageReceiver)e.nextElement();
			receiver.receivePush((PushMessage)pushMessage);
		}

	}

    HashMap<GUID, NodeConnection> getRouterTable(){
        return pingRouteTable.getTable();
    }
	/**
	 *  Records a message to route
	 *
	 */
	class RouteMessage {
		Message m;
		NodeConnection connection;

		RouteMessage(Message m, NodeConnection connection) {
			this.m = m;
			this.connection = connection;
		}

		/**
		 *
		 *
		 */
		Message getMessage() {
			return m;
		}

		/**
		 *
		 *
		 */
		NodeConnection getConnection() {
			return connection;
		}
	}
}
