/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 * *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 * *
 * This file is part of Holocore.                                                  *
 * *
 * --------------------------------------------------------------------------------*
 * *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 * *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 * *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http:></http:>//www.gnu.org/licenses/>.               *
 */
package com.projectswg.holocore.services.support.objects.items

import com.projectswg.common.network.packets.swg.zone.object_controller.ShowLootBox
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.intents.support.objects.items.CreateStaticItemIntent
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.objects.StaticItemCreator
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import kotlinx.coroutines.*
import me.joshlarson.jlcommon.control.Intent
import me.joshlarson.jlcommon.control.IntentChain
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import me.joshlarson.jlcommon.log.Log
import java.util.*

class StaticItemService : Service() {
	
	private val scope = CoroutineScope(Dispatchers.Default)
	
	override fun stop(): Boolean {
		scope.coroutineContext.cancel()
		return true
	}
	
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
		
		val intentChain = IntentChain()
		val objects = ArrayList<SWGObject>()
		for (itemName in itemNames) {
			val obj = StaticItemCreator.createItem(itemName)
			if (obj != null) {
				objects.add(obj)
				obj.moveToContainer(container)
				intentChain.broadcastAfter(ObjectCreatedIntent(obj))
			} else {
				Log.d("%s could not be spawned because the item name is unknown", itemName)
				val requesterOwner = csii.requester.owner
				if (requesterOwner != null)
					SystemMessageIntent.broadcastPersonal(requesterOwner, String.format("%s could not be spawned because the item name is unknown", itemName))
			}
		}
		
		intentChain.broadcastAfter(CompletedStaticItemCreatedCallbacks(objects, objectCreationHandler::success))
	}
	
	@IntentHandler
	private fun handleCompletedStaticItemCreatedCallbacks(csicc: CompletedStaticItemCreatedCallbacks) {
		scope.launch {
			delay(60)
			csicc.objectHandler(csicc.objects)
		}
	}
	
	interface ObjectCreationHandler {
		val isIgnoreVolume: Boolean
		fun success(createdObjects: List<SWGObject>)
		
		fun containerFull() {
			
		}
	}
	
	class LootBoxHandler(private val receiver: CreatureObject) : ObjectCreationHandler {
		
		override val isIgnoreVolume: Boolean
			get() = true
		
		override fun success(createdObjects: List<SWGObject>) {
			val objectIds = LongArray(createdObjects.size)
			
			for (i in objectIds.indices) {
				objectIds[i] = createdObjects[i].objectId
			}
			
			receiver.sendSelf(ShowLootBox(receiver.objectId, objectIds))
		}
		
	}
	
	private data class CompletedStaticItemCreatedCallbacks(val objects: List<SWGObject>, val objectHandler: (createdObjects: List<SWGObject>) -> Unit): Intent()
}
