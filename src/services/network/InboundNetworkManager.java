/************************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
 *                                                                                  *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
 * Our goal is to create an emulator which will provide a server for players to     *
 * continue playing a game similar to the one they used to play. We are basing      *
 * it on the final publish of the game prior to end-game events.                    *
 *                                                                                  *
 * This file is part of Holocore.                                                   *
 *                                                                                  *
 * -------------------------------------------------------------------------------- *
 *                                                                                  *
 * Holocore is free software: you can redistribute it and/or modify                 *
 * it under the terms of the GNU Affero General Public License as                   *
 * published by the Free Software Foundation, either version 3 of the               *
 * License, or (at your option) any later version.                                  *
 *                                                                                  *
 * Holocore is distributed in the hope that it will be useful,                      *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
 * GNU Affero General Public License for more details.                              *
 *                                                                                  *
 * You should have received a copy of the GNU Affero General Public License         *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
 *                                                                                  *
 ***********************************************************************************/
package services.network;

import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import network.NetworkClient;
import resources.config.ConfigFile;
import resources.control.Service;
import resources.server_info.Log;
import utilities.ThreadUtilities;

public class InboundNetworkManager extends Service {
	
	private final ClientManager clientManager;
	private final Deque<NetworkClient> inboundQueue;
	private final ExecutorService inboundProcessor;
	private final AtomicBoolean running;
	private final int threadCount;
	
	public InboundNetworkManager(ClientManager clientManager) {
		this.clientManager = clientManager;
		this.inboundQueue = new ArrayDeque<>();
		threadCount = getConfig(ConfigFile.NETWORK).getInt("PACKET-THREAD-COUNT", 10);
		this.inboundProcessor = Executors.newFixedThreadPool(threadCount, ThreadUtilities.newThreadFactory("inbound-packet-processor-%d"));
		this.running = new AtomicBoolean(false);
	}
	
	public boolean initialize() {
		running.set(true);
		Runnable processBufferRunnable = () -> processBufferRunnable();
		for (int i = 0; i < threadCount; i++) {
			inboundProcessor.execute(processBufferRunnable);
		}
		return super.initialize();
	}
	
	public boolean terminate() {
		running.set(false);
		synchronized (inboundQueue) {
			inboundQueue.notifyAll();
		}
		inboundProcessor.shutdown();
		try {
			inboundProcessor.awaitTermination(3, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Log.e(this, e);
		}
		return super.terminate();
	}
	
	public void onInboundData(InetSocketAddress addr, byte [] data) {
		NetworkClient client = clientManager.getClient(addr);
		client.addToBuffer(data);
		synchronized (inboundQueue) {
			inboundQueue.addLast(client);
			inboundQueue.notify();
		}
	}
	
	public void onSessionCreated(NetworkClient client) {
		
	}
	
	public void onSessionDestroyed(NetworkClient client) {
		synchronized (inboundQueue) {
			inboundQueue.remove(client);
		}
	}
	
	private void processBufferRunnable() {
		try {
			while (running.get()) {
				NetworkClient client = null;
				synchronized (inboundQueue) {
					try {
						if (inboundQueue.isEmpty())
							inboundQueue.wait();
					} catch (InterruptedException e) {
						Log.e(this, e);
					}
					client = inboundQueue.pollFirst();
				}
				if (client == null)
					continue;
				client.processInbound();
			}
		} catch (Exception e) {
			Log.e(this, e);
		}
	}
	
}
