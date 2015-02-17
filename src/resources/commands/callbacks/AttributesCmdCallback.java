package resources.commands.callbacks;

import java.util.Map;

import network.packets.swg.zone.spatial.AttributeListMessage;
import resources.commands.ICmdCallback;
import resources.objects.SWGObject;
import resources.player.Player;
import services.objects.ObjectManager;

public class AttributesCmdCallback implements ICmdCallback {

	@Override
	public void execute(ObjectManager objManager, Player player, SWGObject target, String args) {
		String[] cmd = args.split(" ");
		
		for (String s : cmd) {
			if (s.equals("-255") || s.length() == 0)
				continue;
			
			SWGObject obj = objManager.getObjectById(Long.parseLong(s));
			if (obj != null)
				handleSendItemAttributes(obj, player);
		}
	}

	private void handleSendItemAttributes(SWGObject object, Player player) {
		Map<String, String> attributes = object.getAttributes();
		if (attributes.size() == 0)
			attributes.put("", "");
		
		AttributeListMessage message = new AttributeListMessage(object.getObjectId(), attributes);
		player.sendPacket(message);
	}
}
