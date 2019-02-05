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

package com.projectswg.utility.packets;

import com.projectswg.common.data.CRC;
import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.network.hcap.PacketRecord;
import com.projectswg.common.network.packets.PacketType;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.*;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline;
import com.projectswg.common.network.packets.swg.zone.building.UpdateCellPermissionMessage;
import com.projectswg.common.network.packets.swg.zone.deltas.DeltasMessage;
import com.projectswg.common.network.packets.swg.zone.insertion.CmdStartScene;
import com.projectswg.common.network.packets.swg.zone.object_controller.DataTransform;
import com.projectswg.common.network.packets.swg.zone.object_controller.DataTransformWithParent;
import com.projectswg.common.network.packets.swg.zone.object_controller.ObjectController;
import com.projectswg.holocore.resources.support.data.server_info.loader.BuildingCellLoader.CellInfo;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.control.IntentManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class PacketCaptureAnalysis {
	
	private final Map<Long, SWGObject> objects;
	private final LinkedList<SWGObject> loadingObjects;
	private final List<PacketCaptureAssertion> errors;
	private final List<CreatureObject> players;
	private final AtomicReference<Terrain> currentTerrain;
	private final AtomicLong objectCreations;
	private final AtomicLong objectDeletions;
	private final AtomicLong objectDeletionsImplicit;
	private final AtomicLong currentCharacter;
	private final AtomicInteger characterZoneIns;
	private final AtomicLong packetNumber;
	private final AtomicBoolean zoning;
	
	private PacketCaptureAnalysis() {
		this.objects = new HashMap<>();
		this.loadingObjects = new LinkedList<>();
		this.errors = new ArrayList<>();
		this.players = new ArrayList<>();
		this.currentTerrain = new AtomicReference<>(null);
		this.objectCreations = new AtomicLong(0);
		this.objectDeletions = new AtomicLong(0);
		this.objectDeletionsImplicit = new AtomicLong(0);
		this.currentCharacter = new AtomicLong(0);
		this.characterZoneIns = new AtomicInteger(0);
		this.packetNumber = new AtomicLong(0);
		this.zoning = new AtomicBoolean(false);
	}
	
	public List<PacketCaptureAssertion> getErrors() {
		return Collections.unmodifiableList(errors);
	}
	
	public long getObjectCreations() {
		return objectCreations.get();
	}
	
	public long getObjectDeletions() {
		return objectDeletions.get();
	}
	
	public long getObjectDeletionsImplicit() {
		return objectDeletionsImplicit.get();
	}
	
	public int getCharacterZoneIns() {
		return characterZoneIns.get();
	}
	
	public List<CreatureObject> getPlayers() {
		return Collections.unmodifiableList(players);
	}
	
	private void analyzeNext(PacketRecord record) {
		if (record.isServer()) {
			PacketType type = record.parseType();
			SWGPacket packet = record.parse();
			try {
				if (packet == null)
					throw new PacketCaptureAssertion(null, "unknown packet: " + type);
				processNext(packet);
			} catch (PacketCaptureAssertion e) {
				errors.add(e);
			}
		}
		packetNumber.incrementAndGet();
	}
	
	private void processNext(@NotNull SWGPacket packet) {
		switch (packet.getPacketType()) {
			case SCENE_CREATE_OBJECT_BY_CRC:
				handleSceneCreateObjectByCrc((SceneCreateObjectByCrc) packet);
				break;
			case SCENE_CREATE_OBJECT_BY_NAME:
				handleSceneCreateObjectByName((SceneCreateObjectByName) packet);
				break;
			case BASELINE:
				handleBaseline((Baseline) packet);
				break;
			case DELTA:
				handleDeltasMessage((DeltasMessage) packet);
				break;
			case SCENE_END_BASELINES:
				handleSceneEndBaselines((SceneEndBaselines) packet);
				break;
			case SCENE_DESTROY_OBJECT:
				handleSceneDestroyObject((SceneDestroyObject) packet);
				break;
			case CMD_START_SCENE:
				handleCommandStartScene((CmdStartScene) packet);
				break;
			case CMD_SCENE_READY:
				handleCommandSceneReady((CmdSceneReady) packet);
				break;
			case OBJECT_CONTROLLER:
				handleObjectController((ObjectController) packet);
				break;
			case UPDATE_TRANSFORMS_MESSAGE:
				handleUpdateTransform((UpdateTransformMessage) packet);
				break;
			case UPDATE_TRANSFORM_WITH_PARENT_MESSAGE:
				handleUpdateTransformWithParent((UpdateTransformWithParentMessage) packet);
				break;
			case UPDATE_CELL_PERMISSIONS_MESSAGE:
				handleUpdateCellPermission((UpdateCellPermissionMessage) packet);
				break;
			case UPDATE_PVP_STATUS_MESSAGE:
				handleUpdatePvpStatusMessage((UpdatePvpStatusMessage) packet);
				break;
			case UPDATE_CONTAINMENT_MESSAGE:
				handleUpdateContainment((UpdateContainmentMessage) packet);
				break;
			case UPDATE_POSTURE_MESSAGE:
				handleUpdatePosture((UpdatePostureMessage) packet);
				break;
			default:
				break;
		}
	}
	
	private void handleSceneCreateObjectByCrc(SceneCreateObjectByCrc p) {
		objectCreations.incrementAndGet();
		SWGObject obj = ObjectCreator.createObjectFromTemplate(p.getObjectId(), CRC.getString(p.getObjectCrc()));
		
		if (currentTerrain.get() == null)
			p.setLocation(Location.builder(p.getLocation()).setTerrain(Terrain.GONE).build());
		else
			p.setLocation(Location.builder(p.getLocation()).setTerrain(currentTerrain.get()).build());
		obj.setLocation(p.getLocation());
		loadingObjects.add(obj);
		assertFalse(p, objects.containsKey(p.getObjectId()), "object already exists [initialized]");
	}
	
	private void handleSceneCreateObjectByName(SceneCreateObjectByName p) {
		objectCreations.incrementAndGet();
		assertNotNull(p, currentTerrain.get(), "unknown terrain");
		SWGObject obj = ObjectCreator.createObjectFromTemplate(p.getObjectId(), p.getTemplate());
		p.setLocation(Location.builder(p.getLocation()).setTerrain(currentTerrain.get()).build());
		obj.setLocation(p.getLocation());
		loadingObjects.add(obj);
		assertFalse(p, objects.containsKey(p.getObjectId()), "object already exists [initialized]");
	}
	
	private void handleBaseline(Baseline p) {
		assertTrue(p, !loadingObjects.isEmpty(), "no loading objects defined");
		assertEquals(p, loadingObjects.getLast().getObjectId(), p.getObjectId(), "baseline sent for non-loading object");
		assertFalse(p, objects.containsKey(p.getObjectId()), "already-initialized object");
		loadingObjects.getLast().parseBaseline(p);
	}
	
	private void handleDeltasMessage(DeltasMessage p) {
		assertTrue(p, objects.containsKey(p.getObjectId()), "unknown object");
	}
	
	private void handleSceneEndBaselines(SceneEndBaselines p) {
		SWGObject obj = loadingObjects.pollLast();
		assertNotNull(p, obj, "object was not loading");
		assertEquals(p, obj.getObjectId(), p.getObjectId(), "object end baseline was not sent in order");
		assertNull(p, objects.put(obj.getObjectId(), obj), "object already exists");
		
		if (obj instanceof BuildingObject) {
			List<CellInfo> cellGoal = DataLoader.buildingCells().getBuilding(obj.getTemplate());
			if (cellGoal != null) {
				Collection<SWGObject> cells = obj.getContainedObjects();
				assertEquals(p, cellGoal.size(), cells.size(), "Cells are not fully populated within building");
			}
		}
	}
	
	private void handleSceneDestroyObject(SceneDestroyObject p) {
		objectDeletions.incrementAndGet();
		objectDeletionsImplicit.incrementAndGet();
		SWGObject obj = objects.remove(p.getObjectId());
		assertNotNull(p, obj, "destroyed object that didn't exist");
		handleDestroyObjectChildren(obj);
	}
	
	private void handleDestroyObjectChildren(SWGObject obj) {
		objects.remove(obj.getObjectId());
		obj.getContainedObjects().forEach(this::handleDestroyObjectChildren);
		obj.getSlottedObjects().forEach(this::handleDestroyObjectChildren);
	}
	
	private void handleCommandStartScene(CmdStartScene p) {
		characterZoneIns.incrementAndGet();
		assertFalse(p, zoning.getAndSet(true), "already zoning");
		objectDeletionsImplicit.addAndGet(loadingObjects.size() + objects.size());
		loadingObjects.clear();
		objects.clear();
		currentTerrain.set(p.getLocation().getTerrain());
		currentCharacter.set(p.getCharacterId());
	}
	
	private void handleCommandSceneReady(CmdSceneReady p) {
		assertTrue(p, zoning.getAndSet(false), "not zoning");
		SWGObject player = objects.get(currentCharacter.get());
		assertNotNull(p, player, "character information not sent");
		assertTrue(p, player instanceof CreatureObject, "character is not a creature");
		players.add((CreatureObject) player);
	}
	
	private void handleObjectController(ObjectController p) {
		switch (p.getControllerCrc()) {
			case DataTransform.CRC:
				handleDataTransform((DataTransform) p);
				break;
			case DataTransformWithParent.CRC:
				handleDataTransformWithParent((DataTransformWithParent) p);
				break;
			default:
				break;
		}
	}
	
	private void handleDataTransform(DataTransform p) {
		SWGObject obj = getObject(p.getObjectId());
		assertNotNull(p, obj, "unknown object");
		obj.systemMove(null, Location.builder(p.getLocation()).setTerrain(obj.getTerrain()).build());
	}
	
	private void handleDataTransformWithParent(DataTransformWithParent p) {
		SWGObject obj = getObject(p.getObjectId());
		SWGObject container = getObject(p.getCellId());
		assertNotNull(p, obj, "unknown object");
		assertNotNull(p, container, "unknown container");
		obj.systemMove(container, Location.builder(p.getLocation()).setTerrain(obj.getTerrain()).build());
	}
	
	private void handleUpdateTransform(UpdateTransformMessage p) {
		SWGObject obj = getObject(p.getObjectId());
		assertNotNull(p, obj, "unknown object");
		obj.systemMove(null, Location.builder(obj.getLocation()).setPosition(p.getX()/4.0, p.getY()/4.0, p.getZ()/4.0).build());
	}
	
	private void handleUpdateTransformWithParent(UpdateTransformWithParentMessage p) {
		SWGObject obj = getObject(p.getObjectId());
		SWGObject container = getObject(p.getCellId());
		assertNotNull(p, obj, "unknown object");
		assertNotNull(p, container, "unknown container");
		obj.systemMove(container, Location.builder(obj.getLocation()).setPosition(p.getX()/8.0, p.getY()/8.0, p.getZ()/8.0).build());
	}
	
	private void handleUpdateCellPermission(UpdateCellPermissionMessage p) {
		assertTrue(p, getObject(p.getCellId()) instanceof CellObject, "cellId is not a CellObject");
	}
	
	private void handleUpdatePvpStatusMessage(UpdatePvpStatusMessage p) {
//		SWGObject obj = getObject(p.getObjectId());
//		assertNotNull(p, obj, "unknown object");
//		assertTrue(p, obj instanceof TangibleObject, "object is not a TangibleObject");
//		((TangibleObject) obj).setPvpFaction(p.getPlayerFaction());
//		((TangibleObject) obj).setPvpFlags(p.getPvpFlags());
	}
	
	private void handleUpdateContainment(UpdateContainmentMessage p) {
		SWGObject obj = getObject(p.getObjectId());
		SWGObject container = getObject(p.getContainerId());
		assertNotNull(p, obj, "unknown object");
		assertNotNull(p, container, "unknown container");
		assertEquals(p, container.getArrangementId(obj), p.getSlotIndex(), "illogical slot index");
		obj.systemMove(container);
	}
	
	private void handleUpdatePosture(UpdatePostureMessage p) {
		SWGObject obj = getObject(p.getObjectId());
		assertNotNull(p, obj, "unknown object");
		assertTrue(p, obj instanceof CreatureObject, "object is not a CreatureObject");
		Posture posture = Posture.getFromId(p.getPosture());
		assertNotNull(p, posture, "unknown Posture");
		assertNotEquals(p, Posture.INVALID, posture, "INVALID Posture");
		((CreatureObject) obj).setPosture(posture);
	}
	
	@Nullable
	private SWGObject getObject(long id) {
		SWGObject obj = objects.get(id);
		if (obj == null) {
			for (SWGObject loading : loadingObjects) {
				if (loading.getObjectId() == id)
					return loading;
			}
		}
		return obj;
	}
	
	@Contract("_, !null, _ -> fail")
	private void assertNull(SWGPacket packet, Object obj, String message) {
		assertEquals(packet, null, obj, message);
	}
	
	@Contract("_, null, _ -> fail")
	private void assertNotNull(SWGPacket packet, Object obj, String message) {
		assertNotEquals(packet, null, obj, message);
	}
	
	private void assertEquals(SWGPacket packet, Object expected, Object actual, String message) {
		assertTrue(packet, Objects.equals(expected, actual), message);
	}
	
	private void assertNotEquals(SWGPacket packet, Object expected, Object actual, String message) {
		assertTrue(packet, !Objects.equals(expected, actual), message);
	}
	
	@Contract("_, true, _ -> fail")
	private void assertFalse(SWGPacket packet, boolean val, String message) {
		assertTrue(packet, !val, message);
	}
	
	@Contract("_, false, _ -> fail")
	private void assertTrue(SWGPacket packet, boolean val, String message) {
		if (!val)
			throw new PacketCaptureAssertion(packet, "Packet #"+packetNumber.get() + ": " + message);
	}
	
	@NotNull
	public static PacketCaptureAnalysis from(List<PacketRecord> packets) {
		PacketCaptureAnalysis analysis = new PacketCaptureAnalysis();
		try (IntentManager intentManager = new IntentManager(0)) {
			IntentManager prevIntentManager = IntentManager.getInstance();
			IntentManager.setInstance(intentManager);
			packets.forEach(analysis::analyzeNext);
			IntentManager.setInstance(prevIntentManager);
		}
		return analysis;
	}
	
	public static class PacketCaptureAssertion extends RuntimeException {
		
		private final SWGPacket packet;
		
		public PacketCaptureAssertion(SWGPacket packet, String message) {
			super(message);
			this.packet = packet;
		}
		
		public SWGPacket getPacket() {
			return packet;
		}
		
	}
	
}
