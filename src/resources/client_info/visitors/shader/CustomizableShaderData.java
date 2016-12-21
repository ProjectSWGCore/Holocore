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
package resources.client_info.visitors.shader;

import resources.client_info.IffNode;
import resources.client_info.SWGFile;

public class CustomizableShaderData extends ShaderData {
	
	private String ddsFile;
	
	public CustomizableShaderData() {
		ddsFile = "";
	}
	
	@Override
	public void readIff(SWGFile iff) {
		IffNode node = iff.enterNextForm();
		switch (node.getTag()) {
			case "0000":
				readForm0(iff);
				break;
			case "0001":
				readForm1(iff);
				break;
			default:
				System.err.println("Unknown CustomizableShaderData version: " + node.getTag());
				break;
		}
		iff.exitForm();
	}
	
	@Override
	public String getTextureFile() {
		return ddsFile;
	}
	
	private void readForm0(SWGFile iff) {
		iff.enterForm("TXMS");
		while (iff.enterForm("TXM ") != null) {
			iff.enterForm("0001");
			IffNode nameChunk = iff.enterChunk("NAME");
			IffNode dataChunk = iff.enterChunk("DATA");
			if (dataChunk.readInt() == 'M'*256*256*256+'A'*256*256+'I'*256+'N')
				ddsFile = nameChunk.readString();
			iff.exitForm();
			iff.exitForm();
		}
	}
	
	private void readForm1(SWGFile iff) {
		StaticShaderData staticData = new StaticShaderData();
		iff.enterForm("SSHT");
		staticData.readIff(iff);
		iff.exitForm();
		ddsFile = staticData.getTextureFile();
	}
	
}
