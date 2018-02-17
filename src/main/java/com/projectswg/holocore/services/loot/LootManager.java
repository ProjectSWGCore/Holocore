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
package com.projectswg.holocore.services.loot;

import com.projectswg.common.control.Manager;
import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.info.RelationalDatabase;
import com.projectswg.common.data.info.RelationalServerFactory;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.debug.Assert;
import com.projectswg.common.debug.Log;
import com.projectswg.common.network.packets.swg.zone.ClientOpenContainerMessage;
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectTransformMessage;
import com.projectswg.common.network.packets.swg.zone.PlayMusicMessage;
import com.projectswg.common.network.packets.swg.zone.StopClientEffectObjectByLabelMessage;
import com.projectswg.holocore.intents.chat.ChatCommandIntent;
import com.projectswg.holocore.intents.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.combat.CorpseLootedIntent;
import com.projectswg.holocore.intents.combat.CreatureKilledIntent;
import com.projectswg.holocore.intents.combat.LootItemIntent;
import com.projectswg.holocore.intents.combat.loot.LootRequestIntent;
import com.projectswg.holocore.intents.object.ContainerTransferIntent;
import com.projectswg.holocore.intents.object.CreateStaticItemIntent;
import com.projectswg.holocore.intents.object.DestroyObjectIntent;
import com.projectswg.holocore.intents.object.ObjectCreatedIntent;
import com.projectswg.holocore.resources.config.ConfigFile;
import com.projectswg.holocore.resources.containers.ContainerPermissionsType;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.creature.CreatureDifficulty;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import com.projectswg.holocore.resources.objects.custom.AIObject;
import com.projectswg.holocore.resources.objects.group.GroupObject;
import com.projectswg.holocore.resources.objects.tangible.CreditObject;
import com.projectswg.holocore.resources.player.Player;
import com.projectswg.holocore.resources.server_info.DataManager;
import com.projectswg.holocore.resources.server_info.StandardLog;
import com.projectswg.holocore.resources.server_info.loader.npc.NpcLoader;
import com.projectswg.holocore.resources.server_info.loader.npc.NpcLoader.NpcInfo;
import com.projectswg.holocore.services.objects.ObjectCreator;
import com.projectswg.holocore.services.objects.ObjectManager.ObjectLookup;
import com.projectswg.holocore.services.objects.StaticItemService;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public final class LootManager extends Manager {
	
	private static final String LOOT_TABLE_SELECTOR = "SELECT * FROM loot_table";
	
	private static final float CASH_LOOT_CHANCE_NORMAL = 0.6f;
	private static final float CASH_LOOT_CHANCE_ELITE = 0.8f;
	
	private final Map<String, LootTable> lootTables;    // K: loot_id, V: table contents
	private final Map<String, NPCLoot> npcLoot;    // K: npc_id, V: possible loot
	private final Random random;
	
	public LootManager() {
		lootTables = new HashMap<>();
		npcLoot = new HashMap<>();
		random = new Random();
		
		addChildService(new RareLootService());
		
		registerForIntent(ChatCommandIntent.class, this::handleChatCommand);
		registerForIntent(ContainerTransferIntent.class, this::handleContainerTransfer);
		registerForIntent(CreatureKilledIntent.class, this::handleCreatureKilled);
		registerForIntent(LootRequestIntent.class, this::handleLootRequestIntent);
		registerForIntent(LootItemIntent.class, this::handleLootItemIntent);
	}
	
	@Override
	public boolean initialize() {
		loadLootTables();
		loadNPCLoot();
		
		return super.initialize();
	}
	
	/**
	 * Loads loot tables from loot_table.db. Each "loot table" is in the form of a row.
	 * Each row has an id and several "loot groups," which consist of actual items that can be dropped.
	 * Each group has a different chance of being selected for loot.
	 */
	private void loadLootTables() {
		String what = "loot tables";
		long startTime = StandardLog.onStartLoad(what);
		
		try (RelationalDatabase spawnerDatabase = RelationalServerFactory.getServerData("loot/loot_table.db", "loot_table")) {
			try (ResultSet set = spawnerDatabase.executeQuery(LOOT_TABLE_SELECTOR)) {
				while (set.next()) {
					loadLootTable(set);        // each set is one row in the DB
				}
			} catch (SQLException e) {
				Log.e(e);
			}
		}
		
		StandardLog.onEndLoad(lootTables.size(), what, startTime);
	}
	
	/**
	 * Loads a single loot table.
	 *
	 * @param set a single loot table (a row in loot_table.db)
	 */
	private void loadLootTable(ResultSet set) throws SQLException {
		String tableName = set.getString("loot_id");
		
		if (tableName.equals("-")) {
			return;
		}
		
		LootTable table = new LootTable();
		int totalChance = 0;    // Must not be above 100. Also used to convert chances in the form of "33, 33, 34" to "33, 66, 100"
		
		for (int groupNum = 1; groupNum <= 16 && totalChance < 100; groupNum++) {
			LootGroup lootGroup = loadLootGroup(set, groupNum, totalChance);
			if (lootGroup == null)
				continue;
			table.addLootGroup(lootGroup);
			totalChance = lootGroup.getChance();
		}
		
		if (totalChance != 100)
			Log.w("Loot chance != 100 while loading loot_table! Table: %s, totalChance: %s", tableName, totalChance);
		
		lootTables.put(tableName, table);
	}
	
	/**
	 * Loads a single loot group from a loot table.
	 *
	 * @param set         the loot table
	 * @param groupNum    the group number
	 * @param totalChance the sum of all group chances before this group
	 * @return {@link LootGroup}
	 */
	private LootGroup loadLootGroup(ResultSet set, int groupNum, int totalChance) throws SQLException {
		int groupChance = set.getInt("chance_group_" + groupNum);
		if (groupChance == 0)
			return null;
		groupChance += totalChance;
		String groupItems = set.getString("items_group_" + groupNum);
		String[] itemNames = groupItems.split(";");
		
		return new LootGroup(groupChance, itemNames);
	}
	
	/**
	 * Loads NPC loot tables. There are up to 3 tables per NPC.
	 */
	private void loadNPCLoot() {
		String what = "NPC loot links";
		long startTime = StandardLog.onStartLoad(what);
		
		NpcLoader npcLoader = NpcLoader.load();
		npcLoader.iterate(this::loadNPCLoot);
		
		StandardLog.onEndLoad(npcLoot.size(), what, startTime);
	}
	
	/**
	 * Load tables for a specific NPC.
	 *
	 * @param info the NPC info
	 */
	private void loadNPCLoot(NpcInfo info) {
		// we don't care about non-humanoids
		if (info.getHumanoidInfo() == null)
			return;
		int minCash = info.getHumanoidInfo().getMinCash();
		int maxCash = info.getHumanoidInfo().getMaxCash();
		String creatureId = info.getId();
		NPCLoot loot = new NPCLoot(minCash, maxCash);
		
		// load each loot table (up to 3) and add to loot object
		loadNPCTable(loot, info.getLootTable1(), info.getLootTable1Chance());
		loadNPCTable(loot, info.getLootTable2(), info.getLootTable2Chance());
		loadNPCTable(loot, info.getLootTable3(), info.getLootTable3Chance());
		npcLoot.put(creatureId, loot);
	}
	
	/**
	 * Load a specific table for an NPC.
	 *
	 * @param loot   the loot object for the NPC
	 * @param table  the loot table
	 * @param chance the chance for this loot table (used when generating loot)
	 */
	private void loadNPCTable(NPCLoot loot, String table, int chance) {
		// if chance <= 0, this table for this NPC doesn't exist
		if (chance <= 0)
			return;
		
		LootTable lootTable = lootTables.get(table);
		if (lootTable == null)
			return;
		
		loot.addNPCTable(new NPCTable(chance, lootTable));
	}
	
	private void handleContainerTransfer(ContainerTransferIntent cti) {
		SWGObject object = cti.getObject();
		
		if (cti.getContainer() == null || cti.getContainer().getOwner() == null)
			return;
		
		if (object.getContainerPermissions() == ContainerPermissionsType.LOOT)
			object.setContainerPermissions(ContainerPermissionsType.DEFAULT);
	}
	
	private void handleCreatureKilled(CreatureKilledIntent cki) {
		CreatureObject corpse = cki.getCorpse();
		
		if (corpse.isPlayer()) {
			// Players don't drop loot
			return;
		}
		
		String creatureId = ((AIObject) corpse).getCreatureId();
		NPCLoot loot = npcLoot.get(creatureId);
		
		if (loot == null) {
			Log.w("No NPCLoot associated with NPC ID: " + creatureId);
			return;
		}
		
		SWGObject lootInventory = ObjectCreator.createObjectFromTemplate("object/tangible/inventory/shared_creature_inventory.iff");
		lootInventory.setLocation(corpse.getLocation());
		lootInventory.setContainerPermissions(ContainerPermissionsType.LOOT);
		corpse.addObject(lootInventory);    // It's a slotted object and goes in the inventory slot
		new ObjectCreatedIntent(lootInventory).broadcast();
		
		CreatureObject killer = cki.getKiller();
		
		boolean cashGenerated = false;
		boolean lootGenerated = false;
		
		if (DataManager.getConfig(ConfigFile.LOOTOPTIONS).getBoolean("ENABLE-CASH-LOOT", true))
			cashGenerated = generateCreditChip(loot, killer, lootInventory, corpse.getDifficulty());
		if (DataManager.getConfig(ConfigFile.LOOTOPTIONS).getBoolean("ENABLE-ITEM-LOOT", true))
			lootGenerated = generateLoot(loot, killer, lootInventory);
		
		if (!cashGenerated && !lootGenerated)
			new CorpseLootedIntent(corpse).broadcast();
		else
			showLootDisc(killer, corpse);
	}
	
	private void handleChatCommand(ChatCommandIntent cci) {
		
		if (!cci.getCommand().getName().equalsIgnoreCase("loot")) {
			return;
		}
		
		if (!getLootPermission(cci.getSource(), cci.getTarget()))
			return;
		
		lootAll(cci.getSource(), cci.getTarget());
	}
	
	private void handleLootRequestIntent(LootRequestIntent lri) {
		Player player = lri.getPlayer();
		CreatureObject looter = player.getCreatureObject();
		SWGObject target = lri.getTarget();
		
		switch (lri.getType()) {
			case LOOT:
				if (!getLootPermission(looter, target))
					return;
				
				lootBox(player, target);
				break;
			case LOOT_ALL:
				if (!getLootPermission(looter, target))
					return;
				
				lootAll(looter, target);
				break;
			case CREDITS:
				lootCredits(player, looter, target);
				break;
		}
	}
	
	private void handleLootItemIntent(LootItemIntent lii) {
		CreatureObject looter = lii.getLooter().getCreatureObject();
		SWGObject item = lii.getItem();
		SWGObject container = lii.getContainer();
		
		loot(looter, item, container);
	}
	
	/**
	 * Transfers the item to the looter's inventory and sends appropriate system messages.
	 *
	 * @param looter    the player doing the looting
	 * @param item      the item being looted
	 * @param container the container of the item
	 */
	private void loot(CreatureObject looter, SWGObject item, SWGObject container) {
		Player player = looter.getOwner();
		
		switch (item.moveToContainer(looter, looter.getSlottedObject("inventory"))) {
			case SUCCESS: {
				String itemName = item.getObjectName();
				
				if (item instanceof CreditObject) {
					long cash = ((CreditObject) item).getAmount();
					new SystemMessageIntent(player, new ProsePackage("StringId", new StringId("base_player", "prose_coin_loot_no_target"), "DI", (int) cash))
							.broadcast();
				} else {
					new SystemMessageIntent(player, new ProsePackage("StringId", new StringId("loot_n", "solo_looted"), "TO", itemName)).broadcast();
				}
				
				if (container.getContainedObjects().isEmpty()) {
					CreatureObject corpse = (CreatureObject) container.getParent();
					new CorpseLootedIntent(corpse).broadcast();
					player.sendPacket(new StopClientEffectObjectByLabelMessage(corpse.getObjectId(), "lootMe", false));
				}
				break;
			}
			case CONTAINER_FULL:
				new SystemMessageIntent(player, "@container_error_message:container03").broadcast();
				player.sendPacket(new PlayMusicMessage(0, "sound/ui_danger_message.snd", 1, false));
				break;
			case NO_PERMISSION:
				new SystemMessageIntent(player, "@container_error_message:container08").broadcast();
				player.sendPacket(new PlayMusicMessage(0, "sound/ui_negative.snd", 1, false));
				break;
			case SLOT_NO_EXIST:
				new SystemMessageIntent(player, "@container_error_message:container06").broadcast();
				player.sendPacket(new PlayMusicMessage(0, "sound/ui_negative.snd", 1, false));
				break;
			case SLOT_OCCUPIED:
				new SystemMessageIntent(player, "@container_error_message:container08").broadcast();
				player.sendPacket(new PlayMusicMessage(0, "sound/ui_negative.snd", 1, false));
				break;
		}
	}
	
	private void lootBox(Player player, SWGObject target) {
		SWGObject inventory = target.getSlottedObject("inventory");
		
		player.sendPacket(new ClientOpenContainerMessage(inventory.getObjectId(), ""));
	}
	
	private void lootAll(SWGObject looter, SWGObject corpse) {
		if (!(corpse instanceof AIObject)) {
			return;
		}
		
		SWGObject lootInventory = corpse.getSlottedObject("inventory");
		
		Collection<SWGObject> loot = lootInventory.getContainedObjects();    // No concurrent modification because a copy Collection is returned
		
		loot.forEach(item -> loot((CreatureObject) looter, item, lootInventory));
	}
	
	private void lootCredits(Player player, CreatureObject looter, SWGObject target) {
		SWGObject container = target.getParent();
		SWGObject owner = container.getParent();
		
		if (!getLootPermission(looter, owner))
			return;
		
		long cash = ((CreditObject) target).getAmount();
		looter.addToBank(cash);
		DestroyObjectIntent.broadcast(target);
		
		new SystemMessageIntent(player, new ProsePackage("StringId", new StringId("base_player", "prose_transfer_success"), "DI", (int) cash))
				.broadcast();
		
		if (owner instanceof CreatureObject && container.getContainedObjects().isEmpty()) {
			CreatureObject corpse = (CreatureObject) owner;
			new CorpseLootedIntent(corpse).broadcast();
			player.sendPacket(new StopClientEffectObjectByLabelMessage(corpse.getObjectId(), "lootMe", false));
		}
	}
	
	private void randomGroupLoot(GroupObject lootGroup, SWGObject corpse) {
		if (!(corpse instanceof AIObject)) {
			return;
		}
		
		SWGObject lootInventory = corpse.getSlottedObject("inventory");
		CreatureObject randomPlayer;
		
		for (SWGObject item : lootInventory.getContainedObjects()) {
			// TODO: split cash
			randomPlayer = lootGroup.getRandomPlayer();
			if (randomPlayer != null)
				loot(randomPlayer, item, lootInventory);
		}
	}
	
	private void showLootDisc(CreatureObject requester, SWGObject corpse) {
		Assert.test(requester.isPlayer());
		
		Location effectLocation = Location.builder(corpse.getLocation()).setPosition(0, 0.5, 0).build();
		
		long requesterGroup = requester.getGroupId();
		
		if (requesterGroup != 0) {
			GroupObject requesterGroupObject = (GroupObject) ObjectLookup.getObjectById(requesterGroup);
			
			for (CreatureObject creature : requesterGroupObject.getGroupMemberObjects()) {
				Player player = creature.getOwner();
				if (player != null) {
					player.sendPacket(new PlayClientEffectObjectTransformMessage(corpse
							.getObjectId(), "appearance/pt_loot_disc.prt", effectLocation, "lootMe"));
				}
			}
		} else {
			requester.getOwner().sendPacket(new PlayClientEffectObjectTransformMessage(corpse
					.getObjectId(), "appearance/pt_loot_disc.prt", effectLocation, "lootMe"));
		}
	}
	
	private boolean generateCreditChip(NPCLoot loot, CreatureObject killer, SWGObject lootInventory, CreatureDifficulty difficulty) {
		float cashLootRoll = random.nextFloat();
		int multiplier;
		
		switch (difficulty) {
			default:
			case NORMAL:
				if (cashLootRoll > CASH_LOOT_CHANCE_NORMAL)
					return false;
				multiplier = 1;
				break;
			case ELITE:
				if (cashLootRoll > CASH_LOOT_CHANCE_ELITE)
					return false;
				multiplier = 2;
				break;
			case BOSS:
				// bosses always drop cash loot, so no need to check
				multiplier = 3;
				break;
		}
		
		int maxCash = loot.getMaxCash();
		
		if (maxCash == 0) {
			// No cash is ever dropped on this creature
			return false;
		}
		
		int minCash = loot.getMinCash();
		int cashAmount = random.nextInt((maxCash - minCash) + 1) + minCash;
		cashAmount *= multiplier;
		
		// TODO scale with group size?
		
		CreditObject cashObject = ObjectCreator.createObjectFromTemplate("object/tangible/item/shared_loot_cash.iff", CreditObject.class);
		
		cashObject.setAmount(cashAmount);
		cashObject.setContainerPermissions(ContainerPermissionsType.LOOT);
		cashObject.moveToContainer(lootInventory);
		
		new ObjectCreatedIntent(cashObject).broadcast();
		
		return true;
	}
	
	/**
	 * Generates loot and places it in the inventory of the corpse.
	 *
	 * @param loot          the loot info of the creature killed
	 * @param killer
	 * @param lootInventory the inventory the loot will be placed in (corpse inventory)
	 * @return whether loot was generated or not
	 */
	private boolean generateLoot(NPCLoot loot, CreatureObject killer, SWGObject lootInventory) {
		boolean lootGenerated = false;
		
		int tableRoll = random.nextInt(100) + 1;
		
		for (NPCTable npcTable : loot.getNPCTables()) {
			LootTable lootTable = npcTable.getLootTable();
			int tableChance = npcTable.getChance();
			
			if (tableRoll > tableChance) {
				// Skip ahead if there's no drop chance
				continue;
			}
			
			int groupRoll = random.nextInt(100) + 1;
			
			for (LootGroup group : lootTable.getLootGroups()) {
				int groupChance = group.getChance();
				
				if (groupRoll > groupChance)
					continue;
				
				String[] itemNames = group.getItemNames();
				String itemName = itemNames[random.nextInt(itemNames.length)];
				
				if (itemName.startsWith("dynamic_")) {
					// TODO dynamic item handling
					new SystemMessageIntent(killer.getOwner(), "We don't support this loot item yet: " + itemName).broadcast();
				} else if (itemName.endsWith(".iff")) {
					String sharedTemplate = ClientFactory.formatToSharedFile(itemName);
					SWGObject object = ObjectCreator.createObjectFromTemplate(sharedTemplate);
					object.setContainerPermissions(ContainerPermissionsType.LOOT);
					object.moveToContainer(lootInventory);
					new ObjectCreatedIntent(object).broadcast();
					
					lootGenerated = true;
				} else {
					new CreateStaticItemIntent(killer, lootInventory, new StaticItemService.ObjectCreationHandler() {
						
						@Override
						public void success(SWGObject[] createdObjects) {
							// do nothing - loot disc is created on the return of the generateLoot method
						}
						
						@Override
						public boolean isIgnoreVolume() {
							return true;
						}
					}, ContainerPermissionsType.LOOT, itemName).broadcast();
					
					lootGenerated = true;
				}
				
				break;
			}
		}
		
		return lootGenerated;
	}
	
	private boolean getLootPermission(CreatureObject looter, SWGObject target) {
		if (!isLootable(target))
			return false;
		
		CreatureObject highestDamageDealer = ((CreatureObject) target).getHighestDamageDealer();
		
		if (highestDamageDealer != null && highestDamageDealer.getOwner() != null) {
			long looterGroup = looter.getGroupId();
			long killerGroup = highestDamageDealer.getGroupId();
			
			if (looterGroup == killerGroup && killerGroup != 0) {
				GroupObject killerGroupObject = (GroupObject) ObjectLookup.getObjectById(killerGroup);
				
				switch (killerGroupObject.getLootRule()) {
					case FREE_FOR_ALL:
						return true;
					case MASTER_LOOTER:
						return looter.getObjectId() == killerGroupObject.getLootMaster();
					case LOTTERY: //TODO Lottery
						return false;
					case RANDOM:
						randomGroupLoot(killerGroupObject, target);
						return false;
					default:
						return false;
				}
			} else {
				return highestDamageDealer.getOwner().equals(looter.getOwner());
			}
		}
		return false;
	}
	
	private boolean isLootable(SWGObject target) {
		SWGObject inventory = target.getSlottedObject("inventory");
		
		return !inventory.getContainedObjects().isEmpty() && inventory.getContainerPermissions() == ContainerPermissionsType.LOOT;
		
	}
	
	private static class NPCLoot {
		
		private final int minCash;
		private final int maxCash;
		private final Collection<NPCTable> npcTables;
		
		public NPCLoot(int minCash, int maxCash) {
			this.minCash = minCash;
			this.maxCash = maxCash;
			npcTables = new ArrayList<>();
		}
		
		public int getMinCash() {
			return minCash;
		}
		
		public int getMaxCash() {
			return maxCash;
		}
		
		public Collection<NPCTable> getNPCTables() {
			return npcTables;
		}
		
		public void addNPCTable(NPCTable npcTable) {
			npcTables.add(npcTable);
		}
		
	}
	
	private static class NPCTable {
		
		private final int chance;
		private final LootTable lootTable;
		
		public NPCTable(int chance, LootTable lootTable) {
			this.chance = chance;
			this.lootTable = lootTable;
		}
		
		public int getChance() {
			return chance;
		}
		
		public LootTable getLootTable() {
			return lootTable;
		}
	}
	
	private static class LootTable {
		
		private final Collection<LootGroup> lootGroups;
		
		public LootTable() {
			lootGroups = new ArrayList<>();
		}
		
		public void addLootGroup(LootGroup lootGroup) {
			lootGroups.add(lootGroup);
		}
		
		public Collection<LootGroup> getLootGroups() {
			return lootGroups;
		}
	}
	
	private static class LootGroup {
		
		private final int chance;
		private final String[] itemNames;
		
		public LootGroup(int chance, String[] staticItems) {
			this.chance = chance;
			this.itemNames = staticItems;
		}
		
		public int getChance() {
			return chance;
		}
		
		public String[] getItemNames() {
			return itemNames;
		}
	}
	
}
