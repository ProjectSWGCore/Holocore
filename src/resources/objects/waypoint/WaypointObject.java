package resources.objects.waypoint;

import resources.network.BaselineBuilder;
import resources.objects.intangible.IntangibleObject;
import resources.player.Player;

public class WaypointObject extends IntangibleObject {

	private static final long serialVersionUID = 1L;
	
	private int cellNumber;
	private long targetId = 0;
	private String name = "Waypoint";
	private int color = 1;
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


	public int getColor() {
		return color;
	}


	public void setColor(int color) {
		// BLUE,GREEN,ORANGE,YELLOW,PURPLE,WHITE,MULTICOLOR
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
		bb.addByte(color);
		bb.addBoolean(active);
	}

	public void createObject(Player target) {
		// NOTE: Client is never sent a WAYP baseline in NGE, WaypointObject's just go inside the Waypoint List in PLAY.
		
		/*super.sendSceneCreateObject(target);
		
		BaselineBuilder bb = new BaselineBuilder(this, BaselineType.WAYP, 3);
		createBaseline3(target, bb);
		bb.sendTo(target);
		
		createChildrenObjects(target);
		target.sendPacket(new SceneEndBaselines(getObjectId()));*/
	}
}
