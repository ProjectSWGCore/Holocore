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
package com.projectswg.holocore.services.gameplay.player.experience.skills

import com.projectswg.common.data.CRC
import com.projectswg.common.data.swgfile.ClientFactory
import com.projectswg.holocore.intents.gameplay.player.badge.GrantBadgeIntent
import com.projectswg.holocore.intents.gameplay.player.badge.SetTitleIntent
import com.projectswg.holocore.intents.gameplay.player.experience.GrantSkillIntent
import com.projectswg.holocore.intents.gameplay.player.experience.SkillModIntent
import com.projectswg.holocore.intents.gameplay.player.experience.SurrenderSkillIntent
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader.Companion.badges
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader.Companion.skills
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData.schematicGroups
import com.projectswg.holocore.resources.support.data.server_info.loader.SkillLoader.SkillInfo
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.services.gameplay.player.experience.*
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import java.util.*

class SkillService : Service() {
	private val combatXpCalculator = CombatXpCalculator(SdbCombatXpMultiplierRepository())
	private val combatLevelCalculator = CombatLevelCalculator(SdbCombatLevelRepository())
	private val healthAddedCalculator = HealthAddedCalculator()

	@IntentHandler
	private fun handleGrantSkillIntent(gsi: GrantSkillIntent) {
		if (gsi.intentType != GrantSkillIntent.IntentType.GRANT) {
			return
		}

		val skillName = gsi.skillName
		val target = gsi.target
		val skill = skills().getSkillByName(skillName) ?: return

		val oldCombatLevel = getCombatLevel(target)
		grantSkill(target, skill, gsi.isGrantRequiredSkills)
		val newCombatLevel = getCombatLevel(target)

		val levelChanged = oldCombatLevel != newCombatLevel

		if (levelChanged) {
			changeLevel(target, oldCombatLevel, newCombatLevel)
		}

		val badgeFromKey = badges().getBadgeFromKey(skillName)

		if (badgeFromKey != null) {
			GrantBadgeIntent.broadcast(target, skillName)
		}
	}

	private fun adjustHealth(target: CreatureObject, oldCombatLevel: CombatLevel, newCombatLevel: CombatLevel) {
		val healthChange = healthAddedCalculator.calculate(oldCombatLevel, newCombatLevel)
		target.levelHealthGranted = newCombatLevel.healthAdded
		target.maxHealth += healthChange
		target.health = target.maxHealth
	}

	private fun getCombatLevel(target: CreatureObject): CombatLevel {
		val experienceCollection = getExperienceFromTrainedSkills(target)
		val oldCombatLevelXpFromTrainedSkills = combatXpCalculator.calculate(experienceCollection)
		return combatLevelCalculator.calculate(oldCombatLevelXpFromTrainedSkills)
	}

	private fun getExperienceFromTrainedSkills(target: CreatureObject): Collection<Experience> {
		val trainedSkills = target.skills
		val experienceCollection: MutableCollection<Experience> = ArrayList()
		for (trainedSkill in trainedSkills) {
			val experience = convertTrainedSkillToExperience(trainedSkill)

			if (experience != null) {
				experienceCollection.add(experience)
			}
		}

		return experienceCollection
	}

	private fun convertTrainedSkillToExperience(trainedSkill: String): Experience? {
		val trainedSkillInfo = skills().getSkillByName(trainedSkill)

		if (trainedSkillInfo != null) {
			val xpType = trainedSkillInfo.xpType
			val xpCost = trainedSkillInfo.xpCost

			return Experience(xpType, xpCost)
		}

		return null
	}

	@IntentHandler
	private fun handleSetTitleIntent(sti: SetTitleIntent) {
		val requester = sti.requester
		val playerObject = requester.playerObject ?: return
		val title = sti.title

		if (title.isBlank()) {
			playerObject.title = title
			return
		}

		val skillData = skills().getSkillByName(title) ?: return

		if (!skillData.isTitle) {
			// There's a skill with this name, but it doesn't grant a title
			return
		}

		val creatureObject = requester.creatureObject
		val skills = creatureObject.skills

		if (skills.contains(title)) {
			playerObject.title = title
		}
	}

	@IntentHandler
	private fun handleSurrenderSkillIntent(ssi: SurrenderSkillIntent) {
		val target = ssi.target
		val surrenderedSkill = ssi.surrenderedSkill

		if (!target.hasSkill(surrenderedSkill)) {
			StandardLog.onPlayerError(this, target, "could not surrender skill %s because they do not have it", surrenderedSkill)
			return
		}

		val dependentSkills = target.skills
			.mapNotNull { skills().getSkillByName(it) }
			.map { it.skillsRequired }
			.filter { requiredSkills: Array<String> ->
				for (requiredSkill in requiredSkills) {
					if (requiredSkill == surrenderedSkill) {
						return@filter true
					}
				}
				false
			}
			.flatMap { it.toList() }

		if (dependentSkills.isNotEmpty()) {
			StandardLog.onPlayerError(this, target, "could not surrender skill %s because these skills depend on it: %s", surrenderedSkill, dependentSkills.joinToString(", "))
			return
		}

		val skillInfo = skills().getSkillByName(surrenderedSkill)
		if (skillInfo == null) {
			StandardLog.onPlayerError(this, target, "could not surrender skill %s because it does not exist", surrenderedSkill)
			return
		}

		val oldCombatLevel = getCombatLevel(target)

		target.removeSkill(surrenderedSkill)
		target.removeCommands(*skillInfo.commands)
		skillInfo.skillMods.forEach { (skillModName: String, skillModValue: Int) -> SkillModIntent(skillModName, 0, -skillModValue, target).broadcast() }
		val schematicGroups = skillInfo.schematicsGranted
		for (schematicGroup in schematicGroups) {
			revokeSchematicGroup(target, schematicGroup)
		}
		val newCombatLevel = getCombatLevel(target)

		val levelChanged = oldCombatLevel != newCombatLevel

		if (levelChanged) {
			changeLevel(target, oldCombatLevel, newCombatLevel)
		}
	}

	private fun changeLevel(target: CreatureObject, oldCombatLevel: CombatLevel, newCombatLevel: CombatLevel) {
		target.setLevel(newCombatLevel.level)
		adjustHealth(target, oldCombatLevel, newCombatLevel)
	}

	private fun grantSkill(target: CreatureObject, skill: SkillInfo, grantRequired: Boolean) {
		val pointsRequired = skill.pointsRequired
		val skillPointsSpent = skillPointsSpent(target)

		if (skillPointsSpent + pointsRequired > SKILL_POINT_CAP) {
			val missingPoints = pointsRequired - (SKILL_POINT_CAP - skillPointsSpent)

			StandardLog.onPlayerError(this, target, "cannot learn %s because they lack %d skill points", skill.name, missingPoints)
			return
		}


		val parentSkillName = skill.parent

		if (grantRequired) {
			grantParentSkills(parentSkillName, target)
			grantRequiredSkills(skill, target)
		}

		grantSkill(target, skill)
	}

	private fun grantParentSkills(skillName: String, target: CreatureObject) {
		if (skillName.isEmpty() || target.hasSkill(skillName)) return  // Nothing to do here


		val skillInfo = skills().getSkillByName(skillName)
		if (skillInfo == null) {
			StandardLog.onPlayerTrace(this, target, "requires an invalid parent skill: %s", skillName)
			return
		}

		grantParentSkills(skillInfo.parent, target)
		grantSkill(target, skillInfo)
	}

	private fun grantRequiredSkills(skillData: SkillInfo, target: CreatureObject) {
		val requiredSkills = skillData.skillsRequired ?: return

		val skills = skills()
		for (requiredSkillName in requiredSkills) {
			val requiredSkill = skills.getSkillByName(requiredSkillName)
			if (requiredSkill != null) grantSkill(target, requiredSkill, true)
		}
	}

	private fun grantSkill(target: CreatureObject, skill: SkillInfo) {
		if ((skill.parent.isNotEmpty() && !target.hasSkill(skill.parent)) || !hasRequiredSkills(skill, target)) {
			StandardLog.onPlayerError(this, target, "lacks required skill %s before being granted skill %s", skill.parent, skill.name)
			return
		}
		if (!target.addSkill(skill.name)) return
		target.addCommand(*skill.commands)

		skill.skillMods.forEach { (skillModName, skillModValue) -> SkillModIntent(skillModName, skillModValue, 0, target).broadcast() }

		val schematicGroups = skill.schematicsGranted
		for (schematicGroup in schematicGroups) {
			grantSchematicGroup(target, schematicGroup)
		}
		
		GrantSkillIntent(GrantSkillIntent.IntentType.GIVEN, skill.name, target, false).broadcast()
	}

	private fun grantSchematicGroup(target: CreatureObject, schematicGroup: String) {
		val schematicGroupLoader = schematicGroups
		val schematicsInGroup = schematicGroupLoader.getSchematicsInGroup(schematicGroup)

		for (schematicInGroup in schematicsInGroup) {
			grantSchematic(target, schematicInGroup)
		}
	}

	private fun grantSchematic(target: CreatureObject, schematicInGroup: String) {
		val schematicInGroupShared = ClientFactory.formatToSharedFile(schematicInGroup)
		val serverCrc = getDraftSchematicServerCrc(schematicInGroupShared)
		val clientCrc = getDraftSchematicClientCrc(schematicInGroupShared)
		target.playerObject.setDraftSchematic(serverCrc, clientCrc, 1)
	}

	private fun revokeSchematicGroup(target: CreatureObject, schematicGroup: String) {
		val schematicGroupLoader = schematicGroups
		val schematicsInGroup = schematicGroupLoader.getSchematicsInGroup(schematicGroup)

		for (schematicInGroup in schematicsInGroup) {
			revokeSchematic(target, schematicInGroup)
		}
	}

	private fun revokeSchematic(target: CreatureObject, schematicInGroup: String) {
		val schematicInGroupShared = ClientFactory.formatToSharedFile(schematicInGroup)
		val serverCrc = getDraftSchematicServerCrc(schematicInGroupShared)
		val clientCrc = getDraftSchematicClientCrc(schematicInGroupShared)
		target.playerObject.revokeDraftSchematic(serverCrc, clientCrc)
	}

	private fun getDraftSchematicServerCrc(schematicInGroupShared: String): Int {
		return CRC.getCrc(schematicInGroupShared)
	}

	private fun getDraftSchematicClientCrc(schematicInGroupShared: String): Int {
		val templateWithoutPrefix = schematicInGroupShared.replace("object/draft_schematic/", "")
		return CRC.getCrc(templateWithoutPrefix)
	}


	private fun skillPointsSpent(creature: CreatureObject): Int {
		return creature.skills
			.mapNotNull { skills().getSkillByName(it) }
			.sumOf { it.pointsRequired }
	}

	private fun hasRequiredSkills(skillData: SkillInfo, creatureObject: CreatureObject): Boolean {
		val requiredSkills = skillData.skillsRequired ?: return true

		for (required in requiredSkills) {
			if (!creatureObject.hasSkill(required)) return false
		}
		return true
	}

	companion object {
		private const val SKILL_POINT_CAP = 250
	}
}
