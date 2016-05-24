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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import resources.control.Intent;
import resources.control.Service;
import network.packets.Packet;
import network.packets.swg.zone.ObjectMenuSelect;
import network.packets.swg.zone.object_controller.IntendedTarget;
import network.packets.swg.zone.object_controller.ObjectMenuRequest;
import network.packets.swg.zone.object_controller.ObjectMenuResponse;
import intents.network.GalacticPacketIntent;
import intents.radial.ObjectClickedIntent;
import intents.radial.RadialRegisterIntent;
import intents.radial.RadialRequestIntent;
import intents.radial.RadialResponseIntent;
import intents.radial.RadialSelectionIntent;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
import resources.radial.RadialItem;
import resources.radial.RadialOption;
import resources.server_info.Log;
import services.galaxy.GalacticManager;

public class RadialService extends Service {
	
	private final Set<String> templatesRegistered;
	
	public RadialService() {
		templatesRegistered = new HashSet<>();
		
		registerForIntent(GalacticPacketIntent.TYPE);
		registerForIntent(RadialResponseIntent.TYPE);
		registerForIntent(RadialRegisterIntent.TYPE);
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof GalacticPacketIntent) {
			GalacticPacketIntent gpi = (GalacticPacketIntent) i;
			Packet p = gpi.getPacket();
			if (p instanceof ObjectMenuRequest) {
				onRequest(gpi.getObjectManager(), (ObjectMenuRequest) p);
			} else if (p instanceof ObjectMenuSelect) {
				onSelection(gpi.getGalacticManager(), gpi.getNetworkId(), (ObjectMenuSelect) p);
			} else if (p instanceof IntendedTarget) {
				onObjectClicked(gpi.getObjectManager(), (IntendedTarget) p);
			}
		} else if (i instanceof RadialResponseIntent) {
			onResponse((RadialResponseIntent) i);
		} else if (i instanceof RadialRegisterIntent) {
			synchronized (templatesRegistered) {
				if (((RadialRegisterIntent) i).isRegister()) {
					templatesRegistered.addAll(((RadialRegisterIntent) i).getTemplates());
				} else {
					templatesRegistered.removeAll(((RadialRegisterIntent) i).getTemplates());
				}
			}
		}
	}
	
	private void onRequest(ObjectManager objectManager, ObjectMenuRequest request) {
		SWGObject requestor = objectManager.getObjectById(request.getRequestorId());
		SWGObject target = objectManager.getObjectById(request.getTargetId());
		if (target == null)
			return;
		if (!(requestor instanceof CreatureObject)) {
			Log.w("RadialService", "Requestor of target: %s is not a creature object! %s", target, requestor);
			return;
		}
		Player player = requestor.getOwner();
		if (player == null) {
			Log.w("RadialService", "Requestor of target: %s does not have an owner! %s", target, requestor);
			return;
		}
		synchronized (templatesRegistered) {
			if (templatesRegistered.contains(target.getTemplate())) {
				new RadialRequestIntent(player, target, request).broadcast();
			} else {
				sendResponse(player, target, request.getOptions(), request.getCounter());
			}
		}
	}
	
	private void onResponse(RadialResponseIntent response) {
		Player player = response.getPlayer();
		sendResponse(player, response.getTarget(), response.getOptions(), response.getCounter());
	}
	
	private void onSelection(GalacticManager galacticManager, long networkId, ObjectMenuSelect select) {
		Player player = galacticManager.getPlayerManager().getPlayerFromNetworkId(networkId);
		SWGObject target = galacticManager.getObjectManager().getObjectById(select.getObjectId());
		if (target == null) {
			Log.e("RadialService", "Selection target [%d] does not exist!", select.getObjectId());
			return;
		}
		if (player == null) {
			Log.e("RadialService", "Selection requestor does not exist! Target: [%d] %s", target.getObjectId(), target.getTemplate());
			return;
		}
		RadialItem selection = RadialItem.getFromId(select.getSelection());
		if (selection == null) {
			Log.e("RadialService", "RadialItem does not exist with selection id: %d", select.getSelection());
			return;
		}
		new RadialSelectionIntent(player, target, selection).broadcast();
	}
	
	private void onObjectClicked(ObjectManager objectManager, IntendedTarget it) {
		SWGObject requestor = objectManager.getObjectById(it.getObjectId());
		SWGObject target = objectManager.getObjectById(it.getTargetId());
		if (target == null)
			return;
		if (!(requestor instanceof CreatureObject)) {
			Log.w("RadialService", "Requestor of target: %s is not a creature object! %s", target, requestor);
			return;
		}
		Player player = requestor.getOwner();
		if (player == null) {
			Log.w("RadialService", "Requestor of target: %s does not have an owner! %s", target, requestor);
			return;
		}
		synchronized (templatesRegistered) {
			if (templatesRegistered.contains(target.getTemplate())) {
				new ObjectClickedIntent((CreatureObject) requestor, target).broadcast();
			}
		}
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
