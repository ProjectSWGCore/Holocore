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

package com.projectswg.holocore.services.support.objects.items;

import com.projectswg.common.network.packets.swg.zone.ClientOpenContainerMessage;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent;
import com.projectswg.holocore.intents.support.objects.items.CloseContainerIntent;
import com.projectswg.holocore.intents.support.objects.items.OpenContainerIntent;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

public class ContainerService extends Service {
	
	public ContainerService() {
		
	}
	
	@IntentHandler
	private void handlePlayerEventIntent(PlayerEventIntent pei) {
		CreatureObject creature = pei.getPlayer().getCreatureObject();
		if (creature == null)
			return;
		
		closeAllContainers(creature);
	}
	
	@IntentHandler
	private void handleOpenContainerIntent(OpenContainerIntent oci) {
		openContainer(oci.getCreature(), oci.getContainer(), oci.getSlot());
	}
	
	@IntentHandler
	private void handleCloseContainerIntent(CloseContainerIntent cci) {
		closeContainer(cci.getCreature(), cci.getContainer(), cci.getSlot());
	}
	
	private void openContainer(CreatureObject creature, SWGObject container, String slot) {
		if (!container.isVisible(creature)) {
			Player player = creature.getOwner();
			if (player != null)
				SystemMessageIntent.broadcastPersonal(player, "@container_error_message:container08");
			return;
		}
		if (creature.openContainer(container, slot))
			creature.sendSelf(new ClientOpenContainerMessage(container.getObjectId(), slot));
	}
	
	private void closeContainer(CreatureObject creature, SWGObject container, String slot) {
		creature.closeContainer(container, slot);
	}
	
	private void closeAllContainers(CreatureObject creature) {
		creature.closeAllContainers();
	}
	
}
