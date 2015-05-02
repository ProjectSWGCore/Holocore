package resources.control;

import java.util.ArrayList;
import java.util.List;


/**
 * A Manager is a class that will manage services, and generally controls the
 * program as a whole
 */
public class Manager extends Service {
	
	private static final ServerManager serverManager = ServerManager.getInstance();
	private List <Service> children;
	
	public Manager() {
		children = new ArrayList<Service>();
	}
	
	/**
	 * Initializes this manager. If the manager returns false on this method
	 * then the initialization failed and may not work as intended. This will
	 * initialize all children automatically.
	 * @return TRUE if initialization was successful, FALSE otherwise
	 */
	@Override
	public boolean initialize() {
		boolean success = super.initialize(), cSuccess = true;
		long start = 0, end = 0;
		synchronized (children) {
			for (Service child : children) {
				if (!success)
					break;
				start = System.nanoTime();
				cSuccess = child.initialize();
				end = System.nanoTime();
				serverManager.setServiceInitTime(child, (end-start)/1E6, cSuccess);
				if (!cSuccess) {
					System.err.println(child.getClass().getSimpleName() + " failed to initialize!");
					success = false;
				}
			}
		}
		return success;
	}
	
	/**
	 * Starts this manager. If the manager returns false on this method then
	 * the manger failed to start and may not work as intended. This will start
	 * all children automatically.
	 * @return TRUE if starting was successful, FALSE otherwise
	 */
	@Override
	public boolean start() {
		boolean success = super.start(), cSuccess = true;
		long start = 0, end = 0;
		synchronized (children) {
			for (Service child : children) {
				if (!success)
					break;
				start = System.nanoTime();
				cSuccess = child.start();
				end = System.nanoTime();
				serverManager.setServiceStartTime(child, (end-start)/1E6, cSuccess);
				if (!cSuccess) {
					System.err.println(child.getClass().getSimpleName() + " failed to start!");
					success = false;
				}
			}
		}
		return success;
	}
	
	/**
	 * Terminates this manager. If the manager returns false on this method
	 * then the manager failed to shut down and resources may not have been
	 * cleaned up. This will terminate all children automatically.
	 * @return TRUE if termination was successful, FALSE otherwise
	 */
	@Override
	public boolean terminate() {
		boolean success = super.terminate(), cSuccess = true;
		long start = 0, end = 0;
		synchronized (children) {
			for (Service child : children) {
				start = System.nanoTime();
				cSuccess = child.terminate();
				end = System.nanoTime();
				serverManager.setServiceTerminateTime(child, (end-start)/1E6, cSuccess);
				if (!cSuccess)
					success = false;
			}
		}
		return success;
	}
	
	/**
	 * Determines whether or not this manager is operational
	 * @return TRUE if this manager is operational, FALSE otherwise
	 */
	@Override
	public boolean isOperational() {
		boolean success = true;
		synchronized (children) {
			for (Service child : children) {
				if (!child.isOperational())
					success = false;
			}
		}
		return success;
	}
	
	/**
	 * Adds a child to the manager's list of children. This creates a tree of
	 * managers that allows information to propogate freely through the network
	 * in an easy way.
	 * @param m the manager to add as a child.
	 */
	public void addChildService(Service s) {
		if (s == null)
			throw new NullPointerException("Child service cannot be null!");
		synchronized (children) {
			for (Service child : children) {
				if (s == child || s.equals(child))
					return;
			}
			serverManager.addChild(this, s);
			children.add(s);
		}
	}
	
	/**
	 * Removes the sub-manager from the list of children
	 * @param m the sub-manager to remove
	 */
	public void removeChildService(Service s) {
		if (s == null)
			return;
		synchronized (children) {
			serverManager.removeChild(this, s);
			children.remove(s);
		}
	}
	
	/**
	 * Returns a copied ArrayList of the children of this manager
	 * @return a copied ArrayList of the children of this manager
	 */
	public List<Service> getManagerChildren() {
		synchronized (children) {
			return new ArrayList<Service>(children);
		}
	}
	
}
