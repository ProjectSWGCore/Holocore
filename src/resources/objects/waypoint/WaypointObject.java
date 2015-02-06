package resources.objects.waypoint;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import resources.common.CRC;
import resources.network.BaselineBuilder;
import resources.network.BaselineBuilder.Encodable;
import resources.objects.intangible.IntangibleObject;
import resources.player.Player;
import utilities.Encoder;

public class WaypointObject extends IntangibleObject implements Encodable{

	private static final long serialVersionUID = 1L;
	
	private int cellNumber;
	private long targetId = 0;
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


	public long getTargetId() {
		return targetId;
	}


	public void setTargetId(long targetId) {
		this.targetId = targetId;
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


	public void createBaseline3(Player target, BaselineBuilder bb) {
		super.createBaseline3(target, bb);
		bb.addInt(cellNumber);
		bb.addFloat((float) getLocation().getX());
		bb.addFloat((float) getLocation().getY());
		bb.addFloat((float) getLocation().getZ());
		bb.addLong(targetId); // unsure when this is anything but 0, have yet to see it change
		bb.addInt(getLocation().getTerrain().getCrc());
		bb.addUnicode(name);
		bb.addByte(color.getValue());
		bb.addBoolean(active);
	}

	protected void createObject(Player target) {
		// NOTE: Client is never sent a WAYP baseline in NGE, WaypointObject's just go inside the Waypoint List in PLAY.
		
		/*super.sendSceneCreateObject(target);
		
		BaselineBuilder bb = new BaselineBuilder(this, BaselineType.WAYP, 3);
		createBaseline3(target, bb);
		bb.sendTo(target);
		
		createChildrenObjects(target);
		target.sendPacket(new SceneEndBaselines(getObjectId()));*/
	}


	@Override
	public byte[] encode() {
		ByteBuffer bb = ByteBuffer.allocate(42 + name.length() * 2).order(ByteOrder.LITTLE_ENDIAN);
		bb.putInt(cellNumber);
		bb.putFloat((float) getLocation().getX());
		bb.putFloat((float) 0);
		bb.putFloat((float) getLocation().getZ());
		bb.putLong(targetId); // unsure when this is anything but 0, have yet to see it change
		bb.putInt(CRC.getCrc("tatooine")); // TODO: Add method to terrain's to just get planet name instead of file name
		bb.put(Encoder.encodeUnicode(name));
		bb.putLong(getObjectId());
		bb.put((byte) color.getValue());
		bb.put((byte) (active ? 1 : 0));
		return bb.array();
	}
	
	public enum WaypointColor{
		BLUE(1), GREEN(2), ORANGE(3), YELLOW(4), PURPLE(5), WHITE(6), MULTICOLOR(7);
		
		int i;
		
		private WaypointColor(int i) {
			this.i = i;
		}
		
		public int getValue() { return i; }
	}
}
