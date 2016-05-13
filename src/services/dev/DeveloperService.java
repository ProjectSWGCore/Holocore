package services.dev;

import intents.object.ObjectCreatedIntent;
import resources.Location;
import resources.PvpFlag;
import resources.Terrain;
import resources.control.Service;
import resources.objects.SWGObject;
import resources.objects.custom.DefaultAIObject;
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
		DefaultAIObject dummy = spawnObject("object/mobile/shared_target_dummy_blacksun.iff", new Location(3500, 5, -4800, Terrain.DEV_AREA), DefaultAIObject.class);
		dummy.setPvpFlags(PvpFlag.ATTACKABLE);
	}
	
	private <T extends SWGObject> T spawnObject(String template, Location l, Class<T> c) {
		T obj = ObjectCreator.createObjectFromTemplate(template, c);
		obj.setLocation(l);
		new ObjectCreatedIntent(obj).broadcast();
		return obj;
	}
	
}
