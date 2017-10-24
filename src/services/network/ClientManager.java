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

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import network.NetworkClient;
import network.NetworkClient.NetworkClientFilter;

class ClientManager {
	
	private final Map <SocketAddress, Long> sockets;
	private final Map <Long, NetworkClient> clients;
	private final AtomicLong networkIdCounter;
	
	public ClientManager() {
		sockets = new ConcurrentHashMap<>();
		clients = new ConcurrentHashMap<>();
		networkIdCounter = new AtomicLong(1);
	}
	
	public NetworkClient createSession(SocketAddress addr, PacketSender sender, NetworkClientFilter filter) {
		NetworkClient client = new NetworkClient(addr, networkIdCounter.incrementAndGet(), sender, filter);
		registerClient(client);
		return client;
	}
	
	public NetworkClient destroySession(long networkId) {
		NetworkClient client = getClient(networkId);
		destroySession(client);
		return client;
	}
	
	public NetworkClient getClient(long networkId) {
		return clients.get(networkId);
	}
	
	public NetworkClient getClient(SocketAddress addr) {
		return getClient(getNetworkId(addr));
	}
	
	public long getNetworkId(SocketAddress addr) {
		Long id = sockets.get(addr);
		if (id == null)
			return -1;
		return id;
	}
	
	private void registerClient(NetworkClient client) {
		sockets.put(client.getAddress(), client.getNetworkId());
		clients.put(client.getNetworkId(), client);
	}
	
	private void destroySession(NetworkClient client) {
		if (client == null)
			return;
		clients.remove(client.getNetworkId());
	}
	
}
