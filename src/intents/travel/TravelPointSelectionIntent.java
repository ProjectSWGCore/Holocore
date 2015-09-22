package intents.travel;

import resources.control.Intent;
import resources.objects.creature.CreatureObject;

public class TravelPointSelectionIntent extends Intent {
	
	public static final String TYPE = "TravelPointSelectionIntent";
	
	private CreatureObject creature;
	private boolean instant;
	
	public TravelPointSelectionIntent(CreatureObject creature, boolean instant) {
		super(TYPE);
		this.creature = creature;
		this.instant = instant;
	}
	
	public CreatureObject getCreature() {
		return creature;
	}
	
	public boolean isInstant() {
		return instant;
	}
	
}
