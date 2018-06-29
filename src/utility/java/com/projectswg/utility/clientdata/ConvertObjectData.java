package com.projectswg.utility.clientdata;

import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.data.swgfile.visitors.ObjectData;
import com.projectswg.common.data.swgfile.visitors.ObjectData.ObjectDataAttribute;
import com.projectswg.utility.SdbGenerator;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

class ConvertObjectData implements Converter {
	
	private static final ObjectDataAttribute[] ATTRIBUTES = Arrays.stream(ObjectDataAttribute.values()).filter(a -> a != ObjectDataAttribute.UNKNOWN).collect(Collectors.toList()).toArray(new ObjectDataAttribute[0]);
	private static final File CLIENTDATA = new File("clientdata");
	
	private final Object [] line;
	
	public ConvertObjectData() {
		this.line = new Object[ATTRIBUTES.length+1];
	}
	
	@Override
	public void convert() {
		System.out.println("Converting object data...");
		try (SdbGenerator sdb = new SdbGenerator(new File("serverdata/objects/object_data.sdb"))) {
			{
				List<String> columns = new ArrayList<>();
				columns.add("iff");
				columns.addAll(Arrays.stream(ATTRIBUTES).map(ObjectDataAttribute::getName).collect(Collectors.toList()));
				sdb.writeColumnNames(columns);
			}
			Converter.traverseFiles(this, new File(CLIENTDATA, "object"), sdb, file -> file.getName().endsWith(".iff"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void convertFile(SdbGenerator sdb, File file) throws IOException {
		ObjectData objectData = (ObjectData) ClientFactory.getInfoFromFile(file);
		if (objectData == null) {
			System.err.println("Failed to load object: " + file);
			return;
		}
		if (file.getName().equals(""))
		System.out.println(objectData.getAttribute(ObjectDataAttribute.OBJECT_NAME));
		line[0] = file.getAbsolutePath().replace(CLIENTDATA.getAbsolutePath()+'/', "");
		for (int i = 0; i < ATTRIBUTES.length; i++) {
			ObjectDataAttribute attr = ATTRIBUTES[i];
			line[i+1] = initializeDataType(attr, objectData.getAttribute(attr));
		}
		sdb.writeLine(line);
	}
	
	private static Object initializeDataType(ObjectDataAttribute attribute, Object o) {
		switch (attribute) {
			case ATTACK_TYPE:
			case COLLISION_MATERIAL_BLOCK_FLAGS:
			case COLLISION_MATERIAL_FLAGS:
			case COLLISION_MATERIAL_PASS_FLAGS:
			case COLLISION_ACTION_BLOCK_FLAGS:
			case COLLISION_ACTION_FLAGS:
			case COLLISION_ACTION_PASS_FLAGS:
			case CONTAINER_TYPE:
			case CONTAINER_VOLUME_LIMIT:
			case GAME_OBJECT_TYPE:
			case GENDER:
			case NICHE:
			case RACE:
			case SPECIES:
			case SURFACE_TYPE:
			case WEAPON_EFFECT_INDEX:
			case ACCELERATION:
			case CAMERA_HEIGHT:
			case CLEAR_FLORA_RADIUS:
			case COLLISION_HEIGHT:
			case COLLISION_LENGTH:
			case COLLISION_OFFSET_X:
			case COLLISION_OFFSET_Z:
			case COLLISION_RADIUS:
			case LOCATION_RESERVATION_RADIUS:
			case NO_BUILD_RADIUS:
			case SCALE:
			case SCALE_THRESHOLD_BEFORE_EXTENT_TEST:
			case SLOPE_MOD_ANGLE:
			case SLOPE_MOD_PERCENT:
			case SPEED:
			case STEP_HEIGHT:
			case SWIM_HEIGHT:
			case TURN_RADIUS:
			case WARP_TOLERANCE:
			case WATER_MOD_PERCENT:
				if (o == null)
					return "0";
				return o;
			case CLIENT_VISIBILITY_FLAG:
			case FORCE_NO_COLLISION:
			case HAS_WINGS:
			case ONLY_VISIBLE_IN_TOOLS:
			case PLAYER_CONTROLLED:
			case POSTURE_ALIGN_TO_TERRAIN:
			case SEND_TO_CLIENT:
			case SNAP_TO_TERRAIN:
			case TARGETABLE:
			case USE_STRUCTURE_FOOTPRINT_OUTLINE:
				if (o == null)
					return "false";
				return o;
			default:
				return o;
		}
	}
	
}
