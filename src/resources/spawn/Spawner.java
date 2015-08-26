package resources.spawn;

import resources.objects.SWGObject;

public final class Spawner {

	private SWGObject spawnerObject;
	
	public Spawner(SWGObject spawnerObject) {
		this.spawnerObject = spawnerObject;
	}
	
	public SWGObject getSpawnerObject() {
		return spawnerObject;
	}
}
