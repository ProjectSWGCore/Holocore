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

package com.projectswg.holocore.scripts.commands.admin

import com.projectswg.holocore.intents.chat.SystemMessageIntent
import com.projectswg.holocore.intents.object.CreateStaticItemIntent
import com.projectswg.holocore.resources.containers.ContainerPermissionsType
import com.projectswg.holocore.resources.objects.SWGObject
import com.projectswg.holocore.resources.player.Player
import com.projectswg.holocore.services.galaxy.GalacticManager
import com.projectswg.holocore.services.objects.StaticItemService

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
	def creature = player.getCreatureObject()
	def inventory = creature.getSlottedObject("inventory")
	
	new CreateStaticItemIntent(creature, inventory, new StaticItemService.ObjectCreationHandler() {
		@Override
		void success(SWGObject[] createdObjects) {
			new SystemMessageIntent(player, "@system_msg:give_item_success").broadcast()
		}
		
		@Override
		void containerFull() {
			new SystemMessageIntent(player, "@system_msg:give_item_failure").broadcast()
		}
		
		@Override
		boolean isIgnoreVolume() {
			return true	// This is an admin command - coontainer restrictions is for peasants!
		}
	}, ContainerPermissionsType.DEFAULT, args).broadcast()
}
