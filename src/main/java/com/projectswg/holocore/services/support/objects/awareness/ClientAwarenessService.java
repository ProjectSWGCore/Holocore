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

package com.projectswg.holocore.services.support.objects.awareness;

import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.CmdSceneReady;
import com.projectswg.common.network.packets.swg.zone.object_controller.DataTransform;
import com.projectswg.common.network.packets.swg.zone.object_controller.DataTransformWithParent;
import com.projectswg.common.network.packets.swg.zone.object_controller.TeleportAck;
import com.projectswg.holocore.intents.gameplay.world.travel.pet.DismountIntent;
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent;
import com.projectswg.holocore.intents.support.objects.swg.MoveObjectIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectTeleportIntent;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.player.PlayerEvent;
import com.projectswg.holocore.resources.support.global.player.PlayerState;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState;
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.IntentMultiplexer;
import me.joshlarson.jlcommon.control.IntentMultiplexer.Multiplexer;
import me.joshlarson.jlcommon.control.Service;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This service handles awareness-related packets from the client
 */
public class ClientAwarenessService extends Service {
	
	private final IntentMultiplexer packetMultiplexer;
	private final Set<SWGObject> teleporting;
	
	public ClientAwarenessService() {
		this.packetMultiplexer = new IntentMultiplexer(this, Player.class, SWGPacket.class);
		this.teleporting = ConcurrentHashMap.newKeySet();
	}
	
	@IntentHandler
	private void handleObjectTeleportIntent(ObjectTeleportIntent oti) {
		@NotNull SWGObject obj = oti.getObject();
		
		if (obj instanceof CreatureObject && ((CreatureObject) obj).isLoggedInPlayer())
			teleporting.add(obj);
	}
	
	@IntentHandler
	private void processInboundPacketIntent(InboundPacketIntent gpi) {
		packetMultiplexer.call(gpi.getPlayer(), gpi.getPacket());
	}
	
	@Multiplexer
	private void handleCmdSceneReady(Player player, CmdSceneReady p) {
		teleporting.remove(player.getCreatureObject());
		
		assert player.getPlayerState() == PlayerState.ZONING_IN;
		player.setPlayerState(PlayerState.ZONED_IN);
		StandardLog.onPlayerEvent(this, player, "zoned in from %s", p.getSocketAddress());
		new PlayerEventIntent(player, PlayerEvent.PE_ZONE_IN_SERVER).broadcast();
		player.sendPacket(new CmdSceneReady());
	}
	
	@Multiplexer
	private void handleDataTransform(Player player, DataTransform dt) {
		CreatureObject creature = player.getCreatureObject();
		if (creature == null) {
			StandardLog.onPlayerError(this, player, "sent a DataTransform without being logged in");
			return;
		}
		if (creature.getObjectId() != dt.getObjectId()) {
			StandardLog.onPlayerError(this, player, "sent a DataTransform for another object [%d]", dt.getObjectId());
			return;
		}
		switch (creature.getPosture()) {
			case DEAD:
			case INCAPACITATED:
				return;
			default:
				break;
		}
		if (teleporting.contains(creature))
			return;
		
		Location requestedLocation = Location.builder(dt.getLocation()).setTerrain(creature.getTerrain()).build();
		double speed = dt.getSpeed();
		
		if (creature.isStatesBitmask(CreatureState.RIDING_MOUNT)) {
			// If this is the primary rider, move the mount and all of the riders
			CreatureObject mount = (CreatureObject) creature.getParent();
			assert mount != null && mount.isStatesBitmask(CreatureState.MOUNTED_CREATURE) : "invalid parent for riding mount";
			
			if (mount.getOwnerId() == creature.getObjectId()) {
				moveObjectWithTransform(mount, null, requestedLocation, speed);
				for (SWGObject child : mount.getSlottedObjects()) {
					moveObjectWithTransform(child, mount, requestedLocation, speed);
				}
			}
		} else {
			moveObjectWithTransform(creature, null, requestedLocation, dt.getSpeed());
		}
	}
	
	@Multiplexer
	private void handleDataTransformWithParent(Player player, DataTransformWithParent dt) {
		CreatureObject creature = player.getCreatureObject();
		if (creature == null) {
			StandardLog.onPlayerError(this, player, "sent a DataTransformWithParent without being logged in");
			return;
		}
		if (creature.getObjectId() != dt.getObjectId()) {
			StandardLog.onPlayerError(this, player, "sent a DataTransformWithParent for another object [%d]", dt.getObjectId());
			return;
		}
		switch (creature.getPosture()) {
			case DEAD:
			case INCAPACITATED:
				return;
			default:
				break;
		}
		SWGObject parent = ObjectLookup.getObjectById(dt.getCellId());
		if (parent == null) {
			StandardLog.onPlayerError(this, player, "sent a DataTransformWithParent with an unknown parent [%d]", dt.getCellId());
			return;
		}
		if (teleporting.contains(creature))
			return;
		
		if (creature.isStatesBitmask(CreatureState.RIDING_MOUNT)) {
			// If this is the primary rider, move the mount and all of the riders
			CreatureObject mount = (CreatureObject) creature.getParent();
			assert mount != null && mount.isStatesBitmask(CreatureState.MOUNTED_CREATURE) : "invalid parent for riding mount";
			mount.sendObservers(new DataTransform(mount.getObjectId(), 0, mount.getNextUpdateCount(), mount.getLocation(), 0));
			DismountIntent.broadcast(creature, mount);
		} else {
			Location requestedLocation = Location.builder(dt.getLocation()).setTerrain(creature.getTerrain()).build();
			moveObjectWithTransform(creature, parent, requestedLocation, dt.getSpeed());
		}
	}
	
	@Multiplexer
	private void handleTeleportAck(Player player, TeleportAck p) {
		teleporting.remove(player.getCreatureObject());
	}
	
	private static void moveObjectWithTransform(SWGObject obj, SWGObject parent, Location requestedLocation, double speed) {
		MoveObjectIntent.broadcast(obj, parent, requestedLocation, speed);
	}
	
}
