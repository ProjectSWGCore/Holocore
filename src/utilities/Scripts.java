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
package utilities;

import java.io.FileNotFoundException;
import java.io.FileReader;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class Scripts {

	private static final String SCRIPTS = "scripts/";
	private static final String EXTENSION = ".js";
	private static final ScriptEngine ENGINE = new ScriptEngineManager().getEngineByName("nashorn");
	private static final Invocable INVOCABLE = (Invocable) ENGINE;
	
	static {
		ENGINE.put("intentFactory", new IntentFactory());
	}
	
	// Prevents instantiation.
	private Scripts() {}
	
	/**
	 * @param script name of the script, relative to the scripts folder.
	 * @param function name of the specific function within the script.
	 * @param args to pass to the function.
	 * @return whatever the function returns. If the function doesn't have a return statement, this method returns {@code null}.
	 * If an exception occurs, {@code null} is returned.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T invoke(String script, String function, Object... args) {
		try {
			ENGINE.eval(new FileReader(SCRIPTS + script + EXTENSION));
			return (T) INVOCABLE.invokeFunction(function, args);
		} catch (FileNotFoundException e) {
			// No need to print anything, this is a common error.
			// Returning null is all that's necessary
			return null;
		} catch (Throwable t) {
			t.printStackTrace();
			return null;
		}
	}
	
}
