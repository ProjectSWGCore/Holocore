package com.projectswg.utility.clientdata;

import java.util.function.Supplier;

public enum Converters {
	BUILDOUT_BUILDING_LIST		(ConvertBuildingList::new),
	BUILDOUT_OBJECTS			(ConvertBuildouts::new),
	OBJECTS_OBJECT_APPEARANCE	(ConvertAppearances::new),
	OBJECTS_OBJECT_DATA			(ConvertObjectData::new),
	OBJECTS_BUILDING_CELLS		(ConvertBuildingCells::new),
	ABSTRACT_SLOT_DEFINITION	(ConvertSlotDefinition::new),
	ABSTRACT_SLOT_DESCRIPTORS	(ConvertSlotDescriptor::new),
	ABSTRACT_SLOT_ARRANGEMENT	(ConvertSlotArrangement::new),
	TERRAINS					(ConvertTerrain::new),
	ROLES						(() -> new ConvertDatatable("datatables/role/role.iff", "serverdata/nge/player/role.sdb", false)),
	COMMANDS_GLOBAL				(() -> new ConvertDatatable("datatables/command/command_table.iff", "serverdata/nge/command/commands_global.sdb", false)),
	COMMANDS_GROUND				(() -> new ConvertDatatable("datatables/command/command_table_ground.iff", "serverdata/nge/command/commands_ground.sdb", false)),
	COMMANDS_SPACE				(() -> new ConvertDatatable("datatables/command/command_table_space.iff", "serverdata/nge/command/commands_space.sdb", false)),
	BUFFS						(() -> new ConvertDatatable("datatables/buff/buff.iff", "serverdata/nge/buff/buff.sdb", true)),
	PROFESSION_TEMPLATES		(ConvertProfessionTemplates::new);
	
	private final Supplier<Converter> converter;
	
	Converters(Supplier<Converter> converter) {
		this.converter = converter;
	}
	
	public void load() {
		converter.get().convert();
	}
	
}
