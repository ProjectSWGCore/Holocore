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
package resources.client_info.visitors;

import java.util.ArrayList;
import java.util.List;

import resources.client_info.ClientData;
import resources.client_info.IffNode;
import resources.client_info.SWGFile;

public class ProfTemplateData extends ClientData {
	
	private List<Template> templates = new ArrayList<>();
	
	private class Template {
		
		private List<String> items;
		private String template;
		
		public Template(String template) {
			this.template = template;
			this.items = new ArrayList<>();
		}

		public List<String> getItems() {
			return items;
		}

		public String getTemplate() {
			return template;
		}
	}

	@Override
	public void readIff(SWGFile iff) {
		iff.enterNextForm(); // Version

		IffNode form;
		while((form = iff.enterNextForm()) != null) {
			if (!form.getTag().equals("PTMP")) {
				iff.exitForm();
				continue;
			}

			IffNode chunk;
			while((chunk = iff.enterNextChunk()) != null) {
				String tag = chunk.getTag();
				switch(tag) {
					case "NAME":
						templates.add(new Template(chunk.readString()));
						break;

					case "ITEM":
						int index = templates.size() - 1;
						chunk.readInt(); // empty int it seems for all items
						templates.get(index).getItems().add(chunk.readString());
						break;

					default: break;
				}
			}
			iff.exitForm();
		}
	}

	/**
	 * Gets the items for the race
	 * @param race Race IFF template
	 * @return {@link List} of the newbie clothing items for the racial template
	 */
	public List<String> getItems(String race) {
		for (Template t : templates) {
			if (t.getTemplate().equals(race))
				return t.getItems();
		}
		return null;
	}
}
