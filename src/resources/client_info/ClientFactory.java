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
package resources.client_info;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import resources.client_info.visitors.ClientData;
import resources.client_info.visitors.CrcStringTableData;
import resources.client_info.visitors.DatatableData;
import resources.client_info.visitors.ObjectData;
import resources.client_info.visitors.ProfTemplateData;
import resources.client_info.visitors.SlotArrangementData;
import resources.client_info.visitors.SlotDefinitionData;
import resources.client_info.visitors.SlotDescriptorData;
import utilities.ByteUtilities;

public class ClientFactory {
	
	private Map <String, ClientData> dataMap = new HashMap<String, ClientData>();
	private Map <String, String> typeMap = new HashMap<String, String>();
	
	/**
	 * Creates a new instance of ClientFactory.
	 * <br>
	 * <br>
	 * In order to add parsing for an IFF type which is not yet parsed:
	 * <OL>
	 * <LI>Create a new class which extends {@link ClientData}.
	 * <LI>Perform the parsing of each node within the handleData method using switch-case statements for different node names.
	 * <LI>Add a new entry to the typeMap through populateTypeMap() method by adding in the name of the folder/node you're parsing 
	 * as the Key and the name of the class that was just created as the Value.
	 * <LI>Add in a case statement in the createDataObject method returning a new instance of the class, the case being the Value 
	 * that was entered in Step 3.
	 * </OL>
	 */
	public ClientFactory() {
		populateTypeMap();
	}

	/**
	 * Retrieves information from a client file used by SWG. Parsing of the file is done internally using {@link ClientData} which also
	 * stores the variables and is the returned type. Retrieving info from this file puts a reference of the returned 
	 * {@link ClientData} into a {@link HashMap}. Future calls for this file will try and obtain this reference if it's not null to prevent
	 * the file from being parsed multiple times.
	 * @param file The SWG file you wish to get information from which resides in the ./clientdata/ folder. 
	 * Example: creation/profession_defaults_combat_brawler.iff
	 * @param saveInfo This will save the information into the map for future calls to get information from this file.
	 * @return Specific visitor type of {@link ClientData} relating to the chosen file. For example, loading the file
	 * creation/profession_defaults_combat_brawler.iff would return an instance of {@link ProfTemplateData} extended from {@link ClientData}.
	 * A null instance of {@link ClientData} means that parsing for the type of file is not done, or a file was entered that doesn't exist on the
	 * file system.
	 */
	public synchronized ClientData getInfoFromFile(String file, boolean saveInfo) {
		ClientData data = dataMap.get(file);
		
		if (data == null) {
			data = readFile(file);
			if (data == null) {
				return null;
			}
			
			/*weak = new WeakReference<ClientData>(strong);
			
			if (weak.get() != null) {
				dataMap.put(file, weak);
			}*/
		}
		
		return data;
	}
	
	public synchronized ClientData getInfoFromFile(String file) {
		return getInfoFromFile(file, true);
	}
	
	public synchronized static String formatToSharedFile(String original) {
		if (original.contains("shared_"))
			return original;
		
		int index = original.lastIndexOf("/");
		return original.substring(0, index) + "/shared_" + original.substring(index+1);
	}
	
	// readFile only called if dataMap doesn't contain the file as a key or it's value is null
	private ClientData readFile(String file) {
		FileInputStream stream = null;
		try {
			File f = new File("./clientdata/" + file);
			if (!f.exists()) {
				f = new File("./serverdata/" + file);
				if (!f.exists())
					System.out.println(file + " not found!");
				return null;
			}
			
			stream = new FileInputStream(f);
			ByteBuffer bb = readIntoBuffer(stream);
			
			ClientData visitor = getFileType(bb);
			if (visitor == null) { // Visitor value will be null if parsing for a certain client file type doesn't exist yet.
				return null;
			}
			
			bb.position(8); // Skip first FORM, some forms only have a node and not a parent form
			parseData(bb, visitor);
			return visitor;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}
	
	private ByteBuffer readIntoBuffer(FileInputStream stream) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(stream.available()).order(ByteOrder.LITTLE_ENDIAN);
		stream.read(bb.array());
		return bb;
	}
	
	private ClientData getFileType(ByteBuffer bb) {
		bb.position(8); // Skip the first FORM and data size
		String type = ByteUtilities.nextString(bb); // Get the specific form for this IFF so we know what visitor to use

		ClientData data = createDataObject(type);

		if (data == null) {
			System.err.println("Handling for " + type + " not implemented!");
			return null;
		}
		
		return data;
	}
	
	// Any time a new ClientData is coded for parsing a file, it will need to be added in populateTypeMap() along with a new return 
	// of that instance so the file can be parsed. The type is the name of the folder/node which is then used to get the value associated
	// with it in the typeMap (value being the name of the Class preferably). If populateTypeMap() does not contain that node, then null is returned
	// and getFileType method will print out what the type is along with a "not implemented!" message.
	private ClientData createDataObject(String type) {
		String c = typeMap.get(type);
		if (c == null) {
			System.err.println("Don't know what class to use for " + type);
			return null;
		}
		
		switch (c) {
			case "CrcStringTableData": return new CrcStringTableData();
			case "DatatableData": return new DatatableData();
			case "ObjectData": return new ObjectData(this);
			case "ProfTemplateData": return new ProfTemplateData();
			case "SlotDescriptorData": return new SlotDescriptorData();
			case "SlotDefinitionData": return new SlotDefinitionData();
			case "SlotArrangementData": return new SlotArrangementData();
			
			default: return null;
		}
	}
	
	private void parseData(ByteBuffer bb, ClientData visitor) throws Exception {
		String name = null;
		while (bb.hasRemaining()) {
			name = ByteUtilities.nextString(bb);
			if (name.contains("FORM")) {
				parseFolder(name, bb, visitor);
			} else {
				parseNode(name, bb, visitor);
			}
		}
		
	}
	
	private void parseNode(String name, ByteBuffer bb, ClientData visitor) throws Exception {
		int size = Integer.reverseBytes(bb.getInt()); // Size of this node/folder
		
		if (size == 0)
			return;
		
		// Create a new buffer for parsing data specific to this node (excluding name bytes)
		visitor.handleData(name, getData(bb, size), size);
		
	}
	
	private void parseFolder(String name, ByteBuffer bb, ClientData visitor) throws Exception {
		ByteBuffer data = getData(bb, Integer.reverseBytes(bb.getInt()));
		
		// Notify visitor of the Folder? May be needed at some point.
		
		parseData(data, visitor);
	}

	// Cut down a ByteBuffer from the current position so we are only working with a certain amount of bytes. Useful when we 
	// call the ClientData.handleData method.
	private ByteBuffer getData(ByteBuffer source, int size) {
		ByteBuffer data = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
		int offset = source.position();
		data.put(source.array(), offset, size);
		source.position(offset+size);
		data.flip();
		
		return data;
	}
	
	// The typeMap is used for determining what ClientData class
	private void populateTypeMap() {

		typeMap.put("ARGDFORM", "SlotArrangementData");
		typeMap.put("0006DATA", "SlotDefinitionData");
		typeMap.put("CSTBFORM", "CrcStringTableData");
		typeMap.put("DTIIFORM", "DatatableData");
		typeMap.put("PRFIFORM", "ProfTemplateData");
		typeMap.put("SLTDFORM", "SlotDescriptorData");
		
		// Objects
		typeMap.put("SBMKFORM", "ObjectData"); // object/battlefield_marker
		typeMap.put("SBOTFORM", "ObjectData"); // object/building
		typeMap.put("CCLTFORM", "ObjectData"); // object/cell
		typeMap.put("SCNCFORM", "ObjectData"); // object/construction_contract
		typeMap.put("SCOUFORM", "ObjectData"); // object/counting
		typeMap.put("SCOTFORM", "ObjectData"); // object/creature && object/mobile
		typeMap.put("SDSCFORM", "ObjectData"); // object/draft_schematic
		typeMap.put("SFOTFORM", "ObjectData"); // object/factory
		typeMap.put("SGRPFORM", "ObjectData"); // object/group
		typeMap.put("SGLDFORM", "ObjectData"); // object/guild
		typeMap.put("SIOTFORM", "ObjectData"); // object/installation
		typeMap.put("SITNFORM", "ObjectData"); // object/intangible
		typeMap.put("SJEDFORM", "ObjectData"); // object/jedi_manager
		typeMap.put("SMSCFORM", "ObjectData"); // object/manufacture_schematic
		typeMap.put("SMSOFORM", "ObjectData"); // object/mission
		typeMap.put("SHOTFORM", "ObjectData"); // object/object
		typeMap.put("STOTFORM", "ObjectData"); // object/path_waypoint && object/tangible
		typeMap.put("SPLYFORM", "ObjectData"); // object/player
		typeMap.put("SPQOFORM", "ObjectData"); // object/player_quest
		typeMap.put("RCCTFORM", "ObjectData"); // object/resource_container
		typeMap.put("SSHPFORM", "ObjectData"); // object/ship
		typeMap.put("STATFORM", "ObjectData"); // object/soundobject && object/static
		typeMap.put("STOKFORM", "ObjectData"); // object/token
		typeMap.put("SUNIFORM", "ObjectData"); // object/universe
		typeMap.put("SWAYFORM", "ObjectData"); // object/waypoint
		typeMap.put("SWOTFORM", "ObjectData"); // object/weapon
		//

	}

}
