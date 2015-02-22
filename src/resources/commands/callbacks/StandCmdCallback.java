package resources.commands.callbacks;

import network.packets.swg.zone.object_controller.PostureUpdate;
import resources.Posture;
import resources.commands.ICmdCallback;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
import services.objects.ObjectManager;

public class StandCmdCallback implements ICmdCallback {
	
	@Override
	public void execute(ObjectManager objManager, Player player, SWGObject target, String args) {
		CreatureObject creature = player.getCreatureObject(); 
		creature.setPosture(Posture.UPRIGHT);
		creature.sendObservers(new PostureUpdate(creature.getObjectId(), Posture.UPRIGHT));
	}
	
}
