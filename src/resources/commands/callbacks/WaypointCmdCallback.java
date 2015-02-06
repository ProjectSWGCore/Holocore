package resources.commands.callbacks;

import resources.commands.ICmdCallback;
import resources.objects.SWGObject;
import resources.objects.player.PlayerObject;
import resources.objects.waypoint.WaypointObject;
import resources.player.Player;
import services.objects.ObjectManager;

public class WaypointCmdCallback implements ICmdCallback {

	@Override
	public void execute(ObjectManager objManager, Player player, SWGObject target, String args) {
		PlayerObject ghost = (PlayerObject) player.getPlayerObject();
		if (ghost == null)
			return;
		
		//System.out.println("Create Waypoint: " + args);
		WaypointObject waypoint = (WaypointObject) objManager.createObject("object/waypoint/shared_waypoint.iff", player.getCreatureObject().getLocation());
		ghost.addWaypoint(waypoint);
	}

}
