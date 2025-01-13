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
package com.projectswg.holocore.services.gameplay.combat.cloning

import com.projectswg.common.data.encodables.oob.ProsePackage
import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.common.data.encodables.tangible.PvpFaction
import com.projectswg.common.data.encodables.tangible.PvpStatus
import com.projectswg.common.data.location.Location
import com.projectswg.common.data.sui.SuiEvent
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage
import com.projectswg.common.network.packets.swg.zone.PlayMusicMessage
import com.projectswg.holocore.intents.gameplay.combat.CloneActivatedIntent
import com.projectswg.holocore.intents.gameplay.combat.CreatureKilledIntent
import com.projectswg.holocore.intents.gameplay.gcw.UpdateFactionStatusIntent
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent
import com.projectswg.holocore.intents.support.objects.DestroyObjectIntent
import com.projectswg.holocore.intents.support.objects.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.data.server_info.loader.FacilityData
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
import com.projectswg.holocore.resources.support.data.server_info.loader.TubeData
import com.projectswg.holocore.resources.support.global.player.PlayerEvent
import com.projectswg.holocore.resources.support.global.zone.sui.SuiButtons
import com.projectswg.holocore.resources.support.global.zone.sui.SuiListBox
import com.projectswg.holocore.resources.support.global.zone.sui.SuiWindow
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.services.support.objects.ObjectStorageService.BuildingLookup
import com.projectswg.holocore.utilities.HolocoreCoroutine
import com.projectswg.holocore.utilities.cancelAndWait
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import me.joshlarson.jlcommon.log.Log
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

class CloningService : Service() {
	private val coroutineScope = HolocoreCoroutine.childScope()
	private val reviveTimers: MutableMap<CreatureObject, Job> = HashMap()
	private val cloningFacilities: MutableList<BuildingObject> = ArrayList()

	override fun stop(): Boolean {
		coroutineScope.cancelAndWait()
		return super.stop()
	}

	@IntentHandler
	private fun handleCreatureKilledIntent(i: CreatureKilledIntent) {
		val corpse = i.corpse
		if (!corpse.isPlayer) return

		val corpseOwner = corpse.owner
		if (corpseOwner != null) {
			SystemMessageIntent(corpseOwner, ProsePackage(StringId("base_player", "prose_victim_dead"), "TT", i.killer.objectName)).broadcast()
			SystemMessageIntent(corpseOwner, ProsePackage(StringId("base_player", "revive_exp_msg"), "TT", "$CLONE_TIMER minutes.")).broadcast()
		}

		scheduleCloneTimer(corpse)
	}

	@IntentHandler
	private fun handleObjectCreatedIntent(i: ObjectCreatedIntent) {
		val createdObject = i.obj as? BuildingObject ?: return

		val objectTemplate = createdObject.template
		val facility = ServerData.cloningFacilities.getFacility(objectTemplate)
		if (facility != null) {
			synchronized(cloningFacilities) {
				cloningFacilities.add(createdObject)
			}
		}
	}

	@IntentHandler
	private fun handleDestroyObjectIntent(i: DestroyObjectIntent) {
		synchronized(cloningFacilities) {
			val destroyedObject = i.obj as? BuildingObject ?: return
			cloningFacilities.remove(destroyedObject)
		}
	}

	@IntentHandler
	private fun handlePlayerEventIntent(i: PlayerEventIntent) {
		val creature = i.player.creatureObject
		when (i.event) {
			PlayerEvent.PE_DISAPPEAR  -> reviveTimers.remove(creature)?.cancel()
			PlayerEvent.PE_FIRST_ZONE -> if (creature.posture == Posture.DEAD) {
				// They're dead, but they have no active revive timer.
				// In this case, they didn't clone before the application was shut down and started back up.
				if (reviveTimers.containsKey(creature)) showSuiWindow(creature)
				else scheduleCloneTimer(creature)
			}

			else                      -> {}
		}
	}

	private fun scheduleCloneTimer(corpse: CreatureObject) {
		val availableFacilities = getAvailableFacilities(corpse)
		if (availableFacilities.isEmpty()) {
			val defaultCloner = defaultCloner
			if (defaultCloner != null) availableFacilities.add(defaultCloner)
		}

		val cloningWindow = createSuiWindow(availableFacilities, corpse)

		cloningWindow.display(corpse.owner!!)
		synchronized(reviveTimers) {
			reviveTimers[corpse] = coroutineScope.launch {
				delay(TimeUnit.MINUTES.toMillis(CLONE_TIMER))
				expireCloneTimer(corpse, availableFacilities, cloningWindow)
			}
		}

		StandardLog.onPlayerEvent(this, corpse, "has %d minutes to clone", CLONE_TIMER)
	}

	private fun showSuiWindow(corpse: CreatureObject) {
		val availableFacilities = getAvailableFacilities(corpse)

		if (availableFacilities.isEmpty()) {
			val defaultCloner = defaultCloner
			if (defaultCloner != null) availableFacilities.add(defaultCloner)
		}

		createSuiWindow(availableFacilities, corpse).display(corpse.owner!!)
	}

	private fun createSuiWindow(availableFacilities: List<BuildingObject>, corpse: CreatureObject): SuiWindow {
		val closestFacility = availableFacilities[0]

		val preDesignated = "Pre-Designated: None"
		val cashBalance = "Cash Balance: " + corpse.cashBalance
		val help = "\nSelect the desired operation and click OK"
		val suiWindow = SuiListBox()
		suiWindow.run {
			title = "@base_player:revive_title"
			prompt = java.lang.String.join("\n", preDesignated, cashBalance, help)
			buttons = SuiButtons.OK
			addListItem("@base_player:revive_closest")
			addCallback("handleFacilityChoice") { event: SuiEvent, parameters: Map<String, String> ->
				val selectionIndex = SuiListBox.getSelectedRow(parameters)
				if (event != SuiEvent.OK_PRESSED || selectionIndex >= availableFacilities.size || selectionIndex < 0) {
					suiWindow.display(corpse.owner!!)
					return@addCallback
				}
				if (reviveCorpse(corpse, closestFacility) != CloneResult.SUCCESS) {
					suiWindow.display(corpse.owner!!)
				}
			}
		}

		return suiWindow
	}

	private fun reviveCorpse(corpse: CreatureObject, selectedFacility: BuildingObject): CloneResult {
		val facilityData = ServerData.cloningFacilities.getFacility(selectedFacility.template)

		if (facilityData == null) {
			StandardLog.onPlayerError(this, corpse, "could not clone at facility %s because the object template is not in cloning_respawn.sdb", selectedFacility)
			return CloneResult.TEMPLATE_MISSING
		}

		val cellName = facilityData.cell
		val cellObject = selectedFacility.getCellByName(cellName)

		if (cellObject == null) {
			StandardLog.onPlayerError(this, corpse, "could not clone at facility %s because the target cell is invalid", selectedFacility)
			return CloneResult.INVALID_CELL
		}


		// Cancel the forced cloning timer
		synchronized(reviveTimers) {
			reviveTimers.remove(corpse)?.cancel()
		}

		StandardLog.onPlayerEvent(this, corpse, "cloned to %s @ %s", selectedFacility, selectedFacility.location)
		val diedOnTerrain = corpse.terrain
		teleport(corpse, cellObject, getCloneLocation(facilityData, selectedFacility))
		CloneActivatedIntent(corpse, diedOnTerrain).broadcast()
		return CloneResult.SUCCESS
	}

	private fun getCloneLocation(facilityData: FacilityData, selectedFacility: BuildingObject): Location {
		val cloneLocation = Location.builder()
		val facilityLocation = selectedFacility.location
		val tubeData: List<TubeData> = ArrayList(facilityData.tubes)
		val tubeCount = tubeData.size

		if (tubeCount > 0) {
			val randomData = tubeData[ThreadLocalRandom.current().nextInt(tubeCount)]
			cloneLocation.setTerrain(facilityLocation.terrain)
			cloneLocation.setPosition(randomData.tubeX, 0.0, randomData.tubeZ)
			cloneLocation.setOrientation(facilityLocation.orientationX, facilityLocation.orientationY, facilityLocation.orientationZ, facilityLocation.orientationW)
			cloneLocation.rotateHeading(randomData.tubeHeading)
		} else {
			cloneLocation.setTerrain(facilityLocation.terrain)
			cloneLocation.setPosition(facilityData.x, facilityData.y, facilityData.z)
			cloneLocation.rotateHeading(facilityData.heading.toDouble())
		}

		return cloneLocation.build()
	}

	private fun teleport(corpse: CreatureObject, cellObject: CellObject, cloneLocation: Location) {
		corpse.moveToContainer(cellObject, cloneLocation)
		corpse.posture = Posture.UPRIGHT
		corpse.setTurnScale(1.0)
		corpse.setMovementPercent(1.0)
		corpse.health = corpse.maxHealth
		corpse.sendObservers(PlayClientEffectObjectMessage("clienteffect/player_clone_compile.cef", "", corpse.objectId, ""))
		corpse.sendSelf(PlayMusicMessage(0, "sound/item_repairobj.snd", 1, false))
		if (corpse.pvpFaction != PvpFaction.NEUTRAL) {
			corpse.broadcast(UpdateFactionStatusIntent(corpse, PvpStatus.ONLEAVE))
		}
	}

	/**
	 * Picks the closest cloning facility and clones `cloneRequestor`
	 * there. If an error occurs upon attempting to clone, it will pick the next
	 * facility until no errors occur. If allAlso closes the cloning SUI window.
	 * @param cloneRequestor
	 * @param facilitiesInTerrain list of `BuildingObject` that represent
	 * in-game cloning facilities
	 * @return `true` if forceful cloning was succesful and `false`
	 * if `cloneRequestor` cannot be cloned at any of the given facilities
	 * in `facilitiesInTerrain`
	 */
	private fun forceClone(cloneRequestor: CreatureObject, facilitiesInTerrain: List<BuildingObject>): Boolean {
		for (facility in facilitiesInTerrain) {
			if (reviveCorpse(cloneRequestor, facility) == CloneResult.SUCCESS) {
				return true
			}
		}

		return false
	}

	private fun expireCloneTimer(corpse: CreatureObject, facilitiesInTerrain: List<BuildingObject>, suiWindow: SuiWindow) {
		if (reviveTimers.remove(corpse) != null) {
			val corpseOwner = corpse.owner

			if (corpseOwner != null) {
				SystemMessageIntent(corpseOwner, "@base_player:revive_expired").broadcast()
				suiWindow.close(corpseOwner)
			}

			forceClone(corpse, facilitiesInTerrain)
		} else {
			StandardLog.onPlayerError(this, corpse, "could not be force cloned because no timer was active")
		}
	}

	/**
	 *
	 * @param corpse
	 * @return a sorted list of `BuildingObject`, ordered by distance
	 * to `corpse`. Order is reversed, so the closest facility is
	 * first.
	 */
	private fun getAvailableFacilities(corpse: CreatureObject): MutableList<BuildingObject> {
		synchronized(cloningFacilities) {
			val corpseLocation = corpse.worldLocation
			return cloningFacilities.stream().filter { facilityObject: BuildingObject -> isValidTerrain(facilityObject, corpse) && isFactionAllowed(facilityObject, corpse) }.sorted(Comparator.comparingDouble { facility: BuildingObject -> corpseLocation.distanceTo(facility.location) }).collect(Collectors.toList())
		}
	}

	// TODO below doesn't apply to a a player that died in a heroic. Cloning on Dathomir should be possible if you die during the Axkva Min heroic.
	private fun isValidTerrain(cloningFacility: BuildingObject, corpse: CreatureObject): Boolean {
		return cloningFacility.terrain == corpse.terrain
	}

	private fun isFactionAllowed(cloningFacility: BuildingObject, corpse: CreatureObject): Boolean {
		val facilityData = ServerData.cloningFacilities.getFacility(cloningFacility.template)
		val factionRestriction = facilityData!!.factionRestriction

		return factionRestriction == null || factionRestriction == corpse.pvpFaction
	}

	private enum class CloneResult {
		INVALID_SELECTION,
		TEMPLATE_MISSING,
		INVALID_CELL,
		SUCCESS
	}

	companion object {
		private const val CLONE_TIMER: Long = 30 // Amount of minutes before a player is forced to clone
		private val defaultCloner: BuildingObject?
			get() {
				val defaultCloner = BuildingLookup.getBuildingByTag("tat_moseisley_cloning1")
				if (defaultCloner == null) Log.e("No default cloner found with building id: 'tat_moseisley_cloning1'")

				return defaultCloner
			}
	}
}