package resources.objects.waypoint;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import resources.common.CRC;
import resources.network.BaselineBuilder.Encodable;
import resources.objects.intangible.IntangibleObject;
import resources.player.Player;
import utilities.Encoder;

public class WaypointObject extends IntangibleObject implements Encodable {

	private static final long serialVersionUID = 1L;
	
	private int cellNumber;
	private String name = "New Waypoint";
	private WaypointColor color = WaypointColor.BLUE;
	private boolean active = true;
	
	public WaypointObject(long objectId) {
		super(objectId);
	}

	
	public int getCellNumber() {
		return cellNumber;
	}


	public void setCellNumber(int cellNumber) {
		this.cellNumber = cellNumber;
	}

	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public WaypointColor getColor() {
		return color;
	}


	public void setColor(WaypointColor color) {
		this.color = color;
	}


	public boolean isActive() {
		return active;
	}
	
	public void setActive(boolean active) {
		this.active = active;
	}

	protected void createObject(Player target) {
		// NOTE: Client is never sent a WAYP baseline in NGE, WaypointObject's just go inside the Waypoint List in PLAY.
	}

	@Override
	public byte[] encode() {
		ByteBuffer bb = ByteBuffer.allocate(42 + name.length() * 2).order(ByteOrder.LITTLE_ENDIAN);
		bb.putInt(cellNumber);
		bb.putFloat((float) getLocation().getX());
		bb.putFloat((float) 0);
		bb.putFloat((float) getLocation().getZ());
		bb.putLong(0); // Network id, used for clusters
		bb.putInt(CRC.getCrc(getLocation().getTerrain().getName()));
		bb.put(Encoder.encodeUnicode(name));
		bb.putLong(getObjectId());
		bb.put((byte) color.getValue());
		bb.put((byte) (active ? 1 : 0));
		return bb.array();
	}
	
	@Override
	public boolean equals(Object o) {
		if (!super.equals(o))
			return false;
		if (o instanceof WaypointObject) {
			WaypointObject wp = (WaypointObject) o;
			return wp.name.equals(name) && wp.cellNumber == cellNumber && wp.color == color && wp.active == active;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return ((super.hashCode() * 7 + name.hashCode()) * 13 + color.getValue()) * 17 + cellNumber;
	}
	
	public enum WaypointColor{
		BLUE(1), GREEN(2), ORANGE(3), YELLOW(4), PURPLE(5), WHITE(6), MULTICOLOR(7);
		
		private int i;
		
		private WaypointColor(int i) {
			this.i = i;
		}
		
		public int getValue() { return i; }
	}
}
