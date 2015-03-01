package resources.commands.callbacks;

import resources.commands.ICmdCallback;
import resources.objects.SWGObject;
import resources.objects.player.PlayerObject;
import resources.objects.waypoint.WaypointObject;
import resources.player.Player;
import services.galaxy.GalacticManager;

public class RequestWaypointCmdCallback implements ICmdCallback {

	@Override
	public void execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
		// Args: (^-,=+_)color_1(,+-=_^)=1 planet x 0.0 z
		PlayerObject ghost = (PlayerObject) player.getPlayerObject();
		if (ghost == null)
			return;
		
		String[] cmd = args.split(" ", 5);
		WaypointObject waypoint = (WaypointObject) galacticManager.getObjectManager().createObject("object/waypoint/shared_waypoint.iff", player.getCreatureObject().getLocation());
		waypoint.setLocation(Double.valueOf(cmd[2]), 0, Double.valueOf(cmd[4]));
		ghost.addWaypoint(waypoint);
	}
}
