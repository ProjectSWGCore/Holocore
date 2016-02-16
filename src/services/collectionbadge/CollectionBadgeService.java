
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
package services.collectionbadge;


import java.util.BitSet;

import resources.client_info.ClientFactory;
import resources.client_info.visitors.DatatableData;
import resources.control.Service;
import resources.objects.creature.CreatureObject;
import resources.objects.player.PlayerObject;
import resources.player.Player;
import utilities.IntentFactory;

public class CollectionBadgeService extends Service {

	//TODO 
	//research rewards
	//clearon complete
	//track server first
	//research categories
	//music
	//fix to appropriate message ex: kill_merek_activation_01
	
	private DatatableData collectionTable = (DatatableData) ClientFactory.getInfoFromFile("datatables/collection/collection.iff");
	
	public CollectionBadgeService(){
	}
	
	public void grantBadge(PlayerObject player, int beginSlotId){
		BitSet collections = BitSet.valueOf(player.getCollectionBadges());
		
		collections.set(beginSlotId);
		player.setCollectionBadges(collections.toByteArray());	
	}
	
	public void grantBadgeIncrement(PlayerObject player, int beginSlotId, int endSlotId, int maxSlotValue){
		BitSet collections = BitSet.valueOf(player.getCollectionBadges());
	    int binaryValue = 1;
	    int curValue = 0;
		
	    for (int i=0; i < endSlotId - beginSlotId; i++){
			if (collections.get(beginSlotId + i)){
				curValue = curValue + binaryValue;
			}
			binaryValue = binaryValue * 2;
		}
	    
	    if (curValue < maxSlotValue){
	    	collections.clear(beginSlotId, (endSlotId + 1));
	    	BitSet bitSet = BitSet.valueOf(new long[] { curValue + 1 });
		
	    	for (int i=0; i < endSlotId - beginSlotId; i++){
	    		if (bitSet.get(i)){
	    			collections.set(beginSlotId + i);
	    		}else{
	    			collections.clear(beginSlotId + i);
	    		}
			}
			player.setCollectionBadges(collections.toByteArray());
	    }
	}

	public void handleCollectionBadge(CreatureObject creature, String collectionBadgeName) {
		PlayerObject player = (PlayerObject) creature.getSlottedObject("ghost");
		
		String bookName = "";
		int bookBadgeCount = 0;
		String pageName = "";
		int pageBadgeCount = 0;
		String collectionName = "";
		String slotName = "";
		int beginSlotId = -1;
		int endSlotId = -1;		
		int maxSlotValue = -1;
		boolean preReqMet = true;

		for (int row = 0; row < collectionTable.getRowCount(); row++) {
			if (collectionTable.getCell(row, 0).toString() != ""){
				bookName = collectionTable.getCell(row, 0).toString();
			}else if (collectionTable.getCell(row, 1).toString() != ""){
				pageName = collectionTable.getCell(row, 1).toString();
			}else if (collectionTable.getCell(row, 2).toString() != ""){
				collectionName = collectionTable.getCell(row, 2).toString();
			}else if (collectionTable.getCell(row, 3).toString() != ""){
				slotName = collectionTable.getCell(row, 3).toString();
				if (bookName.equals("badge_book")){
					beginSlotId = (int) collectionTable.getCell(row, 4);
					if (hasBadge(player, beginSlotId)){
						bookBadgeCount ++;
					}
				}				
				if (pageName.equals("bdg_explore")){
					beginSlotId = (int) collectionTable.getCell(row, 4);
					if (hasBadge(player, beginSlotId)){
						pageBadgeCount ++;
					}
				}
				if (slotName.equals(collectionBadgeName)){
					beginSlotId = (int) collectionTable.getCell(row, 4);
					endSlotId = (int) collectionTable.getCell(row, 5);
					maxSlotValue = (int) collectionTable.getCell(row, 6);
				
					boolean isHidden = (boolean) collectionTable.getCell(row, 26);
					String preReqSlotName = (String) collectionTable.getCell(row, 18);
					preReqMet = hasPreReqComplete(player, preReqSlotName); 

					if (endSlotId != -1 && preReqMet == true){
						grantBadgeIncrement(player, beginSlotId, endSlotId, maxSlotValue);
					}else if (!hasBadge(player, beginSlotId) && preReqMet == true){
						grantBadge(player, beginSlotId);
						handleMessage(player, hasCompletedCollection(player, collectionName), collectionName, isHidden, slotName);
						if (bookName.equals("badge_book") && !isHidden){
							bookBadgeCount ++;
						}
						if (pageName.equals("bdg_explore") && !isHidden){
							pageBadgeCount ++;
						}								
					}
				}
			}
		}
		checkBadgeCount(creature, "badge_book", bookBadgeCount);
		checkBadgeCount(creature, "bdg_explore", pageBadgeCount);
	}	
	
	public void checkBadgeCount(CreatureObject creature, String collectionName, int badgeCount){

		if (collectionName.equals("badge_book")){
			
			switch (badgeCount) {
			
			case 0:
					break;
			case 5:
				handleCollectionBadge(creature, "count_5");
				break;
			case 10:
				handleCollectionBadge(creature, "count_10");
				break;
			default:
				if (((badgeCount % 25) == 0) && !(badgeCount > 500)) {
					handleCollectionBadge(creature, "count_" + badgeCount);
					
				}
				break;			
			}

		}else if (collectionName.equals("badge_book")){
			switch (badgeCount) {
			
			case 0:
				break;
			case 10:
				handleCollectionBadge(creature, "bdg_exp_10_badges");
				break;
			case 20:
				handleCollectionBadge(creature, "bdg_exp_20_badges");
				break;
			case 30:
				handleCollectionBadge(creature, "bdg_exp_30_badges");
				break;
			case 40:
				handleCollectionBadge(creature, "bdg_exp_40_badges");
				break;
			case 45:
				handleCollectionBadge(creature, "bdg_exp_45_badges");
				break;
			}			
			
		}
	}
	
	public int getBeginSlotID(String slotName){
		int beginSlotId = -1;

		for (int row = 0; row < collectionTable.getRowCount(); row++) {
			if (collectionTable.getCell(row, 3).toString().equals(slotName)){
				beginSlotId = (int) collectionTable.getCell(row, 4);
			}
		}
		return beginSlotId;
	}		
	
	public void handleMessage(PlayerObject player, boolean collectionComplete, String collectionName, boolean hidden, String slotName){
		Player thisplayer = player.getOwner();
		
		if (hidden){
			sendSystemMessage(thisplayer, "@collection:player_hidden_slot_added", "TO", "@collection_n:" + collectionName);
		}else{
			sendSystemMessage(thisplayer, "@collection:player_slot_added", "TU", "@collection_n:" + slotName, "TO", "@collection_n:" + collectionName);
		}	
		if (collectionComplete){
			sendSystemMessage(thisplayer, "@collection:player_collection_complete", "TO", "@collection_n:" + collectionName);
		}			
	}

	public boolean hasBadge(PlayerObject player, int badgeBeginSlotId){
		BitSet collections = BitSet.valueOf(player.getCollectionBadges()); 
		
		if (collections.get(badgeBeginSlotId)){
			return true;
		}
		return false;
	}
	
	public boolean hasCompletedCollection(PlayerObject player, String collectionTitle){
		
		String collectionName = "";
		BitSet collections = BitSet.valueOf(player.getCollectionBadges()); 
		boolean isComplete = true;

		for (int row = 0; row < collectionTable.getRowCount(); row++) {
			int beginSlotId = (int) collectionTable.getCell(row, 4);
			if (collectionTable.getCell(row, 2).toString() != ""){
				collectionName = collectionTable.getCell(row, 2).toString();
			}else if (collectionName.equals(collectionTitle)){
				if (!collections.get(beginSlotId)){
					isComplete = false;
				}
			}
		}
		
		return isComplete;
	}
	
	public boolean hasPreReqComplete(PlayerObject player, String preReqSlotName){
		Player thisplayer = player.getOwner();
		
		if (preReqSlotName != ""){
			if (!hasBadge(player, getBeginSlotID(preReqSlotName))){
				sendSystemMessage(thisplayer, "@collection:need_to_activate_collection");
				return false;
			}
		}return true;
	}
		
	private void sendSystemMessage(Player target, String id, Object ... objects) {
		IntentFactory.sendSystemMessage(target, id, objects);
	}	
}
