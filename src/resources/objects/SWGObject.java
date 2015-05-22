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
package resources.objects;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import network.packets.Packet;
import network.packets.swg.zone.SceneCreateObjectByCrc;
import network.packets.swg.zone.SceneDestroyObject;
import network.packets.swg.zone.SceneEndBaselines;
import network.packets.swg.zone.UpdateContainmentMessage;
import network.packets.swg.zone.UpdateTransformsMessage;
import network.packets.swg.zone.baselines.Baseline.BaselineType;
import network.packets.swg.zone.object_controller.DataTransform;
import resources.Location;
import resources.common.CRC;
import resources.encodables.Stf;
import resources.network.BaselineBuilder;
import resources.network.DeltaBuilder;
import resources.player.Player;
import resources.player.PlayerState;
import utilities.Encoder.StringType;

public class SWGObject implements Serializable, Comparable<SWGObject> {
	
	private static final long serialVersionUID = 1L;
	
	private final List <SWGObject> children;
	private final Location location;
	private final long objectId;
	private final Map <String, SWGObject> slots; // Can only be occupied one time, containers are slots who have children
	private final Map <String, String> attributes;
	private final Map <String, Object> templateAttributes;
	private transient List <SWGObject> objectsAware;
	private List <List <String>> arrangement;
	
	private Player	owner		= null;
	private SWGObject	parent	= null;
	private Stf 	stf			= new Stf("", "");
	private Stf 	detailStf	= new Stf("", "");
	private String	template	= "";
	private int		crc			= 0;
	private String	objectName	= "";
	private int		volume		= 0; // applies to containers only
	private float	complexity	= 1;
	private int		containmentType = 4;
	private int		transformCounter = 0;
	
	public SWGObject() {
		this(0);
	}
	
	public SWGObject(long objectId) {
		this.objectId = objectId;
		this.location = new Location();
		this.children = new Vector<SWGObject>();
		this.objectsAware = new Vector<SWGObject>();
		this.slots = new HashMap<String, SWGObject>(); // Concurrent maps wont allow for null keys/values, which is what the empty slots are set to :/
		this.attributes = new LinkedHashMap<String, String>();
		this.templateAttributes = new HashMap<String, Object>();
	}
	
	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		ois.defaultReadObject();
		objectsAware = new LinkedList<SWGObject>();
	}
	
	// TODO: Use a "transfer" method for switching objects between parents, this will also check volume limits
	public void addChild(SWGObject object) {
		synchronized (children) {
			if (!children.contains(object))
				children.add(object);
		}
		updateContainment(object);
	}
	
	private void updateContainment(SWGObject child) {
		if (child.parent != null)
			child.parent.removeChild(child);
		child.parent = this;
		Integer containmentType = (Integer)child.getTemplateAttribute("containerType");
		if (containmentType == null)
			child.containmentType = 4;
		else
			child.containmentType = containmentType;
		// TODO: Set containmentType based on if object is in a slot (4) or a container (-1)
		sendObserversAndSelf(new UpdateContainmentMessage(child.objectId, objectId, containmentType));
	}
	
	public void addAttribute(String attribute, String value) {
		attributes.put(attribute, value);
	}
	
	public void addObjectSlot(String name, SWGObject object) {
		synchronized (slots) {
			slots.put(name, object);
		}
	}
	
	public SWGObject getSlottedObject(String slot) {
		if (!slots.containsKey(slot)) {
			System.err.println(getTemplate() + " doesn't contain slot " + slot + "!");
			return null;
		}
		
		return slots.get(slot);
	}
	
	public boolean hasSlot(String slot) {
		return slots.containsKey(slot);
	}
	
	public boolean setSlot(String slot, SWGObject obj) {
		if (!slots.containsKey(slot)) {
			System.err.println("Could not set " + obj.getTemplate() + " to " + getTemplate() + " as it doesn't contain slot " + slot + "!");
			return false;
		}
		
		List<String> occupiedAvailSlots = new ArrayList<String>();
		List<String> arrangement = obj.getArrangement().get(0); // We only care about the main list here, not the children lists
		for (String occupies : arrangement) {
			if (hasSlot(occupies))
				occupiedAvailSlots.add(slot);
			else
				break;
		}
		
		if (occupiedAvailSlots.size() != arrangement.size()) {
			System.err.println("Doesn't have all the slots.");
			return false;
		}
		
		for (String availSlot : occupiedAvailSlots) {
			obj.setParent(this);
			addObjectSlot(availSlot, obj);
			sendObserversAndSelf(new UpdateContainmentMessage(obj.objectId, objectId, containmentType));
		}
		
		return true;
	}
	
	public void clearSlot(String slot) {
		if (!slots.containsKey(slot)) {
			System.err.println("Could not clear " + slot + " as it doesn't contain that slot!");
			return;
		}
		
		synchronized(slots) {
			slots.put(slot, null);
		}
	}
	
	// TODO: Use a "transfer" method for switching objects between parents, this will also check volume limits
	public void removeChild(SWGObject object) {
		synchronized (children) {
			children.remove(object);
		}
		object.parent = null;
	}
	
	public Map <String, SWGObject> getSlots() {
		return new HashMap<String, SWGObject>(slots);
	}
	
	public List <SWGObject> getChildren() {
		return new ArrayList<SWGObject>(children);
	}
	
	public void setOwner(Player player) {
		this.owner = player;
	}
	
	public void setParent(SWGObject parent) {
		this.parent = parent;
	}
	
	public void setLocation(Location l) {
		if (l == null)
			return;
		location.mergeWith(l);
	}
	
	public void setLocation(double x, double y, double z) {
		location.mergeLocation(x, y, z);
	}
	
	public void setStf(String stfFile, String stfKey) {
		this.stf = new Stf(stfFile, stfKey);
	}
	
	public void setStf(String stf) {
		this.stf = new Stf(stf);
	}
	
	public void setDetailStf(String stfFile, String stfKey) {
		this.detailStf = new Stf(stfFile, stfKey);
	}
	
	public void setDetailStf(String stf) {
		this.detailStf = new Stf(stf);
	}
	
	public void setTemplate(String template) {
		this.template = template;
		this.crc = CRC.getCrc(template);
	}
	
	public void setName(String name) {
		this.objectName = name;
	}
	
	public void setVolume(int volume) {
		this.volume = volume;
	}
	
	public void setComplexity(float complexity) {
		this.complexity = complexity;
	}
	
	public Player getOwner() {
		if (owner != null)
			return owner;
		
		if (getParent() != null)
			return getParent().getOwner();	// Ziggy: Player owner is found recursively
		
		return null;
	}
	
	public SWGObject getParent() {
		return parent;
	}
	
	public Stf getStf() {
		return stf;
	}
	
	public Stf getDetailStf() {
		return detailStf;
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
	
	public Location getLocation() {
		return location;
	}
	
	public String getName() {
		return objectName;
	}
	
	public int getVolume() {
		return volume;
	}
	
	public float getComplexity() {
		return complexity;
	}
	
	public Object getTemplateAttribute(String key) {
		return templateAttributes.get(key);
	}
	
	public void setTemplateAttribute(String key, Object value) {
		templateAttributes.put(key, value);
	}
	
	public List<List<String>> getArrangement() {
		return arrangement;
	}
	
	public void setArrangment(List<List<String>> arrangement) {
		this.arrangement = arrangement;
	}
	
	public String getAttribute(String attribute) {
		return attributes.get(attribute);
	}
	
	public Map<String, String> getAttributes() {
		return attributes;
	}
	
	protected final void sendSceneCreateObject(Player target) {
		SceneCreateObjectByCrc create = new SceneCreateObjectByCrc();
		create.setObjectId(objectId);
		create.setLocation(location);
		create.setObjectCrc(crc);
		target.sendPacket(create);
		if (parent != null)
			target.sendPacket(new UpdateContainmentMessage(objectId, parent.getObjectId(), containmentType));

	}
	
	protected final void sendSceneDestroyObject(Player target) {
		SceneDestroyObject destroy = new SceneDestroyObject();
		destroy.setObjectId(objectId);
		target.sendPacket(destroy);
	}
	
	protected void createObject(Player target) {
		sendSceneCreateObject(target);
		//if (target.getCreatureObject().getOwner() == getOwner()) //TODO: Update for view permissions
			createChildrenObjects(target);
		target.sendPacket(new SceneEndBaselines(getObjectId()));
	}
	
	protected void destroyObject(Player target) {
		sendSceneDestroyObject(target);
	}
	
	public void clearAware() {
		SWGObject [] objects;
		synchronized (objectsAware) {
			objects = objectsAware.toArray(new SWGObject[objectsAware.size()]);
		}
		for (SWGObject o : objects) {
			o.awarenessOutOfRange(this);
			awarenessOutOfRange(o);
		}
	}
	
	public List <SWGObject> getObjectsAware() {
		synchronized (objectsAware) {
			return Collections.unmodifiableList(objectsAware);
		}
	}
	
	public void sendObserversAndSelf(Packet ... packets) {
		sendSelf(packets);
		sendObservers(packets);
	}
	
	public void sendObservers(Packet ... packets) {
		synchronized (objectsAware) {
			for (SWGObject obj : objectsAware) {
				Player p = obj.getOwner();
				if (p == null || p.getPlayerState() != PlayerState.ZONED_IN)
					continue;
				p.sendPacket(packets);
			}
			
			SWGObject parent = getParent();
			
			if(parent != null)
				parent.sendObservers(packets);
			
		}
	}
	
	public void sendSelf(Packet ... packets) {
		Player owner = getOwner();
		if (owner != null)
			owner.sendPacket(packets);
	}
	
	public void updateObjectAwareness(List <SWGObject> withinRange) {
		synchronized (objectsAware) {
			List <SWGObject> outOfRange = new ArrayList<SWGObject>(objectsAware);
			outOfRange.removeAll(withinRange);
			for (SWGObject o : outOfRange)
				awarenessOutOfRange(o);
			for (SWGObject o : withinRange)
				awarenessInRange(o);
		}
	}
	
	private void awarenessOutOfRange(SWGObject o) {
		synchronized (objectsAware) {
			if (objectsAware.remove(o)) {
				if (o.getOwner() != null) {
					destroyObject(o.getOwner());
				}
				if (getOwner() != null)
					o.awarenessOutOfRange(this);
			}
		}
	}
	
	private void awarenessInRange(SWGObject o) {
		synchronized (objectsAware) {
			if (!objectsAware.contains(o)) {
				objectsAware.add(o);
				if (o.getOwner() != null) {
					createObject(o.getOwner());
				}
				if (getOwner() != null)
					o.awarenessInRange(this);
			}
		}
	}
	
	public void sendDataTransforms(DataTransform dTransform) {
		Location loc = dTransform.getLocation();
		float speed = dTransform.getSpeed();
		sendDataTransforms(loc, (byte) dTransform.getMovementAngle(), speed);
	}
	
	public void sendDataTransforms(Location loc, int direction, float speed) {
		UpdateTransformsMessage transform = new UpdateTransformsMessage();
		transform.setObjectId(getObjectId()); // (short) (xPosition * 4 + 0.5)
		transform.setX((short) (loc.getX() * 4 + 0.5));
		transform.setY((short) (loc.getY() * 4 + 0.5));
		transform.setZ((short) (loc.getZ() * 4 + 0.5));
		transform.setUpdateCounter(transformCounter++);
		transform.setDirection((byte) direction);
		transform.setSpeed(speed);
		sendObserversAndSelf(transform);
	}
	
	protected void createChildrenObjects(Player target) {
		for (SWGObject child : children) {
			child.createObject(target);
		}
		// TODO: We will need permission checks here in the future which will create the object based on another players permissions to view that slot.
		for (SWGObject slotEntry : slots.values()) {
			if (slotEntry == null)
				continue;
			
			slotEntry.createObject(target);
		}
	}
	
	@Override
	public String toString() {
		return "SWGObject[ID=" + objectId + " NAME=" + objectName + " TEMPLATE=" + template + "]";
	}
	
	@Override
	public int compareTo(SWGObject obj) {
		if (getObjectId() < obj.getObjectId())
			return -1;
		if (getObjectId() == obj.getObjectId())
			return 0;
		return 1;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof SWGObject))
			return false;
		return getObjectId() == ((SWGObject)o).getObjectId();
	}
	
	@Override
	public int hashCode() {
		return Long.valueOf(getObjectId()).hashCode();
	}
	
	public void createBaseline1(Player target, BaselineBuilder bb) {

	}
	
	public void createBaseline3(Player target, BaselineBuilder bb) {
		bb.addFloat(complexity); // 0
		bb.addObject(stf); // 1
		bb.addUnicode(objectName); // custom name -- 2
		bb.addInt(volume); // 3

		bb.incrementOperandCount(4);
	}
	
	public void createBaseline4(Player target, BaselineBuilder bb) {

	}
	
	public void createBaseline6(Player target, BaselineBuilder bb) {
		bb.addInt(target.galaxyId); // 0
		bb.addObject(detailStf); // 1
		
		bb.incrementOperandCount(2);
	}
	
	public void createBaseline7(Player target, BaselineBuilder bb) {

	}
	
	public void createBaseline8(Player target, BaselineBuilder bb) {

	}
	
	public void createBaseline9(Player target, BaselineBuilder bb) {

	}
	
	public void sendDelta(int type, int update, Object value) {

	}
	
	public final void sendDelta(BaselineType baseline, int type, int update, Object value) {
		if (owner == null || (owner.getPlayerState() != PlayerState.ZONED_IN))
			return;

		DeltaBuilder builder = new DeltaBuilder(this, baseline, type, update, value);
		builder.send();
	}
	
	public final void sendDelta(BaselineType baseline, int type, int update, Object value, StringType strType) {
		if (owner == null || (owner.getPlayerState() != PlayerState.ZONED_IN))
			return;
		
		DeltaBuilder builder = new DeltaBuilder(this, baseline, type, update, value, strType);
		builder.send();
	}
	
	/* Baseline send permissions based on packet observations:
	 * 
	 * Baseline1 sent if you have full permissions to the object.
	 * Baseline4 sent if you have full permissions to the object.
	 * 
	 * Baseline8 sent if you have some permissions to the object.
	 * Baseline9 sent if you have some permissions to the object.
	 * 
	 * Baseline3 always sent.
	 * Baseline6 always sent.
	 * 
	 * Baseline7 sent on using the object.
	 * 
	 * Only sent if they are defined (can still be empty if defined).
	 */
}
