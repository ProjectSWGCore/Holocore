package services.sui;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import network.packets.swg.zone.server_ui.SuiCreatePageMessage;
import network.packets.swg.zone.server_ui.SuiEventNotification;
import intents.GalacticPacketIntent;
import intents.sui.SuiWindowIntent;
import intents.sui.SuiWindowIntent.SuiWindowEvent;
import resources.control.Intent;
import resources.control.Service;
import resources.player.Player;
import resources.sui.SuiWindow;

public class SuiService extends Service {

	private Map<Integer, SuiWindow> windows;
	private int maxWindowId;
	
	public SuiService() {
		windows = new ConcurrentHashMap<Integer, SuiWindow>();
		maxWindowId = 0;
	}
	
	@Override
	public boolean initialize() {
		registerForIntent(GalacticPacketIntent.TYPE);
		registerForIntent(SuiWindowIntent.TYPE);
		return super.initialize();
	}

	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof SuiWindowIntent)
			handleSuiWindowIntent((SuiWindowIntent) i);
		else if (i instanceof GalacticPacketIntent) {
			if (((GalacticPacketIntent) i).getPacket() instanceof SuiEventNotification) {
				long netId = ((GalacticPacketIntent) i).getNetworkId();
				Player player = ((GalacticPacketIntent) i).getPlayerManager().getPlayerFromNetworkId(netId);
				handleSuiEventNotification(player, (SuiEventNotification) (((GalacticPacketIntent) i).getPacket()));
			}
		}
	}
	
	private void handleSuiWindowIntent(SuiWindowIntent i) {
		if (i.getEvent() == SuiWindowEvent.NEW)
			displayWindow(i.getPlayer(), i.getWindow());
	}
	
	private void handleSuiEventNotification(Player player, SuiEventNotification r) {
		// TODO: SUI Events
	}
	
	private void displayWindow(Player player, SuiWindow wnd) {
		int id = getNextWindowId();
		wnd.setId(id);
		
		SuiCreatePageMessage packet = new SuiCreatePageMessage(id, wnd.getScript(), wnd.getComponents(), player.getCreatureObject().getObjectId(), wnd.getMaxDistance());
		player.sendPacket(packet);
		
		windows.put(id, wnd);
	}
	
	private int getNextWindowId() {
		return maxWindowId++;
	}
}
