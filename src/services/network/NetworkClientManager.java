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

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

import com.projectswg.common.control.Manager;
import com.projectswg.common.debug.Log;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.common.network.TCPServer;
import com.projectswg.common.network.TCPServer.TCPCallback;
import com.projectswg.common.network.packets.swg.holo.HoloConnectionStopped.ConnectionStoppedReason;

import intents.network.CloseConnectionIntent;
import main.ProjectSWG.CoreException;
import network.AdminNetworkClient;
import network.NetworkClient;
import resources.config.ConfigFile;
import resources.network.UDPServer;
import resources.network.UDPServer.UDPPacket;
import resources.server_info.DataManager;
import services.CoreManager;

public class NetworkClientManager extends Manager {
	
	private final InboundNetworkManager inboundManager;
	private final OutboundNetworkManager outboundManager;
	private final ClientManager clientManager;
	private final TCPServer tcpServer;
	private final UDPServer udpServer;
	private final TCPServer adminServer;
	
	public NetworkClientManager() {
		clientManager = new ClientManager();
		tcpServer = new TCPServer(getBindPort(), getBufferSize());
		try {
			udpServer = new UDPServer(getBindPort(), 1024);
		} catch (SocketException e) {
			throw new CoreException("Socket Exception on UDP bind: " + e);
		}
		int adminServerPort = CoreManager.getGalaxy().getAdminServerPort();
		if (adminServerPort <= 0)
			adminServer = null;
		else
			adminServer = new TCPServer(InetAddress.getLoopbackAddress(), adminServerPort, 1024);
		udpServer.setCallback(SWGPacket -> onUdpPacket(SWGPacket));
		inboundManager = new InboundNetworkManager(clientManager);
		outboundManager = new OutboundNetworkManager(tcpServer, clientManager);
		
		addChildService(inboundManager);
		addChildService(outboundManager);
		
		registerForIntent(CloseConnectionIntent.class, cci -> handleCloseConnectionIntent(cci));
	}
	
	@Override
	public boolean start() {
		try {
			tcpServer.bind();
			if (adminServer != null) {
				adminServer.bind();
				adminServer.setCallback(new AdminNetworkCallback());
			}
			tcpServer.setCallback(new StandardNetworkCallback());
		} catch (IOException e) {
			Log.e(e);
			if (e instanceof BindException)
				Log.e("Failed to bind to " + getBindPort());
			return false;
		}
		return super.start();
	}
	
	@Override
	public boolean stop() {
		tcpServer.close();
		if (adminServer != null) {
			adminServer.close();
		}
		return super.stop();
	}
	
	@Override
	public boolean terminate() {
		udpServer.close();
		return super.terminate();
	}
	
	private void handleCloseConnectionIntent(CloseConnectionIntent ccii) {
		NetworkClient client = clientManager.getClient(ccii.getNetworkId());
		if (client != null) {
			if (client instanceof AdminNetworkClient)
				adminServer.disconnect(client.getAddress());
			else
				tcpServer.disconnect(client.getAddress());
		}
	}
	
	private int getBindPort() {
		return DataManager.getConfig(ConfigFile.NETWORK).getInt("BIND-PORT", 44463);
	}
	
	private int getBufferSize() {
		return DataManager.getConfig(ConfigFile.NETWORK).getInt("BUFFER-SIZE", 4096);
	}
	
	private void onUdpPacket(UDPPacket SWGPacket) {
		if (SWGPacket.getLength() <= 0)
			return;
		switch (SWGPacket.getData()[0]) {
			case 1: sendState(SWGPacket.getAddress(), SWGPacket.getPort()); break;
			default: break;
		}
	}
	
	private void sendState(InetAddress addr, int port) {
		String status = CoreManager.getGalaxy().getStatus().name();
		NetBuffer data = NetBuffer.allocate(3 + status.length());
		data.addByte(1);
		data.addAscii(status);
		udpServer.send(port, addr, data.array());
	}
	
	private class StandardNetworkCallback implements TCPCallback {
		
		private final PacketSender sender;
		
		public StandardNetworkCallback() {
			this.sender = new PacketSender(tcpServer);
		}
		
		@Override
		public void onIncomingConnection(SocketChannel s, SocketAddress addr) {
			NetworkClient client = clientManager.createSession(addr, sender);
			client.onConnected();
			inboundManager.onSessionCreated(client);
			outboundManager.onSessionCreated(client);
		}
		
		@Override
		public void onConnectionDisconnect(SocketChannel s, SocketAddress addr) {
			NetworkClient client = clientManager.getClient(addr);
			if (client != null) {
				client.onDisconnected(ConnectionStoppedReason.APPLICATION);
				client.onSessionDestroyed();
				inboundManager.onSessionDestroyed(client);
				outboundManager.onSessionDestroyed(client);
			}
			tcpServer.disconnect(addr);
		}
		
		@Override
		public void onIncomingData(SocketChannel s, SocketAddress addr, byte [] data) {
			inboundManager.onInboundData(addr, data);
		}
		
	}
	
	private class AdminNetworkCallback implements TCPCallback {
		
		private final PacketSender sender;
		
		public AdminNetworkCallback() {
			this.sender = new PacketSender(adminServer);
		}
		
		@Override
		public void onIncomingConnection(SocketChannel s, SocketAddress addr) {
			NetworkClient client = clientManager.createAdminSession(addr, sender);
			client.onConnected();
			inboundManager.onSessionCreated(client);
			outboundManager.onSessionCreated(client);
		}
		
		@Override
		public void onConnectionDisconnect(SocketChannel s, SocketAddress addr) {
			NetworkClient client = clientManager.getClient(addr);
			if (client != null) {
				client.onDisconnected(ConnectionStoppedReason.APPLICATION);
				client.onSessionDestroyed();
				inboundManager.onSessionDestroyed(client);
				outboundManager.onSessionDestroyed(client);
			}
			adminServer.disconnect(addr);
		}
		
		@Override
		public void onIncomingData(SocketChannel s, SocketAddress addr, byte [] data) {
			inboundManager.onInboundData(addr, data);
		}
		
	}
	
}
