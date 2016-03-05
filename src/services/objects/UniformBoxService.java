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


import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import intents.object.DestroyObjectIntent;
import intents.object.ObjectCreatedIntent;
import intents.radial.RadialSelectionIntent;
import resources.Race;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.server_info.ItemDatabase;
import resources.server_info.RelationalServerData;
import resources.server_info.RelationalServerFactory;

public class UniformBoxService extends Service {
	//TODO: Display loot box
	
	private static final String [] UNIFORM_COLUMNS = {"boots", "pants", "belt", "gloves", "shirt", "vest", "hat", "necklace", "robe", "weapon"};
	private static final String GET_UNIFORMBOX_SQL = "SELECT * FROM npe_uniformbox where profession = ? AND race = ? AND (gender = ? OR gender = 3)";
	
	private final String uniformBoxTemplate = "object/tangible/npe/shared_npe_uniform_box.iff";
	private RelationalServerData uniformBoxDatabase;
	private PreparedStatement getUniformBoxStatement;
	
	public UniformBoxService(){
		uniformBoxDatabase = RelationalServerFactory.getServerData("player/npe_uniformbox.db", "npe_uniformbox");
		if (uniformBoxDatabase == null)
			throw new main.ProjectSWG.CoreException("Unable to load npe_uniformbox.sdb file for UniformBoxService");
		
		getUniformBoxStatement = uniformBoxDatabase.prepareStatement(GET_UNIFORMBOX_SQL);
		
		registerForIntent(RadialSelectionIntent.TYPE);
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof RadialSelectionIntent)
			processUseUniformBox((RadialSelectionIntent) i);
	}
	
	private void processUseUniformBox(RadialSelectionIntent rsi) {
		if (!rsi.getTarget().getTemplate().equals(uniformBoxTemplate))
			return;
		
		CreatureObject creature = rsi.getPlayer().getCreatureObject();
		SWGObject inventory = creature.getSlottedObject("inventory");
		String profession = creature.getPlayerObject().getProfession();
		
		destroyUniformBox(creature);
		handleCreateItems(inventory, profession.substring(0, profession.lastIndexOf('_')), creature.getRace());
	}
	
	private void handleCreateItems(SWGObject inventory, String profession, Race race) {
		synchronized (getUniformBoxStatement) {
			try {
				getUniformBoxStatement.setString(1, profession);
				getUniformBoxStatement.setString(2, race.getSpecies());
				getUniformBoxStatement.setInt(3, race.isMale() ? 1 : 2);
				
				try (ResultSet set = getUniformBoxStatement.executeQuery()) {
					if (set.next())
						createItems(set, inventory);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}		
		}
	}
	
	private void createItems(ResultSet set, SWGObject inventory) throws SQLException {
		for (String uniformItem : UNIFORM_COLUMNS) {
			String item = set.getString(uniformItem);
			if (item.isEmpty())
				continue;
			
			SWGObject object = ObjectCreator.createObjectFromTemplate(getItemIffTemplate(item));
			object.moveToContainer(inventory);
			new ObjectCreatedIntent(object).broadcast();
		}
	}
	
	private String getItemIffTemplate(String item_name){
		String template = "";
		
		try (ItemDatabase db = new ItemDatabase()){
			template = db.getIffTemplate(item_name);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return template;
	}
	
	private void destroyUniformBox(CreatureObject creature){
		Collection<SWGObject> items = creature.getItemsByTemplate("inventory", uniformBoxTemplate);
		
		for (SWGObject item : items){
			if (item.getTemplate().equals(uniformBoxTemplate)){
				new DestroyObjectIntent(item).broadcast();
			}
		}		

	}
}


