package main;

import intents.ServerStatusIntent;
import intents.ServerStatusIntent.ServerStatus;
import resources.Galaxy.GalaxyStatus;
import services.CoreManager;

public class ProjectSWG {
	
	private static ProjectSWG server;
	private final Thread mainThread;
	private CoreManager manager;
	private boolean shutdownRequested;
	private ServerStatus status;
	
	public static final void main(String [] args) {
		server = new ProjectSWG();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				server.forceShutdown();
			}
		});
		try {
			server.run();
		} catch (CoreException e) {
			System.err.println("ProjectSWG: Shutting down. Reason: " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("ProjectSWG: Shutting down - unknown error.");
		}
		server.terminate();
		System.out.println("ProjectSWG: Server shut down.");
	}
	
	/**
	 * Returns the time in milliseconds since the server started initialization
	 * @return the core time represented as a double
	 */
	public static final long getCoreTime() {
		return (long) server.manager.getCoreTime();
	}
	
	private ProjectSWG() {
		mainThread = Thread.currentThread();
		shutdownRequested = false;
	}
	
	private void run() {
		while (!shutdownRequested && (manager == null || (manager != null && !manager.isShutdownRequested()))) {
			manager = new CoreManager();
			initialize();
			start();
			loop();
			terminate();
			if (!shutdownRequested && !manager.isShutdownRequested()) {
				cleanup();
			}
		}
	}
	
	private void setStatus(ServerStatus status) {
		this.status = status;
		new ServerStatusIntent(status).broadcast();
	}
	
	private void forceShutdown() {
		shutdownRequested = true;
		mainThread.interrupt();
		try { mainThread.join(); } catch (InterruptedException e) { }
	}
	
	private void initialize() {
		setStatus(ServerStatus.INITIALIZING);
		System.out.println("ProjectSWG: Initializing...");
		if (!manager.initialize())
			throw new CoreException("Failed to initialize.");
		System.out.println("ProjectSWG: Initialized. Time: " + manager.getCoreTime() + "ms");
	}
	
	private void start() {
		System.out.println("ProjectSWG: Starting...");
		if (!manager.start())
			throw new CoreException("Failed to start.");
		System.out.println("ProjectSWG: Started. Time: " + manager.getCoreTime() + "ms");
	}
	
	private void loop() {
		setStatus((manager.getGalaxyStatus() == GalaxyStatus.UP) ? ServerStatus.OPEN : ServerStatus.LOCKED);
		while (!shutdownRequested && !manager.isShutdownRequested() && manager.isOperational()) {
			manager.flushPackets(); // Sends any packets that weren't sent
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				throw new CoreException("Main Thread Interrupted.");
			}
		}
	}
	
	private void terminate() {
		if (manager == null || status == ServerStatus.OFFLINE)
			return;
		System.out.println("ProjectSWG: Shutting down server...");
		setStatus(ServerStatus.TERMINATING);
		if (!manager.terminate())
			throw new CoreException("Failed to terminate.");
		setStatus(ServerStatus.OFFLINE);
		System.out.println("ProjectSWG: Terminated. Time: " + manager.getCoreTime() + "ms");
	}
	
	private void cleanup() {
		System.out.println("ProjectSWG: Cleaning up memory...");
		manager = null;
		Runtime.getRuntime().gc();
	}
	
	private static class CoreException extends RuntimeException {
		
		private static final long serialVersionUID = 455306876887818064L;
		
		public CoreException(String reason) {
			super(reason);
		}
		
	}
	
}
