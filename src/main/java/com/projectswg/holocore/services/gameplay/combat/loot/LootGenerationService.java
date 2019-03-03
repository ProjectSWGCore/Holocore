package com.projectswg.holocore.services.gameplay.combat.loot;

import com.projectswg.common.data.info.Config;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectTransformMessage;
import com.projectswg.holocore.intents.gameplay.combat.CreatureKilledIntent;
import com.projectswg.holocore.intents.gameplay.combat.loot.CorpseLootedIntent;
import com.projectswg.holocore.intents.gameplay.combat.loot.LootGeneratedIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.objects.items.CreateStaticItemIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.data.config.ConfigFile;
import com.projectswg.holocore.resources.support.data.server_info.DataManager;
import com.projectswg.holocore.resources.support.data.server_info.SdbColumnArraySet.SdbIntegerColumnArraySet;
import com.projectswg.holocore.resources.support.data.server_info.SdbColumnArraySet.SdbTextColumnArraySet;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcLoader.NpcInfo;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureDifficulty;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import com.projectswg.holocore.resources.support.objects.swg.group.GroupObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.CreditObject;
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup;
import com.projectswg.holocore.services.support.objects.items.StaticItemService;
import me.joshlarson.jlcommon.control.IntentChain;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class LootGenerationService extends Service {
	
	private final Map<String, LootTable> lootTables;    // K: loot_id, V: table contents
	private final Map<String, NPCLoot> npcLoot;    // K: npc_id, V: possible loot
	private final Random random;
	
	public LootGenerationService() {
		lootTables = new HashMap<>();
		npcLoot = new HashMap<>();
		random = new Random();
	}
	
	@Override
	public boolean initialize() {
		loadLootTables();
		loadNPCLoot();
		
		return super.initialize();
	}
	
	@IntentHandler
	private void handleCreatureKilled(CreatureKilledIntent cki) {
		AIObject corpse;
		{
			CreatureObject corpseRaw = cki.getCorpse();
			if (corpseRaw.isPlayer()) {
				// Players don't drop loot
				return;
			}
			
			if (!(corpseRaw instanceof AIObject))
				return;
			corpse = (AIObject) corpseRaw;
		}
		
		String creatureId = corpse.getCreatureId();
		NPCLoot loot = npcLoot.get(creatureId);
		
		if (loot == null) {
			Log.w("No NPCLoot associated with NPC ID: " + creatureId);
			return;
		}
		
		SWGObject lootInventory = ObjectCreator.createObjectFromTemplate("object/tangible/inventory/shared_creature_inventory.iff");
		lootInventory.moveToContainer(corpse, corpse.getLocation());
		ObjectCreatedIntent.broadcast(lootInventory);
		
		CreatureObject killer = cki.getKiller();
		
		boolean cashGenerated = false;
		boolean lootGenerated = false;
		
		if (DataManager.getConfig(ConfigFile.LOOTOPTIONS).getBoolean("ENABLE-CASH-LOOT", true))
			cashGenerated = generateCreditChip(loot, lootInventory, corpse.getDifficulty(), corpse.getLevel());
		if (DataManager.getConfig(ConfigFile.LOOTOPTIONS).getBoolean("ENABLE-ITEM-LOOT", true))
			lootGenerated = generateLoot(loot, killer, lootInventory);
		
		if (!cashGenerated && !lootGenerated)
			new CorpseLootedIntent(corpse).broadcast();
		else
			showLootDisc(killer, corpse);
	}
	
	/**
	 * Loads loot tables from loot_table.db. Each "loot table" is in the form of a row. Each row has an id and several "loot groups," which consist of actual items that can be dropped. Each group has
	 * a different chance of being selected for loot.
	 */
	private void loadLootTables() {
		long startTime = StandardLog.onStartLoad("loot tables");
		
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/loot/loot_table.sdb"))) {
			SdbTextColumnArraySet itemGroups = set.getTextArrayParser("items_group_([0-9]+)");
			SdbIntegerColumnArraySet chanceGroups = set.getIntegerArrayParser("chance_group_([0-9]+)");
			while (set.next()) {
				String tableName = set.getText("loot_id");
				if (tableName.equals("-"))
					continue;
				
				LootTable table = new LootTable();
				int totalChance = 0;    // Must not be above 100. Also used to convert chances in the form of "33, 33, 34" to "33, 66, 100"
				
				String [] itemGroupsArray = itemGroups.getArray();
				int [] chanceGroupsArray = chanceGroups.getArray();
				assert itemGroupsArray.length == chanceGroupsArray.length;
				for (int groupNum = 0; groupNum < chanceGroupsArray.length && totalChance < 100; groupNum++) {
					int groupChance = chanceGroupsArray[groupNum];
					if (groupChance == 0 || groupChance == Integer.MAX_VALUE)
						continue;
					groupChance += totalChance;
					
					table.addLootGroup(new LootGroup(groupChance, itemGroupsArray[groupNum].split(";")));
					totalChance = groupChance;
				}
				
				if (totalChance != 100)
					Log.w("Loot chance != 100 while loading loot_table! Table: %s, totalChance: %s", tableName, totalChance);
				
				lootTables.put(tableName, table);
			}
		} catch (IOException e) {
			Log.e(e);
		}
		
		StandardLog.onEndLoad(lootTables.size(), "loot tables", startTime);
	}
	
	/**
	 * Loads NPC loot tables. There are up to 3 tables per NPC.
	 */
	private void loadNPCLoot() {
		long startTime = StandardLog.onStartLoad("NPC loot links");
		
		NpcLoader npcLoader = DataLoader.npcs();
		for (NpcInfo info : npcLoader.getNpcs()) {
			NPCLoot loot = new NPCLoot(info.getHumanoidInfo() != null);
			
			// load each loot table (up to 3) and add to loot object
			loadNPCTable(loot, info.getLootTable1(), info.getLootTable1Chance());
			loadNPCTable(loot, info.getLootTable2(), info.getLootTable2Chance());
			loadNPCTable(loot, info.getLootTable3(), info.getLootTable3Chance());
			npcLoot.put(info.getId(), loot);
		}
		
		StandardLog.onEndLoad(npcLoot.size(), "NPC loot links", startTime);
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
		if (lootTable == null) {
			Log.w("Loot table not found: %s", table);
			return;
		}
		
		loot.addNPCTable(new NPCTable(chance, lootTable));
	}
	
	private void showLootDisc(CreatureObject requester, AIObject corpse) {
		assert requester.isPlayer();
		
		Location effectLocation = Location.builder(corpse.getLocation()).setPosition(0, 0.5, 0).build();
		
		long requesterGroup = requester.getGroupId();
		
		if (requesterGroup != 0) {
			GroupObject requesterGroupObject = (GroupObject) ObjectLookup.getObjectById(requesterGroup);
			assert requesterGroupObject != null;
			
			for (CreatureObject creature : requesterGroupObject.getGroupMemberObjects()) {
				creature.sendSelf(new PlayClientEffectObjectTransformMessage(corpse.getObjectId(), "appearance/pt_loot_disc.prt", effectLocation, "lootMe"));
			}
		} else {
			requester.sendSelf(new PlayClientEffectObjectTransformMessage(corpse.getObjectId(), "appearance/pt_loot_disc.prt", effectLocation, "lootMe"));
		}
		LootGeneratedIntent.broadcast(corpse);
	}
	
	private boolean generateCreditChip(NPCLoot loot, SWGObject lootInventory, CreatureDifficulty difficulty, int combatLevel) {
		if (!loot.isDropCredits())
			return false;
		
		ThreadLocalRandom random = ThreadLocalRandom.current();
		Config lootConfig = DataManager.getConfig(ConfigFile.LOOTOPTIONS);
		
		double range = lootConfig.getDouble("LOOT-CASH-HUMAN-RANGE", 0.05);
		double cashLootRoll = random.nextDouble();
		int credits;
		
		switch (difficulty) {
			default:
			case NORMAL:
				if (cashLootRoll > lootConfig.getDouble("LOOT-CASH-HUMAN-NORMAL-CHANCE", 0.60))
					return false;
				credits = lootConfig.getInt("LOOT-CASH-HUMAN-NORMAL", 2);
				break;
			case ELITE:
				if (cashLootRoll > lootConfig.getDouble("LOOT-CASH-HUMAN-ELITE-CHANCE", 0.80))
					return false;
				credits = lootConfig.getInt("LOOT-CASH-HUMAN-ELITE", 5);
				break;
			case BOSS:
				// bosses always drop cash loot, so no need to check
				credits = lootConfig.getInt("LOOT-CASH-HUMAN-BOSS", 9);
				break;
		}
		credits *= combatLevel;
		credits *= 1 + random.nextDouble(0, 2*range-range);
		
		// TODO scale with group size?
		
		CreditObject cashObject = ObjectCreator.createObjectFromTemplate("object/tangible/item/shared_loot_cash.iff", CreditObject.class);
		
		cashObject.setAmount(credits);
		cashObject.moveToContainer(lootInventory);
		
		ObjectCreatedIntent.broadcast(cashObject);
		return true;
	}
	
	/**
	 * Generates loot and places it in the inventory of the corpse.
	 *
	 * @param loot          the loot info of the creature killed
	 * @param killer        the person that killed the NPC
	 * @param lootInventory the inventory the loot will be placed in (corpse inventory)
	 * @return whether loot was generated or not
	 */
	private boolean generateLoot(NPCLoot loot, CreatureObject killer, SWGObject lootInventory) {
		boolean lootGenerated = false;
		
		int tableRoll = random.nextInt(100) + 1;
		
		// Admin Variables
		boolean admin = killer.hasCommand("admin");
		StringBuilder adminOutput1 = new StringBuilder(tableRoll + " //");
		StringBuilder adminOutput2 = new StringBuilder();
		int tableId = 0;
		
		for (NPCTable npcTable : loot.getNPCTables()) {
			LootTable lootTable = npcTable.getLootTable();
			int tableChance = npcTable.getChance();
			tableId++;
			
			if (tableRoll > tableChance) {
				// Skip ahead if there's no drop chance
				adminOutput1.append("/ \\#FF0000 loot_table").append(tableId);
				continue;
			}
			adminOutput1.append("/ \\#00FF00 loot_table").append(tableId);
			
			int groupRoll = random.nextInt(100) + 1;
			
			for (LootGroup group : lootTable.getLootGroups()) {
				int groupChance = group.getChance();
				
				if (groupRoll > groupChance)
					continue;
				
				String[] itemNames = group.getItemNames();
				String itemName = itemNames[random.nextInt(itemNames.length)];
				
				adminOutput2.append(itemName).append('\n');
				if (itemName.startsWith("dynamic_")) {
					// TODO dynamic item handling
					new SystemMessageIntent(killer.getOwner(), "We don't support this loot item yet: " + itemName).broadcast();
				} else if (itemName.endsWith(".iff")) {
					SWGObject object = ObjectCreator.createObjectFromTemplate(itemName);
					object.moveToContainer(lootInventory);
					ObjectCreatedIntent.broadcast(object);
					
					lootGenerated = true;
				} else {
					new CreateStaticItemIntent(killer, lootInventory, new CreateStaticItemCallback(), itemName).broadcast();
					
					lootGenerated = true;
				}
				
				break;
			}
		}
		if (admin) {
			IntentChain.broadcastChain(
					new SystemMessageIntent(killer.getOwner(), adminOutput1.toString()),
					new SystemMessageIntent(killer.getOwner(), adminOutput2.toString())
			);
		}
		
		return lootGenerated;
	}
	
	private static class NPCLoot {
		
		private final Collection<NPCTable> npcTables;
		private final boolean canDropCredits;
		
		public NPCLoot(boolean canDropCredits) {
			this.npcTables = new ArrayList<>();
			this.canDropCredits = canDropCredits;
		}
		
		public boolean isDropCredits() {
			return canDropCredits;
		}
		
		public Collection<NPCTable> getNPCTables() {
			return Collections.unmodifiableCollection(npcTables);
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
	
	private static class CreateStaticItemCallback implements StaticItemService.ObjectCreationHandler {
		
		@Override
		public void success(SWGObject[] createdObjects) {
			// do nothing - loot disc is created on the return of the generateLoot method
		}
		
		@Override
		public boolean isIgnoreVolume() {
			return true;
		}
	}
	
}
