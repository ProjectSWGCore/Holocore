package resources.commands.callbacks;

import java.util.Locale;

import intents.ObjectTeleportIntent;
import resources.Location;
import resources.Terrain;
import resources.commands.ICmdCallback;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
import services.objects.ObjectManager;
import services.player.PlayerManager;

public class AdminTeleportCallback implements ICmdCallback {

	@Override
	public void execute(ObjectManager objManager, Player player, SWGObject target, String args) {
		String[] cmd = args.split(" ");
		String firstName = null;
		String name = null;
		try {
			Location newLocation = new Location();
			PlayerManager playermgr = new PlayerManager();

			if(cmd.length == 4){			
				newLocation.setTerrain(Terrain.getTerrainFromName(cmd[0]));
				newLocation.setX(Float.parseFloat(cmd[1]));
				newLocation.setY(Float.parseFloat(cmd[2]));
				newLocation.setZ(Float.parseFloat(cmd[3]));
				new ObjectTeleportIntent(player.getCreatureObject(), newLocation).broadcast();
			}else if(cmd.length == 5){
				System.out.println(cmd[0]);
				System.out.println(playermgr.getCharacterIdByName(cmd[0]));
								
			/*	if (cmd[0].indexOf(' ') >= 0) 
					firstName = cmd[0].substring(0, cmd[0].indexOf(' ')); 
				else 
					firstName = cmd[0];*/
				
				long objId = playermgr.getCharacterIdByName(firstName.toLowerCase(Locale.ENGLISH).trim());
				
				newLocation.setTerrain(Terrain.getTerrainFromName(cmd[1]));
				newLocation.setX(Float.parseFloat(cmd[2]));
				newLocation.setY(Float.parseFloat(cmd[3]));
				newLocation.setZ(Float.parseFloat(cmd[4]));
				
				new ObjectTeleportIntent(objManager.getObjectById(objId), newLocation).broadcast();
			}else{
				System.err.println("Error: Wrong Syntax. For teleporting yourself, command has to be: /teleport <planetname> <x> <y> <z>");
				System.err.println("For teleporting another player, command has to be: /teleport <charname> <planetname> <x> <y> <z>");
				return;
			}
		}catch(NumberFormatException e){
			System.err.println("ERROR: Wrong Syntax or Value. Please enter the command like that: /teleport <planetname> <x> <y> <z>");
		}
	}

}
