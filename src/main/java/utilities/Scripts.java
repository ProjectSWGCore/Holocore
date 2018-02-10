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

import java.io.IOException;

import com.projectswg.common.debug.Log;

import groovy.lang.Binding;
import groovy.lang.Script;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;

public class Scripts {
	private static GroovyScriptEngine groovyEngine;

	static {
		try {
			groovyEngine = new GroovyScriptEngine("scripts/");
		} catch (IOException e) {
			Log.e(e);
		}
	}

	/**
	 * Invokes a method from the provided Groovy Script.
	 *
	 * @param scriptName name of the script, relative to the scripts folder.
	 * @param method     name of the specific method within the script.
	 * @param args       to pass to the method.
	 * @return expected return type of the script. If the method doesn't have a return statement, this method returns {@code null}.
	 * If an exception occurs, {@code null} is returned.
	 * @throws ResourceException if script file does not exist
	 * @throws ScriptException   if there is a problem executing the script and/or method within the script
	 */
	@SuppressWarnings("unchecked")
	public static <T> T invoke(String scriptName, String method, Object... args) throws ResourceException, ScriptException {
		Script script = getScript(scriptName);
		if (script == null)
			return null;
		else return (T) script.invokeMethod(method, args);
	}

	/**
	 * Creates a binding from an array of variables. Bindings are a set of <i>global</i> variables that can be used by the script. As such,
	 * you should use a new set of bindings for each invocation of the script on different threads.
	 * <br><br>The name of the variable should lead the instance. An example would be:
	 * <br>&nbsp&nbsp{@code Binding bindings = createBindings("variableNameOne", variableOne, "variableNameTwo", variableTwo);}
	 *
	 * @param variables an array of variables. Variable names should lead the instance of the variable.
	 */
	public static Binding createBindings(Object... variables) {
		Binding binding = new Binding();

		for (int i = 0; i < variables.length; i += 2) {
			if (!(variables[i] instanceof String))
				continue;

			binding.setVariable((String) variables[i], variables[i + 1]);
		}
		return binding;
	}

	/**
	 * Creates the Groovy Script and returns it. The method uses the {@link GroovyScriptEngine}'s createScript method.
	 * A new {@link Binding} is passed to the created {@link Script} to ensure thread safety, providing a new instance of the generated {@link Script} for the accessing thread.
	 *
	 * @param scriptName Name of the script to load
	 * @return a unique instance of {@link Script}
	 */
	public static Script getScript(String scriptName) throws ResourceException, ScriptException {
		return getScript(scriptName, new Binding());
	}

	/**
	 * Creates the Groovy Script and returns it, with the global variables specified within the binding.
	 * The method uses the {@link GroovyScriptEngine}'s createScript method.
	 *
	 * @param scriptName name of the script to load
	 * @param binding    the binding instance to use
	 * @return an instance of the obtained Groovy Script
	 */
	public static Script getScript(String scriptName, Binding binding) throws ResourceException, ScriptException {
		return groovyEngine.createScript(scriptName + ".groovy", binding);
	}
}
