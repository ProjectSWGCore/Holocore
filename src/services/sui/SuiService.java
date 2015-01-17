package services.sui;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.python.core.Py;
import org.python.core.PyObject;

import network.packets.swg.zone.server_ui.SuiCreatePageMessage;
import network.packets.swg.zone.server_ui.SuiEventNotification;
import intents.GalacticPacketIntent;
import intents.sui.SuiWindowIntent;
import intents.sui.SuiWindowIntent.SuiWindowEvent;
import resources.control.Intent;
import resources.control.Service;
import resources.player.Player;
import resources.sui.ISuiCallback;
import resources.sui.SuiWindow;

public class SuiService extends Service {

	private Map<Integer, SuiWindow> windows;
	
	public SuiService() {
		windows = new ConcurrentHashMap<Integer, SuiWindow>();
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
		SuiWindow window = windows.get(r.getWindowId());
		
		if (window == null)
			System.err.println("Null window id " + r.getWindowId());
		
		int eventId = r.getEventId();
		if (window.getJavaCallback(eventId) != null) {
			ISuiCallback callback = window.getJavaCallback(eventId);
			callback.handleEvent(player, player.getCreatureObject(), eventId, r.getDataStrings());
		} else if (window.getScriptCallback(eventId) != null) {
			PyObject callback = window.getScriptCallback(eventId);
			callback.__call__(Py.java2py(player), Py.java2py(player.getCreatureObject()), Py.java2py(eventId), Py.java2py(r.getDataStrings()));
		}
	}
	
	private void displayWindow(Player player, SuiWindow wnd) {
		int id = createWindowId(new Random());
		wnd.setId(id);
		
		SuiCreatePageMessage packet = new SuiCreatePageMessage(id, wnd.getScript(), wnd.getComponents(), player.getCreatureObject().getObjectId(), wnd.getMaxDistance());
		player.sendPacket(packet);
		
		windows.put(id, wnd);
	}
	
	private int createWindowId(Random ran) {
		int id = ran.nextInt();
		if (windows.containsKey(id))
			return createWindowId(ran);
		else return id;
	}
}
