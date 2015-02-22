package resources.commands.callbacks;

import resources.Location;
import resources.Terrain;
import resources.commands.ICmdCallback;
import resources.objects.SWGObject;
import resources.objects.player.PlayerObject;
import resources.objects.waypoint.WaypointObject;
import resources.objects.waypoint.WaypointObject.WaypointColor;
import resources.player.Player;
import services.objects.ObjectManager;

public class WaypointCmdCallback implements ICmdCallback {

	@Override
	public void execute(ObjectManager objManager, Player player, SWGObject target, String args) {
		PlayerObject ghost = (PlayerObject) player.getPlayerObject();
		if (ghost == null)
			return;
		
		String[] cmdArgs = args.split(" ");
		if (cmdArgs.length > 6)
			cmdArgs = args.split(" ", 6);
		
		printCmdArgs(cmdArgs);

		WaypointColor color = null;
		Terrain terrain = null;
		String name = null;
		float x = -1;
		float y = -1;
		
		switch(cmdArgs.length) {
			case 2: // x y
				x = floatValue(cmdArgs[0]);
				if (x == -1)
					return;
				y = floatValue(cmdArgs[1]);
				break;
			case 4: // x z y name
				x = floatValue(cmdArgs[0]);
				if (x == -1)
					return;
				//z = floatValue(cmdArgs[1]);
				y = floatValue(cmdArgs[2]);
				name = cmdArgs[3];
				break;
			case 6: // planet x z y color name
				terrain = Terrain.getTerrainFromName(cmdArgs[0]);
				if (terrain == null)
					return;
				x = floatValue(cmdArgs[1]);
				if (x == -1)
					return;
				//z = floatValue(cmdArgs[2]);
				y = floatValue(cmdArgs[3]);
				color = colorValue(cmdArgs[4]);
				name = cmdArgs[5];
				break;
			default: 
				break;
		}

		WaypointObject waypoint = createWaypoint(objManager, terrain, color, name, x, y, player.getCreatureObject().getLocation());
		ghost.addWaypoint(waypoint);
	}

	private WaypointObject createWaypoint(ObjectManager objManager, Terrain terrain, WaypointColor color, String name, float x, float y, Location loc) {
		WaypointObject waypoint = (WaypointObject) objManager.createObject("object/waypoint/shared_waypoint.iff", loc);
		
		if (color != null)
			waypoint.setColor(color);
		
		if (terrain != null)
			waypoint.getLocation().setTerrain(terrain);
		
		if (x != -1)
			waypoint.getLocation().setX(new Float(x).doubleValue());
		if (y != -1)
			waypoint.getLocation().setZ(new Float(y).doubleValue());
		
		waypoint.getLocation().setY(0);
		waypoint.setName(name == null ? "New Waypoint" : name);
		
		return waypoint;
	}
	
	private void printCmdArgs(String[] args) {
		System.out.println("CmdArgs: ");
		for (String str : args) {
			System.out.print(str + ":");
		}
		System.out.println("");
	}
	
	private float floatValue(String str) {
		try {
			return Float.parseFloat(str);
		} catch (NumberFormatException | NullPointerException e) {
			return (float) -1;
		}
	}
	
	private WaypointColor colorValue(String str) {
		switch (str) {
			case "blue": return WaypointColor.BLUE;
			case "green": return WaypointColor.GREEN;
			case "yellow": return WaypointColor.YELLOW;
			case "white": return WaypointColor.WHITE;
			case "orange": return WaypointColor.ORANGE;
			case "purple": return WaypointColor.PURPLE;
			default: return WaypointColor.BLUE;
		}
	}
}
