package com.projectswg.holocore.services.gameplay.combat.loot

import com.projectswg.common.data.location.Location
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectTransformMessage
import com.projectswg.holocore.intents.gameplay.combat.CreatureKilledIntent
import com.projectswg.holocore.intents.gameplay.combat.loot.CorpseLootedIntent
import com.projectswg.holocore.intents.gameplay.combat.loot.LootGeneratedIntent
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import com.projectswg.holocore.resources.support.objects.swg.group.GroupObject
import com.projectswg.holocore.services.gameplay.combat.loot.generation.CreditLootGenerator
import com.projectswg.holocore.services.gameplay.combat.loot.generation.ItemLootGenerator
import com.projectswg.holocore.services.gameplay.combat.loot.generation.NPCLootTable
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import me.joshlarson.jlcommon.log.Log
import java.util.*

class LootGenerationService : Service() {
	
	private val npcLoot: MutableMap<String, NPCLoot> = HashMap()    // K: npc_id, V: possible loot
	private val creditGenerator = CreditLootGenerator()
	private val itemGenerator = ItemLootGenerator()
	private val lootGenerationThread = ScheduledThreadPool(1, "loot-generation-service")
	
	override fun initialize(): Boolean {
		loadNPCLoot()
		
		return true
	}
	
	override fun start(): Boolean {
		lootGenerationThread.start()
		return true
	}
	
	override fun stop(): Boolean {
		lootGenerationThread.stop()
		return lootGenerationThread.awaitTermination(1000)
	}
	
	@IntentHandler
	private fun handleCreatureKilled(cki: CreatureKilledIntent) {
		val corpse = cki.corpse as? AIObject ?: return
		
		val creatureId = corpse.creatureId
		val lootRecord = npcLoot[creatureId]
		
		if (lootRecord == null) {
			Log.w("No NPCLoot associated with NPC ID: $creatureId")
			return
		}
		
		val loot = ArrayList<SWGObject>()
		val killer = cki.killer
		if (!killer.isPlayer)
			return
		
		// Various kinds of loot
		if (lootRecord.isDropCredits && PswgDatabase.config.getBoolean(this, "cashLoot", true))
			creditGenerator.generate(corpse, loot)
		if (PswgDatabase.config.getBoolean(this, "itemLoot", true))
			itemGenerator.generate(corpse, killer, loot, lootRecord.npcTables)

		if (loot.isEmpty()) {
			CorpseLootedIntent(corpse).broadcast()
			return
		}
		
		val lootInventory = ObjectCreator.createObjectFromTemplate("object/tangible/inventory/shared_creature_inventory.iff")
		loot.forEach { obj ->
			obj.moveToContainer(lootInventory)
			ObjectCreatedIntent.broadcast(obj)
		}
		lootInventory.moveToContainer(corpse, corpse.location)
		ObjectCreatedIntent.broadcast(lootInventory)
		
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
		
		loot.npcTables.add(NPCLootTable(chance, table))
	}
	
	private fun showLootDisc(requester: CreatureObject, corpse: AIObject) {
		assert(requester.isPlayer)
		
		val effectLocation = Location.builder(corpse.location).setPosition(0.0, 0.5, 0.0).build()
		
		val requesterGroup = requester.groupId
		
		if (requesterGroup != 0L) {
			val requesterGroupObject = (ObjectLookup.getObjectById(requesterGroup) as GroupObject?)!!
			
			for (creature in requesterGroupObject.groupMemberObjects) {
				creature.sendSelf(PlayClientEffectObjectTransformMessage(corpse.objectId, "appearance/pt_find_path_end.prt", effectLocation, "lootMe"))
			}
		} else {
			requester.sendSelf(PlayClientEffectObjectTransformMessage(corpse.objectId, "appearance/pt_find_path_end.prt", effectLocation, "lootMe"))
		}
		LootGeneratedIntent.broadcast(corpse)
	}

	private class NPCLoot(val isDropCredits: Boolean, val npcTables: MutableList<NPCLootTable> = ArrayList())
	
}
