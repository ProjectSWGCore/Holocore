/** **********************************************************************************
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
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>. * *
 **********************************************************************************
 */
package services.combat;

import com.projectswg.common.control.Service;
import com.projectswg.common.data.info.RelationalDatabase;
import com.projectswg.common.data.info.RelationalServerFactory;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.debug.Log;
import intents.chat.ChatBroadcastIntent;
import intents.chat.ChatCommandIntent;
import intents.combat.CreatureKilledIntent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import intents.object.ContainerTransferIntent;
import intents.object.CreateStaticItemIntent;
import intents.object.ObjectCreatedIntent;
import intents.radial.RadialRequestIntent;
import intents.radial.RadialResponseIntent;
import intents.radial.RadialSelectionIntent;
import network.packets.swg.zone.ClientOpenContainerMessage;
import network.packets.swg.zone.PlayClientEffectObjectTransformMessage;
import resources.containers.ContainerPermissionsType;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureDifficulty;
import resources.objects.creature.CreatureObject;
import resources.objects.custom.AIObject;
import resources.objects.group.GroupObject;
import resources.objects.tangible.TangibleObject;
import resources.player.Player;
import resources.radial.RadialItem;
import resources.radial.RadialOption;
import resources.server_info.StandardLog;
import services.objects.ObjectCreator;
import services.objects.ObjectManager.ObjectLookup;
import services.objects.StaticItemService;

public final class LootService extends Service {

	private static final String LOOT_TABLE_SELECTOR = "SELECT * FROM loot_table";
	private static final String NPC_LOOT_SELECTOR = "SELECT npc_id, min_cash, max_cash, loot_table1_chance, loot_table1, loot_table2_chance, loot_table2, loot_table3_chance, loot_table3 FROM npc";
	
	private final Map<String, LootTable> lootTables;	// K: loot_id, V: table contents
	private final Map<String, NPCLoot> npcLoot;	// K: npc_id, V: possible loot
	private final Random random;
	
	public LootService() {
		lootTables = new HashMap<>();
		npcLoot = new HashMap<>();
		random = new Random();

		registerForIntent(ChatCommandIntent.class, cci -> handleChatCommand(cci));
		registerForIntent(ContainerTransferIntent.class, cti -> handleContainerTransfer(cti));
		registerForIntent(CreatureKilledIntent.class, cki -> handleCreatureKilled(cki));
		registerForIntent(RadialSelectionIntent.class, rsi -> handleRadialSelection(rsi));
		registerForIntent(RadialRequestIntent.class, rri -> handleRadialRequestIntent(rri));
	}

	@Override
	public boolean initialize() {
		loadLootTables();
		loadNPCLoot();

		return super.initialize();
	}

	private void loadLootTables() {
		String what = "loot tables";
		long startTime = StandardLog.onStartLoad(what);

		try (RelationalDatabase spawnerDatabase = RelationalServerFactory.getServerData("loot/loot_table.db", "loot_table")) {
			try (ResultSet set = spawnerDatabase.executeQuery(LOOT_TABLE_SELECTOR)) {
				while (set.next()) {
					loadLootTable(set);
				}
			} catch (SQLException e) {
				Log.e(e);
			}
		}

		StandardLog.onEndLoad(lootTables.size(), what, startTime);
	}
	
	private void loadLootTable(ResultSet set) throws SQLException {
		String tableName = set.getString("loot_id");

		if (tableName.equals("-")) {
			return;
		}

		LootTable table = new LootTable();
		byte totalChance = 0;	// Must not be above 100
		
		for (int groupNum = 1; groupNum <= 16 && totalChance <= 100; groupNum++) {
			LootGroup lootGroup = loadLootGroup(set, groupNum);
			
			table.addLootGroup(lootGroup);
			totalChance += lootGroup.getChance();
		}

		lootTables.put(tableName, table);
	}
	
	private LootGroup loadLootGroup(ResultSet set, int groupNum) throws SQLException {
		String groupItems = set.getString("items_group_" + groupNum);
		int groupChance = set.getInt("chance_group_" + groupNum);
		String[] itemNames = groupItems.split(";");
			
		return new LootGroup(groupChance, itemNames);
	}

	private void loadNPCLoot() {
		String what = "NPC loot links";
		long startTime = StandardLog.onStartLoad(what);

		try (RelationalDatabase spawnerDatabase = RelationalServerFactory.getServerData("npc/npc.db", "npc")) {
			try (ResultSet set = spawnerDatabase.executeQuery(NPC_LOOT_SELECTOR)) {
				while (set.next()) {
					loadNPCLoot(set);
				}
			} catch (SQLException e) {
				Log.e(e);
			}
		}

		StandardLog.onEndLoad(npcLoot.size(), what, startTime);
	}
	
	private void loadNPCLoot(ResultSet set) throws SQLException {
		int minCash = set.getInt("min_cash");
		int maxCash = set.getInt("max_cash");
		String creatureId = set.getString("npc_id");
		NPCLoot loot = new NPCLoot(minCash, maxCash);
		
		for (byte tableNum = 1; tableNum <= 3; tableNum++) {
			NPCTable npcTable = loadNPCTable(set, tableNum);
			
			if (npcTable == null || npcTable.getChance() <= 0) {
				continue;
			}

			loot.addNPCTable(npcTable);
		}
		
		npcLoot.put(creatureId, loot);
	}
	
	private NPCTable loadNPCTable(ResultSet set, int tableNum) throws SQLException {
		String columnName = "loot_table" + tableNum;
		String columnChance = columnName + "_chance";
		String tableName = set.getString(columnName);
		int tableChance = set.getInt(columnChance);
		LootTable lootTable = lootTables.get(tableName);

		if (lootTable == null) {
			return null;
		}

		return new NPCTable(tableChance, lootTable);
	}

	private void handleContainerTransfer(ContainerTransferIntent cti){
		SWGObject object = cti.getObject();
		
		if (!(cti.getContainer().getOwner() instanceof Player))
			return;
		
		if (object.getContainerPermissions() == ContainerPermissionsType.LOOT){
			object.setContainerPermissions(ContainerPermissionsType.DEFAULT);
		}
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
		corpse.addObject(lootInventory);	// It's a slotted object and goes in the inventory slot
		new ObjectCreatedIntent(lootInventory).broadcast();

		CreatureObject killer = cki.getKiller();

	//	generateCreditChip(loot, killer, lootInventory, corpse.getDifficulty());
		generateLoot(loot, killer, lootInventory);
	}

	private void handleChatCommand(ChatCommandIntent cci) {

		if(!cci.getCommand().getName().equalsIgnoreCase("loot")) {
			return;
		}
		
		if (!getLootPermission(cci.getSource(), cci.getTarget()))
			return;				

		lootAll(cci.getSource(), cci.getTarget());
	}

	private void handleRadialSelection(RadialSelectionIntent rsi) {
		switch (rsi.getSelection()) {
			case LOOT: {
				if (!getLootPermission(rsi.getPlayer().getCreatureObject(), rsi.getTarget()))
					return;
				
				lootBox(rsi.getPlayer(), rsi.getTarget());
				break;
			}
			case LOOT_ALL: {
				if (!getLootPermission(rsi.getPlayer().getCreatureObject(), rsi.getTarget()))
					return;				
				
				lootAll(rsi.getPlayer().getCreatureObject(), rsi.getTarget());
				break;
			}
		}
	}
	
	private void handleRadialRequestIntent(RadialRequestIntent rri){
		SWGObject target = rri.getTarget();

		if (!(target instanceof AIObject)) {
			// We can only loot NPCs
			return;
		}

		CreatureObject creature = (CreatureObject) target;

		if (creature.getHealth() > 0) {
			// Live creatures shouldn't get a loot radial
			return;
		}

		// TODO permissions check

		List<RadialOption> options = new ArrayList<RadialOption>(rri.getRequest().getOptions());
		RadialOption loot = new RadialOption(RadialItem.LOOT);
		loot.addChild(RadialItem.LOOT_ALL);
		options.add(loot);
		new RadialResponseIntent(rri.getPlayer(), target, options, rri.getRequest().getCounter()).broadcast();
	}
	
	private void lootBox(Player player, SWGObject target){
		SWGObject inventory = target.getSlottedObject("inventory");
		
		player.sendPacket(new ClientOpenContainerMessage(inventory.getObjectId(), ""));
	}

	private void lootAll(SWGObject looter, SWGObject corpse) {
		if (!(corpse instanceof AIObject)) {
			return;
		}

		SWGObject lootInventory = corpse.getSlottedObject("inventory");
		SWGObject looterInventory = looter.getSlottedObject("inventory");

		Collection<SWGObject> loot = lootInventory.getContainedObjects();	// No concurrent modification because a copy Collection is returned
		
		loot.forEach(item -> item.moveToContainer(looter, looter.getSlottedObject("inventory")));
	}
	
	private void randomGroupLoot(GroupObject lootGroup, SWGObject corpse) {
		if (!(corpse instanceof AIObject)) {
			return;
		}

		SWGObject lootInventory = corpse.getSlottedObject("inventory");
		CreatureObject randomPlayer;

		Collection<SWGObject> loot = lootInventory.getContainedObjects();	// No concurrent modification because a copy Collection is returned
		
		for (SWGObject item : loot){
			randomPlayer = lootGroup.getRandomPlayer();
			if(randomPlayer != null)
				item.moveToContainer(randomPlayer, randomPlayer.getSlottedObject("inventory"));
		}
	}

	private void showLootDisc(CreatureObject requester, SWGObject corpse) {
		SWGObject inventory = corpse.getSlottedObject("inventory");

		// At this point, something will have dropped for sure.
		if (requester.isPlayer() && !inventory.getContainedObjects().isEmpty()) {	// TODO needs adjustment for group loot
			// If there's something we can loot, draw the loot disc icon on the corpse!
			Location effectLocation = new Location(corpse.getLocation());
			effectLocation.setPosition(0,0.5, 0);	// TODO should 0.5 be hardcoded or grabbed from somewhere?

			// TODO display it to everyone with access to lootInventory. For now, display the disc to the killer
			requester.getOwner().sendPacket(new PlayClientEffectObjectTransformMessage(corpse.getObjectId(), "appearance/pt_loot_disc.prt", effectLocation, "lootMe"));
		}
	}
	
	private void generateCreditChip(NPCLoot loot, CreatureObject killer, SWGObject inventory, CreatureDifficulty difficulty) {
		int maxCash = loot.getMaxCash();

		if (maxCash == 0) {
			// No cash is ever dropped on this creature
			return;
		}

		int minCash = loot.getMinCash();
		int cashAmount = random.nextInt((maxCash - minCash) + 1) + minCash;

		switch (difficulty) {
			default:
			case NORMAL: cashAmount *= 1; break;
			case ELITE: cashAmount *= 2; break;
			case BOSS: cashAmount *= 3; break;
		}

		// TODO scale with group size?

		TangibleObject cashObject = ObjectCreator.createObjectFromTemplate("object/tangible/item/shared_loot_cash.iff", TangibleObject.class);

		cashObject.setObjectName(cashAmount + " cr");
		cashObject.moveToContainer(inventory);

		new ObjectCreatedIntent(cashObject).broadcast();

		showLootDisc(killer, inventory.getParent());
	}
	
	private void generateLoot(NPCLoot loot, CreatureObject requester, SWGObject lootInventory) {
		int tableRoll = random.nextInt(100) + 1;

		for (NPCTable npcTable : loot.getNPCTables()) {
			LootTable lootTable = npcTable.getLootTable();
			int tableChance = npcTable.getChance();

			if (tableChance == 0 || tableChance < tableRoll ) {
				// Skip ahead if there's no drop chance
				continue;
			}

			int itemGroupRoll = random.nextInt(100) + 1;
			int minInterval = 1;

			for (LootGroup itemGroup : lootTable.getLootGroups()) {
				int groupChance = itemGroup.getChance();

				if (minInterval < itemGroupRoll && itemGroupRoll > groupChance) {
					minInterval += groupChance;
					// Check next item group
					continue;
				}

				String[] itemNames = itemGroup.getItemNames();
				String randomItemName = itemNames[random.nextInt(itemNames.length)];	// Selects a completely random item from the group

				if (randomItemName.startsWith("dynamic_")) {
					// TODO dynamic item handling
					new ChatBroadcastIntent(requester.getOwner(), "We don't support this loot item yet: " + randomItemName).broadcast();
				} else if (randomItemName.endsWith(".iff")) {
					String sharedTemplate = ClientFactory.formatToSharedFile(randomItemName);
					SWGObject object = ObjectCreator.createObjectFromTemplate(sharedTemplate);
					object.setContainerPermissions(ContainerPermissionsType.LOOT);
					object.moveToContainer(lootInventory);
					new ObjectCreatedIntent(object).broadcast();
				} else {
					new CreateStaticItemIntent(requester, lootInventory, new StaticItemService.ObjectCreationHandler() {
						@Override
						public void success(SWGObject[] createdObjects) {
							showLootDisc(requester, lootInventory.getParent());
						}

						@Override
						public boolean isIgnoreVolume() {
							return true;
						}
					},ContainerPermissionsType.LOOT, randomItemName).broadcast();
				}
				break;	// Only one group is ever spawned
			}
		}
	}
	
	private boolean getLootPermission(CreatureObject looter, SWGObject target){
		
		if (!isLootable(looter, target))
			return false;
		
		CreatureObject highestDamageDealer = ((CreatureObject) target).getHighestDamageDealer();
		
		if (highestDamageDealer != null && highestDamageDealer.getOwner() != null){
			Long looterGroup = looter.getGroupId();
			Long killerGroup = highestDamageDealer.getGroupId();
			
			if (looterGroup.equals(killerGroup) && killerGroup != 0){
				GroupObject killerGroupObject = (GroupObject) ObjectLookup.getObjectById(killerGroup);

					int lootRuleID = killerGroupObject.getLootRule().getId();

					switch (lootRuleID){
						case 0:
							return true;
						case 1:
							return highestDamageDealer.getOwnerId() == killerGroupObject.getLootMaster();
						case 2: //TODO Lottery
							return false;
						case 3:
							randomGroupLoot(killerGroupObject, target);
							return false;
						default:
							return false;
					}
			}else if (highestDamageDealer.getOwner().equals(looter.getOwner())){
				return true;
			}
		}
		return false;
	}

	private boolean isLootable(CreatureObject looter, SWGObject target){
		SWGObject inventory = target.getSlottedObject("inventory");

		if (inventory.getContainedObjects().isEmpty())
			return false;

		if (inventory.getContainerPermissions() != ContainerPermissionsType.LOOT)
			return false;

		return true;
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
