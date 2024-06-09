/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.gameplay.crafting.survey

import com.projectswg.common.data.encodables.oob.ProsePackage
import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.data.encodables.oob.waypoint.WaypointColor
import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.common.data.location.Location
import com.projectswg.common.data.location.Terrain
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage
import com.projectswg.common.network.packets.swg.zone.PlayMusicMessage
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage.SystemChatType
import com.projectswg.common.network.packets.swg.zone.crafting.surveying.SurveyMessage
import com.projectswg.common.network.packets.swg.zone.crafting.surveying.SurveyMessage.ResourceConcentration
import com.projectswg.holocore.intents.support.objects.ObjectCreatedIntent
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.GalacticResource
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.GalacticResourceSpawn
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.RawResourceType
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData.rawResources
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.ServerAttribute
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import com.projectswg.holocore.resources.support.objects.swg.waypoint.WaypointObject
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool
import me.joshlarson.jlcommon.log.Log
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.atomic.AtomicReference

internal class SurveyHandler(private val creature: CreatureObject, private val surveyTool: TangibleObject, private val executor: ScheduledThreadPool) {
	private val surveyRequest = AtomicReference<ScheduledFuture<*>?>(null)
	private val lastSurveyCompleted = AtomicReference<GalacticResource?>(null)

	fun startSession() {
	}

	fun stopSession() {
		val surveyRequest = surveyRequest.get()
		surveyRequest?.cancel(false)
	}

	val isSurveying: Boolean
		get() {
			val prev = surveyRequest.get()
			return prev != null && !prev.isDone
		}

	val lastResourceSurveyed: GalacticResource?
		get() = lastSurveyCompleted.get()

	fun startSurvey(resource: GalacticResource) {
		if (isSurveying) return
		val resolution = currentResolution
		val location = creature.worldLocation
		if (!isAllowedToSurvey(location, resolution, resource)) return
		checkNotNull(resolution) { "verified in isAllowedToSurvey" }

		creature.modifyAction((-creature.maxAction / 10.0 * resolution.counter).toInt())
		surveyRequest.set(executor.execute(4000) { sendSurveyMessage(resolution, location, resource) })
		creature.sendSelf(ChatSystemMessage(SystemChatType.PERSONAL, ProsePackage(StringId("survey", "start_survey"), "TO", resource.name)))
		creature.sendSelf(PlayMusicMessage(0, getMusicFile(resource), 1, false))
		creature.sendObservers(PlayClientEffectObjectMessage(getEffectFile(resource), "", creature.objectId, ""))
	}

	private fun sendSurveyMessage(resolution: SurveyToolResolution, location: Location, resource: GalacticResource) {
		val baseLocationX = location.x
		val baseLocationZ = location.z
		val rangeHalf = resolution.range / 2.0
		val rangeInc = resolution.range / (resolution.resolution - 1.0)

		val surveyMessage = SurveyMessage()
		val spawns = resource.getSpawns(location.terrain)
		var highestX = baseLocationX
		var highestZ = baseLocationX
		var highestConcentration = 0.0

		var x = baseLocationX - rangeHalf
		var xIndex = 0.0
		while (xIndex < resolution.resolution) {
			var z = baseLocationZ - rangeHalf
			var zIndex = 0.0
			while (zIndex < resolution.resolution) {
				val concentration = getConcentration(spawns, location.terrain, x, z)
				surveyMessage.addConcentration(ResourceConcentration(x, z, concentration))
				if (concentration > highestConcentration) {
					highestX = x
					highestZ = z
					highestConcentration = concentration
				}
				z += rangeInc
				zIndex++
			}
			x += rangeInc
			xIndex++
		}
		creature.sendSelf(surveyMessage)
		lastSurveyCompleted.set(resource)
		if (highestConcentration > 0.1) {
			creature.playerObject.waypoints.entries.stream().filter { e: Map.Entry<Long?, WaypointObject> -> "Resource Survey" == e.value.name }.filter { e: Map.Entry<Long?, WaypointObject> -> e.value.color == WaypointColor.ORANGE }.filter { e: Map.Entry<Long?, WaypointObject> -> e.value.terrain == location.terrain }.map { obj: Map.Entry<Long?, WaypointObject?> -> obj.key }.forEach { objId: Long? -> creature.playerObject.removeWaypoint(objId!!) }
			createResourceWaypoint(creature, Location.builder(location).setX(highestX).setZ(highestZ).build())
			sendErrorMessage(creature, "survey", "survey_waypoint")
		}
	}

	private val currentResolution: SurveyToolResolution?
		get() {
			val counterSetting = surveyTool.getServerAttribute(ServerAttribute.SURVEY_TOOL_RANGE) as Int
			// Must be set before using - invariant enforced within ObjectSurveyToolRadial.java

			val counter = counterSetting

			val resolutions: List<SurveyToolResolution> = SurveyToolResolution.getOptions(creature)
			for (resolution in resolutions) {
				if (resolution.counter == counter) return resolution
			}

			// Attempted to set a resolution that wasn't valid
			return if (resolutions.isEmpty()) null else resolutions[resolutions.size - 1]
		}

	private fun getConcentration(spawns: List<GalacticResourceSpawn>, terrain: Terrain, x: Double, z: Double): Double {
		var concentration = 0
		for (spawn in spawns) {
			concentration += spawn.getConcentration(terrain, x, z)
		}
		if (concentration < 10) // Minimum density
			return 0.0
		return concentration / 100.0
	}

	private fun isAllowedToSurvey(location: Location, resolution: SurveyToolResolution?, resource: GalacticResource): Boolean {
		// Player must be standing
		when (creature.posture) {
			Posture.SITTING -> {
				sendErrorMessage(creature, "error_message", "survey_sitting")
				return false
			}

			Posture.UPRIGHT -> {}
			else            -> {
				sendErrorMessage(creature, "error_message", "survey_standing")
				return false
			}
		}
		// Player cannot be in an instance
		if (creature.instanceLocation.instanceNumber != 0) {
			sendErrorMessage(creature, "error_message", "no_survey_instance")
			return false
		}
		// Player cannot be in combat or dead
		if (creature.isInCombat || creature.posture == Posture.INCAPACITATED || creature.posture == Posture.DEAD) {
			sendErrorMessage(creature, "error_message", "survey_cant")
			return false
		}
		// No survey tool resolution - could be because the player is not a trader or because the survey tool somehow didn't have it's range set
		if (resolution == null) {
			sendErrorMessage(creature, "error_message", "survey_cant")
			return false
		}
		// Player cannot be within a building
		if (creature.parent != null) {
			if (creature.isStatesBitmask(CreatureState.RIDING_MOUNT)) sendErrorMessage(creature, "error_message", "survey_on_mount")
			else sendErrorMessage(creature, "error_message", "survey_in_structure")
			return false
		}
		// Survey tool not within inventory
		if (surveyTool.parent !== creature.inventory) {
			creature.sendSelf(ChatSystemMessage(SystemChatType.PERSONAL, "The survey tool is not in your inventory"))
			return false
		}
		// The specified resource no longer exists (or the player changed planets)
		if (resource.getSpawns(location.terrain).isEmpty()) {
			sendErrorMessage(creature, "error_message", "survey_error")
			return false
		}
		if (creature.action < creature.maxAction / 10.0 * resolution.counter) {
			sendErrorMessage(creature, "error_message", "survey_mind")
			return false
		}
		return true
	}

	companion object {
		private fun getMusicFile(resource: GalacticResource): String {
			val rawResource = rawResources.getResource(resource.rawResourceId)
			if (RawResourceType.MINERAL.isResourceType(rawResource!!)) return "sound/item_mineral_tool_survey.snd"
			if (RawResourceType.WATER.isResourceType(rawResource)) return "sound/item_moisture_tool_survey.snd"
			if (RawResourceType.CHEMICAL.isResourceType(rawResource)) return "sound/item_liquid_tool_survey.snd"
			if (RawResourceType.FLORA_STRUCTURAL.isResourceType(rawResource)) return "sound/item_lumber_tool_survey.snd"
			if (RawResourceType.GAS.isResourceType(rawResource)) return "sound/item_gas_tool_survey.snd"
			if (RawResourceType.ENERGY_RENEWABLE_UNLIMITED_SOLAR.isResourceType(rawResource)) return "sound/item_moisture_tool_survey.snd"
			if (RawResourceType.ENERGY_RENEWABLE_UNLIMITED_WIND.isResourceType(rawResource)) return "sound/item_moisture_tool_survey.snd"
			Log.w("Unknown raw resource survey music file: %s with type %s", rawResource, rawResource.resourceType)
			return ""
		}

		private fun getEffectFile(resource: GalacticResource): String {
			val rawResource = rawResources.getResource(resource.rawResourceId)
			if (RawResourceType.MINERAL.isResourceType(rawResource!!)) return "clienteffect/survey_tool_mineral.cef"
			if (RawResourceType.WATER.isResourceType(rawResource)) return "clienteffect/survey_tool_moisture.cef"
			if (RawResourceType.CHEMICAL.isResourceType(rawResource)) return "clienteffect/survey_tool_liquid.cef"
			if (RawResourceType.FLORA_STRUCTURAL.isResourceType(rawResource)) return "clienteffect/survey_tool_lumber.cef"
			if (RawResourceType.GAS.isResourceType(rawResource)) return "clienteffect/survey_tool_gas.cef"
			if (RawResourceType.ENERGY_RENEWABLE_UNLIMITED_SOLAR.isResourceType(rawResource)) return "clienteffect/survey_tool_moisture.cef"
			if (RawResourceType.ENERGY_RENEWABLE_UNLIMITED_WIND.isResourceType(rawResource)) return "clienteffect/survey_tool_moisture.cef"
			Log.w("Unknown raw resource survey effect file: %s with type %s", rawResource, rawResource.resourceType)
			return ""
		}

		private fun createResourceWaypoint(creature: CreatureObject, location: Location) {
			val waypoint = ObjectCreator.createObjectFromTemplate("object/waypoint/shared_waypoint.iff") as WaypointObject
			waypoint.setPosition(location.terrain, location.x, location.y, location.z)
			waypoint.color = WaypointColor.ORANGE
			waypoint.name = "Resource Survey"
			ObjectCreatedIntent(waypoint).broadcast()
			creature.playerObject.addWaypoint(waypoint)
		}

		private fun sendErrorMessage(creature: CreatureObject, file: String, key: String) {
			creature.sendSelf(ChatSystemMessage(SystemChatType.PERSONAL, ProsePackage(StringId(file, key))))
		}
	}
}
