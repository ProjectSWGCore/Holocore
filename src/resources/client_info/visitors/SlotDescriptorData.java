package resources.client_info.visitors;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import utilities.ByteUtilities;

public class SlotDescriptorData extends ClientData {

	private List<String> slots = new ArrayList<String>();
	
	@Override
	public void handleData(String node, ByteBuffer data, int size) {
		if (!node.equals("0000DATA"))
			return;
		
		while (data.hasRemaining()) {
			slots.add(ByteUtilities.nextString(data));
			data.get();
		}
		
	}

	public List<String> getSlots() {
		return slots;
	}
}
