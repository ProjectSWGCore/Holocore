package services.objects;

import java.util.Map.Entry;

import resources.client_info.ClientFactory;
import resources.client_info.visitors.ObjectData;
import resources.client_info.visitors.SlotArrangementData;
import resources.client_info.visitors.SlotDescriptorData;
import resources.objects.SWGObject;
import resources.objects.building.BuildingObject;
import resources.objects.cell.CellObject;
import resources.objects.creature.CreatureObject;
import resources.objects.installation.InstallationObject;
import resources.objects.intangible.IntangibleObject;
import resources.objects.mobile.MobileObject;
import resources.objects.player.PlayerObject;
import resources.objects.resource.ResourceContainerObject;
import resources.objects.ship.ShipObject;
import resources.objects.sound.SoundObject;
import resources.objects.staticobject.StaticObject;
import resources.objects.tangible.TangibleObject;
import resources.objects.waypoint.WaypointObject;
import resources.objects.weapon.WeaponObject;

public final class ObjectCreator {
	
	private static final ClientFactory clientFac = new ClientFactory();
	
	public static final SWGObject createObjectFromTemplate(long objectId, String template) {
		if (!template.startsWith("object/"))
			return null;
		if (!template.endsWith(".iff"))
			return null;
		SWGObject obj = createObjectFromType(objectId, getFirstTemplatePart(template.substring(7, template.length()-7-4)));
		if (obj == null)
			return null;
		addObjectAttributes(obj, template);
		obj.setTemplate(template);
		return obj;
	}
	
	private static SWGObject createObjectFromType(long objectId, String type) {
		switch (type) {
			case "creature":			return new CreatureObject(objectId);
			case "player":				return new PlayerObject(objectId);
			case "tangible":			return new TangibleObject(objectId);
			case "intangible":			return new IntangibleObject(objectId);
			case "waypoint":			return new WaypointObject(objectId);
			case "weapon":				return new WeaponObject(objectId);
			case "building":			return new BuildingObject(objectId);
			case "cell":				return new CellObject(objectId);
			case "static":				return new StaticObject(objectId);
			case "resource_container":	return new ResourceContainerObject(objectId);
			case "installation":		return new InstallationObject(objectId);
			case "ship":				return new ShipObject(objectId);
			case "soundobject":			return new SoundObject(objectId);
			case "mobile":				return new MobileObject(objectId);
		}
		return null;
	}
	
	private static void addObjectAttributes(SWGObject obj, String template) {
		ObjectData attributes = (ObjectData) clientFac.getInfoFromFile(ClientFactory.formatToSharedFile(template));
		
		String stf = (String) attributes.getAttribute(ObjectData.OBJ_STF);
		String detailStf = (String) attributes.getAttribute(ObjectData.DETAIL_STF);
		Integer volumeLimit = (Integer) attributes.getAttribute(ObjectData.VOLUME_LIMIT);
		
		obj.setStf(stf);
		if (detailStf != null)
			obj.setDetailStf(detailStf);
		if (volumeLimit != null)
			obj.setVolume(volumeLimit);
		for (Entry<String, Object> e : attributes.getAttributes().entrySet()) {
			obj.setTemplateAttribute(e.getKey(), e.getValue());
		}
		
		addSlotsToObject(obj, attributes);
	}
	
	private static void addSlotsToObject(SWGObject obj, ObjectData attributes) {
		if (attributes.getAttribute(ObjectData.SLOT_DESCRIPTOR) != null) {
			// These are the slots that the object *HAS*
			SlotDescriptorData descriptor = (SlotDescriptorData) clientFac.getInfoFromFile((String) attributes.getAttribute(ObjectData.SLOT_DESCRIPTOR));
			
			for (String slotName : descriptor.getSlots()) {
				obj.addObjectSlot(slotName, null);
			}
		}
		
		if (attributes.getAttribute(ObjectData.ARRANGEMENT_FILE) != null) {
			// This is what slots the object *USES*
			SlotArrangementData arrangementData = (SlotArrangementData) clientFac.getInfoFromFile((String) attributes.getAttribute(ObjectData.ARRANGEMENT_FILE));
			obj.setArrangment(arrangementData.getArrangement());
		}
	}
	
	private static String getFirstTemplatePart(String template) {
		int ind = template.indexOf('/');
		if (ind == -1)
			return "";
		return template.substring(0, ind);
	}
	
}
