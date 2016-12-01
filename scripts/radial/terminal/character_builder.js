function getOptions(options, player, target) {
	options.add(new RadialOption(RadialItem.ITEM_USE));
	options.add(new RadialOption(RadialItem.EXAMINE));
}

function handleSelection(player, target, selection) {
	switch (selection) {
		case RadialItem.ITEM_USE:
			var SuiListBox = Java.type("resources.sui.SuiListBox");
			listBox = new SuiListBox(SuiButtons.OK_CANCEL, "Character Builder Terminal", "Select a category.");

			listBox.addListItem("Armour");
			listBox.addListItem("Weapons");

			listBox.addCallback("radial/terminal/character_builder", "handleCategorySelection");
			listBox.display(player);
			break;
	}
}

function handleCategorySelection(player, creature, eventType, parameters) {
	var SuiListBox = Java.type("resources.sui.SuiListBox");
	selection = SuiListBox.getSelectedRow(parameters);
	
	switch(selection) {
		case 0: handleArmour(player); break;
		case 1: handleWeapons(player); break;
	}
}

function spawnItems(player, items) {
	var StaticItemService = Java.type("services.objects.StaticItemService");
	var CreateStaticItemIntent = Java.type("intents.object.CreateStaticItemIntent");
	var creature = player.getCreatureObject();
	var inventory = creature.getSlottedObject("inventory");
	
	new CreateStaticItemIntent(creature, inventory, new StaticItemService.LootBoxHandler(creature), items).broadcast();
}

function handleArmour(player) {
	var SuiListBox = Java.type("resources.sui.SuiListBox");
	listBox = new SuiListBox(SuiButtons.OK_CANCEL, "Character Builder Terminal", "Select a set of armour to receive.");

	listBox.addListItem("Chitin Armour");
	listBox.addListItem("Padded Armour");
	listBox.addListItem("Ubese Armour");

	listBox.addCallback("radial/terminal/character_builder", "handleArmourSelection");
	listBox.display(player);
}

function handleArmourSelection(player, creature, eventType, parameters) {
	var SuiListBox = Java.type("resources.sui.SuiListBox");
	selection = SuiListBox.getSelectedRow(parameters);
	
	switch(selection) {
		case 0: handleChitinArmour(player); break;
		case 1: handlePaddedArmour(player); break;
		case 2: handleUbeseArmour(player); break;
	}
}

function handleChitinArmour(player) {
	spawnItems(player, [
		"armor_assault_sta_lvl80_bicep_l_02_01",
		"armor_assault_sta_lvl80_bicep_r_02_01",
		"armor_assault_sta_lvl80_boots_02_01",
		"armor_assault_sta_lvl80_bracer_l_02_01",
		"armor_assault_sta_lvl80_bracer_r_02_01",
		"armor_assault_sta_lvl80_chest_02_01",
		"armor_assault_sta_lvl80_gloves_02_01",
		"armor_assault_sta_lvl80_helmet_02_01",
		"armor_assault_sta_lvl80_leggings_02_01"
			]);
}

function handlePaddedArmour(player) {
	spawnItems(player, [
		"armor_battle_sta_lvl80_bicep_l_02_01",
		"armor_battle_sta_lvl80_bicep_r_02_01",
		"armor_battle_sta_lvl80_boots_02_01",
		"armor_battle_sta_lvl80_bracer_l_02_01",
		"armor_battle_sta_lvl80_bracer_r_02_01",
		"armor_battle_sta_lvl80_chest_02_01",
		"armor_battle_sta_lvl80_gloves_02_01",
		"armor_battle_sta_lvl80_helmet_02_01",
		"armor_battle_sta_lvl80_leggings_02_01"
			]);
}

function handleUbeseArmour(player) {
	spawnItems(player, [
		"armor_recon_sta_lvl80_boots_02_01",
		"armor_recon_sta_lvl80_bracer_l_02_01",
		"armor_recon_sta_lvl80_bracer_r_02_01",
		"armor_recon_sta_lvl80_chest_02_01",
		"armor_recon_sta_lvl80_gloves_02_01",
		"armor_recon_sta_lvl80_helmet_02_01",
		"armor_recon_sta_lvl80_leggings_02_01"
			]);
}

function handleWeapons(player) {
	var SuiListBox = Java.type("resources.sui.SuiListBox");
	listBox = new SuiListBox(SuiButtons.OK_CANCEL, "Character Builder Terminal", "Select a weapon category to receive a weapon of that type.");
	
	listBox.addListItem("Lightsabers");
	listBox.addListItem("Melee");
	listBox.addListItem("Ranged");

	listBox.addCallback("radial/terminal/character_builder", "handleWeaponSelection");
	listBox.display(player);
}

function handleWeaponSelection(player, creature, eventType, parameters) {
	var SuiListBox = Java.type("resources.sui.SuiListBox");
	selection = SuiListBox.getSelectedRow(parameters);
	
	switch(selection) {
		case 0: handleLightsabers(player); break;
		case 1: handleMelee(player); break;
		case 2: handleRanged(player); break;
	}
}

function handleLightsabers(player) {
	spawnItems(player, [
		"weapon_mandalorian_lightsaber_04_01"
			]);
}

function handleMelee(player) {
	spawnItems(player, [
		"weapon_tow_blasterfist_04_01",
		"weapon_tow_sword_1h_05_02",
		"weapon_tow_sword_2h_05_02",
		"weapon_tow_polearm_05_01"
			]);
}

function handleRanged(player) {
	spawnItems(player, [
		"weapon_tow_pistol_flechette_05_01",
		"weapon_tow_carbine_05_01",
		"weapon_tow_rifle_05_02",
		"weapon_tow_heavy_rocket_launcher_05_01"
			]);
}
