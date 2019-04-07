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
import com.projectswg.holocore.intents.support.data.control.ServerStatusIntent;
import com.projectswg.holocore.resources.support.data.client_info.ServerFactory;
import com.projectswg.holocore.resources.support.data.control.ServerStatus;
import com.projectswg.holocore.resources.support.data.server_info.DataManager;
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase;
import com.projectswg.holocore.services.gameplay.GameplayManager;
import com.projectswg.holocore.services.support.SupportManager;
import com.projectswg.holocore.utilities.ScheduledUtilities;
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
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.time.OffsetTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class ProjectSWG {
	
	public static final String VERSION = "FEB19";
	
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
		CommandLine arguments;
		try {
			arguments = new DefaultParser().parse(createArgumentOptions(), args, true);
		} catch (Throwable t) {
			//noinspection UseOfSystemOutOrSystemErr
			System.err.println("Failed to parse arguments. Reason: " + t.getClass().getName() + ": " + t.getMessage());
			new HelpFormatter().printHelp("java -jar Holocore.jar", createArgumentOptions());
			return -1;
		}
		if (arguments.hasOption("help")) {
			new HelpFormatter().printHelp("java -jar Holocore.jar", createArgumentOptions());
			return 0;
		}
		
		File logDirectory = new File("log");
		if (!logDirectory.isDirectory() && !logDirectory.mkdir())
			Log.w("Failed to make log directory!");
		if (arguments.hasOption("print-colors"))
			Log.addWrapper(new AnsiColorLogWrapper());
		else
			Log.addWrapper(new ConsoleLogWrapper());
		Log.addWrapper(new FileLogWrapper(new File(logDirectory, "log.txt")));
		
		Log.i("Holocore version: %s", VERSION);
		
		if (ProjectSWG.class.getResourceAsStream("/marker.txt") == null) {
			Log.a("Failed to read Holocore resources - aborting");
			return -1;
		}
		setupDatabase(arguments);
		DataManager.initialize();
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
		try {
			ServerFactory.getInstance().updateServerIffs();
		} catch (IOException e) {
			Log.e(e);
		}
	}
	
	private static void shutdownStaticClasses() {
		DataManager.terminate();
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
	
	private static void setupGalaxy(CommandLine arguments) {
		GALAXY.setId(1);
		GALAXY.setName(PswgDatabase.config().getString(ProjectSWG.class, "galaxyName", "Holocore"));
		GALAXY.setAddress("");
		GALAXY.setPopulation(0);
		GALAXY.setZoneOffset(OffsetTime.now().getOffset());
		GALAXY.setZonePort(0);
		GALAXY.setPingPort(0);
		GALAXY.setStatus(GalaxyStatus.DOWN);
		GALAXY.setMaxCharacters(PswgDatabase.config().getInt(ProjectSWG.class, "galaxyMaxCharacters", 2));
		GALAXY.setOnlinePlayerLimit(PswgDatabase.config().getInt(ProjectSWG.class, "galaxyMaxOnline", 3000));
		GALAXY.setOnlineFreeTrialLimit(PswgDatabase.config().getInt(ProjectSWG.class, "galaxyMaxOnline", 3000));
		GALAXY.setRecommended(true);
		try {
			GALAXY.setAdminServerPort(Integer.parseInt(arguments.getOptionValue("admin-port", "-1")));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("admin server port must be an integer", e);
		}
		
		ChatAvatar.setGalaxy(GALAXY.getName());
	}
	
	private static void setupDatabase(CommandLine arguments) {
		String dbStr = arguments.getOptionValue("database", "mongodb://localhost");
		String db = arguments.getOptionValue("database", "nge");
		
		PswgDatabase.initialize(dbStr, db);
	}
	
	private static Options createArgumentOptions() {
		Options options = new Options();
		options.addOption(Option.builder("h").longOpt("help").desc("print this message").build());
		options.addOption(Option.builder("C").longOpt("print-colors").desc("print colors to signify various log levels").build());
		options.addOption(Option.builder("a").longOpt("admin-port").argName("port").hasArg(true).desc("sets the admin server port").build());
		options.addOption(Option.builder("c").longOpt("database").argName("str").hasArg(true).desc("sets the connection string for mongodb (default: mongodb://localhost)").build());
		options.addOption(Option.builder("d").longOpt("dbName").argName("db").hasArg(true).desc("sets the mongodb database (default: nge)").build());
		return options;
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
