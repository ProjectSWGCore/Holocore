package services;

import resources.control.Manager;

public class CoreManager extends Manager {
	
	private long startTime;
	private EngineManager engineManager;
	
	public CoreManager() {
		engineManager = new EngineManager();
		
		addChildService(engineManager);
	}
	
	/**
	 * Determines whether or not the core is operational
	 * @return TRUE if the core is operational, FALSE otherwise
	 */
	public boolean isOperational() {
		return true;
	}
	
	@Override
	public boolean initialize() {
		startTime = System.nanoTime();
		return super.initialize();
	}
	
	/**
	 * Returns the time in milliseconds since the server started initialization
	 * @return the core time represented as a double
	 */
	public double getCoreTime() {
		return (System.nanoTime()-startTime)/1E6;
	}
	
}
