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
package services.objects;

import intents.object.CreateStaticItemIntent;
import intents.object.DestroyObjectIntent;
import intents.radial.RadialSelectionIntent;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
import resources.radial.RadialItem;
import resources.server_info.Log;

import com.projectswg.common.control.Service;
import com.projectswg.common.info.RelationalServerData;
import com.projectswg.common.info.RelationalServerFactory;

public class UniformBoxService extends Service {
	
	private static final String [] UNIFORM_COLUMNS = {"boots", "pants", "belt", "gloves", "shirt", "vest", "hat", "necklace", "robe", "weapon"};
	private static final String GET_UNIFORMBOX_SQL = "SELECT * FROM npe_uniformbox where profession = ? AND race = ?";
	private static final String UNIFORM_BOX_IFF = "object/tangible/npe/shared_npe_uniform_box.iff";
	
	private RelationalServerData uniformBoxDatabase;
	private PreparedStatement getUniformBoxStatement;
	
	public UniformBoxService(){
		uniformBoxDatabase = RelationalServerFactory.getServerData("player/npe_uniformbox.db", "npe_uniformbox");
		if (uniformBoxDatabase == null)
			throw new main.ProjectSWG.CoreException("Unable to load npe_uniformbox.sdb file for UniformBoxService");
		
		getUniformBoxStatement = uniformBoxDatabase.prepareStatement(GET_UNIFORMBOX_SQL);
		
		registerForIntent(RadialSelectionIntent.class, rsi -> handleRadialSelectionIntent(rsi));
	}
	
	@Override
	public boolean terminate() {
		uniformBoxDatabase.close();
		return super.terminate();
	}
	
	private void handleRadialSelectionIntent(RadialSelectionIntent rsi) {
		if (!rsi.getTarget().getTemplate().equals(UNIFORM_BOX_IFF))
			return;
		
		if(!rsi.getSelection().equals(RadialItem.ITEM_USE)) {
			return;
		}
		
		Player player = rsi.getPlayer();
		CreatureObject creature = player.getCreatureObject();
		SWGObject inventory = creature.getSlottedObject("inventory");
		String profession = creature.getPlayerObject().getProfession();
		
		new DestroyObjectIntent(rsi.getTarget()).broadcast();
		handleCreateItems(inventory, profession, creature);
	}
	
	private void handleCreateItems(SWGObject inventory, String profession, CreatureObject creature) {
		synchronized (getUniformBoxStatement) {
			try {
				getUniformBoxStatement.setString(1, profession);
				getUniformBoxStatement.setString(2, creature.getRace().name());
				
				try (ResultSet set = getUniformBoxStatement.executeQuery()) {
					if (set.next())
						createItems(set, creature, inventory);
				}
			} catch (SQLException e) {
				Log.e(e);
			}
		}
	}
	
	private void createItems(ResultSet set, CreatureObject creature, SWGObject inventory) throws SQLException {
		Collection<String> staticItems = new ArrayList<>();
		for (String uniformItem : UNIFORM_COLUMNS) {
			String item = set.getString(uniformItem);
			
			if (!item.isEmpty())
				staticItems.add(item);
		}
		
		new CreateStaticItemIntent(creature, inventory, new StaticItemService.LootBoxHandler(creature), staticItems.toArray(new String[staticItems.size()])).broadcast();
	}
	
}
