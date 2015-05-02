package resources.client_info.visitors;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import utilities.ByteUtilities;

public class CrcStringTableData extends ClientData {
	
	private ArrayList<Integer> crcList = new ArrayList<>();
	private ArrayList<Integer> startList = new ArrayList<>();
	private ArrayList<String> stringList = new ArrayList<>();
	private Map <Integer, String> crcMap = new HashMap<>();
	private int count;
	
	@Override
	public void handleData(String node, ByteBuffer data, int size) {
		switch (node) {
		
		case "0000DATA":
			count = data.getInt();
			break;
			
		case "CRCT":
			for(int i=0; i < count; ++i) {
				crcList.add(data.getInt());
			}
			break;
			
		case "STRT":
			for(int i=0; i < count; ++i) {
				startList.add(data.getInt());
			}
			break;
			
		case "STNG":
			for(int i=0; i < count; ++i) {
				data.position(startList.get(i));
				String str = ByteUtilities.nextString(data);
				crcMap.put(crcList.get(i), str);
				stringList.add(str);
			}
			break;
		}
		
	}

	public boolean isValidCrc(int crc) {
		
		if(!crcList.contains(crc))
			return false;
		return true;
		
	}
	
	public String getTemplateString(int crc) {
		return crcMap.get(crc);
	}
	
	public int getCrcForString(String str) {
		if (stringList.contains(str)) {
			return crcList.get(stringList.indexOf(str));
		} else {
			return 0;
		}
	}
}
