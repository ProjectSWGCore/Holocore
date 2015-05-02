package resources.client_info.visitors;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import utilities.ByteUtilities;

public class SlotArrangementData extends ClientData {

	private List <List <String>> occupiedSlots = new ArrayList<List<String>>();
	
	@Override
	public void handleData(String node, ByteBuffer data, int size) {
		
		ArrayList<String> slots = new ArrayList<String>();
		
		while(data.hasRemaining()) {
			slots.add(ByteUtilities.nextString(data));
			data.get();
		}

		occupiedSlots.add(slots);
	}
	
	public List <List<String>> getArrangement() {
		return occupiedSlots;
	}
}
