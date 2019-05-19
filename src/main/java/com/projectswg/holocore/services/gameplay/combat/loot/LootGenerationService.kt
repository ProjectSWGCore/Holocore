package com.projectswg.holocore.services.gameplay.combat.loot

import com.projectswg.common.data.location.Location
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectTransformMessage
import com.projectswg.holocore.intents.gameplay.combat.CreatureKilledIntent
import com.projectswg.holocore.intents.gameplay.combat.loot.CorpseLootedIntent
import com.projectswg.holocore.intents.gameplay.combat.loot.LootGeneratedIntent
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.intents.support.objects.items.CreateStaticItemIntent
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureDifficulty
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import com.projectswg.holocore.resources.support.objects.swg.group.GroupObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.CreditObject
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup
import com.projectswg.holocore.services.support.objects.items.StaticItemService
import me.joshlarson.jlcommon.control.IntentChain
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import me.joshlarson.jlcommon.log.Log
import java.util.*
import java.util.concurrent.ThreadLocalRandom

class LootGenerationService : Service() {
	
	private val npcLoot: MutableMap<String, NPCLoot> = HashMap()    // K: npc_id, V: possible loot
	
	override fun initialize(): Boolean {
		loadNPCLoot()
		
		return true
	}
	
	@IntentHandler
	private fun handleCreatureKilled(cki: CreatureKilledIntent) {
		val corpse = cki.corpse as? AIObject ?: return
		
		val creatureId = corpse.creatureId
		val loot = npcLoot[creatureId]
		
		if (loot == null) {
			Log.w("No NPCLoot associated with NPC ID: $creatureId")
			return
		}
		
		val lootInventory = ObjectCreator.createObjectFromTemplate("object/tangible/inventory/shared_creature_inventory.iff")
		lootInventory.moveToContainer(corpse, corpse.location)
		ObjectCreatedIntent.broadcast(lootInventory)
		
		val killer = cki.killer
		
		var cashGenerated = false
		var lootGenerated = false
		
		if (PswgDatabase.config.getBoolean(this, "cashLoot", true))
			cashGenerated = generateCreditChip(loot, lootInventory, corpse.difficulty, corpse.level.toInt())
		if (PswgDatabase.config.getBoolean(this, "itemLoot", true))
			lootGenerated = generateLoot(loot, killer, lootInventory, corpse.difficulty, corpse.level.toInt())
		
		if (!cashGenerated && !lootGenerated)
			CorpseLootedIntent(corpse).broadcast()
		else
			showLootDisc(killer, corpse)
	}
	
	/**
	 * Loads NPC loot tables. There are up to 3 tables per NPC.
	 */
	private fun loadNPCLoot() {
		val startTime = StandardLog.onStartLoad("NPC loot links")
		
		for (info in ServerData.npcs.npcs) {
			val loot = NPCLoot(info.humanoidInfo != null)
			
			// load each loot table (up to 3) and add to loot object
			loadNPCTable(loot, info.lootTable1, info.lootTable1Chance)
			loadNPCTable(loot, info.lootTable2, info.lootTable2Chance)
			loadNPCTable(loot, info.lootTable3, info.lootTable3Chance)
			npcLoot[info.id] = loot
		}
		
		StandardLog.onEndLoad(npcLoot.size, "NPC loot links", startTime)
	}
	
	/**
	 * Load a specific table for an NPC.
	 *
	 * @param loot   the loot object for the NPC
	 * @param table  the loot table
	 * @param chance the chance for this loot table (used when generating loot)
	 */
	private fun loadNPCTable(loot: NPCLoot, table: String, chance: Int) {
		// if chance <= 0, this table for this NPC doesn't exist
		if (chance <= 0)
			return
		
		loot.npcTables.add(NPCTable(chance, table))
	}
	
	private fun showLootDisc(requester: CreatureObject, corpse: AIObject) {
		assert(requester.isPlayer)
		
		val effectLocation = Location.builder(corpse.location).setPosition(0.0, 0.5, 0.0).build()
		
		val requesterGroup = requester.groupId
		
		if (requesterGroup != 0L) {
			val requesterGroupObject = (ObjectLookup.getObjectById(requesterGroup) as GroupObject?)!!
			
			for (creature in requesterGroupObject.groupMemberObjects) {
				creature.sendSelf(PlayClientEffectObjectTransformMessage(corpse.objectId, "appearance/pt_loot_disc.prt", effectLocation, "lootMe"))
			}
		} else {
			requester.sendSelf(PlayClientEffectObjectTransformMessage(corpse.objectId, "appearance/pt_loot_disc.prt", effectLocation, "lootMe"))
		}
		LootGeneratedIntent.broadcast(corpse)
	}
	
	private fun generateCreditChip(loot: NPCLoot, lootInventory: SWGObject, difficulty: CreatureDifficulty, combatLevel: Int): Boolean {
		if (!loot.isDropCredits)
			return false
		
		val random = ThreadLocalRandom.current()
		val config = PswgDatabase.config
		
		val range = config.getDouble(this, "lootCashHumanRange", 0.05)
		val cashLootRoll = random.nextDouble()
		
		val credits = combatLevel * (1 + random.nextDouble(0.0, range)).toInt() * when (difficulty) {
			CreatureDifficulty.NORMAL -> {
				if (cashLootRoll > config.getDouble(this, "lootCashHumanNormalChance", 0.60))
					return false
				config.getInt(this, "lootCashHumanNormal", 2)
			}
			CreatureDifficulty.ELITE -> {
				if (cashLootRoll > config.getDouble(this, "lootCashHumanElitechance", 0.80))
					return false
				config.getInt(this, "lootCashHumanElite", 5)
			}
			CreatureDifficulty.BOSS -> // bosses always drop cash loot, so no need to check
				config.getInt(this, "lootCashHumanBoss", 9)
		}
		
		// TODO scale with group size?
		
		val cashObject = ObjectCreator.createObjectFromTemplate("object/tangible/item/shared_loot_cash.iff", CreditObject::class.java)
		
		cashObject.amount = credits.toLong()
		cashObject.moveToContainer(lootInventory)
		
		ObjectCreatedIntent.broadcast(cashObject)
		return true
	}
	
	/**
	 * Generates loot and places it in the inventory of the corpse.
	 *
	 * @param loot          the loot info of the creature killed
	 * @param killer        the person that killed the NPC
	 * @param lootInventory the inventory the loot will be placed in (corpse inventory)
	 * @return whether loot was generated or not
	 */
	private fun generateLoot(loot: NPCLoot, killer: CreatureObject, lootInventory: SWGObject, difficulty: CreatureDifficulty, level: Int): Boolean {
		val random = ThreadLocalRandom.current()
		var lootGenerated = false
		
		val tableRoll = random.nextInt(100) + 1
		
		// Admin Variables
		val admin = killer.hasCommand("admin")
		val adminOutput1 = StringBuilder("$tableRoll //")
		val adminOutput2 = StringBuilder()
		
		val lootTables = ServerData.lootTables
		for ((tableId, npcTable) in loot.npcTables.withIndex()) {
			val tableChance = npcTable.chance
			
			if (tableRoll > tableChance) {
				// Skip ahead if there's no drop chance
				adminOutput1.append("/ \\#FF0000 loot_table").append(tableId+1)
				break
			}
			adminOutput1.append("/ \\#00FF00 loot_table").append(tableId+1)
			
			val groupRoll = random.nextInt(100) + 1
			val lootTableItem = lootTables.getLootTableItem(npcTable.lootTable, difficulty.name, level) ?: continue
			
			for ((chance, items) in lootTableItem.groups) { // group of items to be granted
				if (groupRoll > chance)
					continue
				
				val itemName = items[random.nextInt(items.size)]
				
				adminOutput2.append(itemName).append('\n')
				when {
					itemName.startsWith("dynamic_") -> { // TODO dynamic item handling
						SystemMessageIntent(killer.owner!!, "We don't support this loot item yet: $itemName").broadcast()
					}
					itemName.endsWith(".iff") -> {
						val obj = ObjectCreator.createObjectFromTemplate(itemName)
						obj.moveToContainer(lootInventory)
						ObjectCreatedIntent.broadcast(obj)
						
						lootGenerated = true
					}
					else -> {
						CreateStaticItemIntent(killer, lootInventory, CreateStaticItemCallback(), itemName).broadcast()
						
						lootGenerated = true
					}
				}
				
				break
			}
		}
		if (admin) {
			IntentChain.broadcastChain(
					SystemMessageIntent(killer.owner!!, adminOutput1.toString()),
					SystemMessageIntent(killer.owner!!, adminOutput2.toString())
			)
		}
		
		return lootGenerated
	}
	
	private class NPCLoot(val isDropCredits: Boolean, val npcTables: MutableCollection<NPCTable> = ArrayList())
	
	private class NPCTable(val chance: Int, val lootTable: String)
	
	private class CreateStaticItemCallback : StaticItemService.ObjectCreationHandler {
		
		override fun success(createdObjects: List<SWGObject>) {
			// do nothing - loot disc is created on the return of the generateLoot method
		}
		
		override fun isIgnoreVolume(): Boolean {
			return true
		}
	}
	
}
