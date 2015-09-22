package intents.travel;

import resources.control.Intent;
import resources.objects.creature.CreatureObject;

public class TravelPointSelectionIntent extends Intent {
	
	public static final String TYPE = "TravelPointSelectionIntent";
	
	private CreatureObject creature;
	
	public TravelPointSelectionIntent(CreatureObject creature) {
		super(TYPE);
		this.creature = creature;
	}
	
	public CreatureObject getCreature() {
		return creature;
	}
	
}
