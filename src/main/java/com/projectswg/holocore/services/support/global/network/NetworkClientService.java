/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package com.projectswg.holocore.services.support.global.network;

import com.projectswg.common.network.NetBuffer;
import com.projectswg.common.network.packets.swg.holo.HoloConnectionStopped.ConnectionStoppedReason;
import com.projectswg.holocore.ProjectSWG;
import com.projectswg.holocore.ProjectSWG.CoreException;
import com.projectswg.holocore.intents.support.global.network.CloseConnectionIntent;
import com.projectswg.holocore.intents.support.global.network.ConnectionClosedIntent;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase;
import com.projectswg.holocore.resources.support.global.network.NetworkClient;
import com.projectswg.holocore.resources.support.global.network.UDPServer;
import com.projectswg.holocore.resources.support.global.network.UDPServer.UDPPacket;
import com.projectswg.holocore.resources.support.global.player.Player;
import me.joshlarson.jlcommon.concurrency.BasicThread;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkClientService extends Service {
	
	private static final int INBOUND_BUFFER_SIZE = 4096;
	
	private final ServerSocketChannel tcpServer;
	private final BasicThread acceptThreadPool;
	private final Map<Long, NetworkClient> clients;
	private final ByteBuffer inboundBuffer;
	private final UDPServer udpServer;
	private volatile boolean operational;
	
	public NetworkClientService() {
		this.acceptThreadPool = new BasicThread("network-client-accept", this::acceptLoop);
		this.clients = new ConcurrentHashMap<>();
		this.inboundBuffer = ByteBuffer.allocate(INBOUND_BUFFER_SIZE);
		this.operational = true;
		{
			int bindPort = getBindPort();
			try {
				tcpServer = ServerSocketChannel.open();
				tcpServer.bind(new InetSocketAddress(bindPort), 64);
				tcpServer.configureBlocking(false);
				udpServer = new UDPServer(bindPort, 32);
			} catch (IOException e) {
				throw new CoreException("Failed to start networking", e);
			}
			udpServer.setCallback(this::onUdpPacket);
		}
	}
	
	@Override
	public boolean start() {
		acceptThreadPool.start();
		return true;
	}
	
	@Override
	public boolean isOperational() {
		return operational;
	}
	
	@Override
	public boolean stop() {
		for (NetworkClient client : clients.values())
			client.close(ConnectionStoppedReason.APPLICATION);
		
		try {
			tcpServer.close();
		} catch (IOException e) {
			Log.w("Failed to close TCP server");
		}
		acceptThreadPool.stop(true);
		return acceptThreadPool.awaitTermination(1000);
	}
	
	@Override
	public boolean terminate() {
		udpServer.close();
		return super.terminate();
	}
	
	private void acceptLoop() {
		try (Selector selector = Selector.open()) {
			tcpServer.register(selector, SelectionKey.OP_ACCEPT);
			while (tcpServer.isOpen()) {
				selector.select();
				Iterator<SelectionKey> it = selector.selectedKeys().iterator();
				while (it.hasNext()) {
					SelectionKey key = it.next();
					if (key.isAcceptable()) {
						acceptConnection(selector);
					}
					if (key.isReadable()) {
						read(key);
					}
					it.remove();
				}
			}
		} catch (IOException e) {
			Log.a(e);
		} finally {
			operational = false;
		}
	}
	
	private void acceptConnection(Selector selector) {
		try {
			SocketChannel client = tcpServer.accept();
			if (client != null) {
				client.configureBlocking(false);
				NetworkClient networkClient = new NetworkClient(client);
				client.register(selector, SelectionKey.OP_READ, networkClient);
				clients.put(networkClient.getId(), networkClient);
			}
		} catch (Throwable t) {
			Log.w("%s: Failed to accept connection", getClass().getSimpleName());
		}
	}
	
	private void read(SelectionKey key) {
		Player player = null;
		try {
			SocketChannel channel = (SocketChannel) key.channel();
			NetworkClient client = (NetworkClient) key.attachment();
			player = client.getPlayer();
			inboundBuffer.clear();
			channel.read(inboundBuffer);
			inboundBuffer.flip();
			client.addToInbound(inboundBuffer);
		} catch (Throwable t) {
			if (player != null)
				StandardLog.onPlayerError(this, player, "failed to read data");
			else
				Log.w("%s: Failed to read data", getClass().getSimpleName());
		}
	}
	
	private void disconnect(long networkId) {
		disconnect(clients.get(networkId));
	}
	
	private void disconnect(NetworkClient client) {
		if (client == null)
			return;
		
		client.close(ConnectionStoppedReason.APPLICATION);
	}
	
	private void onUdpPacket(UDPPacket packet) {
		if (packet.getLength() <= 0)
			return;
		if (packet.getData()[0] == 1) {
			sendState(packet.getAddress(), packet.getPort());
		}
	}
	
	private void sendState(InetAddress addr, int port) {
		String status = ProjectSWG.getGalaxy().getStatus().name();
		NetBuffer data = NetBuffer.allocate(3 + status.length());
		data.addByte(1);
		data.addAscii(status);
		udpServer.send(port, addr, data.array());
	}
	
	@IntentHandler
	private void handleCloseConnectionIntent(CloseConnectionIntent ccii) {
		disconnect(ccii.getPlayer().getNetworkId());
	}
	
	@IntentHandler
	private void handleConnectionClosedIntent(ConnectionClosedIntent cci) {
		disconnect(cci.getPlayer().getNetworkId());
	}
	
	private int getBindPort() {
		return PswgDatabase.INSTANCE.getConfig().getInt(this, "bindPort", 44463);
	}
	
}
