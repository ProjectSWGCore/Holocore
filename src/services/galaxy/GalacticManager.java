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

import java.sql.PreparedStatement;
import java.sql.SQLException;

import intents.GalacticIntent;
import intents.network.GalacticPacketIntent;
import intents.network.InboundPacketIntent;
import resources.Galaxy;
import resources.control.Intent;
import resources.control.Manager;
import resources.server_info.DataManager;
import resources.server_info.Log;
import resources.server_info.RelationalDatabase;
import services.chat.ChatManager;
import services.objects.ObjectManager;
import services.player.PlayerManager;

public class GalacticManager extends Manager {
	
	private final Object prevPacketIntentMutex = new Object();
	private final static String resetPopulationSQL = "UPDATE galaxies SET population = 0 WHERE id = ?";
	
	private ObjectManager objectManager;
	private PlayerManager playerManager;
	private GameManager gameManager;
	private ChatManager chatManager;
	private final TravelService travelService;
	private Intent prevPacketIntent;
	private Galaxy galaxy;
	
	public GalacticManager(Galaxy g) {
		this.galaxy = g;
		objectManager = new ObjectManager();
		playerManager = new PlayerManager();
		gameManager = new GameManager();
		chatManager = new ChatManager(g);
		travelService = new TravelService(objectManager);
		prevPacketIntent = null;
		
		addChildService(objectManager);
		addChildService(playerManager);
		addChildService(gameManager);
		addChildService(chatManager);
		addChildService(travelService);
	}
	
	@Override
	public boolean initialize() {
		registerForIntent(InboundPacketIntent.TYPE);
		resetPopulationCount();
		return super.initialize();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof InboundPacketIntent) {
			synchronized (prevPacketIntentMutex) {
				GalacticPacketIntent g = new GalacticPacketIntent((InboundPacketIntent) i);
				if (prevPacketIntent == null)
					broadcastGalacticIntent(g);
				else
					broadcastGalacticIntentAfterIntent(g, i);
				prevPacketIntent = g;
			}
		}
	}
	
	public void broadcastGalacticIntent(GalacticIntent i) {
		synchronized (i) {
			if (i.isBroadcasted())
				return;
			prepareGalacticIntent(i);
			i.broadcast();
		}
	}
	
	public void broadcastGalacticIntentAfterIntent(GalacticIntent g, Intent i) {
		synchronized (g) {
			if (g.isBroadcasted())
				return;
			prepareGalacticIntent(g);
			g.broadcastAfterIntent(i);
		}
	}
	
	public ObjectManager getObjectManager() {
		return objectManager;
	}
	
	public PlayerManager getPlayerManager() {
		return playerManager;
	}
	
	private void prepareGalacticIntent(GalacticIntent i) {
		i.setGalacticManager(this);
		i.setGalaxy(galaxy);
	}
	private void resetPopulationCount() {
		RelationalDatabase db = DataManager.getInstance().getLocalDatabase();
		try(PreparedStatement resetPopulation =  db.prepareStatement(GalacticManager.resetPopulationSQL)) {
			resetPopulation.setInt(1, galaxy.getId());
			resetPopulation.executeUpdate();
		} catch (SQLException e) {
			Log.e("ProjectSWG", "SQLException occured when trying to reset population value.");
			e.printStackTrace();
		}
	}
	
}
