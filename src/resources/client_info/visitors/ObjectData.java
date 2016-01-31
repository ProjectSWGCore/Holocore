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
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import resources.client_info.ClientFactory;
import resources.client_info.ClientData;
import resources.client_info.IffNode;
import resources.client_info.SWGFile;

public class ObjectData extends ClientData {

	private final Map<ObjectDataAttribute, Object> attributes = new HashMap<>();
	private final List<String> parsedFiles = new ArrayList<>();
	
	public enum ObjectDataAttribute {
		ACCELERATION							("acceleration"),
		ANIMATION_MAP_FILENAME					("animationMapFilename"),
		APPEARANCE_FILENAME						("appearanceFilename"),
		ARRANGEMENT_DESCRIPTOR_FILENAME			("arrangementDescriptorFilename"),
		ATTACK_TYPE								("attackType"),
		CAMERA_HEIGHT							("cameraHeight"),
		CERTIFICATIONS_REQUIRED					("certificationsRequired"),
		CLEAR_FLORA_RADIUS						("clearFloraRadius"),
		CLIENT_DATA_FILE						("clientDataFile"),
		CLIENT_VISIBILITY_FLAG					("clientVisabilityFlag"),
		COLLISION_ACTION_BLOCK_FLAGS			("collisionActionBlockFlags"),
		COLLISION_ACTION_FLAGS					("collisionActionFlags"),
		COLLISION_ACTION_PASS_FLAGS				("collisionActionPassFlags"),
		COLLISION_HEIGHT						("collisionHeight"),
		COLLISION_LENGTH						("collisionLength"),
		COLLISION_MATERIAL_BLOCK_FLAGS			("collisionMaterialBlockFlags"),
		COLLISION_MATERIAL_FLAGS				("collisionMaterialFlags"),
		COLLISION_MATERIAL_PASS_FLAGS			("collisionMaterialPassFlags"),
		COLLISION_OFFSET_X						("collisionOffsetX"),
		COLLISION_OFFSET_Z						("collisionOffsetZ"),
		COLLISION_RADIUS						("collisionRadius"),
		CONST_STRING_CUSTOMIZATION_VARIABLES	("constStringCustomizationVariables"),
		CONTAINER_TYPE							("containerType"),
		CONTAINER_VOLUME_LIMIT					("containerVolumeLimit"),
		CUSTOMIZATION_VARIABLE_MAPPING			("customizationVariableMapping"),
		DETAILED_DESCRIPTION					("detailedDescription"),
		FORCE_NO_COLLISION						("forceNoCollision"),
		GAME_OBJECT_TYPE						("gameObjectType"),
		GENDER									("gender"),
		INTERIOR_LAYOUT_FILENAME				("interiorLayoutFileName"),
		LOCATION_RESERVATION_RADIUS				("locationReservationRadius"),
		LOOK_AT_TEXT							("lookAtText"),
		MOVEMENT_DATATABLE						("movementDatatable"),
		NICHE									("niche"),
		NO_BUILD_RADIUS							("noBuildRadius"),
		OBJECT_NAME								("objectName"),
		ONLY_VISIBLE_IN_TOOLS					("onlyVisibleInTools"),
		PALETTE_COLOR_CUSTOMIZATION_VARIABLES	("paletteColorCustomizationVariables"),
		PORTAL_LAYOUT_FILENAME					("portalLayoutFilename"),
		POSTURE_ALIGN_TO_TERRAIN				("postureAlignToTerrain"),
		RACE									("race"),
		RANGED_INT_CUSTOMIZATION_VARIABLES		("rangedIntCustomizationVariables"),
		SCALE									("scale"),
		SCALE_THRESHOLD_BEFORE_EXTENT_TEST		("scaleThresholdBeforeExtentTest"),
		SEND_TO_CLIENT							("sendToClient"),
		SLOPE_MOD_ANGLE							("slopeModAngle"),
		SLOPE_MOD_PERCENT						("slopeModPercent"),
		SLOT_DESCRIPTOR_FILENAME				("slotDescriptorFilename"),
		SNAP_TO_TERRAIN							("snapToTerrain"),
		SOCKET_DESTINATIONS						("socketDestinations"),
		SPECIES									("species"),
		SPEED									("speed"),
		STEP_HEIGHT								("stepHeight"),
		STRUCTURE_FOOTPRINT_FILENAME			("structureFootprintFileName"),
		SURFACE_TYPE							("surfaceType"),
		SWIM_HEIGHT								("swimHeight"),
		TARGETABLE								("targetable"),
		TERRAIN_MODIFICATION_FILENAME			("terrainModificationFileName"),
		TINT_PALETTE							("tintPalette"),
		TURN_RADIUS								("turnRate"),
		USE_STRUCTURE_FOOTPRINT_OUTLINE			("useStructureFootprintOutline"),
		WARP_TOLERANCE							("warpTolerance"),
		WATER_MOD_PERCENT						("waterModPercent"),
		WEAPON_EFFECT							("weaponEffect"),
		WEAPON_EFFECT_INDEX						("weaponEffectIndex");
		
		private static final Map<String, ObjectDataAttribute> ATTRIBUTES = new Hashtable<>(values().length);
		
		static {
			for (ObjectDataAttribute attr : values())
				ATTRIBUTES.put(attr.getName(), attr);
		}
		
		private final String name;
		
		ObjectDataAttribute(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		
		public static ObjectDataAttribute getForName(String name) {
			return ATTRIBUTES.get(name);
		}
	}
	
	public ObjectData() {}

	@Override
	public void readIff(SWGFile iff) {
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
			else if (!tag.isEmpty())
				readNextForm(iff);
			iff.exitForm();
		}
	}
	
	private void readVersionForm(SWGFile iff) {
		IffNode attributeChunk;
		while ((attributeChunk = iff.enterChunk("XXXX")) != null) {
			parseAttributeChunk(attributeChunk);
		}
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
	}

	// Try and parse the attribute to map w/ appropriate Object type.
	private void parseAttributeChunk(IffNode chunk) {
		String str = chunk.readString();
		if (str.isEmpty())
			return;
		ObjectDataAttribute attr = ObjectDataAttribute.getForName(str);
		switch (attr) {
			case APPEARANCE_FILENAME: putString(chunk, attr); break;
			case ARRANGEMENT_DESCRIPTOR_FILENAME: putString(chunk,attr); break;
			case CLIENT_VISIBILITY_FLAG: putBoolean(chunk, attr); break;
			case CONTAINER_TYPE: putInt(chunk, attr); break;
			case CONTAINER_VOLUME_LIMIT: putInt(chunk, attr);break;
			case DETAILED_DESCRIPTION: putStfString(chunk, attr); break;
			case FORCE_NO_COLLISION: putBoolean(chunk, attr); break;
			case GENDER: putInt(chunk, attr); break;
			case OBJECT_NAME: putStfString(chunk, attr); break;
			case PORTAL_LAYOUT_FILENAME: putString(chunk, attr); break;
			case SLOT_DESCRIPTOR_FILENAME: putString(chunk, attr); break;
			case STRUCTURE_FOOTPRINT_FILENAME: putString(chunk, attr); break;
			case TARGETABLE: putBoolean(chunk, attr); break;
			case USE_STRUCTURE_FOOTPRINT_OUTLINE: putBoolean(chunk, attr); break;
			default: break;
		}
	}
	
	private void putStfString(IffNode chunk, ObjectDataAttribute attr) {
		if (chunk.readByte() == 0)
			return;

		String stfFile = getString(chunk);
		if (stfFile.isEmpty())
			return;
		attributes.put(attr, stfFile + ":" + getString(chunk));
	}
	
	private String getString(IffNode chunk) {
		chunk.readByte();
		return chunk.readString();
	}
	
	private void putString(IffNode chunk, ObjectDataAttribute attr) {
		if (chunk.readByte() == 0)
			return;
		String s = chunk.readString();
		if (s.isEmpty())
			return;
		
		attributes.put(attr, s);
	}
	
	private void putInt(IffNode chunk, ObjectDataAttribute attr) {
		if (chunk.readByte() == 0)
			return; // This should always be 1 if there is an int (note that 0x20 follows after this even if it's 0)
		chunk.readByte(); // 0x20 byte for all it seems, unsure what it means
		attributes.put(attr, chunk.readInt());
	}
	
	private void putBoolean(IffNode chunk, ObjectDataAttribute attr) {
		attributes.put(attr, (chunk.readByte() == 1));
	}
	
	public Object getAttribute(ObjectDataAttribute attribute) {
		return attributes.get(attribute);
	}
	
	public Map<ObjectDataAttribute, Object> getAttributes() {
		return attributes;
	}
}
