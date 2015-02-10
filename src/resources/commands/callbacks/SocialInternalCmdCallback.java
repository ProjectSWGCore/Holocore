package resources.commands.callbacks;

import resources.commands.ICmdCallback;
import resources.objects.SWGObject;
import resources.player.Player;
import services.objects.ObjectManager;

public class SocialInternalCmdCallback implements ICmdCallback {

	@Override
	public void execute(ObjectManager objManager, Player player, SWGObject target, String args) {
		// Args: targetId (0), emoteId (1), unk1, unk2
		String[] cmd = args.split(" ", 3);
		
		if (!cmd[0].equals("0"))
			target = objManager.getObjectById(Long.valueOf(cmd[0]));
		
		// TODO: Send ObjControllerMessage with PlayerEmote packet to observers
	}

}
