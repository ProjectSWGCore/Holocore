package resources.spawn;

public enum SpawnType {

	EGG("object/tangible/spawning/shared_spawn_egg.iff");
	
	private String objectTemplate;
	
	SpawnType(String objectTemplate) {
		this.objectTemplate = objectTemplate;
	}
	
	public String getObjectTemplate() {
		return objectTemplate;
	}
	
}
