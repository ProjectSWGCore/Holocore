/***********************************************************************************
 * Copyright (c) 2022 /// Project SWG /// www.projectswg.com                       *
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

/***********************************************************************************
 * Copyright (c) 2022 /// Project SWG /// www.projectswg.com                       *
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

package com.projectswg.holocore.services.gameplay.structures

import com.projectswg.common.network.packets.swg.zone.structures.EnterStructurePlacementModeMessage
import com.projectswg.holocore.intents.gameplay.structures.UseStructureDeedIntent
import com.projectswg.holocore.intents.gameplay.structures.PlaceStructureIntent
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.ServerAttribute
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import me.joshlarson.jlcommon.control.IntentChain
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import me.joshlarson.jlcommon.log.Log

class StructureService : Service() {
	
	@IntentHandler
	private fun handleUseStructureDeedIntent(usdi: UseStructureDeedIntent) {
		val deed = getTemplateForDeed(usdi.deed)
		if (deed == null) {
			StandardLog.onPlayerError(this, usdi.creature, "Attempted to use invalid deed: %s", usdi.deed)
			DestroyObjectIntent.broadcast(usdi.deed)
			return
		}
		StandardLog.onPlayerTrace(this, usdi.creature, "Entering structure placement for deed %s", usdi.deed)
		usdi.creature.owner?.sendPacket(EnterStructurePlacementModeMessage(usdi.deed.objectId, deed))
	}
	
	@IntentHandler
	private fun handlePlaceStructureIntent(psi: PlaceStructureIntent) {
		val template = getTemplateForDeed(psi.deed)
		val structure = ObjectCreator.createObjectFromTemplate(template)
		if (structure is BuildingObject)
			structure.populateCells()
		structure.systemMove(null, psi.location)
		
		val intentChain = IntentChain()
		intentChain.broadcastAfter(ObjectCreatedIntent(structure))
		for (child in structure.childObjects) {
			intentChain.broadcastAfter(ObjectCreatedIntent(child))
		}
		
		Log.i("Place down structure at %s", psi.location)
	}
	
	private fun getTemplateForDeed(deed: TangibleObject): String? {
		return deed.getServerTextAttribute(ServerAttribute.DEED_GEN_TEMPLATE) ?: return null
	}
	
}
