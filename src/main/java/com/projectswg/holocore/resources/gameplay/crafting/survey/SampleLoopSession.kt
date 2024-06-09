/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
 * *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 * *
 * This file is part of Holocore.                                                  *
 * *
 * --------------------------------------------------------------------------------*
 * *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 * *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 * *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http:></http:>//www.gnu.org/licenses/>.               *
 */
package com.projectswg.holocore.resources.gameplay.crafting.survey

import com.projectswg.common.data.encodables.oob.ProsePackage
import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.data.encodables.oob.waypoint.WaypointColor
import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.common.data.location.Location
import com.projectswg.common.data.sui.SuiEvent
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage
import com.projectswg.common.network.packets.swg.zone.PlayMusicMessage
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage.SystemChatType
import com.projectswg.holocore.intents.support.objects.ObjectCreatedIntent
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.GalacticResource
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.RawResourceType
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData.rawResources
import com.projectswg.holocore.resources.support.global.zone.sui.SuiButtons
import com.projectswg.holocore.resources.support.global.zone.sui.SuiListBox
import com.projectswg.holocore.resources.support.global.zone.sui.SuiMessageBox
import com.projectswg.holocore.resources.support.global.zone.sui.SuiWindow
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import com.projectswg.holocore.resources.support.objects.swg.waypoint.WaypointObject
import com.projectswg.holocore.services.gameplay.crafting.resource.ResourceContainerEventHandler
import com.projectswg.holocore.services.gameplay.crafting.resource.ResourceContainerHelper.giveResourcesToPlayer
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool
import me.joshlarson.jlcommon.log.Log
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ThreadLocalRandom

class SampleLoopSession(private val creature: CreatureObject, private val surveyTool: TangibleObject, private val resource: GalacticResource, private val sampleLocation: Location) {
	private var sampleWindow: SuiWindow? = null
	private var loopFuture: ScheduledFuture<*>? = null
	private var paused = false
	private var sampleMultiplier = 1

	fun isMatching(creature: CreatureObject, surveyTool: TangibleObject, resource: GalacticResource?, sampleLocation: Location): Boolean {
		if (this.sampleLocation.distanceTo(sampleLocation) >= 0.5 || this.sampleLocation.terrain != sampleLocation.terrain) return false // Too far away for the same session

		return this.creature == creature && (this.surveyTool == surveyTool) && this.resource.equals(resource)
	}

	@get:Synchronized
	val isSampling: Boolean
		get() = loopFuture != null && !loopFuture!!.isDone

	@Synchronized
	fun onPlayerMoved() {
		val oldLocation = sampleLocation
		val newLocation = creature.worldLocation

		if (oldLocation.distanceTo(newLocation) >= 0.5 || oldLocation.terrain != newLocation.terrain) stopSession()
	}

	/**
	 * Attempts to start the sample loop session with the specified executor.  If the loop is unable to start, this function returns false; otherwise this function returns true.
	 *
	 * @param executor the executor for the loop to run on
	 * @return TRUE if the sample loop has begin, FALSE otherwise
	 */
	@Synchronized
	fun startSession(executor: ScheduledThreadPool): Boolean {
		if (loopFuture != null) {
			loopFuture!!.cancel(false)
		}
		val concentration = concentration
		if (!isAllowedToSample(concentration)) return false

		loopFuture = executor.executeWithFixedRate(30000, 30000) { this.sample() }
		creature.posture = Posture.CROUCHED
		creature.setMovementPercent(0.0)
		creature.setTurnScale(0.0)
		creature.sendSelf(ChatSystemMessage(SystemChatType.PERSONAL, ProsePackage(StringId("@survey:start_sampling"), "TO", resource.name)))
		StandardLog.onPlayerTrace(this, creature, "started a sample session with %s and concentration %.1f", resource.name, concentration)
		return true
	}

	@Synchronized
	fun stopSession() {
		if (loopFuture != null) loopFuture!!.cancel(false)
		else return
		loopFuture = null
		if (sampleWindow != null) {
			val owner = creature.owner
			if (owner != null) sampleWindow!!.close(owner)
		}
		sampleWindow = null

		creature.posture = Posture.UPRIGHT
		creature.setMovementPercent(1.0)
		creature.setTurnScale(1.0)
		creature.sendSelf(ChatSystemMessage(SystemChatType.PERSONAL, "@survey:sample_cancel"))
	}

	private fun sample() {
		val concentration = concentration
		if (!isAllowedToSample(concentration) || paused) return

		val random = ThreadLocalRandom.current()
		if (random.nextDouble() > concentration) {
			creature.sendSelf(ChatSystemMessage(SystemChatType.PERSONAL, ProsePackage(StringId("@survey:sample_failed"), "TO", resource.name)))
			sendSampleEffects()
			return
		}

		val resourceAmount = random.nextInt(19, 25) * sampleMultiplier
		if (random.nextDouble() < 0.1) {
			val roll = random.nextDouble()
			if (roll < 0.5) { // 0.50
				creature.sendSelf(ChatSystemMessage(SystemChatType.PERSONAL, "@survey:critical_success"))
				sampleMultiplier *= 2
			} else if (roll < 0.75) { // 0.75
				openConcentrationWindow()
			}
		}

		val owner = creature.owner
		if (owner != null) {
			val resourceContainerEventHandler: ResourceContainerEventHandler = object : ResourceContainerEventHandler {
				override fun onUnknownError() {
					creature.sendSelf(ChatSystemMessage(SystemChatType.PERSONAL, "The was an unknown server error when transferring resources to your inventory"))
				}

				override fun onInventoryFull() {
					creature.sendSelf(ChatSystemMessage(SystemChatType.PERSONAL, "@survey:no_inv_space"))
					stopSession()
				}

				override fun onSuccess() {
					creature.sendSelf(ChatSystemMessage(SystemChatType.PERSONAL, ProsePackage(StringId("@survey:sample_located"), "DI", resourceAmount, "TO", resource.name)))
					creature.sendSelf(PlayMusicMessage(0, "sound/item_internalstorage_open.snd", 1, false))
				}
			}
			giveResourcesToPlayer(resourceAmount, resource, owner, resourceContainerEventHandler)
			sendSampleEffects()
		}
	}

	private fun openConcentrationWindow() {
		val window = SuiMessageBox(SuiButtons.OK_CANCEL, "@survey:cnode_t", "@survey:cnode_d")
		window.setProperty("btnOk", "Text", "@survey:cnode_2")
		window.setProperty("btnCancel", "Text", "@survey:cnode_1")
		window.addOkButtonCallback("okButton") { event: SuiEvent?, parameters: Map<String?, String?>? ->
			val index = SuiListBox.getSelectedRow(parameters)
			this.sampleWindow = null
			if (index == 0) {
				createHighestConcentrationWaypoint()
				stopSession()
			}
		}
		window.addCancelButtonCallback("cancelButton") { event: SuiEvent?, parameters: Map<String?, String?>? -> this.sampleWindow = null }
		if (this.sampleWindow == null) {
			this.sampleWindow = window
			window.display(creature.owner)
		}
		paused = true
	}

	private fun createHighestConcentrationWaypoint() {
		var highestX = sampleLocation.x
		var highestZ = sampleLocation.z
		var highest = 0.0

		var x = sampleLocation.x - 5
		while (x < sampleLocation.x + 5) {
			var z = sampleLocation.z - 5
			while (z < sampleLocation.z + 5) {
				val spawns = resource.getSpawns(sampleLocation.terrain)
				var concentration = 0.0
				for (spawn in spawns) {
					concentration += spawn.getConcentration(sampleLocation.terrain, x, z).toDouble()
				}
				if (concentration > highest) {
					highestX = x
					highestZ = z
					highest = concentration
				}
				z += 1.0
			}
			x += 1.0
		}

		creature.playerObject.waypoints.entries.stream().filter { e: Map.Entry<Long?, WaypointObject> -> "Resource Survey" == e.value.name }.filter { e: Map.Entry<Long?, WaypointObject> -> e.value.color == WaypointColor.ORANGE }.filter { e: Map.Entry<Long?, WaypointObject> -> e.value.terrain == sampleLocation.terrain }.map { obj: Map.Entry<Long?, WaypointObject?> -> obj.key }.forEach { objId: Long? -> creature.playerObject.removeWaypoint(objId!!) }
		createResourceWaypoint(Location.builder(sampleLocation).setX(highestX).setZ(highestZ).build())
		creature.sendSelf(ChatSystemMessage(SystemChatType.PERSONAL, "@survey:node_waypoint"))
	}

	private fun isAllowedToSample(concentration: Double): Boolean {
		if (resource.getSpawns(creature.terrain).isEmpty()) {
			creature.sendSelf(ChatSystemMessage(SystemChatType.PERSONAL, "@survey:sample_empty"))
			stopSession()
			return false
		}
		if (!creature.inventory.containedObjects.contains(surveyTool)) {
			creature.sendSelf(ChatSystemMessage(SystemChatType.PERSONAL, "@survey:sample_gone"))
			stopSession()
			return false
		}
		if (creature.getSkillModValue("surveying") < 20) {
			creature.sendSelf(ChatSystemMessage(SystemChatType.PERSONAL, "You aren't allowed to sample without a surveying skillmod"))
			return false
		}
		if (creature.isStatesBitmask(CreatureState.RIDING_MOUNT)) {
			creature.sendSelf(ChatSystemMessage(SystemChatType.PERSONAL, "You aren't allowed to sample while on a mount"))
			return false
		}
		if (creature.parent != null) {
			creature.sendSelf(ChatSystemMessage(SystemChatType.PERSONAL, "You aren't allowed to sample while within a building"))
			return false
		}
		if (creature.isInCombat) {
			creature.sendSelf(ChatSystemMessage(SystemChatType.PERSONAL, "@survey:sample_cancel_attack"))
			return false
		}
		if (concentration <= 0.3) {
			creature.sendSelf(ChatSystemMessage(SystemChatType.PERSONAL, ProsePackage(StringId("@survey:density_below_threshold"), "TO", resource.name)))
			stopSession()
			return false
		}
		return true
	}

	private val concentration: Double
		get() {
			val spawns = resource.getSpawns(creature.terrain)
			var concentration = 0.0
			for (spawn in spawns) {
				concentration += spawn.getConcentration(sampleLocation.terrain, sampleLocation.x, sampleLocation.z) / 100.0
			}
			return concentration
		}

	private fun sendSampleEffects() {
		creature.sendSelf(PlayMusicMessage(0, musicFile, 1, false))
		creature.sendObservers(PlayClientEffectObjectMessage(effectFile, "", creature.objectId, ""))
	}

	private val musicFile: String
		get() {
			val rawResource = rawResources.getResource(resource.rawResourceId)
			if (RawResourceType.MINERAL.isResourceType(rawResource!!)) return "sound/item_mineral_tool_sample.snd"
			if (RawResourceType.WATER.isResourceType(rawResource)) return "sound/item_moisture_tool_sample.snd"
			if (RawResourceType.CHEMICAL.isResourceType(rawResource)) return "sound/item_liquid_tool_sample.snd"
			if (RawResourceType.FLORA_STRUCTURAL.isResourceType(rawResource)) return "sound/item_lumber_tool_sample.snd"
			if (RawResourceType.GAS.isResourceType(rawResource)) return "sound/item_gas_tool_sample.snd"
			if (RawResourceType.ENERGY_RENEWABLE_UNLIMITED_SOLAR.isResourceType(rawResource)) return "sound/item_moisture_tool_sample.snd"
			if (RawResourceType.ENERGY_RENEWABLE_UNLIMITED_WIND.isResourceType(rawResource)) return "sound/item_moisture_tool_sample.snd"
			Log.w("Unknown raw resource sample music file: %s with type %s", rawResource, rawResource.resourceType)
			return ""
		}

	private val effectFile: String
		get() {
			val rawResource = rawResources.getResource(resource.rawResourceId)
			if (RawResourceType.MINERAL.isResourceType(rawResource!!)) return "clienteffect/survey_sample_mineral.cef"
			if (RawResourceType.WATER.isResourceType(rawResource)) return "clienteffect/survey_sample_moisture.cef"
			if (RawResourceType.CHEMICAL.isResourceType(rawResource)) return "clienteffect/survey_sample_liquid.cef"
			if (RawResourceType.FLORA_STRUCTURAL.isResourceType(rawResource)) return "clienteffect/survey_sample_lumber.cef"
			if (RawResourceType.GAS.isResourceType(rawResource)) return "clienteffect/survey_sample_gas.cef"
			if (RawResourceType.ENERGY_RENEWABLE_UNLIMITED_SOLAR.isResourceType(rawResource)) return "clienteffect/survey_sample_moisture.cef"
			if (RawResourceType.ENERGY_RENEWABLE_UNLIMITED_WIND.isResourceType(rawResource)) return "clienteffect/survey_sample_moisture.cef"
			Log.w("Unknown raw resource sample effect file: %s with type %s", rawResource, rawResource.resourceType)
			return ""
		}

	private fun createResourceWaypoint(location: Location) {
		val waypoint = ObjectCreator.createObjectFromTemplate("object/waypoint/shared_waypoint.iff") as WaypointObject
		waypoint.setPosition(location.terrain, location.x, location.y, location.z)
		waypoint.color = WaypointColor.ORANGE
		waypoint.name = "Resource Survey"
		ObjectCreatedIntent(waypoint).broadcast()
		creature.playerObject.addWaypoint(waypoint)
	}
}
