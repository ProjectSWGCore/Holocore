/***********************************************************************************
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

import intents.network.CloseConnectionIntent;
import intents.network.ConnectionClosedIntent;
import intents.network.OutboundPacketIntent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import network.NetworkClient;
import network.PacketSender;
import network.packets.Packet;
import resources.config.ConfigFile;
import resources.control.Intent;
import resources.control.Manager;
import resources.network.DisconnectReason;
import resources.network.TCPServer;
import resources.network.TCPServer.TCPCallback;
import resources.server_info.Log;
import utilities.ThreadUtilities;

public class NetworkClientManager extends Manager implements TCPCallback, PacketSender {
	
	private final Map <InetSocketAddress, Long> sockets;
	private final Map <Long, NetworkClient> clients;
	private final Queue<NetworkClient> inboundQueue;
	private final Queue<NetworkClient> outboundQueue;
	private final ExecutorService inboundProcessor;
	private final ExecutorService outboundProcessor;
	private final Runnable processBufferRunnable;
	private final Runnable processOutboundRunnable;
	private final AtomicLong networkIdCounter;
	private final TCPServer tcpServer;
	
	public NetworkClientManager() {
		final int threadCount = getConfig(ConfigFile.NETWORK).getInt("PACKET-THREAD-COUNT", 10);
		sockets = new HashMap<InetSocketAddress, Long>();
		clients = new Hashtable<Long, NetworkClient>();
		inboundQueue = new LinkedList<>();
		outboundQueue = new LinkedList<>();
		networkIdCounter = new AtomicLong(1);
		inboundProcessor = Executors.newFixedThreadPool(threadCount, ThreadUtilities.newThreadFactory("inbound-packet-processor-%d"));
		outboundProcessor = Executors.newFixedThreadPool(threadCount, ThreadUtilities.newThreadFactory("outbound-packet-processor-%d"));
		processBufferRunnable = () -> processBufferRunnable();
		processOutboundRunnable = () -> processOutboundRunnable();
		tcpServer = new TCPServer(getBindPort(), getBufferSize());
		
		registerForIntent(OutboundPacketIntent.TYPE);
		registerForIntent(CloseConnectionIntent.TYPE);
	}
	
	@Override
	public boolean start() {
		try {
			tcpServer.bind();
			tcpServer.setCallback(this);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return super.start();
	}
	
	@Override
	public boolean stop() {
		tcpServer.close();
		return super.stop();
	}
	
	@Override
	public boolean terminate() {
		inboundProcessor.shutdownNow();
		outboundProcessor.shutdownNow();
		boolean success = true;
		try {
			success = inboundProcessor.awaitTermination(5, TimeUnit.SECONDS);
			success = outboundProcessor.awaitTermination(5, TimeUnit.SECONDS) && success;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return super.terminate() && success;
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof OutboundPacketIntent) {
			Packet p = ((OutboundPacketIntent)i).getPacket();
			if (p != null)
				handleOutboundPacket(((OutboundPacketIntent) i).getNetworkId(), p);
		} else if (i instanceof CloseConnectionIntent) {
			long netId = ((CloseConnectionIntent)i).getNetworkId();
			deleteSession(netId);
		}
	}
	
	@Override
	public void onIncomingConnection(Socket s) {
		SocketAddress addr = s.getRemoteSocketAddress();
		if (addr instanceof InetSocketAddress)
			createSession(networkIdCounter.incrementAndGet(), (InetSocketAddress) addr);
		else if (addr != null)
			Log.e(this, "Incoming connection has socket address of instance: %s", addr.getClass().getSimpleName());
	}
	
	@Override
	public void onConnectionDisconnect(Socket s, SocketAddress addr) {
		Log.i(this, "Disconnected from %s", addr);
		if (addr instanceof InetSocketAddress)
			onSessionDisconnect((InetSocketAddress) addr);
		else if (addr != null)
			Log.e(this, "Connection Disconnected. Has socket address of instance: %s", addr.getClass().getSimpleName());
	}
	
	@Override
	public void onIncomingData(Socket s, byte [] data) {
		SocketAddress addr = s.getRemoteSocketAddress();
		if (addr instanceof InetSocketAddress)
			handleIncomingData((InetSocketAddress) addr, data);
		else if (addr != null)
			Log.e(this, "Incoming data has socket address of instance: %s", addr.getClass().getSimpleName());
	}
	
	@Override
	public void sendPacket(InetSocketAddress sock, ByteBuffer data) {
		tcpServer.send(sock, data);
	}
	
	private int getBindPort() {
		return getConfig(ConfigFile.NETWORK).getInt("BIND-PORT", 44463);
	}
	
	private int getBufferSize() {
		return getConfig(ConfigFile.NETWORK).getInt("BUFFER-SIZE", 4096);
	}
	
	private void createSession(long networkId, InetSocketAddress address) {
		NetworkClient client = new NetworkClient(address, networkId, this);
		synchronized (clients) {
			sockets.put(address, networkId);
			clients.put(networkId, client);
		}
		client.onConnected();
	}
	
	private void onSessionDisconnect(InetSocketAddress address) {
		Long networkId;
		synchronized (clients) {
			networkId = sockets.get(address);
		}
		if (networkId != null) {
			deleteSession(networkId);
			new ConnectionClosedIntent(networkId, DisconnectReason.OTHER_SIDE_TERMINATED).broadcast();
		} else {
			System.err.println("Network ID not found for " + address + "!");
		}
	}
	
	private void deleteSession(long networkId) {
		synchronized (clients) {
			NetworkClient client = clients.remove(networkId);
			if (client == null) {
				System.err.println("No NetworkClient found for network id: " + networkId);
				return;
			}
			sockets.remove(client.getAddress());
			synchronized (inboundQueue) {
				inboundQueue.remove(client);
			}
			synchronized (outboundQueue) {
				outboundQueue.remove(client);
			}
			client.close();
		}
	}
	
	private void handleOutboundPacket(long networkId, Packet p) {
		NetworkClient client;
		synchronized (clients) {
			client = clients.get(networkId);
		}
		if (client != null) {
			client.addToOutbound(p);
			synchronized (outboundQueue) {
				while (outboundQueue.remove(client));
				outboundQueue.add(client);
			}
			outboundProcessor.execute(processOutboundRunnable);
		}
	}
	
	private void handleIncomingData(InetSocketAddress addr, byte [] data) {
		Long netId = sockets.get(addr);
		if (netId == null) {
			Log.w(this, "Unknown socket address! Address: %s", addr);
			return;
		}
		NetworkClient client;
		synchronized (clients) {
			client = clients.get(netId);
		}
		if (client != null) {
			client.addToBuffer(data);
			synchronized (inboundQueue) {
				inboundQueue.add(client);
			}
			inboundProcessor.execute(processBufferRunnable);
		} else
			Log.w(this, "Unknown connection! Network ID: %d  Address: %s", netId, addr);
	}
	
	private void processBufferRunnable() {
		try {
			NetworkClient client;
			synchronized (inboundQueue) {
				client = inboundQueue.poll();
				if (client == null)
					return;
			}
			client.processInbound();
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(this, e);
		}
	}
	
	private void processOutboundRunnable() {
		try {
			NetworkClient client;
			synchronized (outboundQueue) {
				client = outboundQueue.poll();
				if (client == null)
					return;
			}
			client.processOutbound();
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(this, e);
		}
	}
	
}
