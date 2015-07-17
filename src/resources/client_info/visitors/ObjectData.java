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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import resources.client_info.ClientFactory;
import resources.client_info.ClientData;
import resources.client_info.IffNode;
import resources.client_info.SWGFile;

public class ObjectData extends ClientData {

	private Map<String, Object> attributes = new HashMap<>();
	private List<String> parsedFiles = new ArrayList<>();
	
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
	public void readIff(SWGFile iff) {
		while(iff.enterNextForm() != null)
			readNextForm(iff);
	}

	private void readNextForm(SWGFile iff) {
		IffNode next;
		while ((next = iff.enterNextForm()) != null) {
			String tag = next.getTag();
			if (tag.equals("DERV"))
				readExtendedAttributes(iff);
			else if (tag.contains("0"))
				readVersionForm(iff);
			else if (!tag.isEmpty()) // More than likely to be a template form
				readNextForm(iff);
		}

		iff.exitForm();
	}

	private void readVersionForm(SWGFile iff) {
		IffNode attributeChunk;
		while ((attributeChunk = iff.enterChunk("XXXX")) != null) {
			parseAttributeChunk(attributeChunk);
		}

		iff.exitForm();
	}

	private void readExtendedAttributes(SWGFile iff) {
		IffNode chunk = iff.enterNextChunk();
		String file = chunk.readString();

		if (parsedFiles.contains(file)) // some repeated and we do not want to replace any attributes unless they're overriden by a more specific obj
			return;

		ClientData attrData = ClientFactory.getInfoFromFile(file, true);
		if (attrData == null || !(attrData instanceof ObjectData)) {
			System.out.println("Could not load attribute data from file " + file + "!");
			return; // break out of whole method as we should only continue if we have all the extended attributes
		}

		// Put all the extended attributes in this map so it's accessible. Note that some of these are overridden.
		attributes.putAll(((ObjectData)attrData).getAttributes());

		parsedFiles.add(file);

		iff.exitForm();
	}

	// Try and parse the attribute to map w/ appropriate Object type.
	private void parseAttributeChunk(IffNode chunk) {
		String attr = chunk.readString();
		switch (attr) {
		case "appearanceFilename": putString(chunk, attr); break;
		case "arrangementDescriptorFilename": putString(chunk,attr); break;
		case "containerType": putInt(chunk, attr); break;
		case "containerVolumeLimit": putInt(chunk, attr);break;
		case "detailedDescription": putStfString(chunk, attr); break;
		case "forceNoCollision": putBoolean(chunk, attr); break;
		case "gender": putInt(chunk, attr); break;
		case "objectName": putStfString(chunk, attr); break;
		case "portalLayoutFilename": putString(chunk, attr); break;
		case "slotDescriptorFilename": putString(chunk, attr); break;
		case "structureFootprintFileName": putString(chunk, attr); break;
		case "useStructureFootprintOutline": putBoolean(chunk, attr); break;
		default:/*System.out.println("Unknown attribute: " + attr);*/ break;
		}
	}
	
	private void putStfString(IffNode chunk, String attr) {
		if (chunk.readByte() == 0)
			return;

		String stfFile = chunk.readString();
		if (stfFile.isEmpty())
			return;
		
		chunk.readShort(); // 0x00, 0x01 (Shows up as well even if stfFile prior data.get() is 0)
		
		String stfName = chunk.readString();

		attributes.put(attr, stfFile + ":" + stfName);
	}
	
	private void putString(IffNode chunk, String attr) {
		if (chunk.readByte() != 0) {
			String s = chunk.readString();
			if (s.isEmpty())
				return;
			
			attributes.put(attr, s);
		}
	}
	
	private void putInt(IffNode chunk, String attr) {
		if (chunk.readByte() != 0) { // This should always be 1 if there is an int (note that 0x20 follows after this even if it's 0)
			chunk.readByte(); // 0x20 byte for all it seems, unsure what it means
			attributes.put(attr, chunk.readInt());
		}
	}
	
	private void putBoolean(IffNode chunk, String attr) {
		attributes.put(attr, (chunk.readByte() == 1));
	}
	
	public Object getAttribute(String attribute) {
		return attributes.get(attribute);
	}
	
	public Map<String, Object> getAttributes() {
		return attributes;
	}
}
