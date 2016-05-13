package resources.objects.custom;

import java.util.concurrent.TimeUnit;

import intents.object.MoveObjectIntent;
import resources.Location;

public class DefaultAIObject extends AIObject {
	
	private static final long serialVersionUID = 1L;
	
	private Location mainLocation;
	private Behavior behavior;
	private int updateCounter;
	private double radius;
	
	public DefaultAIObject(long objectId) {
		super(objectId);
		behavior = Behavior.FLOAT;
		updateCounter = 0;
		radius = 2;
	}
	
	@Override
	protected void aiInitialize() {
		super.aiInitialize();
		setSchedulerProperties(30, 30, TimeUnit.SECONDS);
	}
	
	public Behavior getBehavior() {
		return behavior;
	}
	
	public void setBehavior(Behavior behavior) {
		this.behavior = behavior;
	}
	
	@Override
	public void aiStart() {
		super.aiStart();
		this.mainLocation = getLocation();
	}
	
	@Override
	protected void aiLoop() {
		switch (behavior) {
			case FLOAT:	aiLoopFloat();	break;
			default:	break;
		}
	}
	
	private void aiLoopFloat() {
		Location l = new Location(mainLocation);
		l.translatePosition((Math.random()-.5)*2*radius, 0, (Math.random()-.5)*2*radius);
		new MoveObjectIntent(this, l, 1.37, updateCounter++).broadcast();
	}
	
	public static enum Behavior {
		FLOAT // got lots of options here
	}
	
}
