/***********************************************************************************
 * Copyright (c) 2026 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.services.gameplay.structures

import com.projectswg.common.data.encodables.oob.ProsePackage
import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.holocore.intents.gameplay.world.RepairVehicleIntent
import com.projectswg.holocore.intents.gameplay.world.StoreMountIntent
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent.Companion.broadcastPersonal
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent
import com.projectswg.holocore.intents.support.global.zone.PlayerTransformedIntent
import com.projectswg.holocore.resources.gameplay.structures.VehicleGarages.nearestGarage
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader.Companion.vehicles
import com.projectswg.holocore.resources.support.data.server_info.loader.VehicleLoader.VehicleInfo
import com.projectswg.holocore.resources.support.global.player.PlayerEvent
import com.projectswg.holocore.resources.support.global.zone.sui.SuiButtons
import com.projectswg.holocore.resources.support.global.zone.sui.SuiListBox
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class VehicleGarageService : Service() {

	private val playersNotifiedOfGarage: MutableSet<CreatureObject> = ConcurrentHashMap.newKeySet()

	@IntentHandler
	private fun handlePlayerTransformedIntent(pti: PlayerTransformedIntent) {
		val player = pti.player
		if (!player.isStatesBitmask(CreatureState.RIDING_MOUNT)) return
		val owner = player.owner ?: return

		if (nearestGarage(player) != null) {
			if (playersNotifiedOfGarage.add(player)) broadcastPersonal(owner, "@pet/pet_menu:garage_proximity")
		} else {
			playersNotifiedOfGarage.remove(player)
		}
	}

	@IntentHandler
	private fun handleStoreMountIntent(smi: StoreMountIntent) {
		playersNotifiedOfGarage.remove(smi.creature)
	}

	@IntentHandler
	private fun handlePlayerEventIntent(pei: PlayerEventIntent) {
		if (pei.event == PlayerEvent.PE_DISAPPEAR) playersNotifiedOfGarage.remove(pei.player.creatureObject)
	}

	@IntentHandler
	private fun handleRepairVehicleIntent(rvi: RepairVehicleIntent) {
		val creature = rvi.creature
		val mount = rvi.pet
		val owner = creature.owner ?: return

		if (creature.objectId != mount.ownerId) return
		if (nearestGarage(creature) == null) {
			broadcastPersonal(owner, "@pet/pet_menu:repair_unrecognized_garages")
			return
		}

		val damage = mount.conditionDamage
		if (damage <= 0) {
			broadcastPersonal(owner, "@pet/pet_menu:undamaged_vehicle")
			return
		}

		val vehicleInfo = vehicles().getVehicleFromIff(mount.template)
		if (vehicleInfo == null || vehicleInfo.repairRate <= 0) {
			StandardLog.onPlayerError(this, creature, "unable to determine repair rate for vehicle %s", mount)
			broadcastPersonal(owner, "@pet/pet_menu:repair_error")
			return
		}

		if (damage >= mount.maxHitPoints && !vehicleInfo.isCanRepairDisabled) {
			broadcastPersonal(owner, "@pet/pet_menu:cannot_repair_disabled")
			return
		}

		val cost = repairCost(damage, vehicleInfo.repairRate)
		val funds = creature.bankBalance
		if (funds < cost) {
			broadcastPersonal(owner, lackingFunds(cost - funds))
			return
		}

		SuiListBox().run {
			title = "@pet/pet_menu:confirm_repairs_t"
			prompt = "@pet/pet_menu:vehicle_repair_d"
			buttons = SuiButtons.OK_CANCEL
			addListItem("@pet/pet_menu:vehicle_prompt ${mount.stringId}")
			addListItem("@pet/pet_menu:repair_cost_prompt $cost")
			addListItem("@pet/pet_menu:total_funds_prompt $funds")
			addOkButtonCallback("repair") { _, _ -> repair(creature, mount, cost, vehicleInfo) }
			display(owner)
		}
	}

	private fun repair(creature: CreatureObject, mount: CreatureObject, cost: Long, vehicleInfo: VehicleInfo) {
		val owner = creature.owner ?: return

		if (nearestGarage(creature) == null) {
			broadcastPersonal(owner, "@pet/pet_menu:garage_out_of_range")
			return
		}

		val funds = creature.bankBalance
		if (!creature.removeFromBank(cost)) {
			broadcastPersonal(owner, lackingFunds(cost - funds))
			return
		}

		val damage = mount.conditionDamage
		val paidForPoints = (cost / vehicleInfo.repairRate).roundToInt()
		val repairedPoints = paidForPoints.coerceAtMost(damage)
		var paid = cost

		// Condition may have improved between opening the window and confirming - only charge for what's actually repaired
		if (repairedPoints < paidForPoints) {
			paid = repairCost(repairedPoints, vehicleInfo.repairRate)
			creature.addToBank(cost - paid)
			SystemMessageIntent.broadcastPersonal(owner, ProsePackage(StringId("pet/pet_menu", "repair_condition_changed"), "DI", paid.toInt()))
		}

		SystemMessageIntent.broadcastPersonal(owner, ProsePackage(StringId("base_player", "prose_pay_success_no_target"), "DI", paid.toInt()))

		mount.conditionDamage = damage - repairedPoints
		StandardLog.onPlayerEvent(this, creature, "repaired mount %s for %d credits, condition is now %d/%d", mount, paid, mount.maxHitPoints - mount.conditionDamage, mount.maxHitPoints)
	}

	private fun repairCost(points: Int, repairRate: Double): Long = (points * repairRate).roundToLong()

	private fun lackingFunds(missing: Long): String = "@pet/pet_menu:lacking_funds_prefix $missing @pet/pet_menu:lacking_funds_suffix"
}
