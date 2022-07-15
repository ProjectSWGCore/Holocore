package com.projectswg.holocore.services.gameplay.jedi

import com.projectswg.common.data.encodables.player.Mail
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage
import com.projectswg.common.network.packets.swg.zone.PlayMusicMessage
import com.projectswg.holocore.ProjectSWG
import com.projectswg.holocore.intents.gameplay.jedi.CreateTestLightsaberIntent
import com.projectswg.holocore.intents.gameplay.player.experience.skills.GrantSkillIntent
import com.projectswg.holocore.intents.gameplay.player.experience.skills.SurrenderSkillIntent
import com.projectswg.holocore.intents.support.global.chat.PersistentMessageIntent
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent
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
	fun handleGrantSkillIntent(intent: GrantSkillIntent) {
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
				CreateTestLightsaberIntent.broadcast(player)
			}
		}
	}

	@IntentHandler
	fun handleSurrenderSkillIntent(intent: SurrenderSkillIntent) {
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
		val suiMessageBox = SuiMessageBox(SuiButtons.OK, "@jedi_trials:padawan_trials_title", "@jedi_trials:padawan_trials_completed")
		suiMessageBox.display(player)
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
		ObjectCreatedIntent.broadcast(padawanRobe);
		return padawanRobe
	}

	private fun sendWelcomeMail(player: Player) {
		val mail = Mail("system", "@jedi_spam:welcome_subject", "@jedi_spam:welcome_body", player.creatureObject.objectId)

		PersistentMessageIntent(player.creatureObject, mail, ProjectSWG.getGalaxy().name).broadcast()
	}
}