package resources.commands.callbacks;

import network.packets.swg.zone.object_controller.PostureUpdate;
import resources.Posture;
import resources.commands.ICmdCallback;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
import services.galaxy.GalacticManager;

public class ProneCmdCallback implements ICmdCallback {

	@Override
	public void execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
		CreatureObject creature = player.getCreatureObject(); 
		creature.setPosture(Posture.PRONE);
		creature.setMovementScale(0.50);
		creature.sendObservers(new PostureUpdate(creature.getObjectId(), Posture.PRONE));
		
	}

}
