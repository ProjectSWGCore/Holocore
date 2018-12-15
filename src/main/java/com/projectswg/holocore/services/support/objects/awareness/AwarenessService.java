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

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.network.packets.swg.zone.*;
import com.projectswg.common.network.packets.swg.zone.chat.ChatOnConnectAvatar;
import com.projectswg.common.network.packets.swg.zone.chat.VoiceChatStatus;
import com.projectswg.common.network.packets.swg.zone.insertion.ChatServerStatus;
import com.projectswg.common.network.packets.swg.zone.insertion.CmdStartScene;
import com.projectswg.common.network.packets.swg.zone.object_controller.DataTransform;
import com.projectswg.common.network.packets.swg.zone.object_controller.DataTransformWithParent;
import com.projectswg.holocore.ProjectSWG;
import com.projectswg.holocore.intents.support.global.network.CloseConnectionIntent;
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent;
import com.projectswg.holocore.intents.support.global.zone.PlayerTransformedIntent;
import com.projectswg.holocore.intents.support.global.zone.RequestZoneInIntent;
import com.projectswg.holocore.intents.support.objects.awareness.ForceAwarenessUpdateIntent;
import com.projectswg.holocore.intents.support.objects.swg.*;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.global.network.DisconnectReason;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.player.PlayerEvent;
import com.projectswg.holocore.resources.support.global.player.PlayerState;
import com.projectswg.holocore.resources.support.objects.awareness.ObjectAwareness;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.Intent;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AwarenessService extends Service {
	
	private static final Location GONE_LOCATION = Location.builder().setTerrain(Terrain.GONE).setPosition(0, 0, 0).build();
	
	private final ObjectAwareness awareness;
	private final ScheduledThreadPool chunkUpdater;
	private final BlockingQueue<Runnable> positionUpdates;
	
	public AwarenessService() {
		this.awareness = new ObjectAwareness();
		this.chunkUpdater = new ScheduledThreadPool(1, 8, "awareness-chunk-updater");
		this.positionUpdates = new LinkedBlockingQueue<>();
	}
	
	@Override
	public boolean initialize() {
		chunkUpdater.start();
		chunkUpdater.executeWithFixedDelay(0, 100, this::update);
		return true;
	}
	
	@Override
	public boolean terminate() {
		chunkUpdater.stop();
		return chunkUpdater.awaitTermination(1000);
	}
	
	public void update() {
		awareness.updateChunks();
		while (!positionUpdates.isEmpty()) {
			Runnable r = positionUpdates.poll();
			r.run();
		}
	}
	
	@IntentHandler
	private void handlePlayerEventIntent(PlayerEventIntent pei) {
		Player p = pei.getPlayer();
		CreatureObject creature = p.getCreatureObject();
		
		switch (pei.getEvent()) {
			case PE_DESTROYED:
				assert creature != null;
				creature.setOwner(null);
				awareness.destroyObject(creature);
				break;
			case PE_LOGGED_OUT:
				assert creature != null;
				awareness.updateObject(creature);
				break;
			default:
				break;
		}
	}
	
	@IntentHandler
	private void handleObjectCreatedIntent(ObjectCreatedIntent oci) {
		awareness.createObject(oci.getObject());
	}
	
	@IntentHandler
	private void handleDestroyObjectIntent(DestroyObjectIntent doi) {
		@NotNull SWGObject obj = doi.getObject();
		
		obj.systemMove(null, GONE_LOCATION);
		awareness.destroyObject(doi.getObject());
	}
	
	@IntentHandler
	private void handleObjectTeleportIntent(ObjectTeleportIntent oti) {
		@NotNull SWGObject obj = oti.getObject();
		@Nullable SWGObject oldParent = oti.getOldParent();
		@Nullable SWGObject newParent = oti.getNewParent();
		@NotNull Location oldLocation = oti.getOldLocation();
		@NotNull Location newLocation = oti.getNewLocation();
		
		if (isPlayerZoneInRequired(obj, oldLocation, newLocation)) {
			assert obj instanceof CreatureObject;
			handleZoneIn((CreatureObject) obj, newLocation, newParent);
			return;
		}
		
		{
			Location newWorldLocation = newLocation;
			if (newParent != null)
				newWorldLocation = Location.builder(newWorldLocation).translateLocation(newParent.getWorldLocation()).build();
			if (obj instanceof CreatureObject)
				((CreatureObject) obj).setMovementPercent(0);
			obj.sendSelf(new DataTransform(obj.getObjectId(), 0, obj.getNextUpdateCount(), newWorldLocation, 0));
		}
		awareness.updateObject(obj);
		sendObjectUpdates(obj, oldParent, newParent, oldLocation, newLocation, true, 0);
		if (obj instanceof CreatureObject)
			positionUpdates.add(() -> ((CreatureObject) obj).setMovementPercent(1));
	}
	
	@IntentHandler
	private void handleMoveObjectIntent(MoveObjectIntent moi) {
		moveObjectWithTransform(moi.getObject(), moi.getParent(), moi.getNewLocation(), moi.getSpeed());
	}
	
	@IntentHandler
	private void handleContainerTransferIntent(ContainerTransferIntent cti) {
		@NotNull SWGObject obj = cti.getObject();
		@Nullable SWGObject oldContainer = cti.getOldContainer();
		@Nullable SWGObject newContainer = cti.getContainer();
		
		awareness.updateObject(cti.getObject());
		sendObjectUpdates(obj, oldContainer, newContainer, obj.getLocation(), obj.getLocation(), false, 0);
	}
	
	@IntentHandler
	private void handleForceAwarenessUpdateIntent(ForceAwarenessUpdateIntent faui) {
		awareness.updateObject(faui.getObject());
	}
	
	@IntentHandler
	private void handleRequestZoneInIntent(RequestZoneInIntent rzii) {
		handleZoneIn(rzii.getCreature(), rzii.getCreature().getLocation(), rzii.getCreature().getParent());
	}
	
	private void handleZoneIn(CreatureObject creature, Location loc, SWGObject parent) {
		@NotNull Player player = Objects.requireNonNull(creature.getOwner(), "Player zoning in without an owner");
		@NotNull PlayerState state = player.getPlayerState();
		
		// Fresh login or teleport/travel
		boolean firstZone = (state == PlayerState.LOGGED_IN);
		if (!firstZone && state != PlayerState.ZONED_IN) {
			CloseConnectionIntent.broadcast(player, DisconnectReason.SUSPECTED_HACK);
			return;
		}
		
		@Nullable SWGObject oldParent = creature.getParent();
		@NotNull Location oldLocation = creature.getLocation();
		
		creature.systemMove(parent, loc);
		creature.resetObjectsAware();
		startZone(player, creature, firstZone);
		awareness.updateObject(creature);
		sendObjectUpdates(creature, oldParent, parent, oldLocation, loc, false, 0);
	}
	
	private void startZone(Player player, CreatureObject creature, boolean firstZone) {
		@NotNull Location loc = creature.getWorldLocation();
		
		player.setPlayerState(PlayerState.ZONING_IN);
		Intent firstZoneIntent = null;
		if (firstZone) {
			player.sendPacket(new HeartBeat());
			player.sendPacket(new ChatServerStatus(true));
			player.sendPacket(new VoiceChatStatus());
			player.sendPacket(new ParametersMessage());
			player.sendPacket(new ChatOnConnectAvatar());
			firstZoneIntent = new PlayerEventIntent(player, PlayerEvent.PE_FIRST_ZONE);
			firstZoneIntent.broadcast();
		}
		player.sendPacket(new CmdStartScene(true, creature.getObjectId(), creature.getRace(), loc, ProjectSWG.getGalacticTime(), (int) (System.currentTimeMillis() / 1E3)));
		StandardLog.onPlayerEvent(this, player, "zoning in at %s %s", loc.getTerrain(), loc.getPosition());
		new PlayerEventIntent(player, PlayerEvent.PE_ZONE_IN_CLIENT).broadcastAfterIntent(firstZoneIntent);
	}
	
	private void moveObjectWithTransform(SWGObject obj, SWGObject parent, Location requestedLocation, double speed) {
		@Nullable SWGObject oldParent = obj.getParent();
		@NotNull Location oldLocation = obj.getLocation();
		
		obj.systemMove(parent, requestedLocation);
		awareness.updateObject(obj);
		sendObjectUpdates(obj, oldParent, parent, oldLocation, requestedLocation, false, speed);
	}
	
	private static boolean isPlayerZoneInRequired(@NotNull SWGObject obj, @NotNull Location oldLocation, @NotNull Location newLocation) {
		if (!(obj instanceof CreatureObject))
			return false;
		if (!((CreatureObject) obj).isLoggedInPlayer())
			return false;
		return !oldLocation.getTerrain().getFile().equals(newLocation.getTerrain().getFile());
	}
	
	private void sendObjectUpdates(@NotNull SWGObject obj, @Nullable SWGObject oldParent, @Nullable SWGObject newParent, @NotNull Location oldLocation, @NotNull Location newLocation, boolean forceSelfUpdate, double speed) {
		positionUpdates.add(() -> onObjectMoved(obj, oldParent, newParent, oldLocation, newLocation, forceSelfUpdate, speed));
		positionUpdates.add(obj::onObjectMoved);
	}
	
	private static void onObjectMoved(@NotNull SWGObject obj, @Nullable SWGObject oldParent, @Nullable SWGObject newParent, @NotNull Location oldLocation, @NotNull Location newLocation, boolean forceSelfUpdate, double speed) {
		if (obj instanceof CreatureObject && ((CreatureObject) obj).isLoggedInPlayer())
			new PlayerTransformedIntent((CreatureObject) obj, oldParent, newParent, oldLocation, newLocation).broadcast();
		
		if (newParent != null) {
			onObjectMovedInParent(obj, oldParent, newParent, oldLocation, newLocation, forceSelfUpdate, speed);
		} else {
			onObjectMovedInWorld(obj, oldParent, oldLocation, newLocation, forceSelfUpdate, speed);
		}
	}
	
	private static void onObjectMovedInParent(@NotNull SWGObject obj, @Nullable SWGObject oldParent, @NotNull SWGObject newParent, @NotNull Location oldLocation, @NotNull Location newLocation, boolean forceSelfUpdate, double speed) {
		if (oldParent != newParent)
			obj.sendObservers(new UpdateContainmentMessage(obj.getObjectId(), newParent.getObjectId(), obj.getSlotArrangement()));
		
		// Slotted objects don't get position updates - they inherit their parent's location, plus a client-defined offset (e.g. armor, mounts)
		if (obj.getSlotArrangement() == -1) {
			int counter = obj.getNextUpdateCount();
			if (forceSelfUpdate)
				obj.sendSelf(new DataTransformWithParent(obj.getObjectId(), 0, counter, newParent.getObjectId(), newLocation, (byte) speed));
			
			if (!oldLocation.equals(newLocation))
				obj.sendObservers(new UpdateTransformWithParentMessage(obj.getObjectId(), newParent.getObjectId(), counter, newLocation, (byte) speed));
		}
	}
	
	private static void onObjectMovedInWorld(@NotNull SWGObject obj, @Nullable SWGObject oldParent, @NotNull Location oldLocation, @NotNull Location newLocation, boolean forceSelfUpdate, double speed) {
		int counter = obj.getNextUpdateCount();
		
		if (oldParent != null)
			obj.sendObservers(new UpdateContainmentMessage(obj.getObjectId(), 0, obj.getSlotArrangement()));
		
		if (forceSelfUpdate)
			obj.sendSelf(new DataTransform(obj.getObjectId(), 0, counter, newLocation, (byte) speed));
		
		if (!oldLocation.equals(newLocation))
			obj.sendObservers(new UpdateTransformMessage(obj.getObjectId(), counter, newLocation, (byte) speed));
	}
	
}
