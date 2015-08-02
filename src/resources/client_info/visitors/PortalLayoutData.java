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

import resources.client_info.ClientData;
import resources.client_info.IffNode;
import resources.client_info.SWGFile;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Waverunner
 */
public class PortalLayoutData extends ClientData {

	private List<Cell> cells = new LinkedList<>();

	@Override
	public void readIff(SWGFile iff) {
		IffNode versionForm = iff.enterNextForm();
		if (versionForm == null) {
			System.err.println("Expected version for a POB IFF");
			return;
		}

		int version = versionForm.getVersionFromTag();
		switch(version) {
			case 3: readVersion3(iff); break;
			case 4: readVersion3(iff); break; // Seems to be identical
			default: System.err.println("Do not know how to handle POB version type " + version + " in file " + iff.getFileName());
		}
	}

	private void readVersion3(SWGFile iff) {
		iff.enterForm("CELS");

		while(iff.enterForm("CELL") != null) {
			cells.add(new Cell(iff));
			iff.exitForm();
		}

		iff.exitForm(); // Exit CELS form
	}

	public List<Cell> getCells() {
		return cells;
	}

	public static class Cell extends ClientData {
		private String name;

		public Cell(SWGFile iff) {
			readIff(iff);
		}

		@Override
		public void readIff(SWGFile iff) {
			IffNode versionForm = iff.enterNextForm();
			if (versionForm == null) {
				System.err.println("Expected version for CELL in IFF " + iff.getFileName());
				return;
			}

			int version = versionForm.getVersionFromTag();
			switch(version) {
				case 3: break;
				case 5: readVersion5(iff); break;
				default: System.err.println("Don't know how to handle version " + version + " CELL " + iff.getFileName());
			}

			iff.exitForm();
		}

		private void readVersion5(SWGFile iff) {
			IffNode dataChunk = iff.enterChunk("DATA");
			dataChunk.readInt(); // cellPortals
			dataChunk.readBoolean(); // canSeeParentCell
			name = dataChunk.readString();
		}

		public String getName() {
			return name;
		}
	}
}
