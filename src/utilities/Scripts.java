/*******************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com
 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.
 * Our goal is to create an emulator which will provide a server for players to
 * continue playing a game similar to the one they used to play. We are basing
 * it on the final publish of the game prior to end-game events.
 *
 * This file is part of Holocore.
 *
 * --------------------------------------------------------------------------------
 *
 * Holocore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Holocore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/

package utilities;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;
import resources.server_info.Config;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class Scripts {
	private static Scripts instance = new Scripts();
	private static final String SCRIPTS = "scripts/";
	private final PythonInterpreter interpreter;

	private Scripts() {
		interpreter = new PythonInterpreter();
	}

	public static PyObject execute(String script, String method, Object ... args) {
		if (!exists(script))
			return null;

		instance.interpreter.execfile(script);
		return instance.interpreter.get(method).__call__(Py.javas2pys(args));
	}

	public static boolean exists(String script) {
		return (new File(SCRIPTS + script).exists());
	}

	public static void initialize(Config config) {
		PySystemState systemState = instance.interpreter.getSystemState();
		systemState.setCurrentWorkingDir(systemState.getCurrentWorkingDir() + "/");
		Scripts.initScripts(systemState);
	}

	private static void initScripts(PySystemState systemState) {
		File scripts = new File(systemState.getCurrentWorkingDir());
		final int length = scripts.getAbsolutePath().length();
		final PythonInterpreter interpreter = instance.interpreter;

		try {
			Files.walkFileTree(scripts.toPath(), new FileVisitor<Path>() {
				public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
					String name = path.toFile().getAbsolutePath();
					if (name.endsWith(".py"))
						interpreter.execfile(name.substring(length + 1));
					return FileVisitResult.CONTINUE;
				}
				public FileVisitResult visitFileFailed(Path path, IOException e) throws IOException {
					return FileVisitResult.CONTINUE;
				}
				public FileVisitResult postVisitDirectory(Path path, IOException e) throws IOException {
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void cleanup() {
		instance.interpreter.cleanup();
	}
}
