/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.services.gameplay.junkdealer;

import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.data.sui.SuiEvent;
import com.projectswg.holocore.intents.gameplay.junkdealer.SellItemsIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.objects.DestroyObjectIntent;
import com.projectswg.holocore.intents.support.objects.MoveObjectIntent;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiButtons;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiListBox;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiMessageBox;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiWindow;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.ServerAttribute;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

import java.util.*;

public class JunkDealerService extends Service {
	
	private static final int SUI_ALLOWED_DISTANCE = 7;
	
	private final Map<CreatureObject, Session> suiWindowMap;
	
	public JunkDealerService() {
		suiWindowMap = new HashMap<>();
	}
	
	@IntentHandler
	private void handleSellItemsIntent(SellItemsIntent intent) {
		Player player = intent.getPlayer();
		AIObject npc = intent.getNpc();
		
		displaySuiWindow(player, npc);
	}
	
	@IntentHandler
	private void handleMoveObjectIntent(MoveObjectIntent intent) {
		SWGObject object = intent.getObj();
		
		if (!(object instanceof CreatureObject)) {
			return;
		}
		
		CreatureObject creatureObject = (CreatureObject) object;
		
		if (!suiWindowMap.containsKey(creatureObject)) {
			return;
		}
		
		Session session = suiWindowMap.get(creatureObject);
		AIObject npc = session.getNpc();
		
		
		if (isWithinRange(creatureObject, npc)) {
			return;
		}
		
		suiWindowMap.remove(creatureObject);
		
		SuiWindow suiWindow = session.getWindow();
		
		suiWindow.close(creatureObject.getOwner());
	}
	
	private void displaySuiWindow(Player player, AIObject npc) {
		List<TangibleObject> sellableItems = getSellableItems(player);
		SuiWindow window;
		
		if (sellableItems.isEmpty()) {
			window = createNoItemsWindow();
		} else {
			window = createSellableItemsWindow(player, sellableItems);
		}
		
		window.display(player);
		Session session = new Session(npc, window);
		suiWindowMap.put(player.getCreatureObject(), session);
	}
	
	private SuiWindow createNoItemsWindow() {
		return new SuiMessageBox(SuiButtons.OK, "@loot_dealer:sell_title", "@loot_dealer:no_items");
	}
	
	private SuiWindow createSellableItemsWindow(Player player, List<TangibleObject> sellableItems) {
		SuiListBox listBox = new SuiListBox(SuiButtons.OK_CANCEL, "@loot_dealer:sell_title", "@loot_dealer:sell_prompt");
		
		for (TangibleObject sellableItem : sellableItems) {
			String listItem = createListItem(sellableItem);
			
			listBox.addListItem(listItem, sellableItem);
		}
		
		listBox.setProperty("btnOk", "Text", "@loot_dealer:btn_sell");
		listBox.addCallback(SuiEvent.OK_PRESSED, "handleItemChoice", (event, parameters) -> handleItemChoice(sellableItems, player, parameters));
		
		return listBox;
	}
	
	private List<TangibleObject> getSellableItems(Player player) {
		CreatureObject creatureObject = player.getCreatureObject();
		SWGObject inventory = creatureObject.getInventory();
		List<TangibleObject> sellableItems = new ArrayList<>();
		
		Collection<SWGObject> childObjects = inventory.getChildObjectsRecursively();
		
		for (SWGObject childObject : childObjects) {
			if (childObject instanceof TangibleObject) {
				TangibleObject item = (TangibleObject) childObject;
				Integer itemValue = (Integer) item.getServerAttribute(ServerAttribute.ITEM_VALUE);
				
				if (itemValue != null) {
					if (getPriceForItem(item) > 0) {
						if (isContainer(item)) {
							if (isEmpty(item)) {
								sellableItems.add(item);
							}
						} else {
							sellableItems.add(item);
						}
					}
				}
			}
			
		}
		
		return sellableItems;
	}
	
	private String createListItem(TangibleObject item) {
		String baseFormat = "[%d] %s";
		int price = getPriceForItem(item);
		
		return String.format(baseFormat, price, item.getObjectName());
	}
	
	private void handleItemChoice(List<TangibleObject> sellableItems, Player player, Map<String, String> parameters) {
		int row = SuiListBox.getSelectedRow(parameters);
		TangibleObject sellableItem = sellableItems.get(row);
		
		int price = getPriceForItem(sellableItem);
		
		CreatureObject creatureObject = player.getCreatureObject();
		creatureObject.addToCash(price);
		
		Session session = suiWindowMap.remove(creatureObject);
		
		ProsePackage itemSoldProse = new ProsePackage(new StringId("junk_dealer", "prose_sold_junk"), "TT", sellableItem.getObjectName(), "DI", price);
		SystemMessageIntent.Companion.broadcastPersonal(player, itemSoldProse);
		
		DestroyObjectIntent destroyObjectIntent = new DestroyObjectIntent((sellableItem));
		SellItemsIntent sellItemsIntent = new SellItemsIntent(player, session.getNpc());
		sellItemsIntent.broadcastAfterIntent(destroyObjectIntent);
		destroyObjectIntent.broadcast();
	}
	
	private int getPriceForItem(TangibleObject item) {
		Integer itemValue = (Integer) item.getServerAttribute(ServerAttribute.ITEM_VALUE);
		
		if (itemValue == null) {
			return 0;
		}
		
		int stackSize = Math.max(1, item.getCounter());
		
		return itemValue * stackSize;
	}
	
	private boolean isContainer(TangibleObject item) {
		return item.getContainerType() == 2;
	}
	
	private boolean isEmpty(TangibleObject item) {
		return item.getChildObjects().isEmpty();
	}
	
	private boolean isWithinRange(CreatureObject creatureObject, AIObject npc) {
		Location playerWorldLocation = creatureObject.getWorldLocation();
		Location npcWorldLocation = npc.getWorldLocation();
		Terrain playerTerrain = playerWorldLocation.getTerrain();
		Terrain npcTerrain = npcWorldLocation.getTerrain();
		
		if (playerTerrain != npcTerrain) {
			return false;
		}
		
		double distanceTo = playerWorldLocation.distanceTo(npcWorldLocation);
		
		return distanceTo <= SUI_ALLOWED_DISTANCE;
	}
	
	private static class Session {
		private final AIObject npc;
		private final SuiWindow window;
		
		public Session(AIObject npc, SuiWindow window) {
			this.npc = npc;
			this.window = window;
		}
		
		public AIObject getNpc() {
			return npc;
		}
		
		public SuiWindow getWindow() {
			return window;
		}
	}
}
