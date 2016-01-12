package services.commands;

import intents.SkillModIntent;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.creature.CreatureObject;

public class SkillModService extends Service {

	public SkillModService() {
		registerForIntent(SkillModIntent.TYPE);
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		SkillModIntent smi = (SkillModIntent) i;
		
		for(CreatureObject creature : smi.getAffectedCreatures()) {
			creature.adjustSkillmod(smi.getSkillModName(), smi.getAdjustBase(), smi.getAdjustModifier());
		}
	}
	
}
