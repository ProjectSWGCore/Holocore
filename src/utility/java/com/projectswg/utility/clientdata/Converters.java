package com.projectswg.utility.clientdata;

import java.util.function.Supplier;

public enum Converters {
	BUILDOUT_BUILDING_LIST		(ConvertBuildingList::new),
	BUILDOUT_OBJECTS			(ConvertBuildouts::new),
	OBJECTS_OBJECT_APPEARANCE	(ConvertAppearances::new),
	OBJECTS_OBJECT_DATA			(ConvertObjectData::new),
	OBJECTS_BUILDING_CELLS		(ConvertBuildingCells::new),
	ABSTRACT_SLOT_DEFINITION	(ConvertSlotDefinition::new);
	
	private final Supplier<Converter> converter;
	
	Converters(Supplier<Converter> converter) {
		this.converter = converter;
	}
	
	public void load() {
		converter.get().convert();
	}
	
}
