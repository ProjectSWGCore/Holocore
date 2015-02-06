package resources.commands.callbacks;

import resources.commands.ICmdCallback;
import resources.objects.SWGObject;
import resources.player.Player;
import services.objects.ObjectManager;

public class WaypointCmdCallback implements ICmdCallback {

	@Override
	public void execute(ObjectManager objManager, Player player, SWGObject target, String args) {
		System.out.println("Create Waypoint: " + args);
	}

}
