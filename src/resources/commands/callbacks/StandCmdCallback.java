package resources.commands.callbacks;

import network.packets.swg.zone.object_controller.PostureUpdate;
import resources.Posture;
import resources.commands.ICmdCallback;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
import services.galaxy.GalacticManager;

public class StandCmdCallback implements ICmdCallback {
	
	@Override
	public void execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
		CreatureObject creature = player.getCreatureObject(); 
		creature.setPosture(Posture.UPRIGHT);
		creature.setMovementScale(1);
		creature.setTurnScale(1);
		creature.sendObservers(new PostureUpdate(creature.getObjectId(), Posture.UPRIGHT));
	}
	
}
