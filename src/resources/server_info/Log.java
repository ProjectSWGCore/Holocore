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
package resources.server_info;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import resources.control.Service;

public class Log {
	
	private static final DateFormat LOG_FORMAT = new SimpleDateFormat("dd-mm-yy HH:mm:ss.SSS");
	private static final Log LOG = new Log("log.txt", LogLevel.VERBOSE);
	
	private final File file;
	private BufferedWriter writer;
	private LogLevel level;
	private boolean open;
	
	private Log(String filename, LogLevel level) {
		this.file = new File(filename);
		this.level = level;
		open = false;
	}
	
	private synchronized void open() throws IOException {
		if (!open)
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
		open = true;
	}
	
	private synchronized void close() throws IOException {
		if (open)
			writer.close();
		open = false;
	}
	
	private synchronized void write(String str) throws IOException {
		if (open) {
			writer.write(str);
			writer.newLine();
			writer.flush();
		}
	}
	
	private synchronized void setLevel(LogLevel level) {
		this.level = level;
	}
	
	private synchronized LogLevel getLevel() {
		return level;
	}
	
	protected static final void start() {
		try {
			LOG.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected static final void stop() {
		try {
			LOG.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Sets the minimum level for logs to be reported. This is according to the
	 * following order: VERBOSE, DEBUG, INFO, WARNING, ERROR, then ASSERT.
	 * @param level the minimum log level, inclusively. Default is VERBOSE
	 */
	public static final void setLogLevel(LogLevel level) {
		synchronized (LOG) {
			LOG.setLevel(level);
		}
	}
	
	/**
	 * Logs the string to the server log file, formatted to display the log
	 * severity, time, tag and message.
	 * @param level the log level of this message between VERBOSE and ASSERT
	 * @param tag the tag to use for the log
	 * @param str the format string for the log
	 * @param args the string format arguments, if specified
	 */
	public static final void log(LogLevel level, String tag, String str, Object ... args) {
		synchronized (LOG) {
			if (LOG.getLevel().compareTo(level) > 0)
				return;
		}
		String date;
		synchronized (LOG_FORMAT) {
			date = LOG_FORMAT.format(System.currentTimeMillis());
		}
		String logStr = String.format(str, args);
		String log = String.format("%s %c/[%s]: %s", date, level.getChar(), tag, logStr);
		synchronized (LOG) {
			try {
				LOG.write(log);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Logs the string to the server log file, formatted to display the log
	 * severity as VERBOSE, as well as the time, tag and message.
	 * @param tag the tag to use for the log
	 * @param message the format string for the log
	 * @param args the string format arguments, if specified
	 */
	public static final void v(String tag, String message, Object ... args) {
		log(LogLevel.VERBOSE, tag, message, args);
	}
	
	/**
	 * Logs the string to the server log file, formatted to display the log
	 * severity as VERBOSE, as well as the time, service name and message.
	 * @param service the service outputting this log info
	 * @param message the format string for the log
	 * @param args the string format arguments, if specified
	 */
	public static final void v(Service service, String message, Object ... args) {
		log(LogLevel.VERBOSE, service.getClass().getSimpleName(), message, args);
	}
	
	/**
	 * Logs the string to the server log file, formatted to display the log
	 * severity as DEBUG, as well as the time, tag and message.
	 * @param tag the tag to use for the log
	 * @param message the format string for the log
	 * @param args the string format arguments, if specified
	 */
	public static final void d(String tag, String message, Object ... args) {
		log(LogLevel.DEBUG, tag, message, args);
	}
	
	/**
	 * Logs the string to the server log file, formatted to display the log
	 * severity as DEBUG, as well as the time, tag and message.
	 * @param service the service outputting this log info
	 * @param message the format string for the log
	 * @param args the string format arguments, if specified
	 */
	public static final void d(Service service, String message, Object ... args) {
		log(LogLevel.DEBUG, service.getClass().getSimpleName(), message, args);
	}
	
	/**
	 * Logs the string to the server log file, formatted to display the log
	 * severity as INFO, as well as the time, tag and message.
	 * @param tag the tag to use for the log
	 * @param message the format string for the log
	 * @param args the string format arguments, if specified
	 */
	public static final void i(String tag, String message, Object ... args) {
		log(LogLevel.INFO, tag, message, args);
	}
	
	/**
	 * Logs the string to the server log file, formatted to display the log
	 * severity as INFO, as well as the time, service name and message.
	 * @param service the service outputting this log info
	 * @param message the format string for the log
	 * @param args the string format arguments, if specified
	 */
	public static final void i(Service service, String message, Object ... args) {
		log(LogLevel.INFO, service.getClass().getSimpleName(), message, args);
	}
	
	/**
	 * Logs the string to the server log file, formatted to display the log
	 * severity as WARN, as well as the time, tag and message.
	 * @param tag the tag to use for the log
	 * @param message the format string for the log
	 * @param args the string format arguments, if specified
	 */
	public static final void w(String tag, String message, Object ... args) {
		log(LogLevel.WARN, tag, message, args);
	}
	
	/**
	 * Logs the string to the server log file, formatted to display the log
	 * severity as WARN, as well as the time, tag and message.
	 * @param service the service outputting this log info
	 * @param message the format string for the log
	 * @param args the string format arguments, if specified
	 */
	public static final void w(Service service, String message, Object ... args) {
		log(LogLevel.WARN, service.getClass().getSimpleName(), message, args);
	}
	
	/**
	 * Logs the string to the server log file, formatted to display the log
	 * severity as ERROR, as well as the time, tag and message.
	 * @param tag the tag to use for the log
	 * @param message the format string for the log
	 * @param args the string format arguments, if specified
	 */
	public static final void e(String tag, String message, Object ... args) {
		log(LogLevel.ERROR, tag, message, args);
	}
	
	/**
	 * Logs the string to the server log file, formatted to display the log
	 * severity as ERROR, as well as the time, tag and message.
	 * @param service the service outputting this log info
	 * @param message the format string for the log
	 * @param args the string format arguments, if specified
	 */
	public static final void e(Service service, String message, Object ... args) {
		log(LogLevel.ERROR, service.getClass().getSimpleName(), message, args);
	}
	
	/**
	 * Logs the string to the server log file, formatted to display the log
	 * severity as ASSERT, as well as the time, tag and message.
	 * @param tag the tag to use for the log
	 * @param message the format string for the log
	 * @param args the string format arguments, if specified
	 */
	public static final void a(String tag, String message, Object ... args) {
		log(LogLevel.ASSERT, tag, message, args);
	}
	
	/**
	 * Logs the string to the server log file, formatted to display the log
	 * severity as ASSERT, as well as the time, tag and message.
	 * @param service the service outputting this log info
	 * @param str the format string for the log
	 * @param args the string format arguments, if specified
	 */
	public static final void a(Service service, String message, Object ... args) {
		log(LogLevel.ASSERT, service.getClass().getSimpleName(), message, args);
	}
	
	public static enum LogLevel {
		VERBOSE	('V'),
		DEBUG	('D'),
		INFO	('I'),
		WARN	('W'),
		ERROR	('E'),
		ASSERT	('A');
		
		private char c;
		
		LogLevel(char c) {
			this.c = c;
		}
		
		public char getChar() { return c; }
	}
	
}
