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
package com.projectswg.holocore.services.gameplay.items

import com.projectswg.common.data.sui.SuiEvent
import com.projectswg.holocore.intents.gameplay.items.BioLinkNowIntent
import com.projectswg.holocore.intents.gameplay.items.RequestBioLinkIntent
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.resources.support.global.zone.sui.SuiButtons
import com.projectswg.holocore.resources.support.global.zone.sui.SuiMessageBox
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service

class BioLinkService : Service() {

	@IntentHandler
	private fun handleRequestBioLinkIntent(intent: RequestBioLinkIntent) {
		val linker = intent.linker
		val item = intent.item
		if (!isLinkable(item)) return
		val owner = linker.owner ?: return

		if (item.parent !== linker.inventory) {
			SystemMessageIntent.broadcastPersonal(owner, "@base_player:must_bio_link_from_inventory")
			return
		}

		SuiMessageBox().run {
			title = "@sui:bio_link_item_title"
			prompt = "@sui:bio_link_item_prompt"
			buttons = SuiButtons.YES_NO
			addOkButtonCallback("biolink") { _: SuiEvent, _: Map<String, String> -> BioLinkNowIntent(linker, item).broadcast() }
			display(owner)
		}
	}

	@IntentHandler
	private fun handleBioLinkNowIntent(intent: BioLinkNowIntent) {
		val linker = intent.linker
		val item = intent.item
		if (!isLinkable(item)) return

		item.setBioLinkedTo(linker.objectId)	// In case the name of the character ever changes
		val owner = linker.owner ?: return
		SystemMessageIntent.broadcastPersonal(owner, "@base_player:item_bio_linked")
	}

	private fun isLinkable(item: TangibleObject): Boolean {
		return item.isBioLinkRequired && item.bioLinkedTo == null
	}
}
