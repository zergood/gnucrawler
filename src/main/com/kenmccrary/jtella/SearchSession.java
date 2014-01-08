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

import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 *  A session for initiating searches on the network
 *
 */
public class SearchSession {
	private MessageReceiver receiver;
	private GNUTellaConnection connection;
	private Router router;
	private SendThread sendThread;
	private String query;
	private int queryType;
	//private int maxResults; //not used at this point
	private int minSpeed;
	private List<GUID> searchGUIDList;
	/** Name of Logger used by this class. */
    public static final String LOGGER = "com.kenmccrary.jtella";
    private static Logger LOG = Logger.getLogger(LOGGER);


	SearchSession(
		String query,
		int queryType,
		int maxResults,
		int minSpeed,
		GNUTellaConnection connection,
		Router router,
		MessageReceiver receiver) {
			this.connection = connection;
			this.receiver = receiver;
			this.router = router;
			this.query = query;
			this.queryType = queryType;
			//this.maxResults = maxResults;
			this.minSpeed = minSpeed;
			searchGUIDList = Collections.synchronizedList(new LinkedList<GUID>());
			sendThread = new SendThread();
			sendThread.start();
	}

	/**
	 *   Request a replying servant push a file
	 *
	 *   @param searchReplyMessage search reply containing file to push
	 *   @param pushMessage push message
	 *   @return true if the message could be sent
	 */
	public static boolean sendPushMessage(
		SearchReplyMessage searchReplyMessage,
		PushMessage pushMessage) {
		// the push message will be sent on the connection the searchReply
		// arrived on if it is available
		LOG.debug("In sendPushmessage");
		Connection connection = searchReplyMessage.getOriginatingConnection();

		/*Log.getLog().logInformation*/
		System.out.println("qr connection status: " + connection.getStatus());
		if (connection.getStatus() == NodeConnection.STATUS_OK) {
			try {

				LOG.debug("Sending push");
				connection.prioritySend(pushMessage);
				return true;
			}
			catch (IOException io) {}
		}

		return false;
	}

	/**
	 *  Close the session, ignore future query hits
	 *
	 */
	public void close() {
		// When the session closes, clean out the origination information
		// stored for this session
		router.removeMessageSender(searchGUIDList);
		sendThread.shutdown();
	}

	/**
	 *  Continuously monitors for newly formed active connections to send
	 *  to
	 *
	 */
	class SendThread extends Thread {
		private LinkedList<NodeConnection> sentConnections;
		private boolean shutdown;

		SendThread() {
			super("SearchSession$SendThread");
			sentConnections = new LinkedList<NodeConnection>();
			shutdown = false;
		}

		/**
		 *  Cease operation
		 */
		void shutdown() {
			shutdown = true;
			interrupt();
		}

		/**
		 *  Working loop
		 *
		 */
		public void run() {
			while (!shutdown) {
				try {
					// Get all the active connections
					List<NodeConnection> activeList =
						connection.getConnections().getActiveConnections();

					LOG.debug("Active connection list has "+ activeList.size()+" hosts");
					LOG.debug("Sent connections: "+ sentConnections.size()+" hosts");
					// Check if any new connections exists
					activeList.removeAll(sentConnections);

					for (int i = 0; i < activeList.size(); i++) {
						NodeConnection nodeConnection =
							(NodeConnection)activeList.get(i);

						SearchMessage searchMessage =
							new SearchMessage(query, queryType, minSpeed);
						searchGUIDList.add(searchMessage.getGUID());
						nodeConnection.sendAndReceive(searchMessage, receiver);
						sentConnections.add(nodeConnection);
					}

					sleep(5000);
				}
				catch (Exception e) {
					// keep running
				}
			}
		}
	}
}
