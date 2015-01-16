package services.objects;

import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.objects.intangible.IntangibleObject;
import resources.objects.player.PlayerObject;
import resources.objects.tangible.TangibleObject;
import resources.objects.waypoint.WaypointObject;
import resources.objects.weapon.WeaponObject;

final class ObjectCreator {
	
	public static final SWGObject createObjectFromTemplate(long objectId, String template) {
		if (!template.startsWith("object/"))
			return null;
		if (!template.endsWith(".iff"))
			return null;
		template = template.substring(7, template.length()-7-4);
		switch (getFirstTemplatePart(template)) {
			case "creature": return createCreatureObject(objectId, template);
			case "player": return createPlayerObject(objectId, template);
			case "tangible": return createTangibleObject(objectId, template);
			case "intangible": return createIntangibleObject(objectId, template);
			case "waypoint": return createWaypointObject(objectId, template);
			case "weapon": return createWeaponObject(objectId, template);
			case "building": break;
			case "cell": break;
		}
		return null;
	}
	
	private static String getFirstTemplatePart(String template) {
		int ind = template.indexOf('/');
		if (ind == -1)
			return "";
		return template.substring(0, ind);
	}
	
	private static CreatureObject createCreatureObject(long objectId, String template) {
		return new CreatureObject(objectId);
	}
	
	private static PlayerObject createPlayerObject(long objectId, String template) {
		return new PlayerObject(objectId);
	}
	
	private static TangibleObject createTangibleObject(long objectId, String template) {
		return new TangibleObject(objectId);
	}
	
	private static IntangibleObject createIntangibleObject(long objectId, String template) {
		return new IntangibleObject(objectId);
	}
	
	private static WaypointObject createWaypointObject(long objectId, String template) {
		return new WaypointObject(objectId);
	}
	
	private static WeaponObject createWeaponObject(long objectId, String template) {
		return new WeaponObject(objectId);
	}
	
}
