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

//import protocol.com.kenmccrary.jtella.util.Log;

/** 
 *  Session that can be used to respond to Query messages and receive
 *  Push messages
 *
 */
public class FileServerSession {
	private Router router;
	private MessageReceiver receiver;

	/** Name of Logger used by this class. */
    public static final String LOGGER = "com.kenmccrary.jtella";

    private static Logger LOG = Logger.getLogger(LOGGER);

	/**
	 *  Constructs the <code>FileServerSession</code>, not visible
	 *  to application
	 *
	 */
	FileServerSession(Router router, MessageReceiver receiver) {
		this.router = router;
		this.receiver = receiver;
		router.addSearchMessageReceiver(receiver);
		router.addPushMessageReceiver(receiver);
	}

	/**
	 *  Closes the session
	 *
	 */
	public void close() {
		router.removeSearchMessageReceiver(receiver);
		router.removePushMessageReceiver(receiver);
	}

	/**
	 *  An application should call <code>queryHit</code> to indicate that
	 *  a search query is satisfied
	 *
	 *  @param request search message which has a hit
	 *  @param response hit information
	 */
	public void queryHit(SearchMessage request, SearchReplyMessage response) {

		NodeConnection connection = router.getQuerySource(request);

		try {
			if (null != connection) {
				connection.prioritySend(response);
			}
			else {
				LOG.debug(
					"Null connection for query: \n" + request.toString());
			}
		}
		catch (Exception e) {
			LOG.error(e);
		}

	}
}
