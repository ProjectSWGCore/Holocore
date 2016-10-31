
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
package services.collections;


import java.util.BitSet;

import intents.GrantBadgeIntent;
import intents.GrantClickyCollectionIntent;
import intents.object.DestroyObjectIntent;
import network.packets.swg.zone.PlayMusicMessage;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.DatatableData;
import resources.control.Intent;
import resources.control.Manager;
import resources.objects.SWGObject;
import resources.objects.collections.ClickyCollectionItem;
import resources.objects.collections.CollectionItem;
import resources.objects.creature.CreatureObject;
import resources.objects.player.PlayerObject;
import resources.player.Player;
import utilities.IntentFactory;

public class CollectionBadgeManager extends Manager {

	// TODO grant item rewards and/or schematics
	// TODO grant XP
	// TODO clearon complete (repeatable)
	// TODO track server first
	// TODO research categories
	// TODO play music file stored in collection.iff column "music"
	// TODO fix to appropriate message ex: kill_merek_activation_01

	private final ExplorationBadgeService explorationBadgeService;

	private DatatableData collectionTable = (DatatableData) ClientFactory.getInfoFromFile("datatables/collection/collection.iff");


	public CollectionBadgeManager() {
		explorationBadgeService = new ExplorationBadgeService();

		addChildService(explorationBadgeService);

		registerForIntent(GrantBadgeIntent.TYPE);
		registerForIntent(GrantClickyCollectionIntent.TYPE);
	}

	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof GrantBadgeIntent) {
			handleCollectionBadge(((GrantBadgeIntent) i).getCreature(), ((GrantBadgeIntent) i).getCollectionBadgeName());
		} else if (i instanceof GrantClickyCollectionIntent) {
			CreatureObject creo = ((GrantClickyCollectionIntent) i).getCreature();
			SWGObject inventoryItem = ((GrantClickyCollectionIntent) i).getInventoryItem();
			CollectionItem collection = ((GrantClickyCollectionIntent) i).getCollection();

			handleCollectionBadge(creo, inventoryItem, collection);
		}
	}

	public void grantBadge(PlayerObject player, int beginSlotId, String collectionName, boolean isHidden, String slotName) {
		BitSet collections = BitSet.valueOf(player.getCollectionBadges());

		collections.set(beginSlotId);
		player.setCollectionBadges(collections.toByteArray());
		handleMessage(player, hasCompletedCollection(player, collectionName), collectionName, isHidden, slotName);
	}

	public void grantBadgeIncrement(PlayerObject player, int beginSlotId, int endSlotId, int maxSlotValue) {
		BitSet collections = BitSet.valueOf(player.getCollectionBadges());

		int binaryValue = 1;
		int curValue = 0;

		for (int i = 0; i < endSlotId - beginSlotId; i++) {
			if (collections.get(beginSlotId + i)) {
				curValue = curValue + binaryValue;
			}
			binaryValue = binaryValue * 2;
		}

		if (curValue < maxSlotValue) {
			collections.clear(beginSlotId, (endSlotId + 1));
			BitSet bitSet = BitSet.valueOf(new long[]{curValue + 1});

			for (int i = 0; i < endSlotId - beginSlotId; i++) {
				if (bitSet.get(i)) {
					collections.set(beginSlotId + i);
				} else {
					collections.clear(beginSlotId + i);
				}
			}
			player.setCollectionBadges(collections.toByteArray());
		}
	}

	public void handleCollectionBadge(CreatureObject creature, SWGObject inventoryItem, CollectionItem collection) {
		PlayerObject player = (PlayerObject) creature.getSlottedObject("ghost");
		BadgeInformation badgeInformation = new BadgeInformation(player, collection.getCollectionName());

		CollectionRowData rows = getCollectionRows(collection);

		badgeInformation.setBookName(collectionTable.getCell(rows.bookRow, 0).toString());
		badgeInformation.setPageName(collectionTable.getCell(rows.pageRow, 1).toString());
		badgeInformation.setCollectionName(collectionTable.getCell(rows.collectionRow, 2).toString());
		badgeInformation.setIsHidden((boolean) collectionTable.getCell(rows.slotRow, 26));
		badgeInformation.setBeginSlotId((int) collectionTable.getCell(rows.slotRow, 4));
		badgeInformation.setEndSlotId((int) collectionTable.getCell(rows.slotRow, 5));
		badgeInformation.setMaxSlotValue((int) collectionTable.getCell(rows.slotRow, 6));
		badgeInformation.setPreReqSlotName(collectionTable.getCell(rows.slotRow, 18).toString());
		badgeInformation.setSlotName(collectionTable.getCell(rows.slotRow, 3).toString());
		badgeInformation.setMusic(collectionTable.getCell(rows.slotRow, 24).toString());

		if (hasBadge(player, badgeInformation.beginSlotId)) {
			sendSystemMessage(creature.getOwner(), "@collection:already_have_slot");
			return;
		}

		grantBadge(player, getBeginSlotID(badgeInformation.slotName), badgeInformation.collectionBadgeName, false, badgeInformation.slotName);

		if (!(collection instanceof ClickyCollectionItem))
			new DestroyObjectIntent(inventoryItem).broadcast();

		// TODO: play the sound for getting the collection item, stored in badgeInformation.music
	}

	public void handleCollectionBadge(CreatureObject creature, String collectionName) {
		PlayerObject player = (PlayerObject) creature.getSlottedObject("ghost");
		BadgeInformation badgeInformation = new BadgeInformation(player, collectionName);

		for (int row = 0; row < collectionTable.getRowCount(); row++) {
			if (!collectionTable.getCell(row, 0).toString().isEmpty()) {
				badgeInformation.setBookName(collectionTable.getCell(row, 0).toString());
			} else if (!collectionTable.getCell(row, 1).toString().isEmpty()) {
				badgeInformation.setPageName(collectionTable.getCell(row, 1).toString());
			} else if (!collectionTable.getCell(row, 2).toString().isEmpty()) {
				badgeInformation.setCollectionName(collectionTable.getCell(row, 2).toString());
			} else if (!collectionTable.getCell(row, 3).toString().isEmpty()) {
				badgeInformation.setIsHidden((boolean) collectionTable.getCell(row, 26));
				badgeInformation.setBeginSlotId((int) collectionTable.getCell(row, 4));
				badgeInformation.setEndSlotId((int) collectionTable.getCell(row, 5));
				badgeInformation.setMaxSlotValue((int) collectionTable.getCell(row, 6));
				badgeInformation.setPreReqSlotName((String) collectionTable.getCell(row, 18));
				badgeInformation.setSlotName(collectionTable.getCell(row, 3).toString());
			}
		}

		checkBadgeBookCount(player, "badge_book", badgeInformation.bookBadgeCount);
		checkExplorerBadgeCount(player, "bdg_explore", badgeInformation.pageBadgeCount);
	}

	private void checkBadgeBookCount(PlayerObject player, String collectionName, int badgeCount) {
		String slotName = "";

		switch (badgeCount) {

			case 0:
				break;
			case 5:
				slotName = "count_5";
				break;
			case 10:
				slotName = "count_10";
				break;
			default:
				if (((badgeCount % 25) == 0) && !(badgeCount > 500)) {
					slotName = "count_" + badgeCount;
				}
				break;
		}

		if (!hasBadge(player, getBeginSlotID(slotName)) && !slotName.isEmpty()) {
			grantBadge(player, getBeginSlotID(slotName), collectionName, false, slotName);
		}
	}

	private void checkExplorerBadgeCount(PlayerObject player, String collectionName, int badgeCount) {
		String slotName = "";

		switch (badgeCount) {

			case 0:
				break;
			case 10:
				slotName = "bdg_exp_10_badges";
				break;
			case 20:
				slotName = "bdg_exp_20_badges";
				break;
			case 30:
				slotName = "bdg_exp_30_badges";
				break;
			case 40:
				slotName = "bdg_exp_40_badges";
				break;
			case 45:
				slotName = "bdg_exp_45_badges";
				break;
			default:
				break;
		}

		if (!hasBadge(player, getBeginSlotID(slotName)) && !slotName.isEmpty()) {
			grantBadge(player, getBeginSlotID(slotName), collectionName, false, slotName);
		}
	}

	private int getBeginSlotID(String slotName) {

		for (int row = 0; row < collectionTable.getRowCount(); row++) {
			if (collectionTable.getCell(row, 3).toString().equals(slotName)) {
				return (int) collectionTable.getCell(row, 4);
			}
		}
		return -1;
	}

	private CollectionRowData getCollectionRows(CollectionItem collection) {
		CollectionRowData rows = new CollectionRowData();

		for (int row = 0; row < collectionTable.getRowCount(); row++) {
			if (collectionTable.getCell(row, 2).toString().equals(collection.getCollectionName())) {
				rows.setCollectionRow(row);
			} else if (collectionTable.getCell(row, 3).toString().equals(collection.getSlotName())) {
				rows.setSlotRow(row);
				break;
			}
		}

		// Backtrack up to get the book and page name
		boolean pageRowFound = false;
		boolean bookRowFound = false;

		for (int row = rows.collectionRow; row > 0; row--) {
			if (!pageRowFound && collectionTable.getCell(row, 0).toString().isEmpty() && collectionTable.getCell(row, 2).toString().isEmpty() && !collectionTable.getCell(row, 1).toString().isEmpty()) {
				pageRowFound = true;
				rows.setPageRow(row);
			} else if (!bookRowFound && collectionTable.getCell(row, 1).toString().isEmpty() && !collectionTable.getCell(row, 0).toString().isEmpty()) {
				bookRowFound = true;
				rows.setBookRow(row);
			}
		}

		return rows;
	}

	private void handleMessage(PlayerObject player, boolean collectionComplete, String collectionName, boolean hidden, String slotName) {
		Player thisplayer = player.getOwner();

		if (hidden) {
			sendSystemMessage(thisplayer, "@collection:player_hidden_slot_added", "TO", "@collection_n:" + collectionName);
			thisplayer.sendPacket(new PlayMusicMessage(0, "sound/utinni.snd", 1, false));
		} else {
			sendSystemMessage(thisplayer, "@collection:player_slot_added", "TU", "@collection_n:" + slotName, "TO", "@collection_n:" + collectionName);
			thisplayer.sendPacket(new PlayMusicMessage(0, "sound/utinni.snd", 1, false));
		}
		if (collectionComplete) {
			sendSystemMessage(thisplayer, "@collection:player_collection_complete", "TO", "@collection_n:" + collectionName);
		}
	}

	private boolean hasBadge(PlayerObject player, int badgeBeginSlotId) {
		BitSet collections = BitSet.valueOf(player.getCollectionBadges());

		if (collections.get(badgeBeginSlotId)) {
			return true;
		}
		return false;
	}

	private boolean hasCompletedCollection(PlayerObject player, String collectionTitle) {

		String collectionName = "";
		BitSet collections = BitSet.valueOf(player.getCollectionBadges());

		for (int row = 0; row < collectionTable.getRowCount(); row++) {
			int beginSlotId = (int) collectionTable.getCell(row, 4);
			if (collectionTable.getCell(row, 2).toString() != "") {
				collectionName = collectionTable.getCell(row, 2).toString();
			} else if (collectionName.equals(collectionTitle)) {
				if (!collections.get(beginSlotId)) {
					return false;
				}
			} else if (collectionTitle.equals(collectionTable.getCell(row, 0).toString()) || collectionTitle.equals(collectionTable.getCell(row, 1).toString())) {
				return false;
			}
		}
		return true;
	}

	private boolean hasPreReqComplete(PlayerObject player, String preReqSlotName) {
		Player thisplayer = player.getOwner();

		if (!preReqSlotName.isEmpty() && !hasBadge(player, getBeginSlotID(preReqSlotName))) {
			sendSystemMessage(thisplayer, "@collection:need_to_activate_collection");
			return false;
		}
		return true;
	}

	private void sendSystemMessage(Player target, String id, Object... objects) {
		IntentFactory.sendSystemMessage(target, id, objects);
	}

	private class BadgeInformation {

		private final PlayerObject player;
		private final String collectionBadgeName;

		private int bookBadgeCount = 0;
		private int pageBadgeCount = 0;
		private String bookName = "";
		private String pageName = "";
		private String collectionName = "";
		private String slotName = "";
		private int beginSlotId = -1;
		private int endSlotId = -1;
		private int maxSlotValue = -1;
		private boolean isHidden = false;
		private String preReqSlotName = "";
		private String music = "";

		BadgeInformation(PlayerObject player, String collectionBadgeName) {
			this.player = player;
			this.collectionBadgeName = collectionBadgeName;
		}

		public void setBookName(String bookName) {
			this.bookName = bookName;
		}

		public void setPageName(String pageName) {
			this.pageName = pageName;
		}

		public void setCollectionName(String collectionName) {
			this.collectionName = collectionName;
		}

		public void setSlotName(String slotName) {
			this.slotName = slotName;

			if (this.slotName.equals(collectionBadgeName)) {
				if (hasPreReqComplete(player, preReqSlotName)) {
					if (endSlotId != -1) {
						grantBadgeIncrement(player, beginSlotId, endSlotId, maxSlotValue);
					} else if (!hasBadge(player, beginSlotId)) {
						grantBadge(player, beginSlotId, collectionName, isHidden, this.slotName);
					}
				}
			}

			if (bookName.equals("badge_book") && hasBadge(player, this.beginSlotId)) {
				bookBadgeCount++;
			}

			if (pageName.equals("bdg_explore") && hasBadge(player, this.beginSlotId)) {
				pageBadgeCount++;
			}
		}

		public void setBeginSlotId(int beginSlotId) {
			this.beginSlotId = beginSlotId;
		}

		public void setEndSlotId(int endSlotId) {
			this.endSlotId = endSlotId;
		}

		public void setMaxSlotValue(int maxSlotValue) {
			this.maxSlotValue = maxSlotValue;
		}

		public void setIsHidden(boolean isHidden) {
			this.isHidden = isHidden;
		}

		public void setPreReqSlotName(String preReqSlotName) {
			this.preReqSlotName = preReqSlotName;
		}

		public void setMusic(String music) {
			this.music = music;
		}
	}

	private class CollectionRowData {
		private int bookRow;
		private int pageRow;
		private int collectionRow;
		private int slotRow;

		public CollectionRowData() {
			this(0, 0, 0, 0);
		}

		public CollectionRowData(int bookRow, int pageRow, int collectionRow, int slotRow) {
			this.bookRow = bookRow;
			this.pageRow = pageRow;
			this.collectionRow = collectionRow;
			this.slotRow = slotRow;
		}

		public int getBookRow() {
			return bookRow;
		}

		public int getPageRow() {
			return pageRow;
		}

		public int getCollectionRow() {
			return collectionRow;
		}

		public int getSlotRow() {
			return slotRow;
		}

		public void setBookRow(int bookRow) {
			this.bookRow = bookRow;
		}

		public void setPageRow(int pageRow) {
			this.pageRow = pageRow;
		}

		public void setCollectionRow(int collectionRow) {
			this.collectionRow = collectionRow;
		}

		public void setSlotRow(int slotRow) {
			this.slotRow = slotRow;
		}
	}
}
