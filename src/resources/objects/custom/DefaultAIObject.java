package resources.objects.custom;

import java.util.concurrent.TimeUnit;

import intents.object.MoveObjectIntent;
import resources.Location;

public class DefaultAIObject extends AIObject {
	
	private static final long serialVersionUID = 1L;
	
	private Location mainLocation;
	private AIBehavior behavior;
	private int updateCounter;
	private double radius;
	
	public DefaultAIObject(long objectId) {
		super(objectId);
		behavior = AIBehavior.STOP;
		updateCounter = 0;
		radius = 2;
	}
	
	@Override
	protected void aiInitialize() {
		super.aiInitialize();
		setSchedulerProperties(15, 15, TimeUnit.SECONDS);
	}
	
	public AIBehavior getBehavior() {
		return behavior;
	}
	
	public double getFloatRadius() {
		return radius;
	}
	
	public void setBehavior(AIBehavior behavior) {
		this.behavior = behavior;
	}
	
	public void setFloatRadius(double radius) {
		this.radius = radius;
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
			case STOP:
			default:	break;
		}
	}
	
	private void aiLoopFloat() {
		if (isInCombat())
			return;
		if (Math.random() > 0.25) // Only a 25% movement chance
			return;
		if (getObservers().isEmpty()) // No need to dance if nobody is watching
			return;
		Location l = new Location(mainLocation);
		l.translatePosition((Math.random()-.5)*2*radius, 0, (Math.random()-.5)*2*radius);
		new MoveObjectIntent(this, getParent(), l, 1.37, updateCounter++).broadcast();
	}
	
}
