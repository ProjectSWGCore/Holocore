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

package resources.client_info.visitors;

import utilities.ByteUtilities;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Waverunner
 */
public class PortalLayoutData extends ClientData {

	private int cellCount;
	private int portalCount;
	private List<Cell> cells = new LinkedList<>();

	@Override
	public void handleData(String node, ByteBuffer data, int totalSize) {
		System.out.println("Node: " + node);
		switch (node) {
			case "0003DATA":
				portalCount = data.getInt(); // number of portals
				cellCount = data.getInt(); // number of cells
				break;
/*			case "0005DATA": {

				Cell cell = new Cell();
				data.getInt(); // number of portals cell has

				cell.canSeeParentCell = data.get();
				cell.name = data.getString(Charset.forName("US-ASCII").newDecoder().de);
				cell.appearance = ByteUtilities.nextString(data);
				System.out.println("Name: " + cell.name + " App.: " + cell.appearance);
				cell.hasFloor = data.get();
				cell.floor = (cell.hasFloor == 1 ? ByteUtilities.nextString(data) : "");
				cells.add(cell);

				break;
			}*/
		}
	}

	public List<Cell> getCells() {
		return cells;
	}


	public static class Cell {

		public byte canSeeParentCell;
		public String name;
		public String appearance;
		public byte hasFloor;
		public String floor;
	}
}
