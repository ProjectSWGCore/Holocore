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
import intents.network.InboundPacketIntent;
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
import network.NetworkClient.ClientStatus;
import network.PacketSender;
import network.packets.Packet;
import network.packets.swg.ErrorMessage;
import network.packets.swg.holo.HoloConnectionStarted;
import network.packets.swg.holo.HoloConnectionStopped;
import network.packets.swg.holo.HoloPacket;
import network.packets.swg.holo.HoloSetProtocolVersion;
import network.packets.swg.holo.HoloConnectionStopped.ConnectionStoppedReason;
import resources.config.ConfigFile;
import resources.control.Intent;
import resources.control.Manager;
import resources.network.DisconnectReason;
import resources.network.TCPServer;
import resources.network.TCPServer.TCPCallback;
import resources.server_info.Log;
import utilities.ThreadUtilities;

public class NetworkClientManager extends Manager implements TCPCallback, PacketSender {
	
	private static final String PROTOCOL = "2016-04-13";
	
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
		
		registerForIntent(InboundPacketIntent.TYPE);
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
		switch (i.getType()) {
			case InboundPacketIntent.TYPE:
				if (i instanceof InboundPacketIntent)
					processInboundPacketIntent((InboundPacketIntent) i);
				break;
			case OutboundPacketIntent.TYPE:
				if (i instanceof OutboundPacketIntent)
					processOutboundPacketIntent((OutboundPacketIntent) i);
				break;
			case CloseConnectionIntent.TYPE:
				if (i instanceof CloseConnectionIntent)
					processCloseConnectionIntent((CloseConnectionIntent) i);
				break;
		}
	}
	
	private void processInboundPacketIntent(InboundPacketIntent i) {
		if (!(i.getPacket() instanceof HoloPacket)) {
			NetworkClient client = getClient(i.getNetworkId());
			if (client != null && client.getStatus() != ClientStatus.CONNECTED) {
				client.addToOutbound(new ErrorMessage("Network Manager", "Upgrade your launcher!", false));
				client.processOutbound();
				deleteSession(i.getNetworkId(), ConnectionStoppedReason.INVALID_PROTOCOL);
			}
			return;
		}
		HoloPacket packet = (HoloPacket) i.getPacket();
		if (packet instanceof HoloSetProtocolVersion)
			processSetProtocolVersion((HoloSetProtocolVersion) packet, i.getNetworkId());
	}
	
	private void processOutboundPacketIntent(OutboundPacketIntent i) {
		Packet p = i.getPacket();
		if (p != null)
			handleOutboundPacket(i.getNetworkId(), p);
	}
	
	private void processCloseConnectionIntent(CloseConnectionIntent i) {
		deleteSession(i.getNetworkId(), getHolocoreReason(i.getDisconnectReason()));
	}
	
	private void processSetProtocolVersion(HoloSetProtocolVersion packet, long networkId) {
		NetworkClient client = getClient(networkId);
		if (client == null) {
			Log.w(this, "NetworkClient not found for ID: %d!", networkId);
			deleteSession(networkId, ConnectionStoppedReason.SERVER_ERROR);
			return;
		}
		if (!packet.getProtocol().equals(PROTOCOL)) {
			Log.w(this, "Incoming connection has incorrect protocol version! Expected: %s  Actual: %s", PROTOCOL, packet.getProtocol());
			deleteSession(networkId, ConnectionStoppedReason.INVALID_PROTOCOL);
			return;
		}
		client.onConnected();
		Intent i = new OutboundPacketIntent(new HoloSetProtocolVersion(PROTOCOL), networkId);
		i.broadcast();
		new OutboundPacketIntent(new HoloConnectionStarted(), networkId).broadcastAfterIntent(i);
	}
	
	private ConnectionStoppedReason getHolocoreReason(DisconnectReason reason) {
		switch (reason) {
			case APPLICATION:
				return ConnectionStoppedReason.APPLICATION;
			case CONNECTION_REFUSED:
				return ConnectionStoppedReason.NETWORK;
			case NEW_CONNECTION_ATTEMPT:
				return ConnectionStoppedReason.NETWORK;
			case OTHER_SIDE_TERMINATED:
				return ConnectionStoppedReason.OTHER_SIDE_TERMINATED;
			case TIMEOUT:
				return ConnectionStoppedReason.NETWORK;
		}
		return ConnectionStoppedReason.APPLICATION;
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
	}
	
	private void onSessionDisconnect(InetSocketAddress address) {
		Long networkId;
		synchronized (clients) {
			networkId = sockets.get(address);
		}
		if (networkId != null) {
			deleteSession(networkId, ConnectionStoppedReason.OTHER_SIDE_TERMINATED);
			new ConnectionClosedIntent(networkId, DisconnectReason.OTHER_SIDE_TERMINATED).broadcast();
		} else {
			Log.w(this, "Network ID not found for " + address + "!");
		}
	}
	
	private void deleteSession(long networkId, ConnectionStoppedReason reason) {
		synchronized (clients) {
			NetworkClient client = clients.remove(networkId);
			if (client == null) {
				Log.w(this, "No NetworkClient found for network id: " + networkId);
				return;
			}
			client.addToOutbound(new HoloConnectionStopped(reason));
			client.processOutbound();
			tcpServer.disconnect(client.getAddress());
			sockets.remove(client.getAddress());
			synchronized (inboundQueue) {
				inboundQueue.remove(client);
			}
			synchronized (outboundQueue) {
				outboundQueue.remove(client);
			}
			client.onDisconnected();
			client.close();
		}
	}
	
	private void handleOutboundPacket(long networkId, Packet p) {
		NetworkClient client = getClient(networkId);
		if (client != null) {
			client.addToOutbound(p);
			synchronized (outboundQueue) {
				if (!outboundQueue.contains(client)) {
					outboundQueue.add(client);
					outboundProcessor.execute(processOutboundRunnable);
				}
			}
		}
	}
	
	private void handleIncomingData(InetSocketAddress addr, byte [] data) {
		Long netId = sockets.get(addr);
		if (netId == null) {
			Log.w(this, "Unknown socket address! Address: %s", addr);
			return;
		}
		NetworkClient client = getClient(netId);
		if (client != null) {
			client.addToBuffer(data);
			synchronized (inboundQueue) {
				if (!inboundQueue.contains(client)) {
					inboundQueue.add(client);
					inboundProcessor.execute(processBufferRunnable);
				}
			}
		} else
			Log.w(this, "Unknown connection! Network ID: %d  Address: %s", netId, addr);
	}
	
	private NetworkClient getClient(long networkId) {
		synchronized (clients) {
			return clients.get(networkId);
		}
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
