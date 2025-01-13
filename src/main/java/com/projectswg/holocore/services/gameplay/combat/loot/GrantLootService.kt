/***********************************************************************************
 * Copyright (c) 2025 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is an emulation project for Star Wars Galaxies founded on            *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create one or more emulators which will provide servers for      *
 * players to continue playing a game similar to the one they used to play.        *
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
package com.projectswg.holocore.services.gameplay.combat.loot

import com.projectswg.common.data.encodables.oob.ProsePackage
import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.network.packets.swg.zone.PlayMusicMessage
import com.projectswg.common.network.packets.swg.zone.StopClientEffectObjectByLabelMessage
import com.projectswg.common.network.packets.swg.zone.object_controller.loot.GroupCloseLotteryWindow
import com.projectswg.common.network.packets.swg.zone.object_controller.loot.GroupOpenLotteryWindow
import com.projectswg.common.network.packets.swg.zone.object_controller.loot.GroupRequestLotteryItems
import com.projectswg.holocore.intents.gameplay.combat.*
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent.Companion.broadcastPersonal
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent
import com.projectswg.holocore.intents.support.objects.DestroyObjectIntent
import com.projectswg.holocore.intents.support.objects.OpenContainerIntent
import com.projectswg.holocore.resources.gameplay.combat.loot.LootType
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase.config
import com.projectswg.holocore.resources.support.objects.permissions.ContainerResult
import com.projectswg.holocore.resources.support.objects.permissions.ReadWritePermissions
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import com.projectswg.holocore.resources.support.objects.swg.group.GroupObject
import com.projectswg.holocore.resources.support.objects.swg.group.LootRule
import com.projectswg.holocore.resources.support.objects.swg.tangible.CreditObject
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup
import com.projectswg.holocore.utilities.HolocoreCoroutine
import com.projectswg.holocore.utilities.cancelAndWait
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import me.joshlarson.jlcommon.utilities.Arguments
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Collectors

class GrantLootService : Service() {
	private val lootRestrictions: MutableMap<CreatureObject, CorpseLootRestrictions> = ConcurrentHashMap()
	private val coroutineScope = HolocoreCoroutine.childScope()
	private val lootRange: Int = config.getInt(this, "lootRange", 16)

	override fun stop(): Boolean {
		coroutineScope.cancelAndWait()
		return super.stop()
	}

	@IntentHandler
	private fun handleLootGeneratedIntent(lgi: LootGeneratedIntent) {
		val corpse = lgi.corpse

		val corpseWorldLocation = corpse.worldLocation
		val mostHated = corpse.mostHated
		if (mostHated == null || !mostHated.isPlayer) return
		val groupId = mostHated.groupId

		val looters = ArrayList<CreatureObject>()
		if (groupId != 0L) {
			val group = checkNotNull(ObjectLookup.getObjectById(groupId) as GroupObject?)
			val masterLooter = group.lootMaster
			when (group.lootRule) {
				LootRule.MASTER_LOOTER                                   -> if (masterLooter != null) looters.add(masterLooter)
				LootRule.FREE_FOR_ALL, LootRule.RANDOM, LootRule.LOTTERY -> {
					// Get all looters within range
					looters.addAll(group.groupMemberObjects.stream().filter { it.worldLocation.flatDistanceTo(corpseWorldLocation) <= lootRange }.toList())
				}
			}
			if (group.lootRule == LootRule.LOTTERY) {
				lootRestrictions[corpse] = LotteryLootRestrictions(corpse, looters)
				corpse.inventory.containerPermissions = ReadWritePermissions.from(looters)
				return
			} else if (group.lootRule == LootRule.RANDOM) {
				lootRestrictions[corpse] = RandomLootRestrictions(corpse, looters)
				corpse.inventory.containerPermissions = ReadWritePermissions.from(looters)
				return
			}
		} else {
			looters.add(mostHated)
		}
		lootRestrictions[corpse] = StandardLootRestrictions(corpse, looters)
		val inventory = corpse.inventory
		inventory.containerPermissions = ReadWritePermissions.from(looters)
		for (loot in inventory.containedObjects) loot.containerPermissions = ReadWritePermissions.from(looters)
	}

	@IntentHandler
	private fun handleDestroyObjectIntent(doi: DestroyObjectIntent) {
		// For when the corpse is gone
		val obj = doi.obj
		if (obj is CreatureObject) {
			val restrictions = lootRestrictions.remove(obj)
			restrictions?.setValid(false)
		}
	}

	@IntentHandler
	private fun handleInboundPacketIntent(ipi: InboundPacketIntent) {
		val request = ipi.packet as? GroupRequestLotteryItems ?: return
		val player = ipi.player
		val requestObject = ObjectLookup.getObjectById(request.inventoryId)?.parent ?: return
		if (requestObject !is CreatureObject) return
		val restriction = lootRestrictions[requestObject] as? LotteryLootRestrictions ?: return
		restriction.updatePreferences(player.creatureObject, request.requestedItems.stream().map<SWGObject> { ObjectLookup.getObjectById(it) }.filter(Objects::nonNull).collect(Collectors.toList()))
	}

	@IntentHandler
	private fun handleLootRequestIntent(lri: LootRequestIntent) {
		val player = lri.player
		val looter = player.creatureObject
		val corpse = lri.target

		Arguments.validate(corpse is AIObject, "Attempted to loot a non-AI object")
		val restrictions = lootRestrictions[corpse] ?: // No loot was generated. Do nothing.
		return
		if (!restrictions.canLoot(looter)) {
			broadcastPersonal(player, "You don't have permission to loot '" + corpse.objectName + '\'')
			return
		}

		if (restrictions is LotteryLootRestrictions) {
			if (restrictions.isStarted()) return  // Don't keep re-starting the lottery

			coroutineScope.launch {
				// Close the windows after 30 seconds, and grant the items won
				delay(30_000L)
				if (restrictions.isValid()) {
					restrictions.commitLottery()
				}
			}
		}
		restrictions.handle(looter, lri.type)
	}

	@IntentHandler
	private fun handleLootItemIntent(lii: LootItemIntent) {
		val looter = lii.looter
		val corpse = lii.corpse
		val item = lii.item

		Arguments.validate(corpse is AIObject, "Attempted to loot a non-AI object")
		val restrictions = lootRestrictions[corpse] ?: // No loot was generated. Do nothing.
		return
		if (!restrictions.canLoot(looter)) {
			broadcastPersonal(looter.owner!!, "You don't have permission to loot '" + corpse.objectName + '\'')
			return
		}

		restrictions.loot(looter, item)
	}

	private abstract class CorpseLootRestrictions(val corpse: CreatureObject, val looters: List<CreatureObject>) {
		private val valid = AtomicBoolean(true)

		fun isValid(): Boolean {
			return valid.get()
		}

		fun canLoot(looter: CreatureObject): Boolean {
			return looters.contains(looter)
		}

		fun setValid(valid: Boolean) {
			this.valid.set(valid)
		}

		@Synchronized
		open fun loot(looter: CreatureObject, item: SWGObject) {
			throw UnsupportedOperationException("this loot restriction cannot do manual looting")
		}

		abstract fun handle(looter: CreatureObject, type: LootType)

		@Synchronized
		protected fun transferItem(looter: CreatureObject, item: SWGObject) {
			if (!corpse.inventory.containedObjects.contains(item) || !canLoot(looter)) {
				return
			}
			val player = checkNotNull(looter.owner)
			when (item.moveToContainer(looter, looter.inventory)) {
				ContainerResult.SUCCESS        -> {
					var itemName = item.objectName
					if (itemName.isEmpty()) itemName = item.stringId.toString()

					if (item is CreditObject) {
						onLootedCredits(looter, item.amount)
					} else {
						SystemMessageIntent(player, ProsePackage("StringId", StringId("spam", "loot_item_self"), "TU", itemName, "TT", corpse.objectName)).broadcast()
					}
					onLooted(looter, corpse)
				}

				ContainerResult.CONTAINER_FULL -> {
					SystemMessageIntent(player, "@container_error_message:container03").broadcast()
					player.sendPacket(PlayMusicMessage(0, "sound/ui_danger_message.snd", 1, false))
				}

				ContainerResult.NO_PERMISSION  -> {
					SystemMessageIntent(player, "@container_error_message:container08").broadcast()
					player.sendPacket(PlayMusicMessage(0, "sound/ui_negative.snd", 1, false))
				}

				ContainerResult.SLOT_NO_EXIST  -> {
					SystemMessageIntent(player, "@container_error_message:container06").broadcast()
					player.sendPacket(PlayMusicMessage(0, "sound/ui_negative.snd", 1, false))
				}

				ContainerResult.SLOT_OCCUPIED  -> {
					SystemMessageIntent(player, "@container_error_message:container04").broadcast()
					player.sendPacket(PlayMusicMessage(0, "sound/ui_negative.snd", 1, false))
				}
			}
		}

		protected fun splitCredits(target: CreditObject, looters: Collection<CreatureObject>) {
			val amount = target.amount / looters.size
			assert(amount > 0)

			for (looter in looters) {
				looter.addToBank(amount)
				onLootedCredits(looter, amount)
			}

			DestroyObjectIntent(target).broadcast()
		}

		protected fun onLooted(looter: CreatureObject, corpse: CreatureObject) {
			val lootInventory = corpse.inventory
			if (lootInventory.containedObjects.isEmpty()) {
				CorpseLootedIntent(corpse).broadcast()

				val looterGroupId = looter.groupId
				if (looterGroupId != 0L) {
					val killerGroup = checkNotNull(ObjectLookup.getObjectById(looterGroupId) as GroupObject?)
					for (groupMember in killerGroup.groupMemberObjects) groupMember.owner!!.sendPacket(StopClientEffectObjectByLabelMessage(corpse.objectId, "lootMe", false))
				} else {
					looter.owner!!.sendPacket(StopClientEffectObjectByLabelMessage(corpse.objectId, "lootMe", false))
				}
			}
		}

		protected fun onLootedCredits(looter: CreatureObject, amount: Long) {
			// Perhaps "prose_coin_loot_no_target" is the proper string?
			SystemMessageIntent(looter.owner!!, ProsePackage("StringId", StringId("base_player", "prose_transfer_success"), "DI", amount.toInt())).broadcast()
		}
	}

	private class StandardLootRestrictions(corpse: CreatureObject, looters: List<CreatureObject>) : CorpseLootRestrictions(corpse, looters) {
		@Synchronized
		override fun loot(looter: CreatureObject, item: SWGObject) {
			transferItem(looter, item)
		}

		@Synchronized
		override fun handle(looter: CreatureObject, type: LootType) {
			val lootInventory = corpse.inventory
			val lootItems = lootInventory.containedObjects
			when (type) {
				LootType.LOOT     -> if (!lootItems.isEmpty()) OpenContainerIntent(looter, lootInventory, "").broadcast()
				LootType.LOOT_ALL -> for (loot in lootItems) loot(looter, loot)
			}
		}
	}

	private class RandomLootRestrictions(corpse: CreatureObject, looters: List<CreatureObject>) : CorpseLootRestrictions(corpse, looters) {
		@Synchronized
		override fun loot(looter: CreatureObject, item: SWGObject) {
			if (item is CreditObject && item.amount >= looters.size) splitCredits(item, looters)
			else transferItem(looter, item)
		}

		@Synchronized
		override fun handle(looter: CreatureObject, type: LootType) {
			val lootItems = corpse.inventory.containedObjects
			for (loot in lootItems) loot(looter, loot)
		}
	}

	private class LotteryLootRestrictions(corpse: CreatureObject, looters: List<CreatureObject>) : CorpseLootRestrictions(corpse, looters) {
		private val preferences: MutableMap<CreatureObject, List<SWGObject>> = HashMap()
		private val started = AtomicBoolean(false)
		private val committed = AtomicBoolean(false)

		fun isStarted(): Boolean {
			return started.get()
		}

		@Synchronized
		override fun handle(looter: CreatureObject, type: LootType) {
			if (started.getAndSet(true)) return
			if (corpse.inventory.containedObjects.isEmpty()) return
			LootLotteryStartedIntent(corpse).broadcast()
			for (creature in looters) {
				val player = creature.owner
				player?.sendPacket(GroupOpenLotteryWindow(creature.objectId, corpse.inventory.objectId))
			}
		}

		@Synchronized
		fun updatePreferences(looter: CreatureObject, objects: List<SWGObject>) {
			preferences[looter] = objects
			if (preferences.size == looters.size) commitLottery()
		}

		@Synchronized
		fun commitLottery() {
			if (committed.getAndSet(true)) return
			// Build item map
			val itemPreferences: MutableMap<SWGObject, MutableList<CreatureObject>> = HashMap()
			for ((key, value) in preferences) {
				for (item in value) itemPreferences.computeIfAbsent(item) { ArrayList() }.add(key)
			}


			// Notify the lottery is done
			for (creature in looters) {
				val player = creature.owner
				player?.sendPacket(GroupCloseLotteryWindow(creature.objectId, corpse.inventory.objectId))
			}


			// Give out items
			val random = Random()
			for ((key, looters) in itemPreferences) {
				assert(looters.isNotEmpty())
				if (key is CreditObject && key.amount >= looters.size) {
					splitCredits(key, looters)
				} else {
					val winner = looters[random.nextInt(looters.size)]
					transferItem(winner, key)
				}
			}
		}
	}
}