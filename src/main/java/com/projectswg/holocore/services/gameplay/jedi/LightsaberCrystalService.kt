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

import com.projectswg.common.data.sui.SuiEvent
import com.projectswg.holocore.intents.gameplay.jedi.RequestTuneCrystalIntent
import com.projectswg.holocore.intents.gameplay.jedi.TuneCrystalNowIntent
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.resources.support.global.zone.sui.SuiButtons
import com.projectswg.holocore.resources.support.global.zone.sui.SuiMessageBox
import com.projectswg.holocore.resources.support.objects.swg.ServerAttribute
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service

class LightsaberCrystalService : Service() {
	@IntentHandler
	private fun handleRequestTuneCrystalIntent(intent: RequestTuneCrystalIntent) {
		val tuner = intent.tuner
		val crystal = intent.crystal
		if (isTuned(crystal)) {
			return
		}
		val owner = tuner.owner ?: return
		SuiMessageBox().run {
			title = "@jedi_spam:confirm_tune_title"
			prompt = "@jedi_spam:confirm_tune_prompt"
			buttons = SuiButtons.YES_NO
			addOkButtonCallback("tune") { _: SuiEvent, _: Map<String, String> -> TuneCrystalNowIntent(tuner, crystal).broadcast() }
			display(owner)
		}
	}

	@IntentHandler
	private fun handleTuneCrystalNowIntent(intent: TuneCrystalNowIntent) {
		val tuner = intent.tuner
		val crystal = intent.crystal
		if (isTuned(crystal)) return
		crystal.setServerAttribute(ServerAttribute.LINK_OBJECT_ID, tuner.objectId) // In case the name of the character ever changes
		crystal.objectName = "\\#00FF00" + crystal.objectName + " (tuned)"
		val owner = tuner.owner
		if (owner != null) {
			SystemMessageIntent.broadcastPersonal(owner, "@jedi_spam:crystal_tune_success")
		}
	}

	private fun isTuned(crystal: TangibleObject): Boolean {
		return crystal.getServerAttribute(ServerAttribute.LINK_OBJECT_ID) != null
	}
}
