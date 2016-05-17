package resources.objects.custom;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import intents.object.MoveObjectIntent;
import resources.Location;
import resources.Point3D;

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
			case GUARD:	aiLoopGuard();	break;
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
		double theta;
		Location l = getLocation();
		Point3D point = new Point3D();
		do {
			theta = r.nextDouble() * Math.PI * 2;
			point.setX(l.getX() + Math.cos(theta) * dist);
			point.setZ(l.getZ() + Math.sin(theta) * dist);
		} while (!mainLocation.isWithinFlatDistance(point, radius));
		l.setPosition(point.getX(), l.getY(), point.getZ());
		l.setHeading(l.getYaw() - Math.toDegrees(theta));
		new MoveObjectIntent(this, getParent(), l, 1.37, updateCounter++).broadcast();
	}
	
	private void aiLoopGuard() {
		if (isInCombat())
			return;
		Random r = new Random();
		if (r.nextDouble() > 0.25) // Only a 25% movement chance
			return;
		if (getObservers().isEmpty()) // No need to dance if nobody is watching
			return;
		double theta = r.nextDouble() * 360;
		mainLocation.setHeading(theta);
		new MoveObjectIntent(this, getParent(), mainLocation, 1.37, updateCounter++).broadcast();
	}
	
}
