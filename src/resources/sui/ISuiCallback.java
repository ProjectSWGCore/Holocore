package resources.sui;

import java.util.List;

import resources.objects.SWGObject;
import resources.player.Player;

public interface ISuiCallback {
	public void handleEvent(Player player, SWGObject actor, int eventType, List<String> returnParams);
}
