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

import intents.OutboundUdpPacketIntent;
import resources.Galaxy;
import resources.control.Intent;
import resources.control.Manager;
import resources.network.ServerType;
import resources.network.UDPServer.UDPPacket;

public class NetworkManager extends Manager {
	
	private NetworkListenerService netListenerService;
	private NetworkClientManager netClientManager;
	
	public NetworkManager(Galaxy galaxy) {
		netListenerService = new NetworkListenerService(galaxy);
		netClientManager = new NetworkClientManager();
		
		addChildService(netClientManager);
		addChildService(netListenerService);
	}
	
	@Override
	public boolean initialize() {
		registerForIntent(OutboundUdpPacketIntent.TYPE);
		return super.initialize();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof OutboundUdpPacketIntent) {
			UDPPacket packet = ((OutboundUdpPacketIntent) i).getPacket();
			ServerType type = ((OutboundUdpPacketIntent) i).getServerType();
			netListenerService.send(type, packet.getAddress(), packet.getPort(), packet.getData());
		}
	}
	
}
