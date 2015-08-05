package resources.commands.callbacks;

import resources.commands.ICmdCallback;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
import services.galaxy.GalacticManager;

public class PlayerAppearanceCallback implements ICmdCallback {

	@Override
	public void execute(GalacticManager galacticManager, Player player,
			SWGObject target, String args) {
		if(target instanceof CreatureObject) {
			CreatureObject creatureTarget = (CreatureObject) target;
			
			creatureTarget.setCostume(args);
		}
		
	}

}
