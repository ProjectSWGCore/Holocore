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
	ROLES						(() -> new ConvertDatatable("datatables/role/role.iff", "serverdata/player/role.sdb", false)),
	COMMANDS_GLOBAL				(() -> new ConvertDatatable("datatables/command/command_table.iff", "serverdata/command/commands_global.sdb", false)),
	COMMANDS_GROUND				(() -> new ConvertDatatable("datatables/command/command_table_ground.iff", "serverdata/command/commands_ground.sdb", false)),
	COMMANDS_SPACE				(() -> new ConvertDatatable("datatables/command/command_table_space.iff", "serverdata/command/commands_space.sdb", false)),
	BUFFS						(() -> new ConvertDatatable("datatables/buff/buff.iff", "serverdata/buff/buff.sdb", true)),
	SKILLS						(() -> new ConvertDatatable("datatables/skill/skills.iff", "serverdata/skill/skills.sdb", true)),
	PROFESSION_TEMPLATES		(ConvertProfessionTemplates::new),
	APPEARANCE_TABLE			(() -> new ConvertDatatable("datatables/appearance/appearance_table.iff", "serverdata/appearance/appearance_table.sdb", true));
	
	private final Supplier<Converter> converter;
	
	Converters(Supplier<Converter> converter) {
		this.converter = converter;
	}
	
	public void load() {
		converter.get().convert();
	}
	
}
