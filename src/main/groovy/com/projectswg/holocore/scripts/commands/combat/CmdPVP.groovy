/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
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

package com.projectswg.holocore.scripts.commands.combat

import com.projectswg.common.data.encodables.tangible.PvpFaction
import com.projectswg.holocore.intents.gameplay.gcw.faction.FactionIntent
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.utilities.IntentFactory


static def execute(Player player, SWGObject target, String args) {
	def creature = player.getCreatureObject()
	def intent = null
	
	// Ziggy was using this to test until recruiters are enabled.
	
	if (!args.isEmpty()) {
		if (args.indexOf("imperial") > -1) {
			intent = new FactionIntent(creature, PvpFaction.IMPERIAL)
		} else if (args.indexOf("rebel") > -1) {
			intent = new FactionIntent(creature, PvpFaction.REBEL)
		} else {
			intent = new FactionIntent(creature, PvpFaction.NEUTRAL)
		}
		intent.broadcast()
	} else if (creature.getPvpFaction() != PvpFaction.NEUTRAL) {
		intent = new FactionIntent(creature, FactionIntent.FactionIntentType.SWITCHUPDATE)
		intent.broadcast()
	} else {
		IntentFactory.sendSystemMessage(player, "@faction_recruiter:not_aligned")
	}
}
