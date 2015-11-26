package intents;

import resources.control.Intent;
import resources.objects.creature.CreatureObject;

public class SkillModIntent extends Intent {

	public static final String TYPE = "SkillModIntent";
	
	private final String skillModName;
	private final int adjustBase;
	private final int adjustModifier;
	private final CreatureObject[] affectedCreatures;
	
	public SkillModIntent(String skillModName, int adjustBase, int adjustModifier, CreatureObject... affectedCreatures) {
		super(TYPE);
		this.skillModName = skillModName;
		this.adjustBase = adjustBase;
		this.adjustModifier = adjustModifier;
		this.affectedCreatures = affectedCreatures;
	}

	public int getAdjustModifier() {
		return adjustModifier;
	}

	public int getAdjustBase() {
		return adjustBase;
	}

	public String getSkillModName() {
		return skillModName;
	}
	
	public CreatureObject[] getAffectedCreatures() {
		return affectedCreatures;
	}
	
}
