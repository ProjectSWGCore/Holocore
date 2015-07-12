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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import resources.client_info.ClientFactory;
import resources.client_info.ClientData;
import utilities.ByteUtilities;

public class ObjectData extends ClientData {

	private Map<String, Object> attributes = new HashMap<String, Object>();
	private List<String> parsedFiles = new ArrayList<String>();
	
	public static final String APPEARANCE_FILE = "appearanceFilename";
	public static final String ARRANGEMENT_FILE = "arrangementDescriptorFilename";
	public static final String CONTAINER_TYPE = "containerType";
	public static final String VOLUME_LIMIT = "containerVolumeLimit";
	public static final String DETAIL_STF = "detailedDescription";
	public static final String OBJ_STF = "objectName";
	public static final String PORTAL_LAYOUT = "portalLayoutFilename";
	public static final String SLOT_DESCRIPTOR = "slotDescriptorFilename";

	public ObjectData() {}
	
	@Override
	public void parse(String node, ByteBuffer data, int size) {
		switch(node) {
		
		case "DERVXXXX": // Extended attributes
			String file = ByteUtilities.nextString(data);
			
			if (parsedFiles.contains(file)) // some DERVXXX were repeated and we do not want to replace any attributes unless they're overriden by a more specific obj
				break;
			
			ClientData attrData = ClientFactory.getInfoFromFile(file);
			if (attrData == null || !(attrData instanceof ObjectData)) {
				System.out.println("Could not load attribute data from file " + file + "!");
				return; // break out of whole method as we should only continue if we have all the extended attributes
			}
			
			// Put all the extended attributes in this map so it's accessible. Note that some of these are overridden.
			attributes.putAll(((ObjectData)attrData).getAttributes());

			parsedFiles.add(file);
			break;
		
		case "XXXX":
			String attribute = ByteUtilities.nextString(data);
			
			data.get(); // always seems to be 0x00, separator byte after attr name most likely
			
			// Could put any unknown attributes in the map as a byte[], but most is just for the client which server doesn't need
			parseAttribute(attribute, data); 
			
			break;
			
		default: break;
		}
	}

	// Try and parse the attribute to map w/ appropriate Object type.
	private boolean parseAttribute(String attr, ByteBuffer data) {
		switch (attr) {
		
		case "appearanceFilename": putString(attr, data); break;
		case "arrangementDescriptorFilename": putString(attr, data); break;
		case "containerType": putInt(attr, data); break;
		case "containerVolumeLimit": putInt(attr, data); break;
		case "detailedDescription": putStfString(attr, data); break;
		case "forceNoCollision": putBoolean(attr, data); break;
		case "gender": putInt(attr, data); break;
		case "objectName": putStfString(attr, data); break;
		case "portalLayoutFilename": putString(attr, data); break;
		case "slotDescriptorFilename": putString(attr, data); break;
		case "structureFootprintFileName": putString(attr, data); break;
		case "useStructureFootprintOutline": putBoolean(attr, data); break;
		
		default: break;
		}
		
		return false;
	}
	
	private void putStfString(String attr, ByteBuffer data) {
		if (data.get() == 0)
			return;
		
		data.get(); // 0x01 
		String stfFile = ByteUtilities.nextString(data);
		if (stfFile.isEmpty())
			return;
		
		data.getShort(); // 0x00, 0x01 (Shows up as well even if stfFile prior data.get() is 0)
		
		String stfName = ByteUtilities.nextString(data);

		attributes.put(attr, stfFile + ":" + stfName);
	}
	
	private void putString(String attr, ByteBuffer data) {
		if (data.get() != 0) {
			String s = ByteUtilities.nextString(data);
			if (s.isEmpty())
				return;
			
			attributes.put(attr, s);
		}
	}
	
	private void putInt(String attr, ByteBuffer data) {
		if (data.get() != 0) { // This should always be 1 if there is an int (note that 0x20 follows after this even if it's 0)
			data.get(); // 0x20 byte for all it seems, unsure what it means
			attributes.put(attr, data.getInt());
		}
	}
	
	private void putBoolean(String attr, ByteBuffer data) {
		attributes.put(attr, (data.get() == 1 ? true : false));
	}
	
	public Object getAttribute(String attribute) {
		return attributes.get(attribute);
	}
	
	public Map<String, Object> getAttributes() {
		return attributes;
	}
}
