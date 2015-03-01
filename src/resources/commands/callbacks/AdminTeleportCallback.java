package resources.commands.callbacks;

import intents.ObjectTeleportIntent;
import resources.Location;
import resources.Terrain;
import resources.commands.ICmdCallback;
import resources.objects.SWGObject;
import resources.player.Player;
import services.galaxy.GalacticManager;

public class AdminTeleportCallback implements ICmdCallback {

	@Override
	public void execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
		String [] cmd = args.split(" ");
		if (cmd.length < 4) {
			System.err.println("Error: Wrong Syntax. For teleporting yourself, command has to be: /teleport <planetname> <x> <y> <z>");
			System.err.println("For teleporting another player, command has to be: /teleport <charname> <planetname> <x> <y> <z>");
			return;
		}
		double x = Double.NaN, y = Double.NaN, z = Double.NaN;
		int cmdOffset = 0;
		if (cmd.length > 4)
			cmdOffset = 1; 
		try {
			x = Double.parseDouble(cmd[cmdOffset+1]);
			y = Double.parseDouble(cmd[cmdOffset+2]);
			z = Double.parseDouble(cmd[cmdOffset+3]);
		} catch (NumberFormatException e) {
			System.err.println("ERROR: Wrong Syntax or Value. Please enter the command like this: /teleport <planetname> <x> <y> <z>");
			return;
		}
		
		Location newLocation = new Location(x, y, z, Terrain.getTerrainFromName(cmd[cmdOffset]));
		SWGObject teleportObject = player.getCreatureObject();
		if (cmd.length > 4)
			teleportObject = galacticManager.getPlayerManager().getPlayerByCreatureFirstName(cmd[0]).getCreatureObject();
		new ObjectTeleportIntent(teleportObject, newLocation).broadcast();
	}

}
