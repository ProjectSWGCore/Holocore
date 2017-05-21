/************************************************************************************
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
package utilities.clientdata_printer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.data.swgfile.visitors.DatatableData;
import com.projectswg.common.debug.Log;
import com.projectswg.common.debug.Log.LogLevel;
import com.projectswg.common.debug.log_wrapper.ConsoleLogWrapper;

public class ClientdataPrinterCommands {
	
	public static void main(String [] args) {
		Log.addWrapper(new ConsoleLogWrapper(LogLevel.VERBOSE));
		List<String> strings = loadBaseCommands();
		for (String str : strings) {
			System.out.println(str);
		}
	}
	
	private static List<String> loadBaseCommands() {
		// First = Higher Priority, Last = Lower Priority ---- Some tables contain duplicates, ORDER MATTERS!
		String [] commandTables = new String[] {
				"command_table", "command_table_ground", "client_command_table",
				"command_table_space", "client_command_table_ground", "client_command_table_space"
		};
		
		Set<String> strings = new HashSet<>();
		for (String table : commandTables) {
			loadBaseCommands(strings, table);
		}
		List<String> stringSorted = new ArrayList<>(strings);
		stringSorted.sort((a, b) -> a.compareTo(b));
		return stringSorted;
	}
	
	private static void loadBaseCommands(Set<String> strings, String table) {
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
			strings.add(baseCommands.getString(row, "commandName"));
		}
	}
	
}
