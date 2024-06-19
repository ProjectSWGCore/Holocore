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
package com.projectswg.holocore.services.gameplay.training

import com.projectswg.common.data.sui.SuiEvent
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.global.zone.sui.SuiListBox

class AvailableSkillsWindow {
	fun show(professionName: String?, player: Player) {
		val profession = Profession(professionName)
		val skillRepository = SkillsSdbSkillRepository()
		val trainee: Trainee = TraineeImpl(player, skillRepository)
		val training = Training(skillRepository)

		createWindow(trainee, training, profession, player)
	}

	private fun createWindow(trainee: Trainee, training: Training, profession: Profession, player: Player) {
		val skills: List<Skill> = ArrayList(training.whatCanTraineeLearnRightNow(profession, trainee))
		SuiListBox().run {
			title = "Training"
			prompt = "Skills you can learn right now"

			addSkillsAsRows(trainee, training, skills, this)

			addOkButtonCallback("trainskill") { _: SuiEvent, params: Map<String, String> ->
				val selectedRow = SuiListBox.getSelectedRow(params)
				if (isInvalidSelection(skills, selectedRow)) {
					return@addOkButtonCallback
				}

				val skill = skills[selectedRow]
				training.trainSkill(trainee, skill)
			}

			display(player)
		}
	}

	private fun addSkillsAsRows(trainee: Trainee, training: Training, skills: List<Skill>, listBox: SuiListBox) {
		for (skill in skills) {
			val skillDisplayName = "@skl_n:" + skill.key
			val requiredSkillPoints = getRequiredSkillPoints(training, skill, trainee)
			val requiredCredits = getRequiredCredits(training, trainee, skill)
			listBox.addListItem("$skillDisplayName $WHITE| $requiredSkillPoints | $requiredCredits")
		}
	}

	private fun getRequiredSkillPoints(training: Training, skill: Skill, trainee: Trainee): String {
		val requiredSkillPoints = skill.requiredSkillPoints
		val enoughAvailableSkillPoints = training.enoughAvailableSkillPoints(trainee, skill)

		val color = if (enoughAvailableSkillPoints) {
			GREEN
		} else {
			RED
		}

		return "$color$requiredSkillPoints skill points$WHITE"
	}

	private fun getRequiredCredits(training: Training, trainee: Trainee, skill: Skill): String {
		val color = if (training.traineeHasEnoughCredits(trainee, skill)) {
			GREEN
		} else {
			RED
		}

		return color + skill.requiredCredits + " credits" + WHITE
	}

	private fun isInvalidSelection(skills: List<Skill>, selectedRow: Int): Boolean {
		return selectedRow < 0 || selectedRow >= skills.size
	}

	companion object {
		private const val GREEN = "\\#00FF00"
		private const val RED = "\\#FF0000"
		private const val WHITE = "\\#FFFFFF"
	}
}
