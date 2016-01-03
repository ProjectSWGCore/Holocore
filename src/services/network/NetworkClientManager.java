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
import intents.network.ConnectionOpenedIntent;
import intents.network.OutboundPacketIntent;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
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
import resources.server_info.Config;
import resources.server_info.Log;
import utilities.ThreadUtilities;

public class NetworkClientManager extends Manager implements TCPCallback, PacketSender {
	
	private final Map <InetSocketAddress, Long> sockets;
	private final Map <Long, NetworkClient> clients;
	private final Queue<ReceivedPacket> receivedPackets;
	private final ExecutorService packetProcessor;
	private final Runnable processPacketRunnable;
	private final AtomicLong networkIdCounter;
	private final TCPServer tcpServer;
	
	public NetworkClientManager() {
		sockets = new HashMap<InetSocketAddress, Long>();
		clients = new Hashtable<Long, NetworkClient>();
		receivedPackets = new LinkedList<>();
		networkIdCounter = new AtomicLong(1);
		packetProcessor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), ThreadUtilities.newThreadFactory("packet-processor-%d"));
		processPacketRunnable = new Runnable() {
			public void run() {
				synchronized (receivedPackets) {
					ReceivedPacket recv = receivedPackets.poll();
					if (recv == null)
						return;
					handlePacket(recv);
				}
			}
		};
		tcpServer = new TCPServer(getBindAddr(), getBindPort(), getBufferSize());
		
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
		packetProcessor.shutdownNow();
		boolean success = true;
		try {
			success = packetProcessor.awaitTermination(5, TimeUnit.SECONDS);
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
		else
			Log.e(this, "Incoming connection has socket address of instance: %s", addr.getClass().getSimpleName());
	}
	
	@Override
	public void onConnectionDisconnect(Socket s) {
		SocketAddress addr = s.getRemoteSocketAddress();
		if (addr instanceof InetSocketAddress)
			onSessionDisconnect((InetSocketAddress) addr);
		else
			Log.e(this, "Connection Disconnected. Has socket address of instance: %s", addr.getClass().getSimpleName());
	}
	
	@Override
	public void onIncomingData(Socket s, byte [] data) {
		synchronized (receivedPackets) {
			SocketAddress addr = s.getRemoteSocketAddress();
			if (addr instanceof InetSocketAddress)
				receivedPackets.add(new ReceivedPacket((InetSocketAddress) addr, data));
			else
				Log.e(this, "Incoming data has socket address of instance: %s", addr.getClass().getSimpleName());
			packetProcessor.submit(processPacketRunnable);
		}
	}
	
	@Override
	public void sendPacket(InetSocketAddress sock, byte[] data) {
		tcpServer.send(sock, data);
	}
	
	private InetAddress getBindAddr() {
		Config c = getConfig(ConfigFile.NETWORK);
		String ip = c.getString("BIND-ADDR", "::1");
		try {
			return InetAddress.getByName(ip);
		} catch (UnknownHostException e) {
			System.err.println("NetworkListenerService: Unknown host for IP: " + ip);
		}
		return null;
	}
	
	private int getBindPort() {
		return getConfig(ConfigFile.NETWORK).getInt("BIND-PORT", 44463);
	}
	
	private int getBufferSize() {
		return getConfig(ConfigFile.NETWORK).getInt("BUFFER-SIZE", 1024);
	}
	
	private void createSession(long networkId, InetSocketAddress address) {
		synchronized (clients) {
			sockets.put(address, networkId);
			clients.put(networkId, new NetworkClient(address, networkId, this));
			new ConnectionOpenedIntent(networkId).broadcast();
			System.out.println("Created Session: " + networkId + " / " + address);
		}
	}
	
	private void onSessionDisconnect(InetSocketAddress address) {
		synchronized (clients) {
			Long networkId = sockets.get(address);
			if (networkId != null) {
				deleteSession(networkId);
				new ConnectionClosedIntent(networkId, DisconnectReason.OTHER_SIDE_TERMINATED).broadcast();
			}
		}
	}
	
	private void deleteSession(long networkId) {
		synchronized (clients) {
			NetworkClient client = clients.get(networkId);
			if (client == null)
				return;
			clients.remove(networkId);
			sockets.remove(client.getAddress());
		}
	}
	
	private void handleOutboundPacket(long networkId, Packet p) {
		synchronized (clients) {
			NetworkClient client = clients.get(networkId);
			if (client != null)
				client.sendPacket(p);
			else
				Log.w(this, "NetworkClient does not exist for ID: %d", networkId);
		}
	}
	
	private void handlePacket(ReceivedPacket packet) {
		synchronized (clients) {
			Long netId = sockets.get(packet.getAddress());
			if (netId == null) {
				Log.w(this, "Unknown socket address! Address: %s", packet.getAddress());
				return;
			}
			NetworkClient client = clients.get(netId);
			if (client != null)
				client.process(packet.getData());
			else
				Log.w(this, "Unknown connection! Network ID: %d  Address: %s", netId, packet.getAddress());
		}
	}
	
	private static class ReceivedPacket {
		private final InetSocketAddress address;
		private final byte [] data;
		
		public ReceivedPacket(InetSocketAddress address, byte [] data) {
			this.address = address;
			this.data = data;
		}
		
		public InetSocketAddress getAddress() {
			return address;
		}
		
		public byte [] getData() {
			return data;
		}
	}
	
}
