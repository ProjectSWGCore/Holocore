package resources.objects.custom;

import java.util.Random;
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
		long delay = (long) (30E3 + Math.random() * 10E3);
		setSchedulerProperties(delay, delay, TimeUnit.MILLISECONDS); // Using milliseconds allows for more distribution between AI loops
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
		Random r = new Random();
		if (r.nextDouble() > 0.25) // Only a 25% movement chance
			return;
		if (getObservers().isEmpty()) // No need to dance if nobody is watching
			return;
		double dist = Math.sqrt(radius);
		double x, z, theta;
		Location l = getLocation();
		do {
			theta = r.nextDouble() * Math.PI * 2;
			x = l.getX() + Math.cos(theta) * dist;
			z = l.getZ() + Math.sin(theta) * dist;
		} while (mainLocation.isWithinDistance(mainLocation.getTerrain(), x, mainLocation.getY(), z, radius));
		l.setPosition(x, mainLocation.getY(), z);
		l.setHeading(l.getYaw() - Math.toDegrees(theta));
		new MoveObjectIntent(this, getParent(), l, 1.37, updateCounter++).broadcast();
	}
	
}
