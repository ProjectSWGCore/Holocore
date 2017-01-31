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
import java.net.Socket;
import java.net.SocketAddress;

import intents.network.CloseConnectionIntent;
import network.NetworkClient;
import network.packets.swg.holo.HoloConnectionStopped.ConnectionStoppedReason;
import resources.config.ConfigFile;
import resources.control.Manager;
import resources.network.DisconnectReason;
import resources.network.NetworkCallback;
import resources.network.TCPServer;
import resources.server_info.Log;

public class NetworkClientManager extends Manager implements NetworkCallback {
	
	private final InboundNetworkManager inboundManager;
	private final OutboundNetworkManager outboundManager;
	private final ClientManager clientManager;
	private final TCPServer tcpServer;
	
	public NetworkClientManager() {
		clientManager = new ClientManager();
		tcpServer = new TCPServer(getBindPort(), getBufferSize());
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
			tcpServer.setCallback(this);
		} catch (IOException e) {
			Log.e(this, e);
			if (e instanceof BindException)
				Log.e(this, "Failed to bind to " + getBindPort());
			return false;
		}
		return super.start();
	}
	
	@Override
	public boolean stop() {
		tcpServer.close();
		return super.stop();
	}
		
	private void handleCloseConnectionIntent(CloseConnectionIntent ccii) {
		NetworkClient client = clientManager.getClient(ccii.getNetworkId());
		if (client != null)
			onSessionDisconnect(client.getAddress(), getHolocoreReason(ccii.getDisconnectReason()));
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
	public void onIncomingConnection(Socket s, SocketAddress addr) {
		onSessionConnect(addr);
	}
	
	@Override
	public void onConnectionDisconnect(Socket s, SocketAddress addr) {
		onSessionDisconnect(addr, ConnectionStoppedReason.APPLICATION);
	}
	
	@Override
	public void onIncomingData(Socket s, SocketAddress addr, byte [] data) {
		onInboundData(addr, data);
	}
	
	private int getBindPort() {
		return getConfig(ConfigFile.NETWORK).getInt("BIND-PORT", 44463);
	}
	
	private int getBufferSize() {
		return getConfig(ConfigFile.NETWORK).getInt("BUFFER-SIZE", 4096);
	}
	
	private void onSessionConnect(SocketAddress addr) {
		NetworkClient client = clientManager.createSession(addr);
		client.onConnected();
		inboundManager.onSessionCreated(client);
		outboundManager.onSessionCreated(client);
	}
	
	private void onInboundData(SocketAddress addr, byte [] data) {
		inboundManager.onInboundData(addr, data);
	}
	
	private void onSessionDisconnect(SocketAddress addr, ConnectionStoppedReason reason) {
		NetworkClient client = clientManager.getClient(addr);
		if (client != null) {
			client.onDisconnected(reason);
			client.onSessionDestroyed();
			inboundManager.onSessionDestroyed(client);
			outboundManager.onSessionDestroyed(client);
		}
		tcpServer.disconnect(addr);
	}
	
}
