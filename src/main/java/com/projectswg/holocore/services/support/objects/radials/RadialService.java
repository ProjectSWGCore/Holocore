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
package com.projectswg.holocore.services.support.objects.radials;

import com.projectswg.common.data.radial.RadialItem;
import com.projectswg.common.data.radial.RadialOption;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.ObjectMenuSelect;
import com.projectswg.common.network.packets.swg.zone.object_controller.ObjectMenuRequest;
import com.projectswg.common.network.packets.swg.zone.object_controller.ObjectMenuResponse;
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.radial.RadialHandler;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.util.*;

public class RadialService extends Service {
	
	public RadialService() {
		
	}
	
	@IntentHandler
	private void handleInboundPacketIntent(InboundPacketIntent gpi) {
		SWGPacket p = gpi.getPacket();
		if (p instanceof ObjectMenuRequest) {
			onRequest((ObjectMenuRequest) p);
		} else if (p instanceof ObjectMenuSelect) {
			onSelection(gpi.getPlayer(), (ObjectMenuSelect) p);
		}
	}
	
	private void onRequest(ObjectMenuRequest request) {
		SWGObject requestor = ObjectLookup.getObjectById(request.getRequestorId());
		SWGObject target = ObjectLookup.getObjectById(request.getTargetId());
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
		
		StandardLog.onPlayerTrace(this, player, "requested radials for %s", target);
		LinkedHashMap<RadialItem, RadialOption> options = new LinkedHashMap<>();
		for (RadialOption option : request.getOptions())
			options.put(option.getType(), option);
		
		{ // Load server radials
			List<RadialOption> serverOptions = new ArrayList<>();
			RadialHandler.INSTANCE.getOptions(serverOptions, player, target);
			for (RadialOption option : serverOptions)
				options.put(option.getType(), option);
		}
		
		sendResponse(player, target, options.values(), request.getCounter());
	}
	
	private void onSelection(Player player, ObjectMenuSelect select) {
		SWGObject target = ObjectLookup.getObjectById(select.getObjectId());
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
		
		StandardLog.onPlayerTrace(this, player, "selected radial %s for %s", selection, target);
		RadialHandler.INSTANCE.handleSelection(player, target, selection);
	}
	
	private static void sendResponse(Player player, SWGObject target, Collection<RadialOption> options, int counter) {
		ObjectMenuResponse menuResponse = new ObjectMenuResponse(player.getCreatureObject().getObjectId());
		menuResponse.setTargetId(target.getObjectId());
		menuResponse.setRequestorId(player.getCreatureObject().getObjectId());
		menuResponse.setRadialOptions(new ArrayList<>(options));
		menuResponse.setCounter(counter);
		player.sendPacket(menuResponse);
	}
	
}
