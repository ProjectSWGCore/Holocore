package utilities;

import resources.control.Service;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
import resources.server_info.Log;

public class DebugUtilities {
	
	public static void printPlayerCharacterDebug(Service service, Player p, String identifyingText) {
		CreatureObject c = p.getCreatureObject();
		boolean hasGhost = c.hasSlot("ghost");
		Log.d(service, "[%s]  PLAY=%s/%d  CREO=%s/%d  hasGhost=%b", identifyingText, p.getUsername(), p.getUserId(), c.getName(), c.getObjectId(), hasGhost);
	}
	
	public static void printPlayerCharacterDebug(Service service, CreatureObject c, String identifyingText) {
		Player p = c.getOwner();
		String username = "null";
		int id = 0;
		if (p != null) {
			username = p.getUsername();
			id = p.getUserId();
		}
		boolean hasGhost = c.hasSlot("ghost");
		Log.d(service, "[%s]  PLAY=%s/%d  CREO=%s/%d  hasGhost=%b", identifyingText, username, id, c.getName(), c.getObjectId(), hasGhost);
	}
	
}
