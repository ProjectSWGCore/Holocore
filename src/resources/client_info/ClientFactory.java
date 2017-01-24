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

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

import resources.client_info.visitors.CrcStringTableData;
import resources.client_info.visitors.DatatableData;
import resources.client_info.visitors.ObjectData;
import resources.client_info.visitors.PortalLayoutData;
import resources.client_info.visitors.ProfTemplateData;
import resources.client_info.visitors.SlotArrangementData;
import resources.client_info.visitors.SlotDefinitionData;
import resources.client_info.visitors.SlotDescriptorData;
import resources.client_info.visitors.WorldSnapshotData;
import resources.client_info.visitors.appearance.AppearanceTemplateData;
import resources.client_info.visitors.appearance.AppearanceTemplateList;
import resources.client_info.visitors.appearance.BasicSkeletonTemplate;
import resources.client_info.visitors.appearance.DetailedAppearanceTemplateData;
import resources.client_info.visitors.appearance.LodMeshGeneratorTemplateData;
import resources.client_info.visitors.appearance.LodSkeletonTemplateData;
import resources.client_info.visitors.appearance.MeshAppearanceTemplate;
import resources.client_info.visitors.appearance.SkeletalAppearanceData;
import resources.client_info.visitors.appearance.SkeletalMeshGeneratorTemplateData;
import resources.client_info.visitors.shader.CustomizableShaderData;
import resources.client_info.visitors.shader.StaticShaderData;
import resources.server_info.Log;

public class ClientFactory extends DataFactory {
	
	private static final ClientFactory INSTANCE = new ClientFactory();

	private Map <String, SoftReference<ClientData>> dataMap = new HashMap<>();
	private Map <String, String> typeMap = new HashMap<>();
	
	/**
	 * Creates a new instance of ClientFactory.
	 * <br>
	 * <br>
	 * In order to add parsing for an IFF type which is not yet parsed:
	 * <OL>
	 * <LI>Create a new class which extends {@link ClientData}.
	 * <LI>Perform the parsing of each node within the parse method using switch-case statements for different node names.
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
	 * the file from being parsed multiple times if the save variable is true.
	 * @param file The SWG file you wish to get information from which resides in the ./clientdata/ folder.
	 * Example: creation/profession_defaults_combat_brawler.iff
	 * @return Specific visitor type of {@link ClientData} relating to the chosen file. For example, loading the file
	 * creation/profession_defaults_combat_brawler.iff would return an instance of {@link ProfTemplateData} extended from {@link ClientData}.
	 * A null instance of {@link ClientData} means that parsing for the type of file is not done, or a file was entered that doesn't exist on the
	 * file system.
	 * @param save Future calls for this file will try and obtain this reference if it's not null to prevent the file from being parsed multiple times
	 */
	public synchronized static ClientData getInfoFromFile(String file, boolean save) {
		ClientFactory factory = ClientFactory.getInstance();
		SoftReference<ClientData> reference = factory.dataMap.get(file);
		ClientData data = null;
		if (reference != null)
			data = reference.get();
		
		if (data == null) {
			data = factory.readFile(file);
			if (data == null) {
				return null;
			}
			reference = new SoftReference<ClientData>(data);
			
			// Soft used over Weak because Weak cleared as soon as the reference was not longer needed, Soft will be cleared when memory is needed by the JVM.
			if (save) {
				factory.dataMap.put(file, reference);
			}
		}
		
		return data;
	}

	/**
	 * Retrieves information from a client file used by SWG. Parsing of the file is done internally using {@link ClientData} which also
	 * stores the variables and is the returned type.
	 * @param file The SWG file you wish to get information from which resides in the ./clientdata/ folder.
	 * Example: creation/profession_defaults_combat_brawler.iff
	 * @return Specific visitor type of {@link ClientData} relating to the chosen file. For example, loading the file
	 * creation/profession_defaults_combat_brawler.iff would return an instance of {@link ProfTemplateData} extended from {@link ClientData}.
	 * A null instance of {@link ClientData} means that parsing for the type of file is not done, or a file was entered that doesn't exist on the
	 * file system.
	 */
	public synchronized static ClientData getInfoFromFile(String file) {
		return getInfoFromFile(file, false);
	}

	public synchronized static String formatToSharedFile(String original) {
		if (original.contains("shared_"))
			return original;
		
		int index = original.lastIndexOf('/');
		return original.substring(0, index) + "/shared_" + original.substring(index+1);
	}

	// Any time a new DataObject is coded for parsing a file, it will need to be added in populateTypeMap() along with a new return 
	// of that instance so the file can be parsed. The type is the name of the folder/node which is then used to get the value associated
	// with it in the typeMap (value being the name of the Class preferably). If populateTypeMap() does not contain that node, then null is returned
	// and getFileType method will print out what the type is along with a "not implemented!" message.
	@Override
	protected ClientData createDataObject(String type) {
		String c = typeMap.get(type);
		if (c == null) {
			Log.e("ClientFactory", "Don't know what class to use for " + type);
			return null;
		}
		
		switch (c) {
			case "": return null; // Disabled clientdata
			case "AppearanceTemplateData": return new AppearanceTemplateData();
			case "AppearanceTemplateList": return new AppearanceTemplateList();
			case "BasicSkeletonTemplate": return new BasicSkeletonTemplate();
			case "CrcStringTableData": return new CrcStringTableData();
			case "CustomizableShaderData": return new CustomizableShaderData();
			case "DatatableData": return new DatatableData();
			case "DetailedAppearanceTemplateData": return new DetailedAppearanceTemplateData();
			case "LodMeshGeneratorTemplateData": return new LodMeshGeneratorTemplateData();
			case "LodSkeletonTemplateData": return new LodSkeletonTemplateData();
			case "MeshAppearanceTemplate": return new MeshAppearanceTemplate();
			case "ObjectData": return new ObjectData();
			case "PortalLayoutData": return new PortalLayoutData();
			case "ProfTemplateData": return new ProfTemplateData();
			case "SlotDescriptorData": return new SlotDescriptorData();
			case "SlotDefinitionData": return new SlotDefinitionData();
			case "SlotArrangementData": return new SlotArrangementData();
			case "SkeletalAppearanceData": return new SkeletalAppearanceData();
			case "SkeletalMeshGeneratorTemplateData": return new SkeletalMeshGeneratorTemplateData();
			case "StaticShaderData": return new StaticShaderData();
			case "WorldSnapshotData": return new WorldSnapshotData();
			default: Log.e("ClientFactory", "Unimplemented typeMap value: " + c); return null;
		}
	}

	// The typeMap is used for determining what DataObject class
	private void populateTypeMap() {
		typeMap.put("CSTB", "CrcStringTableData");
		typeMap.put("DTII", "DatatableData");
		typeMap.put("PRTO", "PortalLayoutData");
		typeMap.put("PRFI", "ProfTemplateData");
		typeMap.put("ARGD", "SlotArrangementData");
		typeMap.put("0006", "SlotDefinitionData");
		typeMap.put("SLTD", "SlotDescriptorData");
		typeMap.put("WSNP", "WorldSnapshotData");
		// Appearance Related Data
		boolean loadAppearanceData = false;
		typeMap.put("APPR", !loadAppearanceData?"":"AppearanceTemplateData");
		typeMap.put("APT ", !loadAppearanceData?"":"AppearanceTemplateList");
		typeMap.put("CSHD", !loadAppearanceData?"":"CustomizableShaderData");
		typeMap.put("DTLA", !loadAppearanceData?"":"DetailedAppearanceTemplateData");
		typeMap.put("SKTM", !loadAppearanceData?"":"BasicSkeletonTemplate");
		typeMap.put("MESH", !loadAppearanceData?"":"MeshAppearanceTemplate");
		typeMap.put("MLOD", !loadAppearanceData?"":"LodMeshGeneratorTemplateData");
		typeMap.put("SLOD", !loadAppearanceData?"":"LodSkeletonTemplateData");
		typeMap.put("SMAT", !loadAppearanceData?"":"SkeletalAppearanceData");
		typeMap.put("SKMG", !loadAppearanceData?"":"SkeletalMeshGeneratorTemplateData");
		typeMap.put("SSHT", !loadAppearanceData?"":"StaticShaderData");
		// Objects
		typeMap.put("SBMK", "ObjectData"); // object/battlefield_marker
		typeMap.put("SBOT", "ObjectData"); // object/building
		typeMap.put("CCLT", "ObjectData"); // object/cell
		typeMap.put("SCNC", "ObjectData"); // object/construction_contract
		typeMap.put("SCOU", "ObjectData"); // object/counting
		typeMap.put("SCOT", "ObjectData"); // object/creature && object/mobile
		typeMap.put("SDSC", "ObjectData"); // object/draft_schematic
		typeMap.put("SFOT", "ObjectData"); // object/factory
		typeMap.put("SGRP", "ObjectData"); // object/group
		typeMap.put("SGLD", "ObjectData"); // object/guild
		typeMap.put("SIOT", "ObjectData"); // object/installation
		typeMap.put("SITN", "ObjectData"); // object/intangible
		typeMap.put("SJED", "ObjectData"); // object/jedi_manager
		typeMap.put("SMSC", "ObjectData"); // object/manufacture_schematic
		typeMap.put("SMSO", "ObjectData"); // object/mission
		typeMap.put("SHOT", "ObjectData"); // object/object
		typeMap.put("STOT", "ObjectData"); // object/path_waypoint && object/tangible
		typeMap.put("SPLY", "ObjectData"); // object/player
		typeMap.put("SPQO", "ObjectData"); // object/player_quest
		typeMap.put("RCCT", "ObjectData"); // object/resource_container
		typeMap.put("SSHP", "ObjectData"); // object/ship
		typeMap.put("STAT", "ObjectData"); // object/soundobject && object/static
		typeMap.put("STOK", "ObjectData"); // object/token
		typeMap.put("SUNI", "ObjectData"); // object/universe
		typeMap.put("SWAY", "ObjectData"); // object/waypoint
		typeMap.put("SWOT", "ObjectData"); // object/weapon
		//
	}

	@Override
	protected String getFolder() {
		return "./clientdata/";
	}

	private static ClientFactory getInstance() {
		return INSTANCE;
	}
}
