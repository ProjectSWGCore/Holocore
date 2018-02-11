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
package com.projectswg.holocore.services.galaxy;

import com.projectswg.common.control.IntentChain;
import com.projectswg.common.control.Manager;
import com.projectswg.common.debug.Assert;
import com.projectswg.holocore.intents.network.ConnectionClosedIntent;
import com.projectswg.holocore.intents.network.ConnectionOpenedIntent;
import com.projectswg.holocore.intents.network.GalacticPacketIntent;
import com.projectswg.holocore.intents.network.InboundPacketIntent;
import com.projectswg.holocore.resources.player.Player;
import com.projectswg.holocore.services.CoreManager;
import com.projectswg.holocore.services.chat.ChatManager;
import com.projectswg.holocore.services.dev.DeveloperService;
import com.projectswg.holocore.services.galaxy.travel.TravelService;
import com.projectswg.holocore.services.objects.ObjectManager;
import com.projectswg.holocore.services.objects.UniformBoxService;
import com.projectswg.holocore.services.player.PlayerManager;
import com.projectswg.holocore.services.trade.TradeService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GalacticManager extends Manager {
	
	private final ObjectManager objectManager;
	private final PlayerManager playerManager;
	private final Map<Long, IntentChain> prevIntentMap;
	
	public GalacticManager() {
		objectManager = new ObjectManager();
		playerManager = new PlayerManager();
		prevIntentMap = new ConcurrentHashMap<>();
		
		addChildService(objectManager);
		addChildService(playerManager);
		addChildService(new GameManager());
		addChildService(new ChatManager());
		addChildService(new TravelService());
		addChildService(new DeveloperService());
		addChildService(new UniformBoxService());
		addChildService(new TradeService());
		
		registerForIntent(InboundPacketIntent.class, this::handleInboundPacketIntent);
		registerForIntent(ConnectionOpenedIntent.class, this::handleConnectionOpenedIntent);
		registerForIntent(ConnectionClosedIntent.class, this::handleConnectionClosedIntent);
	}
	
	@Override
	public boolean initialize() {
		resetPopulationCount();
		return super.initialize();
	}
	
	private void handleInboundPacketIntent(InboundPacketIntent ipi){
		Player player = playerManager.getPlayerFromNetworkId(ipi.getNetworkId());
		Assert.notNull(player);
		GalacticPacketIntent g = new GalacticPacketIntent(ipi.getPacket(), player);
		g.setGalacticManager(this);
		prevIntentMap.get(player.getNetworkId()).broadcastAfter(g);
	}
	
	private void handleConnectionOpenedIntent(ConnectionOpenedIntent coi){
		prevIntentMap.put(coi.getNetworkId(), new IntentChain(coi));
	}
	
	private void handleConnectionClosedIntent(ConnectionClosedIntent cci){
		prevIntentMap.remove(cci.getNetworkId()).reset();
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
