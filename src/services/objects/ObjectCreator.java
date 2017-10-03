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

import java.lang.reflect.InvocationTargetException;
import java.util.Map.Entry;

import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.data.swgfile.visitors.ObjectData;
import com.projectswg.common.data.swgfile.visitors.ObjectData.ObjectDataAttribute;
import com.projectswg.common.data.swgfile.visitors.SlotArrangementData;
import com.projectswg.common.data.swgfile.visitors.SlotDescriptorData;
import com.projectswg.common.debug.Log;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;

import resources.objects.GameObjectType;
import resources.objects.SWGObject;
import resources.objects.building.BuildingObject;
import resources.objects.cell.CellObject;
import resources.objects.creature.CreatureObject;
import resources.objects.factory.FactoryObject;
import resources.objects.group.GroupObject;
import resources.objects.guild.GuildObject;
import resources.objects.installation.InstallationObject;
import resources.objects.intangible.IntangibleObject;
import resources.objects.manufacture.ManufactureSchematicObject;
import resources.objects.mission.MissionObject;
import resources.objects.player.PlayerObject;
import resources.objects.resource.ResourceContainerObject;
import resources.objects.ship.ShipObject;
import resources.objects.sound.SoundObject;
import resources.objects.staticobject.StaticObject;
import resources.objects.tangible.TangibleObject;
import resources.objects.waypoint.WaypointObject;
import resources.objects.weapon.WeaponObject;

public final class ObjectCreator {
	
	private static final Object OBJECT_ID_MUTEX = new Object();
	private static long nextObjectId = 1;
	
	private static void updateMaxObjectId(long objectId) {
		synchronized (OBJECT_ID_MUTEX) {
			if (objectId >= nextObjectId)
				nextObjectId = objectId+1;
		}
	}
	
	private static long getNextObjectId() {
		synchronized (OBJECT_ID_MUTEX) {
			return nextObjectId++;
		}
	}
	
	public static SWGObject createObjectFromTemplate(long objectId, String template) {
		if (!template.startsWith("object/"))
			return null;
		if (!template.endsWith(".iff"))
			return null;
		template = ClientFactory.formatToSharedFile(template);
		ObjectData attributes = (ObjectData) ClientFactory.getInfoFromFile(template, true);
		if (attributes == null)
			return null;
		SWGObject obj = createObjectFromType(objectId, template, attributes);
		if (obj == null)
			return null;
		obj.setTemplate(template);

		handlePostCreation(obj, attributes);
		updateMaxObjectId(objectId);
		return obj;
	}
	
	public static <T extends SWGObject> T createObjectFromTemplate(long objectId, String template, Class <T> c) {
		T obj;
		try {
			obj = c.getConstructor(Long.TYPE).newInstance(objectId);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			Log.e(e);
			obj = null;
		}
		if (obj == null)
			return null;
		template = ClientFactory.formatToSharedFile(template);
		obj.setTemplate(template);

		handlePostCreation(obj, (ObjectData) ClientFactory.getInfoFromFile(template, true));
		updateMaxObjectId(objectId);
		return obj;
	}
	
	public static SWGObject createObjectFromTemplate(String template) {
		return createObjectFromTemplate(getNextObjectId(), template);
	}
	
	public static <T extends SWGObject> T createObjectFromTemplate(String template, Class <T> c) {
		return createObjectFromTemplate(getNextObjectId(), template, c);
	}
	
	private static SWGObject createObjectFromType(long objectId, String template, ObjectData attributes) {
		Integer gotInt = (Integer) attributes.getAttribute(ObjectDataAttribute.GAME_OBJECT_TYPE);
		if (gotInt == null)
			return null;
		
		GameObjectType got = GameObjectType.getTypeFromId(gotInt);
		BaselineType baseline = got.getBaselineType();
		if (baseline == null) {
			return createSlowFromType(objectId, template);
		}
		
		switch (baseline) {
			case BUIO:	return new BuildingObject(objectId);
			case CREO:	return new CreatureObject(objectId);
			case FCYT:	return new FactoryObject(objectId);
			case GILD:	return new GuildObject(objectId);
			case GRUP:	return new GroupObject(objectId);
			case INSO:	return new InstallationObject(objectId);
			case ITNO:	return new IntangibleObject(objectId);
			case MISO:	return new MissionObject(objectId);
			case MSCO:	return new ManufactureSchematicObject(objectId);
			case PLAY:	return new PlayerObject(objectId);
			case RCNO:	return new ResourceContainerObject(objectId);
			case SCLT:	return new CellObject(objectId);
			case SHIP:	return new ShipObject(objectId);
			case STAO:	return new StaticObject(objectId);
			case TANO:	return new TangibleObject(objectId);
			case WAYP:	return new WaypointObject(objectId);
			case WEAO:	return new WeaponObject(objectId);
			/* Unimplemented baselines */
			default:
				Log.w("Could not create unimplemented baseline: %s", baseline);
				return null;
		}
	}
	
	private static SWGObject createSlowFromType(long objectId, String template) {
		String type = getObjectType(template);
		switch (type) {
			case "building":				return new BuildingObject(objectId);
			case "cell":					return new CellObject(objectId);
			case "creature":				return new CreatureObject(objectId);
			case "factory":					return new FactoryObject(objectId);
			case "group":					return new GroupObject(objectId);
			case "guild":					return new GuildObject(objectId);
			case "installation":			return new InstallationObject(objectId);
			case "intangible":				return new IntangibleObject(objectId);
			case "manufacture_schematic":	return new ManufactureSchematicObject(objectId);
			case "mission":					return new MissionObject(objectId);
			case "mobile":					return new CreatureObject(objectId);
			case "player":					return new PlayerObject(objectId);
			case "resource_container":		return new ResourceContainerObject(objectId);
			case "ship":					return new ShipObject(objectId);
			case "soundobject":				return new SoundObject(objectId);
			case "static":					return new StaticObject(objectId);
			case "tangible":				return new TangibleObject(objectId);
			case "waypoint":				return new WaypointObject(objectId);
			case "weapon":					return new WeaponObject(objectId);
			default:						Log.e("Unknown type: " + type); return null;
		}
	}
	
	private static void handlePostCreation(SWGObject object, ObjectData attributes) {
		addObjectAttributes(object, attributes);
		createObjectSlots(object);
		Object got = object.getDataAttribute(ObjectDataAttribute.GAME_OBJECT_TYPE);
		if (got != null)
			object.setGameObjectType(GameObjectType.getTypeFromId((Integer) got));
	}

	private static void addObjectAttributes(SWGObject obj, ObjectData attributes) {
		if (attributes == null)
			return;

		for (Entry<ObjectDataAttribute, Object> e : attributes.getAttributes().entrySet()) {
			setObjectAttribute(e.getKey(), e.getValue(), obj);
		}
	}

	private static void setObjectAttribute(ObjectDataAttribute key, Object value, SWGObject object) {
		object.setDataAttribute(key, value);
		switch (key) {
			case OBJECT_NAME: object.setStringId(value.toString()); break;
			case DETAILED_DESCRIPTION: object.setDetailStringId(value.toString()); break;
			case CONTAINER_TYPE: object.setContainerType((Integer) value); break;
			default: break;
		}
	}

	private static void createObjectSlots(SWGObject object) {
		if (object.getDataAttribute(ObjectDataAttribute.SLOT_DESCRIPTOR_FILENAME) != null) {
			// These are the slots that the object *HAS*
			SlotDescriptorData descriptor = (SlotDescriptorData) ClientFactory.getInfoFromFile((String) object.getDataAttribute(ObjectDataAttribute.SLOT_DESCRIPTOR_FILENAME), true);
			if (descriptor == null)
				return;

			for (String slotName : descriptor.getSlots()) {
				object.setSlot(slotName, null);
			}
		}
		
		if (object.getDataAttribute(ObjectDataAttribute.ARRANGEMENT_DESCRIPTOR_FILENAME) != null) {
			// This is what slots the created object is able to go into/use
			SlotArrangementData arrangementData = (SlotArrangementData) ClientFactory.getInfoFromFile((String) object.getDataAttribute(ObjectDataAttribute.ARRANGEMENT_DESCRIPTOR_FILENAME), true);
			if (arrangementData == null)
				return;

			object.setArrangement(arrangementData.getArrangement());
		}
	}

	/*
		Misc helper methods
	 */
	private static String getObjectType(String template) {
		return template.substring(7, template.indexOf('/', 8));
	}
	
}
