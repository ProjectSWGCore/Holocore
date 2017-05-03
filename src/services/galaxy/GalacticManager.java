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

import java.util.Map;

import com.projectswg.common.concurrency.SynchronizedMap;
import com.projectswg.common.control.IntentChain;
import com.projectswg.common.control.Manager;
import com.projectswg.common.debug.Assert;

import intents.network.ConnectionClosedIntent;
import intents.network.ConnectionOpenedIntent;
import intents.network.GalacticPacketIntent;
import intents.network.InboundPacketIntent;
import resources.player.Player;
import services.CoreManager;
import services.chat.ChatManager;
import services.dev.DeveloperService;
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
	private final DeveloperService developerService;
	private final UniformBoxService uniformBox;
	private final Map<Long, IntentChain> prevIntentMap;
	
	public GalacticManager() {
		objectManager = new ObjectManager();
		playerManager = new PlayerManager();
		gameManager = new GameManager();
		chatManager = new ChatManager();
		travelService = new TravelService();
		developerService = new DeveloperService();
		uniformBox = new UniformBoxService();
		prevIntentMap = new SynchronizedMap<>();
		
		addChildService(objectManager);
		addChildService(playerManager);
		addChildService(gameManager);
		addChildService(chatManager);
		addChildService(travelService);
		addChildService(developerService);
		addChildService(uniformBox);
		
		registerForIntent(InboundPacketIntent.class, ipi -> handleInboundPacketIntent(ipi));
		registerForIntent(ConnectionOpenedIntent.class, coi -> handleConnectionOpenedIntent(coi));
		registerForIntent(ConnectionClosedIntent.class, cci -> handleConnectionClosedIntent(cci));
	}
	
	@Override
	public boolean initialize() {
		resetPopulationCount();
		return super.initialize();
	}
	
	private void handleInboundPacketIntent(InboundPacketIntent ipi){
		Player player = playerManager.getPlayerFromNetworkId(((InboundPacketIntent) ipi).getNetworkId());
		Assert.notNull(player);
		GalacticPacketIntent g = new GalacticPacketIntent(((InboundPacketIntent) ipi).getPacket(), player);
		g.setGalacticManager(this);
		prevIntentMap.get(player.getNetworkId()).broadcastAfter(g);
	}
	
	private void handleConnectionOpenedIntent(ConnectionOpenedIntent coi){
		prevIntentMap.put(((ConnectionOpenedIntent) coi).getNetworkId(), new IntentChain(coi));
	}
	
	private void handleConnectionClosedIntent(ConnectionClosedIntent cci){
		prevIntentMap.remove(((ConnectionClosedIntent) cci).getNetworkId()).reset();
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
