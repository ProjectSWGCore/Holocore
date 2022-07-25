/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package com.projectswg.holocore;

import com.projectswg.common.data.encodables.chat.ChatAvatar;
import com.projectswg.common.data.encodables.galaxy.Galaxy;
import com.projectswg.common.data.encodables.galaxy.Galaxy.GalaxyStatus;
import com.projectswg.common.data.swgiff.parsers.SWGParser;
import com.projectswg.holocore.intents.support.data.control.ServerStatusIntent;
import com.projectswg.holocore.resources.support.data.client_info.ServerFactory;
import com.projectswg.holocore.resources.support.data.control.ServerStatus;
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase;
import com.projectswg.holocore.services.gameplay.GameplayManager;
import com.projectswg.holocore.services.support.SupportManager;
import com.projectswg.holocore.utilities.ScheduledUtilities;
import me.joshlarson.jlcommon.argparse.Argument;
import me.joshlarson.jlcommon.argparse.ArgumentParser;
import me.joshlarson.jlcommon.argparse.ArgumentParserException;
import me.joshlarson.jlcommon.concurrency.Delay;
import me.joshlarson.jlcommon.control.IntentManager;
import me.joshlarson.jlcommon.control.IntentManager.IntentSpeedStatistics;
import me.joshlarson.jlcommon.control.Manager;
import me.joshlarson.jlcommon.control.SafeMain;
import me.joshlarson.jlcommon.control.ServiceBase;
import me.joshlarson.jlcommon.log.Log;
import me.joshlarson.jlcommon.log.log_wrapper.AnsiColorLogWrapper;
import me.joshlarson.jlcommon.log.log_wrapper.ConsoleLogWrapper;
import me.joshlarson.jlcommon.log.log_wrapper.FileLogWrapper;
import me.joshlarson.jlcommon.utilities.ThreadUtilities;

import java.io.File;
import java.io.IOException;
import java.time.OffsetTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ProjectSWG {
	
	private static final Galaxy GALAXY = new Galaxy();
	
	public static void main(String [] args) {
		SafeMain.main("holocore", ProjectSWG::run, args);
	}
	
	/**
	 * Returns the server's galactic time. This is the official time sent to
	 * the client and should be used for any official client-time purposes.
	 * @return the server's galactic time in seconds
	 */
	public static long getGalacticTime() {
		return (long) (System.currentTimeMillis()/1E3 - 1309996800L); // Date is 07/07/2011 GMT
	}
	
	public static Galaxy getGalaxy() {
		return GALAXY;
	}
	
	static int run(String [] args) {
		ArgumentParser parser = createArgumentOptions();
		Map<String, Object> arguments;
		try {
			arguments = parser.parse(args);
		} catch (ArgumentParserException e) {
			System.err.println("Failed to parse arguments. Reason: " + e.getClass().getName() + ": " + e.getMessage());
			printHelp(parser);
			return -1;
		}
		if (arguments.containsKey("help")) {
			printHelp(parser);
			return 0;
		}
		
		setupLogging(arguments);
		if (ProjectSWG.class.getResourceAsStream("/marker.txt") == null) {
			Log.a("Failed to read Holocore resources - aborting");
			return -1;
		}
		setupDatabase(arguments);
		Thread.currentThread().setPriority(10);
		initializeServerFactory();
		setupGalaxy(arguments);
		try (IntentManager intentManager = new IntentManager(false, Runtime.getRuntime().availableProcessors(), 8)) {
			IntentManager.setInstance(intentManager);
			List<ServiceBase> managers = Arrays.asList(new GameplayManager(), new SupportManager()); // Must be in this order to ensure Gameplay sees Support intents
			managers.forEach(m -> m.setIntentManager(intentManager));

			setStatus(ServerStatus.INITIALIZING);
			if (Manager.start(managers)) {
				setStatus(ServerStatus.OPEN);
				Manager.run(managers, 50);
			}
			Delay.clearInterrupted();
			setStatus(ServerStatus.TERMINATING);
			Manager.stop(managers);
		}

		shutdownStaticClasses();
		printFinalPswgState();
		return 0;
	}
	
	// TODO: Replace all iffs with sdbs
	private static void initializeServerFactory() {
		SWGParser.setBasePath("serverdata");
		try {
			ServerFactory.getInstance().updateServerIffs();
		} catch (IOException e) {
			Log.e(e);
		}
	}
	
	private static void shutdownStaticClasses() {
		ScheduledUtilities.shutdown();
	}
	
	private static void printFinalPswgState() {
		assert IntentManager.getInstance() != null;
		ThreadUtilities.printActiveThreads();
		List<IntentSpeedStatistics> intentTimes = IntentManager.getInstance().getSpeedRecorder();
		intentTimes.sort(Comparator.comparingLong(IntentSpeedStatistics::getTotalTime).reversed());
		Log.i("    Intent Times: [%d]", intentTimes.size());
		Log.i("        %-30s%-60s%-40s%-10s%-20s", "Intent", "Receiver Class", "Receiver Method", "Count", "Time");
		for (IntentSpeedStatistics record : intentTimes) {
			if (record.getCount() == 0)
				continue;
			String receiverName = record.getKey().toString();
			if (receiverName.indexOf('$') != -1)
				receiverName = receiverName.substring(0, receiverName.indexOf('$'));
			receiverName = receiverName.replace("com.projectswg.holocore.services.", "");
			String intentName = record.getIntent().getSimpleName();
			String recordCount = Long.toString(record.getCount());
			String recordTime = String.format("%.6fms", record.getTotalTime() / 1E6);
			String [] receiverSplit = receiverName.split("#", 2);
			Log.i("        %-30s%-60s%-40s%-10s%-20s", intentName, receiverSplit[0], receiverSplit[1], recordCount, recordTime);
		}
	}
	
	private static void setStatus(ServerStatus status) {
		new ServerStatusIntent(status).broadcast();
	}
	
	private static void setupGalaxy(Map<String, Object> arguments) {
		GALAXY.setId(1);
		GALAXY.setName(PswgDatabase.INSTANCE.getConfig().getString(ProjectSWG.class, "galaxyName", "Holocore"));
		GALAXY.setAddress("");
		GALAXY.setPopulation(0);
		GALAXY.setZoneOffset(OffsetTime.now().getOffset());
		GALAXY.setZonePort(0);
		GALAXY.setPingPort(0);
		GALAXY.setStatus(GalaxyStatus.DOWN);
		GALAXY.setMaxCharacters(PswgDatabase.INSTANCE.getConfig().getInt(ProjectSWG.class, "galaxyMaxCharacters", 2));
		GALAXY.setOnlinePlayerLimit(PswgDatabase.INSTANCE.getConfig().getInt(ProjectSWG.class, "galaxyMaxOnline", 3000));
		GALAXY.setOnlineFreeTrialLimit(PswgDatabase.INSTANCE.getConfig().getInt(ProjectSWG.class, "galaxyMaxOnline", 3000));
		GALAXY.setRecommended(true);
		try {
			GALAXY.setAdminServerPort(Integer.parseInt((String) arguments.getOrDefault("admin-port", "-1")));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("admin server port must be an integer", e);
		}
		
		ChatAvatar.setGalaxy(GALAXY.getName());
	}
	
	private static void setupDatabase(Map<String, Object> arguments) {
		String dbStr = (String) arguments.getOrDefault("database", "mongodb://localhost");
		String db = (String) arguments.getOrDefault("dbName", "cu");
		
		PswgDatabase.INSTANCE.initialize(dbStr, db);
	}
	
	private static void setupLogging(Map<String, Object> arguments) {
		Log.LogLevel logLevel = Log.LogLevel.TRACE;
		if (arguments.containsKey("log-level")) {
			try {
				logLevel = Log.LogLevel.valueOf((String) arguments.get("log-level"));
			} catch (IllegalArgumentException | ClassCastException e) {
				System.err.println("Invalid log level: " + arguments.get("log-level"));
			}
		}
		
		if (arguments.containsKey("print-colors"))
			Log.addWrapper(new AnsiColorLogWrapper(logLevel));
		else
			Log.addWrapper(new ConsoleLogWrapper(logLevel));
		
		if (arguments.containsKey("log-file")) {
			File logDirectory = new File("log");
			if (!logDirectory.isDirectory() && !logDirectory.mkdir())
				Log.w("Failed to make log directory!");
			Log.addWrapper(new FileLogWrapper(new File(logDirectory, "log.txt")));
		}
	}
	
	private static ArgumentParser createArgumentOptions() {
		ArgumentParser parser = new ArgumentParser();
		parser.addArgument(Argument.builder("help").shortName('h').longName("help").isOptional(true).description("print this message").build());
		parser.addArgument(Argument.builder("print-colors").shortName('C').longName("print-colors").isOptional(true).description("print colors to signify various log levels").build());
		parser.addArgument(Argument.builder("log-file").shortName('f').longName("log-file").isOptional(true).description("enable logging to file").build());
		parser.addArgument(Argument.builder("log-level").shortName('l').longName("log-level").argCount('1').isOptional(true).description("specify log level [TRACE, DATA, INFO, WARN, ERROR, ASSERT]").build());
		parser.addArgument(Argument.builder("database").shortName('c').longName("database").argCount('1').isOptional(true).description("sets the connection string for mongodb (default: mongodb://localhost)").build());
		parser.addArgument(Argument.builder("database-name").shortName('d').longName("dbName").argCount('1').isOptional(true).description("sets the mongodb database (default: cu)").build());
		
		return parser;
	}
	
	private static void printHelp(ArgumentParser parser) {
		int maxLengthName = 0;
		int maxLengthLongName = 0;
		for (Argument arg : parser.getArguments()) {
			maxLengthName = Math.max(maxLengthName, arg.getName().length());
			maxLengthLongName = Math.max(maxLengthLongName, arg.getLongName().length());
		}
		System.out.println("Help:");
		for (Argument arg : parser.getArguments()) {
			System.out.printf("%-"+maxLengthName+"s -%c%-"+(maxLengthLongName)+"s  %s%n", arg.getName(), arg.getShortName(), "", arg.getDescription());
			System.out.printf("%-"+maxLengthName+"s --%s%n", "", arg.getLongName());
		}
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
