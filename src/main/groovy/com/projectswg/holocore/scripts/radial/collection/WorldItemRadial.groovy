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

package com.projectswg.holocore.scripts.radial.collection

import com.projectswg.common.data.radial.RadialItem
import com.projectswg.common.data.radial.RadialOption
import com.projectswg.holocore.intents.gameplay.player.collections.GrantClickyCollectionIntent
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.gameplay.player.collections.CollectionItem
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.scripts.radial.RadialHandlerInterface

class WorldItemRadial implements RadialHandlerInterface {
	
	private final CollectionItem details
	
	WorldItemRadial(CollectionItem details) {
		this.details = details
	}
	
	def getOptions(List<RadialOption> options, Player player, SWGObject target) {
		def use = null
		for (RadialOption option : options) {
			if (option.getOptionType() == RadialItem.ITEM_USE.getId()) {
				use = option
				break
			}
		}
		if (use == null) {
			use = new RadialOption(RadialItem.ITEM_USE)
			options.add(0, use)
		}
		use.setOverriddenText("@collection:consume_item")
	}
	
	def handleSelection(Player player, SWGObject target, RadialItem selection) {
		if (selection != RadialItem.ITEM_USE)
			return
		
		new GrantClickyCollectionIntent(player.getCreatureObject(), target, details).broadcast();
	}
	
}

