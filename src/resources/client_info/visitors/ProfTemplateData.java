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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import utilities.ByteUtilities;

public class ProfTemplateData extends ClientData {
	
	private List<Template> templates = new ArrayList<Template>();
	
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
	public void handleData(String node, ByteBuffer data, int size) {
		switch(node) {
		
		case "PTMPNAME":
			templates.add(new Template(ByteUtilities.nextString(data)));
			break;
			
		case "ITEM":
			int index = templates.size() - 1;
			data.getInt(); // empty int it seems for all items
			templates.get(index).getItems().add(ByteUtilities.nextString(data));
			break;
			
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
