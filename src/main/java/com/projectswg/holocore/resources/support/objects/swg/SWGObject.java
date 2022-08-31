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
package com.projectswg.holocore.resources.support.objects.swg;

import com.projectswg.common.data.CRC;
import com.projectswg.common.data.encodables.mongo.MongoData;
import com.projectswg.common.data.encodables.mongo.MongoPersistable;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Point3D;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.data.objects.GameObjectType;
import com.projectswg.common.data.swgfile.visitors.ObjectData.ObjectDataAttribute;
import com.projectswg.common.encoding.StringType;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;
import com.projectswg.common.network.packets.swg.zone.spatial.AttributeList;
import com.projectswg.holocore.ProjectSWG;
import com.projectswg.holocore.intents.support.objects.swg.ContainerTransferIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectTeleportIntent;
import com.projectswg.holocore.resources.support.data.location.InstanceLocation;
import com.projectswg.holocore.resources.support.data.location.InstanceType;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.SlotDefinitionLoader.SlotDefinition;
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder;
import com.projectswg.holocore.resources.support.global.network.BaselineObject;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.awareness.AwarenessType;
import com.projectswg.holocore.resources.support.objects.awareness.ObjectAware;
import com.projectswg.holocore.resources.support.objects.permissions.ContainerPermissions;
import com.projectswg.holocore.resources.support.objects.permissions.ContainerResult;
import com.projectswg.holocore.resources.support.objects.permissions.DefaultPermissions;
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.Portal;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.control.Intent;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class SWGObject extends BaselineObject implements Comparable<SWGObject>, MongoPersistable {
	
	private final long 								objectId;
	private final InstanceLocation 					location		= new InstanceLocation();
	private final Set<SWGObject>					containedObjects= new CopyOnWriteArraySet<>();
	private final Map <String, SWGObject>			slots			= new ConcurrentHashMap<>();
	private final Map<String, SlotDefinition>		slotsAvailable	= new ConcurrentHashMap<>();
	private final ObjectAware						awareness		= new ObjectAware();
	private final Set<CreatureObject>				observers		= ConcurrentHashMap.newKeySet();
	private final Map<ObjectDataAttribute, Object>	dataAttributes	= new EnumMap<>(ObjectDataAttribute.class);
	private final Map<ServerAttribute, Object>		serverAttributes= new EnumMap<>(ServerAttribute.class);
	private final AtomicInteger						updateCounter	= new AtomicInteger(1);
	private final Set<SWGObject>					containedObjectsView	= Collections.unmodifiableSet(containedObjects);
	private final Collection<SWGObject>				slottedObjectsView		= Collections.unmodifiableCollection(slots.values());
	
	private GameObjectType 				gameObjectType	= GameObjectType.GOT_NONE;
	private ContainerPermissions		permissions		= DefaultPermissions.getPermissions();
	private List <List <String>>		arrangement		= new ArrayList<>();
	
	private SWGObject	parent			= null;
	private StringId 	stringId		= new StringId("", "");
	private StringId 	detailStringId	= new StringId("", "");
	private String		template		= "";
	private int			crc				= 0;
	private int			cashBalance		= 0;
	private int			bankBalance		= 0;
	private String		objectName		= "";
	private int			volume			= 0;
	private float		complexity		= 1;
	private int     	containerType	= 0;
	private int			areaId			= -1;
	private String		buildoutEvent	= "";
	private String		buildoutTag		= "";
	private int     	slotArrangement	= -1;
	private boolean		observeWithParent = true;
	private boolean		generated		= true;
	private boolean		persisted		= false;
	private boolean 	noTrade			= false;
	
	public SWGObject() {
		this(0, null);
	}
	
	public SWGObject(long objectId, BaselineType objectType) {
		super(objectType);
		this.objectId = objectId;
	}
	
	/**
	 * Adds the specified object to this object and places it in the appropriate slot if needed
	 * @param object Object to add to this container, which will either be put into the appropriate slot(s) or become a contained object
	 */
	public void addObject(SWGObject object) {
		int arrangementId = getArrangementId(object);
		if (arrangementId == -1) {
			addContainedObject(object);
		} else {
			List<List<String>> arrangements = object.getArrangement();
			
			for (List<String> possibleSlots : arrangements) {
				for (String possibleSlot : possibleSlots) {
					SWGObject slottedObject = getSlottedObject(possibleSlot);
					
					if (slottedObject == null) {
						addSlottedObject(object, possibleSlots, arrangementId);
						return;
					}
				}
			}
			addSlottedObject(object, object.getArrangement().get(arrangementId - 4), arrangementId);
		}
	}
	
	private void addContainedObject(SWGObject object) {
		containedObjects.add(object);
		
		// We need to adjust the volume of our container accordingly!
		setVolume(getVolume() + object.getVolume() + 1);
		
		object.observeWithParent = true;
		object.slotArrangement = -1;
		object.parent = this;
		object.setTerrain(getTerrain());
		onAddedChild(object);
	}
	
	private void addSlottedObject(SWGObject object, List<String> slots, int arrangementId) {
		boolean observeWithParent = false;
		handleSlotReplacement(object.parent, object, slots);
		for (String requiredSlot : slots) {
			this.slots.put(requiredSlot, object);
			SlotDefinition def = slotsAvailable.get(requiredSlot);
			observeWithParent |= def.isObserveWithParent();
		}
		object.observeWithParent = observeWithParent;
		object.slotArrangement = arrangementId;
		object.parent = this;
		object.setTerrain(getTerrain());
		onAddedChild(object);
	}
	
	/**
	 * Removes the specified object from this current object.
	 * @param object Object to remove
	 */
	public void removeObject(SWGObject object) {
		if (object.getSlotArrangement() == -1) {
			containedObjects.remove(object);
			
			// We need to adjust the volume of our container accordingly!
			setVolume(getVolume() - object.getVolume() - 1);
		} else {
			removeSlottedObject(object);
		}
		
		// Remove as parent
		object.parent = null;
		object.observeWithParent = true;
		object.slotArrangement = -1;
		
		onRemovedChild(object);
	}
	
	private void removeSlottedObject(SWGObject object) {
		List<List<String>> arrangements = object.getArrangement();
		
		for (List<String> possibleSlots : arrangements) {
			for (String possibleSlot : possibleSlots) {
				if (slots.remove(possibleSlot, object)) {
					return;
				}
			}
		}
	}
	
	/**
	 * Moves the object to the new parent without invoking any intents. This is only meant for very specific services (awareness and buildouts)
	 * @param newParent the container to move this object to
	 * @return TRUE if the container changed, FALSE otherwise
	 */
	public boolean systemMove(SWGObject newParent) {
		SWGObject oldParent = parent;
		if (oldParent != newParent) {
			if (oldParent != null)
				oldParent.removeObject(this);
			if (newParent != null)
				newParent.addObject(this);
			return true;
		}
		return false;
	}
	
	/**
	 * Moves the object to the new parent and new location without invoking any intents. This is only meant for very specific services (awareness and buildouts)
	 * @param newParent the container to move this object to
	 * @param newLocation the location to move this object to
	 * @return TRUE if the container or location changed, FALSE otherwise
	 */
	public boolean systemMove(SWGObject newParent, Location newLocation) {
		SWGObject oldParent = parent;
		Location oldLocation = getLocation();
		if (oldParent != newParent) {
			if (oldParent != null)
				oldParent.removeObject(this);
			setLocation(newLocation);
			if (newParent != null)
				newParent.addObject(this);
		} else {
			setLocation(newLocation);
		}
		return oldParent != newParent || !oldLocation.equals(newLocation);
	}
	
	/**
	 * Attempts to move this object to the defined container without checking for permissions
	 * @param newParent the container to move this object to
	 */
	public void moveToContainer(@Nullable SWGObject newParent) {
		SWGObject oldParent = parent;
		int oldArrangement = this.slotArrangement;
		if (systemMove(newParent))
			broadcast(new ContainerTransferIntent(this, oldParent, oldArrangement, newParent, this.slotArrangement));
	}
	
	public void moveToSlot(@NotNull SWGObject newParent, String slot, int arrangementId) {
		SWGObject oldParent = parent;
		int oldArrangement = this.slotArrangement;
		if (oldParent != newParent) {
			if (oldParent != null)
				oldParent.removeObject(this);
			newParent.addSlottedObject(this, List.of(slot), arrangementId);
			broadcast(new ContainerTransferIntent(this, oldParent, oldArrangement, newParent, slotArrangement));
		}
	}
	
	/**
	 * Attempts to move this object to the defined container and location without checking for permissions
	 * @param newParent the container to move this object to
	 * @param newLocation the location to move this object to
	 */
	public void moveToContainer(@Nullable SWGObject newParent, @NotNull Location newLocation) {
		assert newParent != this;
		Location oldLocation = getLocation();
		SWGObject oldParent = parent;
		if (systemMove(newParent, newLocation))
			broadcast(new ObjectTeleportIntent(this, oldParent, newParent, oldLocation, newLocation));
	}
	
	/**
	 * Attempts to move this object to the defined container and location without checking for permissions
	 * @param newParent the container to move this object to
	 * @param x the x location to move this object to
	 * @param y the y location to move this object to
	 * @param z the z location to move this object to
	 */
	public void moveToContainer(@Nullable SWGObject newParent, double x, double y, double z) {
		moveToContainer(newParent, Location.builder(getLocation()).setPosition(x, y, z).build());
	}
	
	/**
	 * Attempts to move this object to the specified location within the current parent
	 * @param location the location to move this object to
	 */
	public void moveToLocation(@NotNull Location location) {
		moveToContainer(parent, location);
	}
	
	/**
	 * Moves this object to the passed container if the requester has the MOVE permission for the container
	 *
	 * @param requester Object that is requesting to move the object, used for permission checking
	 * @param newParent Where this object should be moved to
	 * @return {@link ContainerResult}
	 */
	public ContainerResult moveToContainer(@NotNull CreatureObject requester, SWGObject newParent) {
		ContainerResult result = isAllowedToMove(requester, newParent);
		if (result == ContainerResult.SUCCESS) {
			moveToContainer(newParent);
		}
		
		return result;
	}
	
	/**
	 * Checks if an object can be moved to the container by the requester
	 * @param requester Object that is requesting to move the object, used for permission checking
	 * @param container Where this object should be moved to
	 * @return {@link ContainerResult}
	 */
	protected ContainerResult isAllowedToMove(@NotNull CreatureObject requester, SWGObject container) {
		if (!permissions.canMove(requester, this)) {
			Log.w("No permission 'MOVE' for requestor %s with object %s", requester, this);
			return ContainerResult.NO_PERMISSION;
		}
		if (container == null)
			return ContainerResult.SUCCESS;
		
		// Check if the requester has MOVE permissions to the destination container
		if (!permissions.canMove(requester, container)) {
			Log.w("No permission 'MOVE' for requestor %s with container %s", requester, this);
			return ContainerResult.NO_PERMISSION;
		}
		
		// Check if object can fit into container or slots
		int arrangementId = container.getArrangementId(this);
		if (arrangementId == -1) {
			if (container.getMaxContainerSize() <= container.getContainedObjects().size() && container.getMaxContainerSize() > 0) {
				Log.w("Unable to add object to container! Container Full. Max Size: %d", container.getMaxContainerSize());
				return ContainerResult.CONTAINER_FULL;
			}
		}
		return ContainerResult.SUCCESS;
	}
	
	protected void handleSlotReplacement(SWGObject oldParent, SWGObject obj, List<String> slots) {
		for (String slot : slots) {
			SWGObject slotObj = getSlottedObject(slot);
			if (slotObj != null)
				slotObj.moveToContainer(oldParent);
		}
	}
	
	protected void onAddedChild(SWGObject child) {
		assert child != this;
		SWGObject parent = this.parent;
		if (parent != null)
			parent.onAddedChild(child);
	}
	
	protected void onRemovedChild(SWGObject child) {
		SWGObject parent = this.parent;
		if (parent != null)
			parent.onRemovedChild(child);
	}
	
	public void broadcast(Intent intent) {
		intent.broadcast();
	}
	
	public boolean isVisible(CreatureObject target) {
		if (target == null)
			return true;
		if (!permissions.canView(target, this))
			return false;
		
		SWGObject parent = this.parent;
		return parent == null || parent.isVisible(target);
	}
	
	public boolean isLineOfSight(@NotNull SWGObject target) {
		SWGObject myParent = getEffectiveParent();
		SWGObject theirParent = target.getEffectiveParent();
		SWGObject superParent = null;
		if (myParent == theirParent)
			return true;
		
		Portal portal = null;
		if (myParent instanceof CellObject) {
			portal = theirParent instanceof CellObject ? ((CellObject) myParent).getPortalTo((CellObject) theirParent) : ((CellObject) myParent).getPortalTo(null);
			superParent = myParent.getParent();
		} else if (theirParent instanceof CellObject) {
			portal = ((CellObject) theirParent).getPortalTo(null);
			superParent = theirParent.getParent();
		}
		if (portal == null)
			return false; // If no portal, and they aren't in the same parent, then they can't see each other
		assert superParent != null;
		
		Point3D p1 = portal.getFrame1();
		Point3D p2 = portal.getFrame2();
		double headingToTarget = getWorldLocation().getHeadingTo(target.getWorldLocation());
		double headingToPortalLeft = getWorldLocation().getHeadingTo(Location.builder().setPosition(p1.getX(), p1.getY(), p1.getZ()).translateLocation(superParent.getLocation()).build());
		double headingToPortalRight = getWorldLocation().getHeadingTo(Location.builder().setPosition(p2.getX(), p2.getY(), p2.getZ()).translateLocation(superParent.getLocation()).build());
		if (headingToPortalLeft-headingToPortalRight > 180)
			headingToPortalRight += 360;
		if (headingToPortalLeft-headingToPortalRight < -180)
			headingToPortalLeft += 360;
		if (Math.abs(headingToPortalLeft-headingToTarget) > 180)
			headingToTarget += 360;
		
		if (headingToTarget > headingToPortalLeft)
			return headingToTarget < headingToPortalRight;
		return headingToTarget > headingToPortalRight;
	}
	
	/**
	 * Gets the object that occupies the specified slot
	 * @param slotName
	 * @return The {@link SWGObject} occupying the slot. Returns null if there is nothing in the slot or it doesn't exist.
	 */
	public SWGObject getSlottedObject(String slotName) {
		return slots.get(slotName);
	}
	
	public Collection<SWGObject> getChildObjects() {
		Set<SWGObject> ret = new HashSet<>(containedObjects.size() + slots.size());
		ret.addAll(containedObjects);
		ret.addAll(slots.values());
		ret.remove(null);
		return ret;
	}
	
	/**
	 * Gets a list of all the objects in the current container. This should only be used for viewing the objects
	 * in the current container.
	 * @return An unmodifiable {@link Collection} of {@link SWGObject}'s in the container
	 */
	public Collection<SWGObject> getContainedObjects() {
		return containedObjectsView;
	}
	
	public Collection<SWGObject> getChildObjectsRecursively() {
		Collection<SWGObject> combined = new ArrayList<>();
		
		combined.addAll(getSlottedObjects());
		combined.addAll(getContainedObjects());
		
		for (SWGObject object : new ArrayList<>(combined)) {
			combined.addAll(object.getChildObjectsRecursively());
		}
		
		return combined;
	}
	
	public void runOnChildObjectsRecursively(Consumer<SWGObject> op) {
		for (SWGObject obj : getContainedObjects()) {
			op.accept(obj);
			obj.runOnChildObjectsRecursively(op);
		}
		for (SWGObject obj : getSlottedObjects()) {
			op.accept(obj);
			obj.runOnChildObjectsRecursively(op);
		}
	}
	
	public void setSlots(@NotNull Collection<String> slots) {
		this.slotsAvailable.clear();
		for (String slot : slots)
			this.slotsAvailable.put(slot, DataLoader.Companion.slotDefinitions().getSlotDefinition(slot));
	}
	
	@NotNull
	public Collection<SlotDefinition> getSlotDefinitions() {
		return Collections.unmodifiableCollection(slotsAvailable.values());
	}
	
	@Nullable
	public SlotDefinition getSlotDefinition(String slot) {
		return slotsAvailable.get(slot);
	}

	public boolean hasSlot(@NotNull String slotName) {
		return slotsAvailable.containsKey(slotName);
	}
	
	public boolean isObserveWithParent() {
		if (observeWithParent) {
			SWGObject parent = this.parent;
			return parent == null || parent.isObserveWithParent();
		}
		return false;
	}
	
	@NotNull
	public Map<String, SWGObject> getSlots() {
		return Collections.unmodifiableMap(slots);
	}
	
	@NotNull
	public Collection<SWGObject> getSlottedObjects() {
		return slottedObjectsView;
	}
	
	public void setLocation(Location location) {
		if (parent != null && location.getTerrain() != parent.getTerrain())
			throw new IllegalArgumentException("Attempted to set different terrain from parent!");
		this.location.setLocation(location);
		updateChildrenTerrain();
	}
	
	public void setTerrain(@NotNull Terrain terrain) {
		if (parent != null && terrain != parent.getTerrain())
			throw new IllegalArgumentException("Attempted to set different terrain from parent!");
		if (location.getTerrain() != terrain) {
			location.setTerrain(terrain);
			updateChildrenTerrain();
		}
	}
	
	public void setPosition(@NotNull Terrain terrain, double x, double y, double z) {
		if (parent != null && terrain != parent.getTerrain())
			throw new IllegalArgumentException("Attempted to set different terrain from parent!");
		location.setPosition(terrain, x, y, z);
		updateChildrenTerrain();
	}
	
	public void setPosition(double x, double y, double z) {
		location.setPosition(x, y, z);
	}
	
	public void setOrientation(double oX, double oY, double oZ, double oW) {
		location.setOrientation(oX, oY, oZ, oW);
	}
	
	public void setHeading(double heading) {
		location.setHeading(heading);
	}
	
	public void setInstance(InstanceType instanceType, int instanceNumber) {
		location.setInstance(instanceType, instanceNumber);
	}
	
	private void updateChildrenTerrain() {
		Terrain terrain = getTerrain();
		for (SWGObject child : containedObjects) {
			child.setTerrain(terrain);
		}
		for (SWGObject child : slots.values()) {
			child.setTerrain(terrain);
		}
	}
	
	public int getCashBalance() {
		return cashBalance;
	}
	
	public void setCashBalance(long cashBalance) {
		if (cashBalance < 0)
			cashBalance = 0;
		if (cashBalance > 2_000_000_000L) { // 2 billion cap
			long leftover = cashBalance - 2_000_000_000L;
			cashBalance = 2_000_000_000L;
			long bank = bankBalance + leftover;
			leftover = bank - 2_000_000_000L;
			if (leftover > 0) {
				bank = 2_000_000_000L;
			}
			this.cashBalance = (int) cashBalance;
			sendDelta(1, 1, (int) cashBalance);
			setBankBalance(bank);
		} else {
			this.cashBalance = (int) cashBalance;
			sendDelta(1, 1, (int) cashBalance);
		}
	}
	
	public int getBankBalance() {
		return bankBalance;
	}

	public void setBankBalance(long bankBalance) {
		if (bankBalance < 0)
			bankBalance = 0;
		if (bankBalance > 2_000_000_000L) { // 2 billion cap
			long leftover = bankBalance - 2_000_000_000L;
			bankBalance = 2_000_000_000L;
			long cash = cashBalance + leftover;
			leftover = cash - 2_000_000_000L;
			if (leftover > 0) {
				cash = 2_000_000_000L;
			}
			this.bankBalance = (int) bankBalance;
			sendDelta(1, 0, (int) bankBalance);
			setCashBalance(cash);
		} else {
			this.bankBalance = (int) bankBalance;
			sendDelta(1, 0, (int) bankBalance);
		}
	}
	
	/**
	 * Removes amount from cash first, then bank after. Returns true if the
	 * operation was successful
	 * @param amount the amount to remove
	 * @return TRUE if successfully withdrawn, FALSE otherwise
	 */
	public boolean removeFromCashAndBank(long amount) {
		long amountBalance = bankBalance + cashBalance;
		if (amountBalance < amount)
			return false;
		if (cashBalance < amount) {
			setBankBalance(bankBalance - (amount - cashBalance));
			setCashBalance(0);
		} else {
			setCashBalance(cashBalance - amount);
		}
		return true;
	}
	
	/**
	 * Removes amount from bank first, then cash after. Returns true if the
	 * operation was successful
	 * @param amount the amount to remove
	 * @return TRUE if successfully withdrawn, FALSE otherwise
	 */
	public boolean removeFromBankAndCash(long amount) {
		long amountBalance = bankBalance + cashBalance;
		if (amountBalance < amount)
			return false;
		if (bankBalance < amount) {
			setCashBalance(cashBalance - (amount - bankBalance));
			setBankBalance(0);
		} else {
			setBankBalance(bankBalance - amount);
		}
		return true;
	}
	
	/**
	 * Adds amount to cash balance.
	 * @param amount the amount to add
	 */
	public void addToCash(long amount) {
		setCashBalance(cashBalance + amount);
	}
	
	/**
	 * Adds amount to bank balance.
	 * @param amount the amount to add
	 */
	public void addToBank(long amount) {
		setBankBalance(bankBalance + amount);
	}
	
	public void setStf(String stfFile, String stfKey) {
		this.stringId = new StringId(stfFile, stfKey);
	}
	
	public void setStringId(StringId stringId) {
		this.stringId = stringId;
	}
	
	public void setDetailStf(StringId detailStringId) {
		this.detailStringId = detailStringId;
	}
	
	public void setTemplate(String template) {
		this.template = template;
		this.crc = CRC.getCrc(template);
	}
	
	public void setObjectName(String name) {
		this.objectName = name;
		sendDelta(3, 2, objectName, StringType.UNICODE);
	}
	
	public void setVolume(int volume) {
		this.volume = volume;
	}
	
	public void setComplexity(float complexity) {
		this.complexity = complexity;
	}
	
	public void setBuildoutAreaId(int areaId) {
		this.areaId = areaId;
	}
	
	public void setBuildoutEvent(String event) {
		this.buildoutEvent = event;
	}
	
	public void setBuildoutTag(String tag) {
		this.buildoutTag = tag;
	}
	
	public void setArrangement(List<List<String>> arrangement) {
		this.arrangement = arrangement;
	}
	
	@Nullable
	public Player getOwner() {
		SWGObject parent = this.parent;
		return parent != null ? parent.getOwner() : null;
	}
	
	public Player getOwnerShallow() {
		return null;
	}
	
	@Nullable
	public SWGObject getParent() {
		return parent;
	}
	
	/**
	 * Gets the effective parent, which is the true parent unless mounted. When mounted, the effective parent is NULL
	 * @return the effective parent
	 */
	@Nullable
	public SWGObject getEffectiveParent() {
		return parent;
	}
	
	/**
	 * Gets the highest level parent, which does not have a parent itself
	 * @return the super parent
	 */
	@Nullable
	public SWGObject getSuperParent() {
		SWGObject sParent = parent;
		if (sParent == null)
			return null;
		while (sParent.getParent() != null)
			sParent = sParent.getParent();
		return sParent;
	}
	
	public StringId getStringId() {
		return stringId;
	}
	
	public StringId getDetailStringId() {
		return detailStringId;
	}
	
	public String getTemplate() {
		return template;
	}
	
	public int getCrc() {
		return crc;
	}
	
	public long getObjectId() {
		return objectId;
	}
	
	public InstanceLocation getInstanceLocation() {
		return location;
	}
	
	public Location getLocation() {
		return location.getLocation();
	}
	
	public Location getWorldLocation() {
		return location.getWorldLocation(this);
	}
	
	public double distanceTo(@NotNull SWGObject obj) {
		if (parent == obj.getParent())
			return getLocation().distanceTo(obj.getLocation());
		return getWorldLocation().distanceTo(obj.getWorldLocation());
	}
	
	public double flatDistanceTo(@NotNull SWGObject obj) {
		if (parent == obj.getParent())
			return getLocation().flatDistanceTo(obj.getLocation());
		return getWorldLocation().flatDistanceTo(obj.getWorldLocation());
	}
	
	public double getX() {
		return location.getPositionX();
	}
	
	public double getY() {
		return location.getPositionY();
	}
	
	public double getZ() {
		return location.getPositionZ();
	}
	
	public int getTruncX() {
		return (int) location.getPositionX();
	}
	
	public int getTruncY() {
		return (int) location.getPositionY();
	}
	
	public int getTruncZ() {
		return (int) location.getPositionZ();
	}
	
	@NotNull
	public Terrain getTerrain() {
		return location.getTerrain();
	}
	
	public String getObjectName() {
		return objectName;
	}
	
	public int getVolume() {
		return volume;
	}
	
	public float getComplexity() {
		return complexity;
	}
	
	public int getBuildoutAreaId() {
		return areaId;
	}
	
	public String getBuildoutEvent() {
		return buildoutEvent;
	}
	
	public String getBuildoutTag() {
		return buildoutTag;
	}
	
	public Object getServerAttribute(ServerAttribute key) {
		return serverAttributes.get(key);
	}
	
	public int getServerIntAttribute(ServerAttribute key) {
		return ((Number) serverAttributes.get(key)).intValue();
	}
	
	public long getServerLongAttribute(ServerAttribute key) {
		return ((Number) serverAttributes.get(key)).longValue();
	}
	
	public double getServerDoubleAttribute(ServerAttribute key) {
		return ((Number) serverAttributes.get(key)).doubleValue();
	}
	
	public String getServerTextAttribute(ServerAttribute key) {
		return (String) serverAttributes.get(key);
	}
	
	public StringId getServerStfAttribute(ServerAttribute key) {
		return (StringId) serverAttributes.get(key);
	}

	public void setServerAttribute(ServerAttribute key, Object value) {
		serverAttributes.put(key, value);
	}
	
	public Object getDataAttribute(ObjectDataAttribute key) {
		return dataAttributes.get(key);
	}
	
	public int getDataIntAttribute(ObjectDataAttribute key) {
		return ((Number) dataAttributes.get(key)).intValue();
	}
	
	public long getDataLongAttribute(ObjectDataAttribute key) {
		return ((Number) dataAttributes.get(key)).longValue();
	}
	
	public double getDataDoubleAttribute(ObjectDataAttribute key) {
		return ((Number) dataAttributes.get(key)).doubleValue();
	}
	
	public String getDataTextAttribute(ObjectDataAttribute key) {
		return (String) dataAttributes.get(key);
	}
	
	public StringId getDataStfAttribute(ObjectDataAttribute key) {
		return (StringId) dataAttributes.get(key);
	}

	public void setDataAttribute(ObjectDataAttribute key, Object value) {
		dataAttributes.put(key, value);
	}
	
	public ObjectAware getAwareness() {
		return awareness;
	}
	
	public List<List<String>> getArrangement() {
		return Collections.unmodifiableList(arrangement);
	}
	
	public boolean isNoTrade() {
		return noTrade;
	}
	
	public void setNoTrade(boolean noTrade) {
		this.noTrade = noTrade;
	}
	
	public AttributeList getAttributeList(CreatureObject viewer) {
		AttributeList attributeList = new AttributeList();
		
		if (noTrade) {
			attributeList.putNumber("no_trade", 1);
		}
		
		return attributeList;
	}
	
	public int getContainerType() {
		return containerType;
	}

	public void setContainerType(int containerType) {
		this.containerType = containerType;
	}

	public int getSlotArrangement() {
		return slotArrangement;
	}

	public void setSlotArrangement(int slotArrangement) {
		this.slotArrangement = slotArrangement;
	}
	
	public int getMaxContainerSize() {
		return getDataIntAttribute(ObjectDataAttribute.CONTAINER_VOLUME_LIMIT);
	}
	
	public int getNextUpdateCount() {
		return updateCounter.getAndIncrement();
	}
	
	public void setGenerated(boolean generated) {
		this.generated = generated;
	}
	
	public GameObjectType getGameObjectType() {
		return gameObjectType;
	}
	
	public void setGameObjectType(GameObjectType gameObjectType) {
		this.gameObjectType = gameObjectType;
	}
	
	public ContainerPermissions getContainerPermissions() {
		return permissions;
	}
	
	public void setContainerPermissions(ContainerPermissions permissions) {
		this.permissions = permissions;
	}
	
	public boolean isGenerated() {
		return generated;
	}
	
	public boolean isPersisted() {
		return persisted;
	}
	
	public void setPersisted(boolean persisted) {
		this.persisted = persisted;
	}
	
	/**
	 * Gets the arrangementId for the {@link SWGObject} for the current instance
	 * @param child
	 * @return Arrangement ID for the object
	 */
	public int getArrangementId(SWGObject child) {
		if (slotsAvailable.isEmpty() || child.getArrangement() == null)
			return -1;
		
		int arrangementId = -1;
		int slotSize = Integer.MAX_VALUE;
		
		int filledId = -1;
		int displaced = Integer.MAX_VALUE;
		
		int index = 3;
		
		for (List<String> arrangementList : child.getArrangement()) {
			index++;
			if (!slotsAvailable.keySet().containsAll(arrangementList))
				continue;
			
			if (arrangementList.size() < slotSize) {
				slotSize = arrangementList.size();
				arrangementId = index;
			}
			
			int calculatedDisplaced = 0;
			for (String slot : arrangementList) {
				if (slots.containsKey(slot))
					calculatedDisplaced++;
			}
			if (calculatedDisplaced < displaced || (calculatedDisplaced == displaced && child.getArrangement().get(filledId).size() > arrangementList.size())) {
				displaced = calculatedDisplaced;
				filledId = displaced;
			}
		}
		
		return arrangementId != -1 ? arrangementId : filledId;
	}
	
	public void setAware(AwarenessType type, Collection<SWGObject> aware) {
		awareness.setAware(type, aware);
	}
	
	public Set<SWGObject> getObjectsAware() {
		return getAware(AwarenessType.OBJECT);
	}
	
	/**
	 * Called when an object has entered this object's awareness
	 * @param aware the object that has entered awareness
	 */
	public void onObjectEnteredAware(SWGObject aware) {
		
	}
	
	/**
	 * Called when an object has exited this object's awareness
	 * @param aware the object that has entered awareness
	 */
	public void onObjectExitedAware(SWGObject aware) {
		
	}
	
	/**
	 * Called when this object has been moved somehow via awareness. This will
	 * not run on any buildout or snapshot object!
	 */
	public void onObjectMoved() {
		if (!isGenerated())
			return;
		for (SWGObject a : getAware()) {
			try {
				a.onObjectMoveInAware(this);
			} catch (Throwable t) {
				Log.e(t);
			}
		}
	}
	
	/**
	 * Called when an object moves within this object's awareness
	 * @param aware the object that moved
	 */
	public void onObjectMoveInAware(SWGObject aware) {
		
	}
	
	public Set<CreatureObject> getObserverCreatures() {
		return Collections.unmodifiableSet(observers);
	}
	
	public Set<Player> getObservers() {
		return observers.stream().map(CreatureObject::getOwnerShallow).filter(Objects::nonNull).collect(Collectors.toSet());
	}
	
	public void addObserver(CreatureObject player) {
		observers.add(player);
	}
	
	public void removeObserver(CreatureObject player) {
		observers.remove(player);
	}
	
	public Set<SWGObject> getAware() {
		return awareness.getAware();
	}
	
	public Set<SWGObject> getAware(AwarenessType type) {
		return awareness.getAware(type);
	}
	
	public boolean isAwareOf(SWGObject obj) {
		return awareness.isAwareOf(obj);
	}
	
	public void sendObservers(SWGPacket packet) {
		for (CreatureObject observer : observers) {
			observer.sendSelf(packet);
		}
	}
	
	public void sendObservers(SWGPacket packet1, SWGPacket packet2) {
		for (CreatureObject observer : observers) {
			observer.sendSelf(packet1, packet2);
		}
	}
	
	public void sendObservers(SWGPacket packet1, SWGPacket packet2, SWGPacket packet3) {
		for (CreatureObject observer : observers) {
			observer.sendSelf(packet1, packet2, packet3);
		}
	}
	
	public void sendObservers(SWGPacket packet1, SWGPacket packet2, SWGPacket packet3, SWGPacket packet4) {
		for (CreatureObject observer : observers) {
			observer.sendSelf(packet1, packet2, packet3, packet4);
		}
	}
	
	public void sendObservers(SWGPacket packet1, SWGPacket packet2, SWGPacket packet3, SWGPacket packet4, SWGPacket packet5) {
		for (CreatureObject observer : observers) {
			observer.sendSelf(packet1, packet2, packet3, packet4, packet5);
		}
	}
	
	public void sendSelf(SWGPacket packet) {
		Player owner = getOwner();
		if (owner != null)
			owner.sendPacket(packet);
	}
	
	public void sendSelf(SWGPacket packet1, SWGPacket packet2) {
		Player owner = getOwner();
		if (owner != null)
			owner.sendPacket(packet1, packet2);
	}
	
	public void sendSelf(SWGPacket packet1, SWGPacket packet2, SWGPacket packet3) {
		Player owner = getOwner();
		if (owner != null)
			owner.sendPacket(packet1, packet2, packet3);
	}
	
	public void sendSelf(SWGPacket packet1, SWGPacket packet2, SWGPacket packet3, SWGPacket packet4) {
		Player owner = getOwner();
		if (owner != null)
			owner.sendPacket(packet1, packet2, packet3, packet4);
	}
	
	public void sendSelf(SWGPacket packet1, SWGPacket packet2, SWGPacket packet3, SWGPacket packet4, SWGPacket packet5) {
		Player owner = getOwner();
		if (owner != null)
			owner.sendPacket(packet1, packet2, packet3, packet4, packet5);
	}
	
	public boolean isInBuilding() {
		SWGObject parent = getParent();
		if (parent == null)
			return false;
		parent = parent.getParent();
		return parent instanceof BuildingObject;
	}

	@Override
	public String toString() {
		return String.format("%s[%d '%s' %s]", getClass().getSimpleName(), objectId, objectName, template.replace("object/", ""));
	}
	
	@Override
	public int compareTo(@NotNull SWGObject obj) {
		return Long.compare(objectId, obj.getObjectId());
	}
	
	@Override
	public boolean equals(Object o) {
		return o instanceof SWGObject && objectId == ((SWGObject) o).objectId;
	}
	
	@Override
	public int hashCode() {
		return Long.hashCode(objectId);
	}
	
	@Override
	public void createBaseline1(Player target, BaselineBuilder bb) {
		super.createBaseline1(target, bb); // 0 variables
//		if (getStringId().toString().equals("@obj_n:unknown_object"))
//			return;
		bb.addInt(bankBalance); // 0
		bb.addInt(cashBalance); // 1
		
		bb.incrementOperandCount(2);
	}
	
	@Override
	protected void createBaseline3(Player target, BaselineBuilder bb) {
		super.createBaseline3(target, bb);
		bb.addFloat(complexity); // 0
		bb.addObject(stringId); // 1
		bb.addUnicode(objectName); // custom name -- 2
		bb.addInt(volume); // 3

		bb.incrementOperandCount(4);
	}
	
	@Override
	protected void createBaseline6(Player target, BaselineBuilder bb) {
		super.createBaseline6(target, bb);
		bb.addInt(ProjectSWG.getGalaxy().getId()); // 0
		bb.addObject(detailStringId); // 1
		
		bb.incrementOperandCount(2);
	}
	
	@Override
	protected void parseBaseline1(NetBuffer buffer) {
		super.parseBaseline1(buffer);
//		if (getStringId().toString().equals("@obj_n:unknown_object"))
//			return;
		bankBalance = buffer.getInt();
		cashBalance = buffer.getInt();
	}
	
	@Override
	protected void parseBaseline3(NetBuffer buffer) {
		super.parseBaseline3(buffer);
		complexity = buffer.getFloat();
		stringId = buffer.getEncodable(StringId.class);
		objectName = buffer.getUnicode();
		volume = buffer.getInt();
	}
	
	@Override
	protected void parseBaseline6(NetBuffer buffer) {
		super.parseBaseline6(buffer);
		buffer.getInt(); // Immutable ... can't change the galaxy id
		detailStringId = buffer.getEncodable(StringId.class);
	}
	
	@Override
	public void saveMongo(MongoData data) {
		data.putDocument("base1", new MongoData());
		data.putDocument("base3", new MongoData());
		data.putDocument("base4", new MongoData());
		data.putDocument("base6", new MongoData());
		data.putDocument("base8", new MongoData());
		data.putDocument("base9", new MongoData());
		{
			MongoData base1 = data.getDocument("base1");
			base1.putInteger("cashBalance", cashBalance);
			base1.putInteger("bankBalance", bankBalance);
		}
		{
			MongoData base3 = data.getDocument("base3");
			base3.putFloat("complexity", complexity);
			base3.putDocument("stringId", stringId);
			base3.putString("objectName", objectName);
			base3.putInteger("volume", volume);
		}
		{
			MongoData base6 = data.getDocument("base6");
			// galaxyId
			base6.putDocument("detailStringId", detailStringId);
		}
		{
			SWGObject parent = this.parent;
			if (parent != null) {
				SWGObject grandparent = parent.getParent();
				if (parent instanceof CellObject && grandparent instanceof BuildingObject) {
					data.putLong("parent", grandparent.getObjectId());
					data.putInteger("parentCell", ((CellObject) parent).getNumber());
				} else {
					data.putLong("parent", parent.getObjectId());
					data.putInteger("parentCell", 0);
				}
			} else {
				data.putLong("parent", 0);
				data.putInteger("parentCell", 0);
			}
		}
		data.putLong("id", objectId);
		data.putString("template", template);
		data.putDocument("location", location);
		data.putDocument("permissions", ContainerPermissions.save(new MongoData(), permissions));
		data.putMap("serverAttributes", serverAttributes, ServerAttribute::getKey, Function.identity());
		data.putBoolean("persisted", persisted);
		data.putBoolean("noTrade", noTrade);
	}
	
	@Override
	public void readMongo(MongoData data) {
		{
			MongoData base1 = data.getDocument("base1");
			cashBalance = base1.getInteger("cashBalance", cashBalance);
			bankBalance = base1.getInteger("bankBalance", bankBalance);
		}
		{
			MongoData base3 = data.getDocument("base3");
			complexity = base3.getFloat("complexity", 0);
			stringId = base3.getDocument("stringId", new StringId());
			objectName = base3.getString("objectName", "");
			volume = base3.getInteger("volume", 0);
		}
		{
			MongoData base6 = data.getDocument("base6");
			// galaxyId
			detailStringId = base6.getDocument("detailStringId", new StringId());
		}
		location.readMongo(data.getDocument("location"));
		permissions = ContainerPermissions.create(data.getDocument("permissions"));
		data.getMap("serverAttributes", String.class, Object.class).forEach((key, val) -> serverAttributes.put(ServerAttribute.getFromKey(key), val));
		persisted = data.getBoolean("persisted", false);
		noTrade = data.getBoolean("noTrade", false);
	}
	
}
