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
package services.network;

import com.projectswg.common.control.Manager;
import com.projectswg.common.debug.Log;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.common.network.TCPServer;
import com.projectswg.common.network.packets.swg.holo.HoloConnectionStopped.ConnectionStoppedReason;
import intents.network.CloseConnectionIntent;
import intents.network.ConnectionClosedIntent;
import intents.network.InboundPacketPendingIntent;
import intents.network.OutboundPacketIntent;
import main.ProjectSWG.CoreException;
import network.AdminNetworkClient;
import network.NetworkClient;
import resources.config.ConfigFile;
import resources.network.UDPServer;
import resources.network.UDPServer.UDPPacket;
import resources.server_info.DataManager;
import services.CoreManager;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class NetworkClientManager extends Manager {
	
	private final TCPServer<NetworkClient> tcpServer;
	private final TCPServer<AdminNetworkClient> adminServer;
	private final UDPServer udpServer;
	
	public NetworkClientManager() {
		{
			int bindPort = getBindPort();
			int bufferSize = getBufferSize();
			tcpServer = new TCPServer<>(bindPort, bufferSize, NetworkClient::new);
			try {
				udpServer = new UDPServer(bindPort, bufferSize);
			} catch (SocketException e) {
				throw new CoreException("Socket Exception on UDP bind: " + e);
			}
			udpServer.setCallback(this::onUdpPacket);
		}
		{
			int adminServerPort = CoreManager.getGalaxy().getAdminServerPort();
			if (adminServerPort <= 0)
				adminServer = null;
			else
				adminServer = new TCPServer<>(new InetSocketAddress(InetAddress.getLoopbackAddress(), adminServerPort), 1024, AdminNetworkClient::new);
		}
		
		registerForIntent(CloseConnectionIntent.class, this::handleCloseConnectionIntent);
		registerForIntent(ConnectionClosedIntent.class, this::handleConnectionClosedIntent);
		registerForIntent(InboundPacketPendingIntent.class, NetworkClientManager::handleInboundPacketPendingIntent);
		registerForIntent(OutboundPacketIntent.class, this::handleOutboundPacketIntent);
	}
	
	@Override
	public boolean start() {
		int bindPort = -1;
		try {
			bindPort = getBindPort();
			tcpServer.bind();
			bindPort = CoreManager.getGalaxy().getAdminServerPort();
			if (adminServer != null) {
				adminServer.bind();
			}
		} catch (IOException e) {
			if (e instanceof BindException)
				Log.e("Failed to bind to %d", bindPort);
			else
				Log.e(e);
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
	
	private void disconnect(long networkId) {
		disconnect(getClient(networkId));
	}
	
	private void disconnect(NetworkClient client) {
		if (client == null)
			return;
		
		client.close(ConnectionStoppedReason.APPLICATION);
	}
	
	private void onUdpPacket(UDPPacket packet) {
		if (packet.getLength() <= 0)
			return;
		switch (packet.getData()[0]) {
			case 1: sendState(packet.getAddress(), packet.getPort()); break;
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
	
	private void handleCloseConnectionIntent(CloseConnectionIntent ccii) {
		disconnect(ccii.getNetworkId());
	}
	
	private void handleConnectionClosedIntent(ConnectionClosedIntent cci) {
		disconnect(cci.getNetworkId());
	}
	
	private void handleOutboundPacketIntent(OutboundPacketIntent opi) {
		NetworkClient client = getClient(opi.getNetworkId());
		if (client == null)
			return;
		
		client.addToOutbound(opi.getPacket());
	}
	
	private NetworkClient getClient(long id) {
		NetworkClient client = tcpServer.getSession(id);
		if (client != null || adminServer == null)
			return client;
		return adminServer.getSession(id);
	}
	
	private static void handleInboundPacketPendingIntent(InboundPacketPendingIntent ippi) {
		ippi.getClient().processInbound();
	}
	
	private static int getBindPort() {
		return DataManager.getConfig(ConfigFile.NETWORK).getInt("BIND-PORT", 44463);
	}
	
	private static int getBufferSize() {
		return DataManager.getConfig(ConfigFile.NETWORK).getInt("BUFFER-SIZE", 4096);
	}
	
}
