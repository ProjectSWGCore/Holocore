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
package com.projectswg.holocore.services.support.objects.items

import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.intents.support.objects.CreateStaticItemIntent
import com.projectswg.holocore.intents.support.objects.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.objects.StaticItemCreator
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import me.joshlarson.jlcommon.log.Log
import java.util.*

class StaticItemService : Service() {
	
	@IntentHandler
	private fun handleCreateStaticItemIntent(csii: CreateStaticItemIntent) {
		val container = csii.container
		val itemNames = csii.itemNames
		val objectCreationHandler = csii.objectCreationHandler
		
		// If adding these items to the container would exceed the max capacity...
		if (!objectCreationHandler.isIgnoreVolume && container.containedObjects.size + itemNames.size > container.maxContainerSize) {
			objectCreationHandler.containerFull()
			return
		}
		
		val objects = ArrayList<SWGObject>()
		for (itemName in itemNames) {
			val obj = StaticItemCreator.createItem(itemName)
			if (obj != null) {
				objects.add(obj)
				obj.moveToContainer(container)
				ObjectCreatedIntent(obj).broadcast()
			} else {
				Log.d("%s could not be spawned because the item name is unknown", itemName)
				val requesterOwner = csii.requester.owner
				if (requesterOwner != null)
					SystemMessageIntent.broadcastPersonal(requesterOwner, String.format("%s could not be spawned because the item name is unknown", itemName))
			}
		}

		objectCreationHandler.success(objects)
	}

	interface ObjectCreationHandler {
		val isIgnoreVolume: Boolean
		fun success(createdObjects: List<SWGObject>)
		
		fun containerFull() {
			
		}
	}
	
	class SystemMessageHandler(private val receiver: CreatureObject) : ObjectCreationHandler {
		
		override val isIgnoreVolume: Boolean
			get() = true
		
		override fun success(createdObjects: List<SWGObject>) {
			for (createdObject in createdObjects) {
				val owner = receiver.owner

				if (owner != null) {
					SystemMessageIntent.broadcastPersonal(owner, "${createdObject.stringId} has been placed in your inventory.")
				}
			}
		}
		
	}
	
}
