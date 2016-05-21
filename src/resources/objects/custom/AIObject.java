package resources.objects.custom;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import resources.objects.creature.CreatureObject;
import utilities.ScheduledUtilities;

public abstract class AIObject extends CreatureObject {
	
	private static final long serialVersionUID = 1L;
	
	private transient ScheduledFuture<?> future;
	private long initialDelay;
	private long delay;
	private TimeUnit unit;
	
	public AIObject(long objectId) {
		super(objectId);
		aiInitialize();
	}
	
	/**
	 * Called upon object creation.  If overridden, you must call this
	 * function via super.aiInitialize()
	 */
	protected void aiInitialize() {
		setSchedulerProperties(0, 5, TimeUnit.SECONDS);
	}
	
	/**
	 * Sets scheduler properties for how often aiLoop runs
	 * @param initialDelay the initial delay
	 * @param delay the delay between each loop
	 * @param unit the time unit for both delays
	 */
	protected void setSchedulerProperties(long initialDelay, long delay, TimeUnit unit) {
		this.initialDelay = initialDelay;
		this.delay = delay;
		this.unit = unit;
	}
	
	/**
	 * Called when Holocore is starting.  If overridden, you must call this
	 * function via super.aiStart()
	 */
	public void aiStart() {
		if (future != null) {
			return;
		}
		future = ScheduledUtilities.scheduleAtFixedRate(() -> aiLoop(), initialDelay, delay, unit);
	}
	
	/**
	 * Called periodically for move updates, etc.
	 */
	protected abstract void aiLoop();
	
	/**
	 * Called when Holocore is stopping.  If overridden, you must call this
	 * function via super.aiStop()
	 */
	public void aiStop() {
		if (future == null) {
			return;
		}
		future.cancel(true);
		future = null;
	}
	
}
