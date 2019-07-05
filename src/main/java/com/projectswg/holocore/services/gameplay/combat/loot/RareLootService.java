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
package com.projectswg.holocore.services.gameplay.combat.loot;

import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage;
import com.projectswg.common.network.packets.swg.zone.PlayMusicMessage;
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowLootBox;
import com.projectswg.holocore.intents.gameplay.combat.CreatureKilledIntent;
import com.projectswg.holocore.intents.gameplay.combat.loot.OpenRareChestIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.objects.items.CreateStaticItemIntent;
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureDifficulty;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import com.projectswg.holocore.services.support.objects.items.StaticItemService;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class RareLootService extends Service {
	
	public static final String RARE_CHEST = "object/tangible/item/shared_rare_loot_chest_1.iff";
	public static final String EXCEPTIONAL_CHEST = "object/tangible/item/shared_rare_loot_chest_2.iff";
	public static final String LEGENDARY_CHEST = "object/tangible/item/shared_rare_loot_chest_3.iff";
	
	// TODO these two could be config options
	private static final short MAX_LEVEL_DIFFERENCE = 6;    // +-6 difference is allowed between killer and corpse
	private static final int DROP_CHANCE = 1;    // One in a hundred eligible kills will drop a chest
	
	private final Random random;
	private final List<String> rareItems;
	private final List<String> exceptionalItems;
	private final List<String> legendaryItems;
	
	public RareLootService() {
		random = new Random();
		
		rareItems = new ArrayList<>();
		exceptionalItems = new ArrayList<>();
		legendaryItems = new ArrayList<>();
	}
	
	@Override
	public boolean initialize() {
		String what = "Rare Loot System items";
		long start = StandardLog.onStartLoad(what);
		try (SdbLoader.SdbResultSet set = SdbLoader.load(new File("serverdata/loot/rls_items.sdb"))) {
			while (set.next()) {
				String rarity = set.getText("rarity");
				String staticItemId = set.getText("static_item_id");
				
				switch (rarity) {
					case "RARE":
						rareItems.add(staticItemId);
						break;
					case "EXCEPTIONAL":
						exceptionalItems.add(staticItemId);
						break;
					case "LEGENDARY":
						legendaryItems.add(staticItemId);
						break;
				}
			}
		} catch (IOException e) {
			Log.e(e);
		}
		
		int quantity = rareItems.size() + exceptionalItems.size() + legendaryItems.size();
		
		StandardLog.onEndLoad(quantity, what, start);
		
		return super.initialize();
	}
	
	@IntentHandler
	private void handleCreatureKilledIntent(CreatureKilledIntent cki) {
		// TODO only the player that delivered the killing blow is considered for this
		CreatureObject corpse = cki.getCorpse();
		CreatureObject killer = cki.getKiller();
		
		if (!isPlayerEligible(killer.isPlayer(), corpse.isPlayer())) {
			return;
		}
		
		if (!isLevelEligible(corpse.getLevel(), killer.getLevel())) {
			return;
		}
		
		int roll = random.nextInt(100) + 1;    // Rolls from 0 to 99, then we add 1 and it becomes 1 to 100
		
		if (!isDrop(roll)) {
			Log.d("No RLS drop from %s with roll %d", corpse, roll);
			return;
		}
		
		String template = templateForDifficulty(corpse.getDifficulty());
		SWGObject chest = ObjectCreator.createObjectFromTemplate(template);
		SWGObject inventory = killer.getSlottedObject("inventory");
		
		chest.setStf("loot_n", chestIdForTemplate(template) + "_n");
		chest.setDetailStf(new StringId("loot_n", chestIdForTemplate(template) + "_d"));    // Not located in loot_d, for whatever reason...
		
		chest.moveToContainer(inventory);
		sendSuccessPackets(chest, corpse, killer);
	}
	
	@IntentHandler
	private void handleOpenRareChestIntent(OpenRareChestIntent intent) {
		CreatureObject actor = intent.getActor();
		TangibleObject chest = intent.getChest();
		
		// Figure out rarity of the chest
		String chestTemplate = chest.getTemplate();
		String chosenItem;
		
		switch (chestTemplate) {
			case RARE_CHEST:
				chosenItem = randomItem(rareItems);
				break;
			case EXCEPTIONAL_CHEST:
				chosenItem = randomItem(exceptionalItems);
				break;
			case LEGENDARY_CHEST:
				chosenItem = randomItem(legendaryItems);
				break;
			default:
				Log.e("%s tried to open a Rare Loot System chest with an unrecognized object template", actor, chestTemplate);
				return;
		}
		
		StandardLog.onPlayerEvent(this, actor, "opened a RLS chest and received static item: %s", chosenItem);
		
		// Destroy the chest
		DestroyObjectIntent.broadcast(chest);
		
		// Give the player the item
		new CreateStaticItemIntent(
				actor,	// The object requesting the item transfer
				actor.getInventory(),	// The item should land in the inventory of the player who opened the chest
				new RareLootCreationHandler(actor),	// Show a loot box window with the item
				chosenItem
		).broadcast();
		
	}
	
	boolean isPlayerEligible(boolean killerPlayer, boolean corpsePlayer) {
		return killerPlayer && !corpsePlayer;
	}
	
	boolean isLevelEligible(int corpseLevel, int killerLevel) {
		// Ensure a positive levelDifference
		int highestLevel = Math.max(corpseLevel, killerLevel);
		int lowestLevel = Math.min(corpseLevel, killerLevel);
		int levelDifference = highestLevel - lowestLevel;
		
		return levelDifference <= MAX_LEVEL_DIFFERENCE;
	}
	
	boolean isDrop(int roll) {
		return roll <= DROP_CHANCE;
	}
	
	String templateForDifficulty(CreatureDifficulty difficulty) {
		switch (difficulty) {
			case NORMAL:
				return RARE_CHEST;
			case ELITE:
				return EXCEPTIONAL_CHEST;
			case BOSS:
				return LEGENDARY_CHEST;
			default:
				throw new IllegalArgumentException("Unknown CreatureDifficulty: " + difficulty);
		}
	}
	
	String chestIdForTemplate(String template) {
		return template.replace("object/tangible/item/shared_", "").replace(".iff", "");
	}
	
	private void sendSuccessPackets(SWGObject chest, CreatureObject corpse, CreatureObject killer) {
		ObjectCreatedIntent.broadcast(chest);
		
		PlayClientEffectObjectMessage effect = new PlayClientEffectObjectMessage("appearance/pt_rare_chest.prt", "", corpse.getObjectId(), "");
		PlayMusicMessage sound = new PlayMusicMessage(0, "sound/rare_loot_chest.snd", 1, false);
		ShowLootBox box = new ShowLootBox(killer.getObjectId(), new long[] { chest.getObjectId() });
		
		killer.getOwner().sendPacket(effect, sound, box);
	}

	private String randomItem(List<String> items) {
		int availableItems = items.size();
		int randomIndex = ThreadLocalRandom.current().nextInt(availableItems);
		
		return items.get(randomIndex);
	}
	
	private static class RareLootCreationHandler implements StaticItemService.ObjectCreationHandler {
		
		private final CreatureObject actor;
		
		public RareLootCreationHandler(CreatureObject actor) {
			this.actor = actor;
		}
		
		@Override
		public void success(@NotNull List<? extends SWGObject> createdObjects) {
			// Apply rarity item attribute to the created RLS item
			for (SWGObject createdObject : createdObjects) {
				createdObject.addAttribute("@obj_attr_n:rare_loot_category", "Rare Item");
			}
			
			// Show items in loot box window
			new StaticItemService.LootBoxHandler(actor).success(createdObjects);
		}
		
		@Override
		public boolean isIgnoreVolume() {
			return false;
		}
		
		@Override
		public void containerFull() {
			Player owner = actor.getOwner();
			
			if (owner == null) {
				return;
			}
			
			SystemMessageIntent.broadcastPersonal(owner, "@container_error_message:container03");
			
		}
	}
}
