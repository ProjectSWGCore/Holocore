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

import intents.network.OutboundPacketIntent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import network.NetworkClient;
import resources.config.ConfigFile;
import resources.control.Intent;
import resources.control.Service;
import resources.network.TCPServer;
import resources.server_info.Log;
import utilities.ThreadUtilities;

public class OutboundNetworkManager extends Service {
	
	private final ClientManager clientManager;
	private final Deque<NetworkClient> outboundQueue;
	private final ExecutorService outboundProcessor;
	private final PacketSender sender;
	private final AtomicBoolean running;
	private final int threadCount;
	
	public OutboundNetworkManager(TCPServer server, ClientManager clientManager) {
		this.sender = new PacketSender(server);
		this.clientManager = clientManager;
		threadCount = getConfig(ConfigFile.NETWORK).getInt("PACKET-THREAD-COUNT", 10);
		outboundQueue = new ArrayDeque<>();
		outboundProcessor = Executors.newFixedThreadPool(threadCount, ThreadUtilities.newThreadFactory("outbound-packet-processor-%d"));
		this.running = new AtomicBoolean(false);
		
		registerForIntent(OutboundPacketIntent.TYPE);
	}
	
	@Override
	public boolean initialize() {
		running.set(true);
		Runnable processBufferRunnable = () -> processOutboundRunnable();
		for (int i = 0; i < threadCount; i++) {
			outboundProcessor.execute(processBufferRunnable);
		}
		return super.initialize();
	}
	
	@Override
	public boolean terminate() {
		running.set(false);
		synchronized (outboundQueue) {
			outboundQueue.notifyAll();
		}
		outboundProcessor.shutdown();
		try {
			outboundProcessor.awaitTermination(3, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Log.e(this, e);
		}
		return super.terminate();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof OutboundPacketIntent) {
			OutboundPacketIntent opi = (OutboundPacketIntent) i;
			NetworkClient client = clientManager.getClient(opi.getNetworkId());
			if (client == null)
				return;
			client.addToOutbound(opi.getPacket());
			synchronized (outboundQueue) {
				outboundQueue.addLast(client);
				outboundQueue.notify();
			}
		}
	}
	
	public void onSessionCreated(NetworkClient client) {
		client.setPacketSender(sender);
	}
	
	public void onSessionDestroyed(NetworkClient client) {
		synchronized (outboundQueue) {
			outboundQueue.remove(client);
		}
	}
	
	private void processOutboundRunnable() {
		try {
			while (running.get()) {
				NetworkClient client = null;
				synchronized (outboundQueue) {
					try {
						if (outboundQueue.isEmpty())
							outboundQueue.wait();
					} catch (InterruptedException e) {
						Log.e(this, e);
					}
					client = outboundQueue.pollFirst();
				}
				if (client == null)
					continue;
				client.processOutbound();
			}
		} catch (Exception e) {
			Log.e(this, e);
		}
	}
	
}
