/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.gameplay.crafting.survey

import com.projectswg.common.network.packets.swg.zone.PlayMusicMessage
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage.SystemChatType
import com.projectswg.common.network.packets.swg.zone.crafting.resources.ResourceListForSurveyMessage
import com.projectswg.common.network.packets.swg.zone.crafting.resources.ResourceListForSurveyMessage.ResourceItem
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.GalacticResource
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.RawResourceType
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.storage.GalacticResourceContainer.getRawResource
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.storage.GalacticResourceContainer.getSpawnedResources
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import kotlinx.coroutines.CoroutineScope
import me.joshlarson.jlcommon.log.Log

class SurveyToolSession(private val creature: CreatureObject, private val surveyTool: TangibleObject, surveyScope: CoroutineScope) {
	private val surveyHandler = SurveyHandler(creature, surveyTool, surveyScope)
	private val sampleHandler = SampleHandler(creature, surveyTool, surveyScope)

	@Synchronized
	fun startSession() {
		surveyHandler.startSession()
		sampleHandler.startSession()

		creature.sendSelf(PlayMusicMessage(0, "sound/item_surveypad_lp.snd", 1, false))

		val resourceType = surveyTool.template.substring(47, surveyTool.template.length - 4)
		val survey = ResourceListForSurveyMessage(creature.objectId, surveyTool.template)
		val surveyToolType = getTypeFromSurveyTool(resourceType)
		for (resource in getSpawnedResources(creature.terrain)) {
			val rawResource = getRawResource(resource.rawResourceId)
			if (!surveyToolType.isResourceType(rawResource!!) || rawResource.resourceType.isResourceType(RawResourceType.CREATURE_RESOURCES)) continue
			survey.addResource(ResourceItem(resource.name, rawResource.name, resource.id))
		}
		creature.sendSelf(survey)
	}

	@Synchronized
	fun stopSession() {
		surveyHandler.stopSession()
		sampleHandler.stopSession()
	}

	@Synchronized
	fun onPlayerMoved() {
		sampleHandler.onPlayerMoved()
	}

	@Synchronized
	fun startSurvey(resource: GalacticResource) {
		if (sampleHandler.isSampling) {
			creature.sendSelf(ChatSystemMessage(SystemChatType.PERSONAL, "@survey:survey_sample")) // "You can't take a survey while you are collecting samples."
			return
		}
		surveyHandler.startSurvey(resource)
	}

	@Synchronized
	fun startSample(resource: GalacticResource) {
		if (surveyHandler.isSurveying) {
			creature.sendSelf(ChatSystemMessage(SystemChatType.PERSONAL, "@survey:sample_survey")) // "You can't take resource samples while you are surveying"
			return
		}
		if (surveyHandler.lastResourceSurveyed !== resource) {
			creature.sendSelf(ChatSystemMessage(SystemChatType.PERSONAL, "You can't get a sample without having scanned the area first"))
			return
		}
		sampleHandler.startSampleLoop(resource)
	}

	@Synchronized
	fun stopSample() {
		sampleHandler.stopSampleLoop()
	}

	companion object {
		private fun getTypeFromSurveyTool(surveyTool: String): RawResourceType {
			when (surveyTool) {
				"mineral"            -> return RawResourceType.MINERAL
				"lumber"             -> return RawResourceType.FLORA_STRUCTURAL
				"liquid"             -> return RawResourceType.CHEMICAL
				"moisture"           -> return RawResourceType.WATER
				"gas", "gas_thermal" -> return RawResourceType.GAS
				"wind"               -> return RawResourceType.ENERGY_RENEWABLE_UNLIMITED_WIND
				"solar"              -> return RawResourceType.ENERGY_RENEWABLE_UNLIMITED_SOLAR
				"inorganic"          -> return RawResourceType.INORGANIC
				"organic"            -> return RawResourceType.ORGANIC
				"all"                -> return RawResourceType.RESOURCE
				else                 -> {
					Log.w("Unknokwn survey tool type: %s", surveyTool)
					return RawResourceType.RESOURCE
				}
			}
		}
	}
}
