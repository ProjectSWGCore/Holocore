package resources.commands.callbacks;

import java.util.List;

import network.packets.swg.zone.object_controller.ObjectController;
import network.packets.swg.zone.object_controller.PlayerEmote;
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
		
		PlayerEmote emote = new PlayerEmote(player.getCreatureObject().getObjectId(), ((target == null) ? 0 : target.getObjectId()), Short.valueOf(cmd[1]));
		player.sendPacket(new ObjectController(PlayerEmote.CRC, player.getCreatureObject().getObjectId(), emote));
		
		List<Player> observers = player.getCreatureObject().getObservers();
		for (Player observer : observers) {
			if (observer.getCreatureObject() == null)
				continue;
			
			observer.sendPacket(new ObjectController(PlayerEmote.CRC, observer.getCreatureObject().getObjectId(), emote));
		}
	}
}
