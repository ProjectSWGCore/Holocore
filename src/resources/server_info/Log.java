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
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Log {
	
	private static Log INSTANCE = null;
	
	private final Lock logLock;
	private final DateFormat timeFormat;
	private final File file;
	private final AtomicReference<LogLevel> level;
	private BufferedWriter writer;
	private boolean open;
	
	private Log(String filename, LogLevel level) {
		this.logLock = new ReentrantLock(true);
		this.timeFormat = new SimpleDateFormat("dd-MM-yy HH:mm:ss.SSS");
		this.file = new File(filename);
		this.level = new AtomicReference<>(level);
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
		this.level.set(level);
	}
	
	private synchronized LogLevel getLevel() {
		return level.get();
	}
	
	private synchronized void logRaw(LogLevel level, String logStr) {
		PrintStream stream;
		if (level.compareTo(LogLevel.WARN) >= 0)
			stream = System.err;
		else
			stream = System.out;
		stream.println(logStr);
		stream.flush();
		try {
			write(logStr);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void logImplementation(LogLevel level, String str, Object ... args) {
		if (getLevel().compareTo(level) > 0)
			return;
		String date;
		synchronized (timeFormat) {
			date = timeFormat.format(System.currentTimeMillis());
		}
		if (args.length == 0)
			logRaw(level, date + ' ' + level.getChar() + ": " + str);
		else
			logRaw(level, date + ' ' + level.getChar() + ": " + String.format(str, args));
	}
	
	private void lock() {
		logLock.lock();
	}
	
	private void unlock() {
		logLock.unlock();
	}
	
	private static synchronized final Log getInstance() {
		if (INSTANCE == null)
			INSTANCE = new Log("log.txt", LogLevel.VERBOSE);
		return INSTANCE;
	}
	
	protected static final void start() {
		try {
			getInstance().open();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected static final void stop() {
		try {
			getInstance().close();
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
		getInstance().setLevel(level);
	}
	
	/**
	 * Logs the string to the server log file, formatted to display the log
	 * severity, time and message.
	 * @param level the log level of this message between VERBOSE and ASSERT
	 * @param tag the tag to use for the log
	 * @param str the format string for the log
	 * @param args the string format arguments, if specified
	 */
	public static final void log(LogLevel level, String str, Object ... args) {
		try {
			getInstance().lock();
			getInstance().logImplementation(level, str, args);
		} finally {
			getInstance().unlock();
		}
	}
	
	/**
	 * Logs the string to the server log file, formatted to display the log
	 * severity as VERBOSE, as well as the time and message.
	 * @param message the format string for the log
	 * @param args the string format arguments, if specified
	 */
	public static final void v(String message, Object ... args) {
		log(LogLevel.VERBOSE, message, args);
	}
	
	/**
	 * Logs the string to the server log file, formatted to display the log
	 * severity as DEBUG, as well as the time and message.
	 * @param message the format string for the log
	 * @param args the string format arguments, if specified
	 */
	public static final void d(String message, Object ... args) {
		log(LogLevel.DEBUG, message, args);
	}
	
	/**
	 * Logs the string to the server log file, formatted to display the log
	 * severity as INFO, as well as the time and message.
	 * @param message the format string for the log
	 * @param args the string format arguments, if specified
	 */
	public static final void i(String message, Object ... args) {
		log(LogLevel.INFO, message, args);
	}
	
	/**
	 * Logs the string to the server log file, formatted to display the log
	 * severity as WARN, as well as the time and message.
	 * @param message the format string for the log
	 * @param args the string format arguments, if specified
	 */
	public static final void w(String message, Object ... args) {
		log(LogLevel.WARN, message, args);
	}
	
	
	/**
	 * Logs the exception to the server log file, formatted to display the log
	 * severity as WARN, as well as the time, and tag.
	 * @param exception the exception to print
	 */
	public static final void w(Throwable exception) {
		printException(LogLevel.WARN, exception);
	}
	
	/**
	 * Logs the string to the server log file, formatted to display the log
	 * severity as ERROR, as well as the time and message.
	 * @param tag the tag to use for the log
	 * @param message the format string for the log
	 * @param args the string format arguments, if specified
	 */
	public static final void e(String message, Object ... args) {
		log(LogLevel.ERROR, message, args);
	}
	
	/**
	 * Logs the exception to the server log file, formatted to display the log
	 * severity as ERROR, as well as the time, and tag.
	 * @param exception the exception to print
	 */
	public static final void e(Throwable exception) {
		printException(LogLevel.ERROR, exception);
	}
	
	/**
	 * Logs the string to the server log file, formatted to display the log
	 * severity as ASSERT, as well as the time and message.
	 * @param message the format string for the log
	 * @param args the string format arguments, if specified
	 */
	public static final void a(String message, Object ... args) {
		log(LogLevel.ASSERT, message, args);
	}
	
	/**
	 * Logs the exception to the server log file, formatted to display the log
	 * severity as ASSERT, as well as the time, and tag.
	 * @param exception the exception to print
	 */
	public static final void a(Throwable exception) {
		printException(LogLevel.ASSERT, exception);
	}
	
	private static final void printException(LogLevel level, Throwable exception) {
		Log instance = getInstance();
		try {
			String header1 = String.format("Exception in thread \"%s\" %s: %s", Thread.currentThread().getName(), exception.getClass().getName(), exception.getMessage());
			String header2 = String.format("Caused by: %s: %s", exception.getClass().getCanonicalName(), exception.getMessage());
			StackTraceElement [] elements = exception.getStackTrace();
			instance.lock();
			instance.logImplementation(level, header1);
			instance.logImplementation(level, header2);
			for (StackTraceElement e : elements) {
				instance.logImplementation(level, "    " + e.toString());
			}
		} finally {
			instance.unlock();
		}
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
