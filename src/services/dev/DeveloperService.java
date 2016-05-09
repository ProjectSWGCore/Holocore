package services.dev;

import intents.object.ObjectCreatedIntent;
import resources.Location;
import resources.PvpFlag;
import resources.Terrain;
import resources.control.Service;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import services.objects.ObjectCreator;

public class DeveloperService extends Service {
	
	public DeveloperService() {
		
	}
	
	@Override
	public boolean start() {
		setupDeveloperArea();
		return super.start();
	}
	
	private void setupDeveloperArea() {
		CreatureObject dummy = (CreatureObject) spawnObject("object/mobile/shared_target_dummy_blacksun.iff", new Location(3500, 5, -4800, Terrain.DEV_AREA));
		dummy.setPvpFlags(PvpFlag.ATTACKABLE);
	}
	
	private SWGObject spawnObject(String template, Location l) {
		SWGObject obj = ObjectCreator.createObjectFromTemplate(template);
		obj.setLocation(l);
		new ObjectCreatedIntent(obj).broadcast();
		return obj;
	}
	
}
