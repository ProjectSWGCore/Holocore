package resources.client_info.visitors;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import resources.client_info.ClientFactory;
import utilities.ByteUtilities;

public class ObjectData extends ClientData {

	private Map<String, Object> attributes = new HashMap<String, Object>();
	private ClientFactory factory;
	private List<String> parsedFiles = new ArrayList<String>();
	
	public static final String APPEARANCE_FILE = "appearanceFilename";
	public static final String ARRANGEMENT_FILE = "arrangementDescriptorFilename";
	public static final String VOLUME_LIMIT = "containerVolumeLimit";
	public static final String DETAIL_STF = "detailedDescription";
	public static final String OBJ_STF = "objectName";
	public static final String PORTAL_LAYOUT = "portalLayoutFilename";
	public static final String SLOT_DESCRIPTOR = "slotDescriptorFilename";

	public ObjectData(ClientFactory factory) {
		this.factory = factory;
	}
	
	@Override
	public void handleData(String node, ByteBuffer data, int size) {
		switch(node) {
		
		case "DERVXXXX": // Extended attributes
			String file = ByteUtilities.nextString(data);
			
			if (parsedFiles.contains(file)) // some DERVXXX were repeated and we do not want to replace any attributes unless they're overriden by a more specific obj
				break;
			
			ClientData attrData = factory.getInfoFromFile(file);
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
