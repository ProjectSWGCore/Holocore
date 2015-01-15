package resources.sui;

import intents.sui.SuiWindowIntent;
import intents.sui.SuiWindowIntent.SuiWindowEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import resources.player.Player;
import network.packets.swg.zone.server_ui.SuiCreatePageMessage.SuiWindowComponent;

public class SuiWindow {

	private int id;
	private String script;
	private Player owner;
	private long rangeObjId;
	private float maxDistance = 0;
	private List<SuiWindowComponent> components = new ArrayList<SuiWindowComponent>();
	//private Map<Integer, PyObject> scriptCallbacks;
	private Map<Integer, ISuiCallback> javaCallbacks;
	
	public SuiWindow(String script, Player owner) {
		this.script = script;
		this.owner = owner;
	}

	public final void addCallback(int eventId, String source, byte trigger, List<String> returnParams, ISuiCallback callback) {
		SuiWindowComponent component = new SuiWindowComponent();
		component.setType((byte) 5);
		component.getNarrowParams().add(source);
		component.getNarrowParams().add(new String(new byte[] { trigger }));
		component.getNarrowParams().add("handleSUI");
		
		for (String returnParam : returnParams) {
			for (String s : returnParam.split(":")) {
				component.getNarrowParams().add(s);
			}
		}
		
		components.add(component);
		javaCallbacks.put(eventId, callback);
	}
	
	public final void setProperty(String property, String value) {
		SuiWindowComponent component = new SuiWindowComponent();
		component.setType((byte) 3);
		
		for (String s : property.split(":")) {
			component.getNarrowParams().add(s);
		}
		
		component.getWideParams().add(value);
		components.add(component);
	}
	
	public final void display() { new SuiWindowIntent(owner, this, SuiWindowEvent.NEW).broadcast(); }
	public final void display(Player player) { new SuiWindowIntent(player, this, SuiWindowEvent.NEW).broadcast();}
	
	public final long getRangeObjId() { return rangeObjId; }
	public final void setRangeObjId(long rangeObjId) { this.rangeObjId = rangeObjId; }
	public final int getId() { return id; }
	public final void setId(int id) { this.id = id; }
	public final String getScript() { return script; }
	public final Player getOwner() { return owner; }
	public final float getMaxDistance() { return maxDistance; }
	public final void setMaxDistance(float maxDistance) { this.maxDistance = maxDistance; }
	public final List<SuiWindowComponent> getComponents() { return components; }

	public enum Trigger {;
		public static byte UPDATE = 4;
		public static byte OK = 9;
		public static byte CANCEL = 10;
	}
}
