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

package com.projectswg.holocore.scripts.radial.terminal

import com.projectswg.common.data.radial.RadialItem
import com.projectswg.common.data.radial.RadialOption
import com.projectswg.holocore.resources.objects.SWGObject
import com.projectswg.holocore.resources.player.Player

static def getOptions(String type, List<RadialOption> options, Player player, SWGObject target) {
	switch (type) {
		case "bank":
			TerminalBankRadial.getOptions(options, player, target)
			break
		case "bazaar":
			TerminalBazaarRadial.getOptions(options, player, target)
			break
		case "character_builder":
			TerminalCharacterBuilderRadial.getOptions(options, player, target)
			break
		case "ticket":
			TerminalTicketRadial.getOptions(options, player, target)
			break
		case "ticket_collector":
			TerminalTicketCollectorRadial.getOptions(options, player, target)
			break
		case "travel":
			TerminalTravelRadial.getOptions(options, player, target)
			break
	}
}

static def handleSelection(String type, Player player, SWGObject target, RadialItem selection) {
	switch (type) {
		case "bank":
			TerminalBankRadial.handleSelection(player, target, selection)
			break
		case "bazaar":
			TerminalBazaarRadial.handleSelection(player, target, selection)
			break
		case "character_builder":
			TerminalCharacterBuilderRadial.handleSelection(player, target, selection)
			break
		case "ticket":
			TerminalTicketRadial.handleSelection(player, target, selection)
			break
		case "ticket_collector":
			TerminalTicketCollectorRadial.handleSelection(player, target, selection)
			break
		case "travel":
			TerminalTravelRadial.handleSelection(player, target, selection)
			break
	}
}
