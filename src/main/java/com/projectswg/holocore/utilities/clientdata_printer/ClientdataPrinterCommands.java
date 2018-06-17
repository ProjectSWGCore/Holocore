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
package com.projectswg.holocore.utilities.clientdata_printer;

import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.data.swgfile.visitors.DatatableData;
import me.joshlarson.jlcommon.log.Log;
import me.joshlarson.jlcommon.log.Log.LogLevel;
import me.joshlarson.jlcommon.log.log_wrapper.ConsoleLogWrapper;

import java.util.*;

public class ClientdataPrinterCommands {
	
	public static void main(String [] args) {
		Log.addWrapper(new ConsoleLogWrapper());
		List<Command> commands = loadBaseCommands();
		for (Command command : commands) {
			System.out.println(command);
		}
	}
	
	private static List<Command> loadBaseCommands() {
		// First = Higher Priority, Last = Lower Priority ---- Some tables contain duplicates, ORDER MATTERS!
		String [] commandTables = new String[] {
				"command_table", "command_table_ground", "client_command_table",
				"command_table_space", "client_command_table_ground", "client_command_table_space"
		};
		
		Set<Command> commands = new HashSet<>();
		for (String table : commandTables) {
			loadBaseCommands(commands, table);
		}
		List<Command> commandsSorted = new ArrayList<>(commands);
		commandsSorted.sort(Comparator.comparing(a -> a.name));
		return commandsSorted;
	}
	
	private static void loadBaseCommands(Set<Command> commands, String table) {
		DatatableData baseCommands = (DatatableData) ClientFactory.getInfoFromFile("datatables/command/" + table + ".iff");
		
		System.out.println("Table: " + table);
		System.out.print("    [");
		for (int col = 0; col < baseCommands.getColumnCount(); col++) {
			System.out.print(baseCommands.getColumnName(col));
			if (col+1 < baseCommands.getColumnCount())
				System.out.print(", ");
		}
		System.out.println("]");
		
		for (int row = 0; row < baseCommands.getRowCount(); row++) {
			commands.add(new Command(baseCommands.getString(row, "commandName"), baseCommands.getString(row, "scriptHook")));
		}
	}
	
	private static class Command {
		
		private final String name;
		private final String scriptHook;
		
		public Command(String name, String scriptHook) {
			this.name = name;
			this.scriptHook = scriptHook;
		}
		
		@Override
		public String toString() {
			return String.format("Command: %-25s\t%s", name, scriptHook);
		}
		
	}
	
}
