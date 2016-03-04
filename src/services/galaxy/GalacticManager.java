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
package services.galaxy;

import java.util.Hashtable;
import java.util.Map;

import intents.network.ConnectionClosedIntent;
import intents.network.ConnectionOpenedIntent;
import intents.network.GalacticPacketIntent;
import intents.network.InboundPacketIntent;
import resources.control.Intent;
import resources.control.Manager;
import services.CoreManager;
import services.chat.ChatManager;
import services.galaxy.travel.TravelService;
import services.objects.ObjectManager;
import services.objects.UniformBoxService;
import services.player.PlayerManager;

public class GalacticManager extends Manager {
	
	private final ObjectManager objectManager;
	private final PlayerManager playerManager;
	private final GameManager gameManager;
	private final ChatManager chatManager;
	private final TravelService travelService;
	private final UniformBoxService uniformBox;
	private final Map<Long, Intent> prevIntentMap;
	
	public GalacticManager() {
		objectManager = new ObjectManager();
		playerManager = new PlayerManager();
		gameManager = new GameManager();
		chatManager = new ChatManager();
		travelService = new TravelService(objectManager);
		uniformBox = new UniformBoxService(objectManager);
		prevIntentMap = new Hashtable<>();
		
		addChildService(objectManager);
		addChildService(playerManager);
		addChildService(gameManager);
		addChildService(chatManager);
		addChildService(travelService);
		addChildService(uniformBox);
		
		registerForIntent(InboundPacketIntent.TYPE);
		registerForIntent(ConnectionOpenedIntent.TYPE);
		registerForIntent(ConnectionClosedIntent.TYPE);
	}
	
	@Override
	public boolean initialize() {
		resetPopulationCount();
		return super.initialize();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof InboundPacketIntent) {
			long networkId = ((InboundPacketIntent) i).getNetworkId();
			GalacticPacketIntent g = new GalacticPacketIntent((InboundPacketIntent) i);
			g.setGalacticManager(this);
			synchronized (prevIntentMap) {
				g.broadcastAfterIntent(prevIntentMap.get(networkId));
				prevIntentMap.put(networkId, g);
			}
		} else if (i instanceof ConnectionClosedIntent) {
			synchronized (prevIntentMap) {
				prevIntentMap.remove(((ConnectionClosedIntent) i).getNetworkId());
			}
		}
	}
	
	public ObjectManager getObjectManager() {
		return objectManager;
	}
	
	public PlayerManager getPlayerManager() {
		return playerManager;
	}
	
	private void resetPopulationCount() {
		CoreManager.getGalaxy().setPopulation(0);
	}
	
}
