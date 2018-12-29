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

import com.projectswg.common.data.info.Config;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.common.network.packets.swg.holo.HoloConnectionStopped.ConnectionStoppedReason;
import com.projectswg.holocore.ProjectSWG;
import com.projectswg.holocore.ProjectSWG.CoreException;
import com.projectswg.holocore.intents.support.global.network.CloseConnectionIntent;
import com.projectswg.holocore.intents.support.global.network.ConnectionClosedIntent;
import com.projectswg.holocore.intents.support.global.network.InboundPacketPendingIntent;
import com.projectswg.holocore.intents.support.global.network.OutboundPacketIntent;
import com.projectswg.holocore.resources.support.data.config.ConfigFile;
import com.projectswg.holocore.resources.support.data.server_info.DataManager;
import com.projectswg.holocore.resources.support.global.network.AdminNetworkClient;
import com.projectswg.holocore.resources.support.global.network.NetworkClient;
import com.projectswg.holocore.resources.support.global.network.UDPServer;
import com.projectswg.holocore.resources.support.global.network.UDPServer.UDPPacket;
import me.joshlarson.jlcommon.concurrency.ThreadPool;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;
import me.joshlarson.jlcommon.network.TCPServer;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.security.KeyStore;

public class NetworkClientService extends Service {
	
	private final ThreadPool securityExecutor;
	private final TCPServer<NetworkClient> tcpServer;
	private final TCPServer<AdminNetworkClient> adminServer;
	private final UDPServer udpServer;
	private final SSLContext sslContext;
	
	public NetworkClientService() {
		this.securityExecutor = new ThreadPool(3, "network-client-security-%d");
		this.sslContext = initializeSecurity();
		{
			int bindPort = getBindPort();
			int bufferSize = getBufferSize();
			tcpServer = new TCPServer<>(bindPort, bufferSize, channel -> new NetworkClient(channel, sslContext, securityExecutor));
			try {
				udpServer = new UDPServer(bindPort, bufferSize);
			} catch (SocketException e) {
				throw new CoreException("Socket Exception on UDP bind: " + e);
			}
			udpServer.setCallback(this::onUdpPacket);
		}
		{
			int adminServerPort = ProjectSWG.getGalaxy().getAdminServerPort();
			if (adminServerPort > 0) {
				InetSocketAddress localhost = new InetSocketAddress(InetAddress.getLoopbackAddress(), adminServerPort);
				adminServer = new TCPServer<>(localhost, 1024, channel -> new AdminNetworkClient(channel, sslContext, securityExecutor));
			} else {
				adminServer = null;
			}
		}
	}
	
	@Override
	public boolean start() {
		securityExecutor.start();
		int bindPort = -1;
		try {
			bindPort = getBindPort();
			tcpServer.bind();
			bindPort = ProjectSWG.getGalaxy().getAdminServerPort();
			if (adminServer != null) {
				adminServer.bind();
			}
		} catch (BindException e) {
			Log.e("Failed to bind to %d", bindPort);
			return false;
		} catch (IOException e) {
			Log.e(e);
			return false;
		}
		return super.start();
	}
	
	@Override
	public boolean stop() {
		for (NetworkClient client : tcpServer.getSessions())
			client.close(ConnectionStoppedReason.APPLICATION);
		
		tcpServer.close();
		if (adminServer != null) {
			for (NetworkClient client : adminServer.getSessions())
				client.close(ConnectionStoppedReason.APPLICATION);
			adminServer.close();
		}
		securityExecutor.stop(false);
		return securityExecutor.awaitTermination(3000);
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
	
	@IntentHandler
	private void handleOutboundPacketIntent(OutboundPacketIntent opi) {
		NetworkClient client = getClient(opi.getPlayer().getNetworkId());
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
	
	private SSLContext initializeSecurity() {
		Log.t("Initializing encryption...");
		Config config = DataManager.getConfig(ConfigFile.NETWORK);
		try {
			File keystoreFile = new File(config.getString("KEYSTORE-FILE", ""));
			InputStream keystoreStream;
			char[] passphrase;
			KeyStore keystore;
			if (!keystoreFile.isFile()) {
				Log.w("Failed to enable security! Keystore file does not exist: %s", keystoreFile);
				keystoreStream = getClass().getResourceAsStream("/security/Holocore.p12");
				passphrase = new char[]{'p', 'a', 's', 's'};
				keystore = KeyStore.getInstance("PKCS12");
			} else {
				keystoreStream = new FileInputStream(keystoreFile);
				passphrase = config.getString("KEYSTORE-PASS", "").toCharArray();
				keystore = KeyStore.getInstance(config.getString("KEYSTORE-TYPE", "PKCS12"));
			}
			
			keystore.load(keystoreStream, passphrase);
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(keystore, passphrase);
			TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(keystore);
			SSLContext ctx = SSLContext.getInstance("TLSv1.3");
			ctx.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
			Log.i("Enabled TLS encryption");
			return ctx;
		} catch (Exception e) {
			Log.a("Failed to enable security! %s: %s", e.getClass(), e.getMessage());
			throw new RuntimeException("Failed to enable TLS", e);
		}
	}
	
	@IntentHandler
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
