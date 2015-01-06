package resources.commands;

import resources.objects.SWGObject;
import resources.player.Player;
import services.objects.ObjectManager;

public interface ICmdCallback {
	public void execute(ObjectManager objManager, Player player, SWGObject target, String args);
}
