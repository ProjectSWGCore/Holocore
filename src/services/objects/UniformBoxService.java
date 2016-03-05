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
import resources.control.Intent;
import resources.control.Service;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.objects.player.PlayerObject;
import resources.player.Player;
import resources.server_info.ItemDatabase;
import resources.server_info.RelationalServerData;
import resources.server_info.RelationalServerFactory;

public class UniformBoxService extends Service {
	//TODO: Display loot box
	
	private final String uniformBoxTemplate = "object/tangible/npe/shared_npe_uniform_box.iff";
	private static final String GET_UNIFORMBOX_SQL = "SELECT * FROM npe_uniformbox where profession = ? AND race = ? AND (gender = ? OR gender = 3)";
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
		if ((((RadialSelectionIntent) i).getTarget().getTemplate()).equals(uniformBoxTemplate)){
			processUseUniformBox(((RadialSelectionIntent) i).getPlayer());
		}
		
	}	
	
	private void processUseUniformBox(Player player){
		CreatureObject creature = player.getCreatureObject();
		PlayerObject playerObj = creature.getPlayerObject();	
		SWGObject ghost = player.getCreatureObject();
		SWGObject inventory = ghost.getSlottedObject("inventory");	
		String profession = playerObj.getProfession().substring(0, playerObj.getProfession().lastIndexOf("_"));
		String race = creature.getRace().toString().substring(0, creature.getRace().toString().indexOf("_")).toLowerCase();
		String gender = creature.getRace().toString().substring(creature.getRace().toString().lastIndexOf("_") + 1).toLowerCase();
		
		destroyUniformBox(creature);
		handleCreateItems(inventory, gender, profession, race);

	}
	
	private void handleCreateItems(SWGObject inventory, String playerGender, String profession, String race) {
		String gender = getGenderValue(playerGender); 

		try {
			getUniformBoxStatement.setString(1, profession);
			getUniformBoxStatement.setString(2, race);
			getUniformBoxStatement.setString(3, gender);
			
			try (ResultSet set = getUniformBoxStatement.executeQuery()) {
				if (set.next()){
					for (int i = 4; i <= 13; i++){
						if (!set.getObject(i).toString().isEmpty()){
							SWGObject item = ObjectCreator.createObjectFromTemplate(getItemIffTemplate(set.getObject(i).toString()));
							item.moveToContainer(inventory);
							new ObjectCreatedIntent(item).broadcast();							
						}
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}		
	}
	
	private String getGenderValue(String gender){
		
		switch (gender) {
		
		case "male":
			return "1";
		case "female":
			return "2";
		}
		
		return null;
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


