package resources.sui;

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
	
	public SuiWindow(int id, String script, Player owner) {
		this.id = id;
		this.script = script;
		this.owner = owner;
	}

	public void addCallback(int eventId, String source, byte trigger, List<String> returnParams, ISuiCallback callback) {
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
	
	public void setProperty(String property, String value) {
		SuiWindowComponent component = new SuiWindowComponent();
		component.setType((byte) 3);
		
		for (String s : property.split(":")) {
			component.getNarrowParams().add(s);
		}
		
		component.getWideParams().add(value);
		components.add(component);
	}
	
	public long getRangeObjId() { return rangeObjId; }
	public void setRangeObjId(long rangeObjId) { this.rangeObjId = rangeObjId; }
	public int getId() { return id; }
	public String getScript() { return script; }
	public Player getOwner() { return owner; }
	public float getMaxDistance() { return maxDistance; }
	public void setMaxDistance(float maxDistance) { this.maxDistance = maxDistance; }
	
	public enum Trigger {;
		public static byte UPDATE = 4;
		public static byte OK = 9;
		public static byte CANCEL = 10;
	}
}
