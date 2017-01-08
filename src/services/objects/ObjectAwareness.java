/***********************************************************************************
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

import intents.PlayerEventIntent;
import intents.RequestZoneInIntent;
import intents.network.GalacticPacketIntent;
import intents.object.ContainerTransferIntent;
import intents.object.DestroyObjectIntent;
import intents.object.MoveObjectIntent;
import intents.object.ObjectCreatedIntent;
import intents.object.ObjectTeleportIntent;
import intents.player.PlayerTransformedIntent;
import intents.server.ConfigChangedIntent;
import main.ProjectSWG;
import network.packets.Packet;
import network.packets.swg.zone.CmdSceneReady;
import network.packets.swg.zone.HeartBeat;
import network.packets.swg.zone.ParametersMessage;
import network.packets.swg.zone.chat.ChatOnConnectAvatar;
import network.packets.swg.zone.chat.VoiceChatStatus;
import network.packets.swg.zone.insertion.ChatServerStatus;
import network.packets.swg.zone.insertion.CmdStartScene;
import network.packets.swg.zone.object_controller.DataTransform;
import network.packets.swg.zone.object_controller.DataTransformWithParent;
import resources.Location;
import resources.Race;
import resources.Terrain;
import resources.config.ConfigFile;
import resources.control.Assert;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.SWGObject;
import resources.objects.awareness.AwarenessHandler;
import resources.objects.awareness.DataTransformHandler;
import resources.objects.awareness.TerrainMap.TerrainMapCallback;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
import resources.player.PlayerEvent;
import resources.player.PlayerState;
import resources.server_info.Log;

public class ObjectAwareness extends Service implements TerrainMapCallback {
	
	private static final Location GONE_LOCATION = new Location(0, 0, 0, Terrain.GONE);
	
	private final AwarenessHandler awarenessHandler;
	private final DataTransformHandler dataTransformHandler;
	
	public ObjectAwareness() {
		awarenessHandler = new AwarenessHandler(this);
		dataTransformHandler = new DataTransformHandler();
		dataTransformHandler.setSpeedCheck(getConfig(ConfigFile.FEATURES).getBoolean("SPEED-HACK-CHECK", true));
		
		registerForIntent(PlayerEventIntent.TYPE);
		registerForIntent(ObjectCreatedIntent.TYPE);
		registerForIntent(DestroyObjectIntent.TYPE);
		registerForIntent(ObjectTeleportIntent.TYPE);
		registerForIntent(GalacticPacketIntent.TYPE);
		registerForIntent(MoveObjectIntent.TYPE);
		registerForIntent(ConfigChangedIntent.TYPE);
		registerForIntent(ContainerTransferIntent.TYPE);
		registerForIntent(RequestZoneInIntent.TYPE);
	}
	
	@Override
	public boolean terminate() {
		awarenessHandler.close();
		return super.terminate();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		switch (i.getType()) {
			case PlayerEventIntent.TYPE:
				if (i instanceof PlayerEventIntent)
					handlePlayerEventIntent((PlayerEventIntent) i);
				break;
			case ObjectCreatedIntent.TYPE:
				if (i instanceof ObjectCreatedIntent)
					handleObjectCreatedIntent((ObjectCreatedIntent) i);
				break;
			case DestroyObjectIntent.TYPE:
				if (i instanceof DestroyObjectIntent)
					handleDestroyObjectIntent((DestroyObjectIntent) i);
				break;
			case ObjectTeleportIntent.TYPE:
				if (i instanceof ObjectTeleportIntent)
					processObjectTeleportIntent((ObjectTeleportIntent) i);
				break;
			case GalacticPacketIntent.TYPE:
				if (i instanceof GalacticPacketIntent)
					processGalacticPacketIntent((GalacticPacketIntent) i);
				break;
			case MoveObjectIntent.TYPE:
				if (i instanceof MoveObjectIntent)
					processMoveObjectIntent((MoveObjectIntent) i);
				break;
			case ConfigChangedIntent.TYPE:
				if (i instanceof ConfigChangedIntent)
					processConfigChangedIntent((ConfigChangedIntent) i);
				break;
			case ContainerTransferIntent.TYPE:
				if (i instanceof ContainerTransferIntent)
					processContainerTransferIntent((ContainerTransferIntent) i);
				break;
			case RequestZoneInIntent.TYPE:
				if (i instanceof RequestZoneInIntent)
					handleZoneIn(((RequestZoneInIntent) i).getCreature(), ((RequestZoneInIntent) i).getPlayer(), ((RequestZoneInIntent) i).isFirstZone());
				break;
			default:
				break;
		}
	}
	
	@Override
	public void onWithinRange(SWGObject obj, SWGObject inRange) {
		Assert.notNull(obj);
		Assert.notNull(inRange);
		Assert.isNull(obj.getParent());
		Assert.isNull(inRange.getParent());
		Assert.notNull(obj.getTerrain());
		Assert.notNull(inRange.getTerrain());
		obj.addObjectAware(inRange);
	}
	
	@Override
	public void onOutOfRange(SWGObject obj, SWGObject outRange) {
		Assert.notNull(obj);
		Assert.notNull(outRange);
		obj.removeObjectAware(outRange);
	}
	
	@Override
	public void onMoveSuccess(SWGObject obj) {
		
	}
	
	@Override
	public void onMoveFailure(SWGObject obj) {
		Assert.notNull(obj);
		obj.clearObjectsAware();
	}

	private void handlePlayerEventIntent(PlayerEventIntent pei) {
		Player p = pei.getPlayer();
		CreatureObject creature = p.getCreatureObject();
		switch (pei.getEvent()) {
			case PE_ZONE_IN_CLIENT:
				Assert.notNull(creature);
				if (creature.getParent() == null)
					moveObject(creature, creature.getLocation());
				break;
			case PE_DISAPPEAR:
				Assert.notNull(creature);
				disappearObject(creature, true, true);
				break;
			case PE_DESTROYED:
				Assert.notNull(creature);
				creature.setOwner(null);
				break;
			default:
				break;
		}
	}
	
	private void handleObjectCreatedIntent(ObjectCreatedIntent oci) {
		SWGObject object = oci.getObject();
		if (object.getParent() == null) {
			if (object.getTerrain() != null)
				moveObject(object, object.getLocation());
		} else {
			moveObject(object, object.getParent(), object.getLocation());
		}
	}
	
	private void handleDestroyObjectIntent(DestroyObjectIntent doi) {
		SWGObject obj = doi.getObject();
		disappearObject(obj, true, true);
		obj.setLocation(GONE_LOCATION);
		obj.moveToContainer(null);
	}
	
	private void processObjectTeleportIntent(ObjectTeleportIntent oti) {
		SWGObject obj = oti.getObject();
		Player owner = obj.getOwner();
		for (Player observer : obj.getObservers()) {
			obj.destroyObject(observer);
		}
		if (oti.getParent() != null)
			moveObject(obj, oti.getParent(), oti.getNewLocation());
		else
			moveObject(obj, oti.getNewLocation());
		if (obj instanceof CreatureObject && ((CreatureObject) obj).isLoggedInPlayer())
			handleZoneIn((CreatureObject) obj, owner, false);
	}
	
	private void processGalacticPacketIntent(GalacticPacketIntent i) {
		Packet packet = i.getPacket();
		if (packet instanceof DataTransform) {
			DataTransform trans = (DataTransform) packet;
			SWGObject obj = i.getObjectManager().getObjectById(trans.getObjectId());
			Assert.test(obj instanceof CreatureObject);
			Location requestedLocation = new Location(trans.getLocation());
			requestedLocation.setTerrain(obj.getTerrain());
			moveObjectWithTransform(obj, requestedLocation, trans.getSpeed(), trans.getUpdateCounter());
		} else if (packet instanceof DataTransformWithParent) {
			DataTransformWithParent trans = (DataTransformWithParent) packet;
			SWGObject obj = i.getObjectManager().getObjectById(trans.getObjectId());
			SWGObject parent = i.getObjectManager().getObjectById(trans.getCellId());
			Assert.test(obj instanceof CreatureObject);
			if (parent == null) {
				Log.w(this, "Unknown data transform parent! Obj: %d/%s  Parent: %d", trans.getObjectId(), obj, trans.getCellId());
				return;
			}
			Location requestedLocation = new Location(trans.getLocation());
			requestedLocation.setTerrain(obj.getTerrain());
			moveObjectWithTransform(obj, parent, requestedLocation, trans.getSpeed(), trans.getUpdateCounter());
		} else if (packet instanceof CmdSceneReady) {
			handleCmdSceneReady(i.getPlayer(), (CmdSceneReady) packet);
		}
	}
	
	private void processMoveObjectIntent(MoveObjectIntent i) {
		if (i.getParent() == null)
			moveObjectWithTransform(i.getObject(), i.getNewLocation(), i.getSpeed(), i.getUpdateCounter());
		else
			moveObjectWithTransform(i.getObject(), i.getParent(), i.getNewLocation(), i.getSpeed(), i.getUpdateCounter());
	}
	
	private void processConfigChangedIntent(ConfigChangedIntent i) {
		if (i.getChangedConfig() == ConfigFile.FEATURES && i.getKey().equals("SPEED-HACK-CHECK"))
			dataTransformHandler.setSpeedCheck(Boolean.parseBoolean(i.getNewValue()));
	}
	
	private void processContainerTransferIntent(ContainerTransferIntent i) {
		Assert.notNull(i.getObject());
		if (i.getContainer() == null) {
			if (i.getObject().getTerrain() != null)
				moveObject(i.getObject(), i.getObject().getLocation());
		} else {
			moveObject(i.getObject(), i.getContainer(), i.getObject().getLocation());
		}
	}
	
	private void handleZoneIn(CreatureObject creature, Player player, boolean firstZone) {
		creature.setOwner(player);
		// Fresh login or teleport/travel
		Assert.test(player.getPlayerState() == PlayerState.LOGGED_IN || player.getPlayerState() == PlayerState.ZONED_IN);
		player.setPlayerState(PlayerState.ZONING_IN);
		Log.i(this, "Zoning in %s with character %s", player.getUsername(), player.getCharacterName());
		if (firstZone)
			startFirstZone(creature, player);
		startZone(creature, player);
		Assert.notNull(creature.getTerrain());
	}
	
	private void startFirstZone(CreatureObject creature, Player player) {
		player.sendPacket(new HeartBeat());
		player.sendPacket(new ChatServerStatus(true));
		player.sendPacket(new VoiceChatStatus());
		player.sendPacket(new ParametersMessage());
		player.sendPacket(new ChatOnConnectAvatar());
		creature.clearCustomAware(false);
		new PlayerEventIntent(player, PlayerEvent.PE_FIRST_ZONE).broadcast();
	}
	
	private void startZone(CreatureObject creature, Player player) {
		startScene(creature);
		new PlayerEventIntent(player, PlayerEvent.PE_ZONE_IN_CLIENT).broadcast();
	}
	
	private void startScene(CreatureObject creature) {
		Location loc = creature.getWorldLocation();
		long time = ProjectSWG.getGalacticTime();
		Race race = ((CreatureObject) creature).getRace();
		boolean ignoreSnapshots = loc.getTerrain() == Terrain.DEV_AREA;
		Player owner = creature.getOwner();
		creature.resetAwareness();
		owner.sendPacket(new CmdStartScene(ignoreSnapshots, creature.getObjectId(), race, loc, time, (int)(System.currentTimeMillis()/1E3)));
		recursiveCreateObject(creature, creature.getOwner());
		if (creature.getParent() != null) {
			for (SWGObject obj : creature.getSuperParent().getObjectsAware()) {
				obj.createObject(creature);
				creature.createObject(obj);
			}
		}
	}
	
	private void handleCmdSceneReady(Player player, CmdSceneReady p) {
		Assert.test(player.getPlayerState() == PlayerState.ZONING_IN);
		player.setPlayerState(PlayerState.ZONED_IN);
		Log.i("ZoneService", "%s with character %s zoned in from %s:%d", player.getUsername(), player.getCharacterName(), p.getAddress(), p.getPort());
		new PlayerEventIntent(player, PlayerEvent.PE_ZONE_IN_SERVER).broadcast();
		player.sendPacket(new CmdSceneReady());
	}
	
	private void recursiveCreateObject(SWGObject obj, Player owner) {
		SWGObject parent = obj.getParent();
		if (parent != null)
			recursiveCreateObject(parent, owner);
		else
			obj.createObject(owner);
	}
	
	private void moveObject(SWGObject obj, Location requestedLocation) {
		Assert.notNull(requestedLocation.getTerrain());
		awarenessHandler.moveObject(obj, requestedLocation);
	}
	
	private void moveObject(SWGObject obj, SWGObject parent, Location requestedLocation) {
		awarenessHandler.moveObject(obj, parent, requestedLocation);
	}
	
	private void moveObjectWithTransform(SWGObject obj, Location requestedLocation, double speed, int update) {
		moveObject(obj, requestedLocation);
		dataTransformHandler.handleMove(obj, speed, update);
		if (obj instanceof CreatureObject && ((CreatureObject) obj).isLoggedInPlayer())
			new PlayerTransformedIntent((CreatureObject) obj, obj.getParent(), null, obj.getLocation(), requestedLocation).broadcast();
	}
	
	private void moveObjectWithTransform(SWGObject obj, SWGObject parent, Location requestedLocation, double speed, int update) {
		moveObject(obj, parent, requestedLocation);
		dataTransformHandler.handleMove(obj, parent, speed, update);
		if (obj instanceof CreatureObject && ((CreatureObject) obj).isLoggedInPlayer())
			new PlayerTransformedIntent((CreatureObject) obj, obj.getParent(), parent, obj.getLocation(), requestedLocation).broadcast();
	}
	
	private void disappearObject(SWGObject obj, boolean disappearObjects, boolean disappearCustom) {
		awarenessHandler.disappearObject(obj, disappearObjects, disappearCustom);
	}
	
}