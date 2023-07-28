/***********************************************************************************
 * Copyright (c) 2023 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.services.gameplay.crafting.resource

import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.GalacticResource
import com.projectswg.holocore.resources.gameplay.crafting.resource.raw.RawResource
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData.rawResources
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.permissions.ContainerResult
import com.projectswg.holocore.resources.support.objects.permissions.ReadWritePermissions
import com.projectswg.holocore.resources.support.objects.swg.ServerAttribute
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.resource.ResourceContainerObject
import me.joshlarson.jlcommon.control.IntentChain

object ResourceContainerHelper {
	fun giveResourcesToPlayer(amount: Int, resource: GalacticResource, player: Player, resourceContainerEventHandler: ResourceContainerEventHandler) {
		val resourceObject = getOrCreateResourceObject(player.creatureObject, resource, resourceContainerEventHandler)
		if (resourceObject != null) {
			resourceObject.quantity = resourceObject.quantity + amount
			resourceContainerEventHandler.onSuccess()
		}
	}

	private fun getOrCreateResourceObject(creature: CreatureObject, resource: GalacticResource, resourceContainerEventHandler: ResourceContainerEventHandler): ResourceContainerObject? {
		val inventory = creature.inventory
		val rawResource = rawResources.getResource(resource.rawResourceId)
		return inventory.containedObjects.stream()
			.filter { swgObject -> swgObject is ResourceContainerObject }
			.map { swgObject -> swgObject as ResourceContainerObject }
			.filter { resourceContainerObject -> resource.rawResourceId == resourceContainerObject.resourceType }
			.findAny()
			.orElseGet { createResourceObject(creature, rawResource, resource, resourceContainerEventHandler) }
	}

	private fun createResourceObject(creature: CreatureObject, rawResource: RawResource, resource: GalacticResource, resourceContainerEventHandler: ResourceContainerEventHandler): ResourceContainerObject? {
		val resourceObject = ObjectCreator.createObjectFromTemplate(rawResource.crateTemplate) as ResourceContainerObject
		resourceObject.volume = 1
		resourceObject.parentName = StringId("resource/resource_names", rawResource.parent.name).toString()
		resourceObject.resourceType = resource.rawResourceId
		resourceObject.resourceName = resource.name
		resourceObject.objectName = resource.name
		resourceObject.stats = resource.stats
		resourceObject.setServerAttribute(ServerAttribute.GALACTIC_RESOURCE_ID, resource.id)
		resourceObject.containerPermissions = ReadWritePermissions.from(creature)
		when (resourceObject.moveToContainer(creature, creature.inventory)) {
			ContainerResult.CONTAINER_FULL -> {
				IntentChain.broadcastChain(ObjectCreatedIntent(resourceObject), DestroyObjectIntent(resourceObject))
				resourceContainerEventHandler.onInventoryFull()
				return null
			}

			ContainerResult.SUCCESS        -> ObjectCreatedIntent.broadcast(resourceObject)

			else                           -> {
				resourceContainerEventHandler.onUnknownError()
				IntentChain.broadcastChain(ObjectCreatedIntent(resourceObject), DestroyObjectIntent(resourceObject))
				return null
			}
		}
		return resourceObject
	}

}