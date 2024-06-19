/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore

import com.projectswg.common.data.encodables.chat.ChatAvatar
import com.projectswg.common.data.encodables.galaxy.Galaxy
import com.projectswg.common.data.encodables.galaxy.Galaxy.GalaxyStatus
import com.projectswg.common.data.swgiff.parsers.SWGParser
import com.projectswg.holocore.intents.support.data.control.ServerStatusIntent
import com.projectswg.holocore.resources.support.data.control.ServerStatus
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase.config
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase.initialize
import com.projectswg.holocore.services.gameplay.GameplayManager
import com.projectswg.holocore.services.support.SupportManager
import com.projectswg.holocore.utilities.ScheduledUtilities
import me.joshlarson.jlcommon.argparse.Argument
import me.joshlarson.jlcommon.argparse.ArgumentParser
import me.joshlarson.jlcommon.argparse.ArgumentParserException
import me.joshlarson.jlcommon.concurrency.Delay
import me.joshlarson.jlcommon.control.IntentManager
import me.joshlarson.jlcommon.control.IntentManager.IntentSpeedStatistics
import me.joshlarson.jlcommon.control.Manager
import me.joshlarson.jlcommon.control.SafeMain
import me.joshlarson.jlcommon.control.ServiceBase
import me.joshlarson.jlcommon.log.Log
import me.joshlarson.jlcommon.log.log_wrapper.AnsiColorLogWrapper
import me.joshlarson.jlcommon.log.log_wrapper.ConsoleLogWrapper
import me.joshlarson.jlcommon.log.log_wrapper.FileLogWrapper
import me.joshlarson.jlcommon.utilities.ThreadUtilities
import java.io.File
import java.time.OffsetTime
import java.util.*
import java.util.function.Consumer
import kotlin.math.max

object ProjectSWG {
	val galaxy = Galaxy()
	
	@JvmStatic
	fun main(args: Array<String>) {
		SafeMain.main("holocore", { obj: Array<String> -> run(obj) }, args)
	}

	@JvmStatic
	val galacticTime: Long
		/**
		 * Returns the server's galactic time. This is the official time sent to
		 * the client and should be used for any official client-time purposes.
		 * @return the server's galactic time in seconds
		 */
		get() = (System.currentTimeMillis() / 1E3 - 1309996800L).toLong() // Date is 07/07/2011 GMT

	fun run(args: Array<String>): Int {
		val parser = createArgumentOptions()
		val arguments = try {
			parser.parse(args)
		} catch (e: ArgumentParserException) {
			System.err.println("Failed to parse arguments. Reason: " + e.javaClass.name + ": " + e.message)
			printHelp(parser)
			return -1
		}
		if (arguments.containsKey("help")) {
			printHelp(parser)
			return 0
		}
		setupLogging(arguments)
		if (ProjectSWG::class.java.getResource("/marker.txt") == null) {
			Log.a("Failed to read Holocore resources - aborting")
			return -1
		}
		setupDatabase(arguments)
		Thread.currentThread().priority = 10
		initializeSWGParser()
		setupGalaxy(arguments)
		IntentManager(false, Runtime.getRuntime().availableProcessors(), 8).use { intentManager ->
			IntentManager.setInstance(intentManager)
			val managers = listOf<ServiceBase>(
				// Must be in this order to ensure Gameplay sees Support intents
				GameplayManager(),
				SupportManager()
			)
			managers.forEach(Consumer { m: ServiceBase -> m.setIntentManager(intentManager) })
			setStatus(ServerStatus.INITIALIZING)
			if (Manager.start(managers)) {
				setStatus(ServerStatus.OPEN)
				Manager.run(managers, 50)
			}
			Delay.clearInterrupted()
			setStatus(ServerStatus.TERMINATING)
			Manager.stop(managers)
		}
		shutdownStaticClasses()
		printFinalPswgState()
		return 0
	}

	private fun initializeSWGParser() {
		SWGParser.setBasePath("serverdata")
	}

	private fun shutdownStaticClasses() {
		ScheduledUtilities.shutdown()
	}

	private fun printFinalPswgState() {
		assert(IntentManager.getInstance() != null)
		ThreadUtilities.printActiveThreads()
		val intentTimes = IntentManager.getInstance()!!.speedRecorder
		intentTimes.sortWith(Comparator.comparingLong { obj: IntentSpeedStatistics -> obj.totalTime }.reversed())
		Log.i("    Intent Times: [%d]", intentTimes.size)
		Log.i("        %-30s%-60s%-40s%-10s%-20s", "Intent", "Receiver Class", "Receiver Method", "Count", "Time")
		for (record in intentTimes) {
			if (record.count == 0L) continue
			var receiverName = record.key.toString()
			if (receiverName.indexOf('$') != -1) receiverName = receiverName.substring(0, receiverName.indexOf('$'))
			receiverName = receiverName.replace("com.projectswg.holocore.services.", "")
			val intentName = record.intent.simpleName
			val recordCount = record.count.toString()
			val recordTime = String.format("%.6fms", record.totalTime / 1E6)
			val receiverSplit = receiverName.split("#".toRegex(), limit = 2).toTypedArray()
			Log.i("        %-30s%-60s%-40s%-10s%-20s", intentName, receiverSplit[0], receiverSplit[1], recordCount, recordTime)
		}
	}

	private fun setStatus(status: ServerStatus) {
		ServerStatusIntent(status).broadcast()
	}

	private fun setupGalaxy(arguments: Map<String, Any>) {
		galaxy.id = 1
		galaxy.name = config.getString(ProjectSWG::class.java, "galaxyName", "Holocore")
		galaxy.address = ""
		galaxy.population = 0
		galaxy.setZoneOffset(OffsetTime.now().offset)
		galaxy.zonePort = 0
		galaxy.pingPort = 0
		galaxy.status = GalaxyStatus.DOWN
		galaxy.maxCharacters = config.getInt(ProjectSWG::class.java, "galaxyMaxCharacters", 2)
		galaxy.onlinePlayerLimit = config.getInt(ProjectSWG::class.java, "galaxyMaxOnline", 3000)
		galaxy.onlineFreeTrialLimit = config.getInt(ProjectSWG::class.java, "galaxyMaxOnline", 3000)
		galaxy.isRecommended = true
		try {
			galaxy.adminServerPort = ((arguments["admin-port"] ?: "-1") as String).toInt()
		} catch (e: NumberFormatException) {
			throw IllegalArgumentException("admin server port must be an integer", e)
		}
		ChatAvatar.setGalaxy(galaxy.name)
	}

	private fun setupDatabase(arguments: Map<String, Any>) {
		val dbStr = (arguments["database"] ?: "mongodb://localhost") as String
		val db = (arguments["dbName"] ?: "cu") as String
		initialize(dbStr, db)
	}

	private fun setupLogging(arguments: Map<String, Any>) {
		var logLevel = Log.LogLevel.TRACE
		if (arguments.containsKey("log-level")) {
			try {
				logLevel = Log.LogLevel.valueOf((arguments["log-level"] as String?)!!)
			} catch (e: IllegalArgumentException) {
				System.err.println("Invalid log level: " + arguments["log-level"])
			} catch (e: ClassCastException) {
				System.err.println("Invalid log level: " + arguments["log-level"])
			}
		}
		if (arguments.containsKey("print-colors")) {
			Log.addWrapper(AnsiColorLogWrapper(logLevel))
		} else {
			Log.addWrapper(ConsoleLogWrapper(logLevel))
		}
		if (arguments.containsKey("log-file")) {
			val logDirectory = File("log")
			if (!logDirectory.isDirectory && !logDirectory.mkdir()) Log.w("Failed to make log directory!")
			Log.addWrapper(FileLogWrapper(File(logDirectory, "log.txt")))
		}
	}

	private fun createArgumentOptions(): ArgumentParser {
		val parser = ArgumentParser()
		parser.addArgument(Argument.builder("help").shortName('h').longName("help").isOptional(true).description("print this message").build())
		parser.addArgument(
			Argument.builder("print-colors")
				.shortName('C')
				.longName("print-colors")
				.isOptional(true)
				.description("print colors to signify various log levels")
				.build()
		)
		parser.addArgument(
			Argument.builder("log-file")
				.shortName('f')
				.longName("log-file")
				.isOptional(true)
				.description("enable logging to file")
				.build()
		)
		parser.addArgument(
			Argument.builder("log-level")
				.shortName('l')
				.longName("log-level")
				.argCount('1')
				.isOptional(true)
				.description("specify log level [TRACE, DATA, INFO, WARN, ERROR, ASSERT]")
				.build()
		)
		parser.addArgument(
			Argument.builder("database")
				.shortName('c')
				.longName("database")
				.argCount('1')
				.isOptional(true)
				.description("sets the connection string for mongodb (default: mongodb://localhost)")
				.build()
		)
		parser.addArgument(
			Argument.builder("database-name")
				.shortName('d')
				.longName("dbName")
				.argCount('1')
				.isOptional(true)
				.description("sets the mongodb database (default: cu)")
				.build()
		)
		return parser
	}

	private fun printHelp(parser: ArgumentParser) {
		var maxLengthName = 0
		var maxLengthLongName = 0
		for (arg in parser.arguments) {
			maxLengthName = max(maxLengthName, arg.name.length)
			maxLengthLongName = max(maxLengthLongName, arg.longName.length)
		}
		println("Help:")
		for (arg in parser.arguments) {
			System.out.printf("%-" + maxLengthName + "s -%c%-" + maxLengthLongName + "s  %s%n", arg.name, arg.shortName, "", arg.description)
			System.out.printf("%-" + maxLengthName + "s --%s%n", "", arg.longName)
		}
	}

	class CoreException : RuntimeException {
		constructor(reason: String?) : super(reason)
		constructor(reason: String, cause: Throwable) : super(reason, cause)

		companion object {
			private const val serialVersionUID = 455306876887818064L
		}
	}
}
