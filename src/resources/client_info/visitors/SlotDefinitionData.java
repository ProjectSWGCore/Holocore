package resources.client_info.visitors;

import java.nio.ByteBuffer;
import java.util.HashMap;

import utilities.ByteUtilities;

public class SlotDefinitionData extends ClientData {

	private HashMap<String, SlotDefinition> definitions = new HashMap<>();

	public class SlotDefinition {
		public String name;
		public boolean isGlobal;
		public boolean isModdable;
		public boolean isExclusive;
		public boolean hasHardpoint;
		public String hardpointName;
		public int unk1;
	}
	
	@Override
	public void handleData(String node, ByteBuffer data, int size) {
		while (data.hasRemaining()) { 
			SlotDefinition def = new SlotDefinition();

			def.name = ByteUtilities.nextString(data);
			def.isGlobal = (data.get() == (byte) 1);
			def.isModdable = (data.get() == (byte) 1);
			def.isExclusive = (data.get() == (byte) 1);
			def.hasHardpoint = (data.get() == (byte) 1);

			if (def.hasHardpoint) {
				data.mark();
				if (data.get() != (byte) 0) {
					data.reset();
					def.hardpointName = ByteUtilities.nextString(data);
					data.get();
				}
			} else {
				data.get();
			}

			def.unk1 = data.getInt(); // This seems to be a couple more booleans together, not sure what they would represent.

			definitions.put(def.name, def);

			//System.out.println("Added slot definition: " + def.name + (def.hardpointName != null ? " - Hardpoint name: " + def.hardpointName : ""));
		}
	}

	public SlotDefinition getDefinition(String name) {
		return definitions.get(name);
	}
}
