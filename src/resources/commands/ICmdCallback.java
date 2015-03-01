package resources.commands;

import resources.objects.SWGObject;
import resources.player.Player;
import services.galaxy.GalacticManager;

public interface ICmdCallback {
	public void execute(GalacticManager galacticManager, Player player, SWGObject target, String args);
}
