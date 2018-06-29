package com.projectswg.utility.clientdata;

import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.data.swgfile.visitors.SlotDefinitionData;
import com.projectswg.common.data.swgfile.visitors.SlotDefinitionData.SlotDefinition;
import com.projectswg.utility.SdbGenerator;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

class ConvertSlotDefinition implements Converter {
	
	@Override
	public void convert() {
		System.out.println("Converting slot definitions...");
		try (SdbGenerator sdb = new SdbGenerator(new File("serverdata/abstract/slot_definitions.sdb"))) {
			sdb.writeColumnNames("slotName", "global", "modifiable", "observeWithParent", "exposeToWorld");
			convertFile(sdb, new File("clientdata/abstract/slot/slot_definition/slot_definitions.iff"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void convertFile(SdbGenerator sdb, File file) throws IOException {
		SlotDefinitionData clientdata = (SlotDefinitionData) ClientFactory.getInfoFromFile(file);
		Objects.requireNonNull(clientdata, "Failed to load clientdata");
		
		for (SlotDefinition sd : clientdata.getDefinitions().values()) {
			sdb.writeLine(sd.getName(), sd.isGlobal(), sd.isModifiable(), sd.isObserveWithParent(), sd.isExposeToWorld());
		}
	}
}
