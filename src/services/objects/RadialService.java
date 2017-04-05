/************************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
 *                                                                                  *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
 * Our goal is to create an emulator which will provide a server for players to     *
 * continue playing a game similar to the one they used to play. We are basing      *
 * it on the final publish of the game prior to end-game events.                    *
 *                                                                                  *
 * This file is part of Holocore.                                                   *
 *                                                                                  *
 * -------------------------------------------------------------------------------- *
 *                                                                                  *
 * Holocore is free software: you can redistribute it and/or modify                 *
 * it under the terms of the GNU Affero General Public License as                   *
 * published by the Free Software Foundation, either version 3 of the               *
 * License, or (at your option) any later version.                                  *
 *                                                                                  *
 * Holocore is distributed in the hope that it will be useful,                      *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
 * GNU Affero General Public License for more details.                              *
 *                                                                                  *
 * You should have received a copy of the GNU Affero General Public License         *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
 *                                                                                  *
 ***********************************************************************************/
package services.objects;

import intents.network.GalacticPacketIntent;
import intents.radial.RadialRegisterIntent;
import intents.radial.RadialRequestIntent;
import intents.radial.RadialResponseIntent;
import intents.radial.RadialSelectionIntent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import network.packets.Packet;
import network.packets.swg.zone.ObjectMenuSelect;
import network.packets.swg.zone.object_controller.ObjectMenuRequest;
import network.packets.swg.zone.object_controller.ObjectMenuResponse;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
import resources.radial.RadialItem;
import resources.radial.RadialOption;
import resources.server_info.Log;
import services.galaxy.GalacticManager;

import com.projectswg.common.control.Service;

public class RadialService extends Service {

	private final Set<String> templatesRegistered;

	public RadialService() {
		templatesRegistered = new HashSet<>();

		registerForIntent(GalacticPacketIntent.class, gpi -> handleGalacticPacketIntent(gpi));
		registerForIntent(RadialResponseIntent.class, rri -> handleRadialResponseIntent(rri));
		registerForIntent(RadialRegisterIntent.class, rri -> handleRadialRegisterIntent(rri));
	}

	private void handleGalacticPacketIntent(GalacticPacketIntent gpi){
		Packet p = gpi.getPacket();
		if (p instanceof ObjectMenuRequest) {
			onRequest(gpi.getObjectManager(), (ObjectMenuRequest) p);
		} else if (p instanceof ObjectMenuSelect) {
			onSelection(gpi.getGalacticManager(), gpi.getPlayer(), (ObjectMenuSelect) p);
		}
	}
	
	private void handleRadialRegisterIntent(RadialRegisterIntent rri){
		synchronized (templatesRegistered) {
			if (rri.isRegister()) {
				templatesRegistered.addAll(rri.getTemplates());
			} else {
				templatesRegistered.removeAll(rri.getTemplates());
			}
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

		new RadialRequestIntent(player, target, request).broadcast();
	}

	private void handleRadialResponseIntent(RadialResponseIntent rri) {
		Player player = rri.getPlayer();
		sendResponse(player, rri.getTarget(), rri.getOptions(), rri.getCounter());
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
		new RadialSelectionIntent(player, target, selection).broadcast();
	}

	private void sendResponse(Player player, SWGObject target, List<RadialOption> options, int counter) {
		ObjectMenuResponse menuResponse = new ObjectMenuResponse(player.getCreatureObject().getObjectId());
		menuResponse.setTargetId(target.getObjectId());
		menuResponse.setRequestorId(player.getCreatureObject().getObjectId());
		menuResponse.setRadialOptions(options);
		menuResponse.setCounter(counter);
		player.sendPacket(menuResponse);
	}

}
