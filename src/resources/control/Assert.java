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
package resources.control;

import resources.server_info.Log;

public class Assert {
	
	private static volatile AssertLevel level = AssertLevel.ASSERT;
	
	public static void setLevel(AssertLevel level) {
		Assert.level = level;
	}
	
	public static boolean debug() {
		return level != AssertLevel.IGNORE;
	}
	
	public static void notNull(Object o) {
		notNull(o, "");
	}
	
	public static void notNull(Object o, String message) {
		if (debug() && o == null)
			handle(new NullPointerException(message));
	}
	
	public static void isNull(Object o) {
		isNull(o, "");
	}
	
	public static void isNull(Object o, String message) {
		if (debug() && o != null)
			handle(new AssertionException(message));
	}
	
	public static void test(boolean expr) {
		test(expr, "");
	}
	
	public static void test(boolean expr, String message) {
		if (debug() && !expr)
			handle(new AssertionException(message));
	}
	
	public static void fail() {
		fail("");
	}
	
	public static void fail(String message) {
		if (debug())
			handle(new AssertionException(message));
	}
	
	private static void handle(RuntimeException e) {
		AssertLevel level = Assert.level;
		switch (level) {
			case WARN:
				warn(e);
				break;
			case ASSERT:
				throw e;
			default:
				break;
		}
	}
	
	private static void warn(Exception e) {
		StackTraceElement [] elements = e.getStackTrace();
		if (elements.length <= 1)
			Log.e(e);
		else
			Log.e(e);
	}
	
	private static class AssertionException extends RuntimeException {
		
		private static final long serialVersionUID = 1L;
		
		public AssertionException(String message) {
			super(message);
		}
		
	}
	
	public enum AssertLevel {
		IGNORE,
		WARN,
		ASSERT
	}
	
}
