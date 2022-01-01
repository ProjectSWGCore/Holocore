package com.projectswg.utility;

import com.projectswg.utility.clientdata.Converters;

import java.io.IOException;

public class ClientdataConvertAll {
	
	public static void main(String [] args) throws IOException {
		Converters.OBJECTS_OBJECT_DATA.load();

		Converters.ABSTRACT_SLOT_DEFINITION.load();
		Converters.ABSTRACT_SLOT_ARRANGEMENT.load();
		Converters.ABSTRACT_SLOT_DESCRIPTORS.load();

		Converters.OBJECTS_BUILDING_CELLS.load();
		Converters.BUILDOUT_OBJECTS.load();

		Converters.PROFESSION_TEMPLATES.load();
		
		Converters.ROLES.load();
		Converters.COMMANDS_GLOBAL.load();
		Converters.COMMANDS_GROUND.load();
		Converters.COMMANDS_SPACE.load();
		Converters.BUFFS.load();
	}
	
}
