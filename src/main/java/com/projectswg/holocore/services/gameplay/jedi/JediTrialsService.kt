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
package com.projectswg.holocore.services.gameplay.jedi

import com.projectswg.common.data.encodables.player.Mail
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage
import com.projectswg.common.network.packets.swg.zone.PlayMusicMessage
import com.projectswg.holocore.ProjectSWG
import com.projectswg.holocore.intents.gameplay.jedi.CreateTestLightsaberIntent
import com.projectswg.holocore.intents.gameplay.player.experience.GrantSkillIntent
import com.projectswg.holocore.intents.gameplay.player.experience.SurrenderSkillIntent
import com.projectswg.holocore.intents.support.global.chat.PersistentMessageIntent
import com.projectswg.holocore.intents.support.objects.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.global.zone.sui.SuiButtons
import com.projectswg.holocore.resources.support.global.zone.sui.SuiMessageBox
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service

class JediTrialsService : Service() {

	private val jediInitiateSkill = "force_title_jedi_rank_01"
	private val jediPadawanSkill = "force_title_jedi_rank_02"

	@IntentHandler
	private fun handleGrantSkillIntent(intent: GrantSkillIntent) {
		if (intent.intentType == GrantSkillIntent.IntentType.GIVEN) {
			if (intent.skillName == jediPadawanSkill) {
				val player = intent.target.owner ?: return

				if (player.playerObject.jediState < 2) {
					player.playerObject.jediState = 2
				}

				showPadawanTrialsCompletedSui(player)

				playElectricEffect(player)
				playBecomeJediMusic(player)

				val padawanRobe = createPadawanRobe()
				padawanRobe.moveToContainer(player.creatureObject.inventory)

				sendWelcomeMail(player)

				// This is temporary until crafting is possible
				CreateTestLightsaberIntent(player).broadcast()
			}
		}
	}

	@IntentHandler
	private fun handleSurrenderSkillIntent(intent: SurrenderSkillIntent) {
		val surrenderedSkill = intent.surrenderedSkill
		val target = intent.target

		if (surrenderedSkill == jediPadawanSkill) {
			target.playerObject.jediState = 1
		}

		if (surrenderedSkill == jediInitiateSkill) {
			target.playerObject.jediState = 0
		}
	}

	private fun showPadawanTrialsCompletedSui(player: Player) {
		SuiMessageBox().run {
			title = "@jedi_trials:padawan_trials_title"
			prompt = "@jedi_trials:padawan_trials_completed"
			buttons = SuiButtons.OK
			display(player)
		}
	}

	private fun playElectricEffect(player: Player) {
		val playClientEffectObjectMessage = PlayClientEffectObjectMessage("clienteffect/trap_electric_01.cef", "", player.creatureObject.objectId, "")
		player.creatureObject.sendObservers(playClientEffectObjectMessage)
	}

	private fun playBecomeJediMusic(player: Player) {
		val playMusicMessage = PlayMusicMessage(0, "sound/music_become_jedi.snd", 1, false)
		player.sendPacket(playMusicMessage)
	}

	private fun createPadawanRobe(): TangibleObject {
		val padawanRobe = ObjectCreator.createObjectFromTemplate("object/tangible/wearables/robe/shared_robe_jedi_padawan.iff") as TangibleObject
		padawanRobe.requiredSkill = "force_title_jedi_rank_01"
		padawanRobe.isNoTrade = true
		padawanRobe.adjustSkillmod("jedi_force_power_max", 250, 0)
		padawanRobe.adjustSkillmod("jedi_force_power_regen", 10, 0)
		ObjectCreatedIntent(padawanRobe).broadcast()
		return padawanRobe
	}

	private fun sendWelcomeMail(player: Player) {
		val mail = Mail("system", "@jedi_spam:welcome_subject", "@jedi_spam:welcome_body", player.creatureObject.objectId)

		PersistentMessageIntent(player.creatureObject, mail, ProjectSWG.galaxy.name).broadcast()
	}
}