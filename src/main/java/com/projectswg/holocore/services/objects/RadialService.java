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
package com.projectswg.holocore.services.objects;

import com.projectswg.common.control.Service;
import com.projectswg.common.data.radial.RadialItem;
import com.projectswg.common.data.radial.RadialOption;
import com.projectswg.common.debug.Log;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.ObjectMenuSelect;
import com.projectswg.common.network.packets.swg.zone.object_controller.ObjectMenuRequest;
import com.projectswg.common.network.packets.swg.zone.object_controller.ObjectMenuResponse;
import com.projectswg.holocore.intents.network.GalacticPacketIntent;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import com.projectswg.holocore.resources.player.Player;
import com.projectswg.holocore.scripts.radial.RadialHandler;
import com.projectswg.holocore.services.galaxy.GalacticManager;

import java.util.ArrayList;
import java.util.List;

public class RadialService extends Service {
	
	public RadialService() {
		registerForIntent(GalacticPacketIntent.class, this::handleGalacticPacketIntent);
	}
	
	@Override
	public boolean initialize() {
		RadialHandler.initialize();
		return super.initialize();
	}
	
	private void handleGalacticPacketIntent(GalacticPacketIntent gpi) {
		SWGPacket p = gpi.getPacket();
		if (p instanceof ObjectMenuRequest) {
			onRequest(gpi.getObjectManager(), (ObjectMenuRequest) p);
		} else if (p instanceof ObjectMenuSelect) {
			onSelection(gpi.getGalacticManager(), gpi.getPlayer(), (ObjectMenuSelect) p);
		}
	}
	
	private void onRequest(ObjectManager objectManager, ObjectMenuRequest request) {
		SWGObject requestor = objectManager.getObjectById(request.getRequestorId());
		SWGObject target = objectManager.getObjectById(request.getTargetId());
		if (target == null)
			return;
		if (!(requestor instanceof CreatureObject)) {
			Log.w("Requestor of target: %s is not a creature object! %s", target, requestor);
			return;
		}
		Player player = requestor.getOwner();
		if (player == null) {
			Log.w("Requestor of target: %s does not have an owner! %s", target, requestor);
			return;
		}
		
		List<RadialOption> options = new ArrayList<>();
		RadialHandler.getOptions(options, player, target);
		sendResponse(player, target, options, request.getCounter());
	}
	
	private void onSelection(GalacticManager galacticManager, Player player, ObjectMenuSelect select) {
		SWGObject target = galacticManager.getObjectManager().getObjectById(select.getObjectId());
		if (target == null) {
			Log.e("Selection target [%d] does not exist!", select.getObjectId());
			return;
		}
		if (player == null) {
			Log.e("Selection requestor does not exist! Target: [%d] %s", target.getObjectId(), target.getTemplate());
			return;
		}
		
		RadialItem selection = RadialItem.getFromId(select.getSelection());
		if (selection == null) {
			Log.e("RadialItem does not exist with selection id: %d", select.getSelection());
			return;
		}
		
		RadialHandler.handleSelection(player, target, selection);
	}
	
	private static void sendResponse(Player player, SWGObject target, List<RadialOption> options, int counter) {
		ObjectMenuResponse menuResponse = new ObjectMenuResponse(player.getCreatureObject().getObjectId());
		menuResponse.setTargetId(target.getObjectId());
		menuResponse.setRequestorId(player.getCreatureObject().getObjectId());
		menuResponse.setRadialOptions(options);
		menuResponse.setCounter(counter);
		player.sendPacket(menuResponse);
	}
	
}