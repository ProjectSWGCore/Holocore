function getOptions(options, player, target) {
	options.add(new RadialOption(RadialItem.ITEM_USE));
	options.add(new RadialOption(RadialItem.EXAMINE));
}

function handleSelection(player, target, selection) {
	switch (selection) {
		case RadialItem.ITEM_USE:
			var SuiListBox = Java.type("resources.sui.SuiListBox");
			listBox = new SuiListBox(SuiButtons.OK_CANCEL, "Character Builder Terminal", "Select a category.");

			listBox.addListItem("Armor");
			listBox.addListItem("Weapons");
			listBox.addListItem("Wearables");
			listBox.addListItem("Travel");

			listBox.addCallback("radial/terminal/character_builder", "handleCategorySelection");
			listBox.display(player);
			break;
	}
}

function handleCategorySelection(player, creature, eventType, parameters) {
	var SuiEvent = Java.type("resources.sui.SuiEvent");
	
	if (eventType != SuiEvent.OK_PRESSED) {
		return;
	}
	
	var SuiListBox = Java.type("resources.sui.SuiListBox");
	selection = SuiListBox.getSelectedRow(parameters);
	
	switch(selection) {
		case 0: handleArmor(player); break;
		case 1: handleWeapons(player); break;
		case 2: handleWearables(player); break;
		case 3: handleTravel(player); break;
	}
}

function spawnItems(player, items) {
	var StaticItemService = Java.type("services.objects.StaticItemService");
	var CreateStaticItemIntent = Java.type("intents.object.CreateStaticItemIntent");
	var creature = player.getCreatureObject();
	var inventory = creature.getSlottedObject("inventory");
	
	new CreateStaticItemIntent(creature, inventory, new StaticItemService.LootBoxHandler(creature), items).broadcast();
}

function handleArmor(player) {
	var SuiListBox = Java.type("resources.sui.SuiListBox");
	listBox = new SuiListBox(SuiButtons.OK_CANCEL, "Character Builder Terminal", "Select a set of armor to receive.");

	listBox.addListItem("R.I.S. Armor");
	listBox.addListItem("Mandalorian Armor");
	listBox.addListItem("Bone Armor");
	listBox.addListItem("Chitin Armor");
	listBox.addListItem("Padded Armor");
	listBox.addListItem("Ubese Armor");
	listBox.addListItem("Ithorian Defender Armor");
	listBox.addListItem("Wookiee Black Mountain Armor");
	listBox.addListItem("Deathtrooper Armor");
	listBox.addListItem("Imperial - Galactic Marine Armor");
	listBox.addListItem("Imperial - Scout trooper Armor");
	listBox.addListItem("Imperial - Shock trooper Armor");
	listBox.addListItem("Imperial - Stormtrooper Armor");
	listBox.addListItem("Imperial - Black Crusader Armor");
	listBox.addListItem("Imperial - Black Spec Ops Armor");
	listBox.addListItem("Imperial - White Spec Ops Armor");
	listBox.addListItem("Imperial - Snow trooper Armor");
	listBox.addListItem("Imperial - Forest Camouflage Armor");
	listBox.addListItem("Rebel - Forest Camouflage Armor");
	listBox.addListItem("Rebel - Assault Armor");
	listBox.addListItem("Rebel - Battle Armor");
	listBox.addListItem("Rebel - Marine Armor");
	listBox.addListItem("Rebel - Spec Force Armor");
	listBox.addListItem("Rebel - Black Crusader Armor");
	listBox.addListItem("Rebel - Alliance Weather Cold Armor");
	listBox.addCallback("radial/terminal/character_builder", "handleArmorSelection");
	listBox.display(player);
}

function handleArmorSelection(player, creature, eventType, parameters) {
	var SuiEvent = Java.type("resources.sui.SuiEvent");
	
	if (eventType != SuiEvent.OK_PRESSED) {
		return;
	}
	
	var SuiListBox = Java.type("resources.sui.SuiListBox");
	selection = SuiListBox.getSelectedRow(parameters);
	
	switch(selection) {
		case 0: handleRisArmor(player); break;
		case 1: handleMandoArmor(player); break;
		case 2: handleBoneArmor(player); break;
		case 3: handleChitinArmor(player); break;
		case 4: handlePaddedArmor(player); break;
		case 5: handleUbeseArmor(player); break;	
		case 6: handleIthoriandefenderArmor(player); break;
		case 7: handleWookieeblackmtnArmor(player); break;
		case 8: handleDeathtrooperArmor(player); break;
		case 9: handleImpBattlemarineArmor(player); break;
		case 10: handleImpBattlewornscoutArmor(player); break;
		case 11: handleImpBattlewornshockArmor(player); break;
		case 12: handleImpBattlewornstormArmor(player); break;
		case 13: handleImpBlackcrusaderArmor(player); break;
		case 14: handleImpBlackpvpspecopsArmor(player); break;
		case 15: handleImpWhitepvpspecopsArmor(player); break;
		case 16: handleImpSnowtrooperArmor(player); break;	
		case 17: handleImpForestCamoArmor(player); break;
		case 18: handleRebForestCamoArmor(player); break;
		case 19: handleRebBattlewornassaultArmor(player); break;
		case 20: handleRebBattlewornbattleArmor(player); break;
		case 21: handleRebBattlewornmarineArmor(player); break;
		case 22: handleRebBattlewornspecforceArmor(player); break;
		case 23: handleRebBlackcrusaderArmor(player); break;
		case 24: handleRebAlliancecoldArmor(player); break;
	}
}

function handleRisArmor(player) {
	spawnItems(player, [
		"armor_ris_bicep_l",
		"armor_ris_bicep_r",
		"armor_ris_boots",
		"armor_ris_bracer_l",
		"armor_ris_bracer_r",
		"armor_ris_chest_plate",
		"armor_ris_gloves",
		"armor_ris_helmet",
		"armor_ris_leggings"
			]);
}

function handleMandoArmor(player) {
	spawnItems(player, [
		"armor_mandalorian_bicep_l",
		"armor_mandalorian_bicep_r",
		"armor_mandalorian_bracer_l",
		"armor_mandalorian_bracer_r",
		"armor_mandalorian_chest_plate",
		"armor_mandalorian_gloves",
		"armor_mandalorian_helmet",
		"armor_mandalorian_leggings",
		"armor_mandalorian_shoes"
			]);
}

function handleBoneArmor(player) {
	spawnItems(player, [
		"armor_bone_bicep_l_02_01",
		"armor_bone_bicep_r_02_01",
		"armor_bone_boots_02_01",
		"armor_bone_bracer_l_02_01",
		"armor_bone_bracer_r_02_01",
		"armor_bone_chest_02_01",
		"armor_bone_gloves_02_01",
		"armor_bone_helmet_02_01",
		"armor_bone_leggings_02_01"
			]);
}

function handleChitinArmor(player) {
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

function handlePaddedArmor(player) {
	spawnItems(player, [
		"armor_tow_battle_bicep_l_03_01",
		"armor_tow_battle_bicep_r_03_01",
		"armor_tow_battle_boots_03_01",
		"armor_tow_battle_bracer_l_03_01",
		"armor_tow_battle_bracer_r_03_01",
		"armor_tow_battle_chest_03_01",
		"armor_tow_battle_gloves_03_01",
		"armor_tow_battle_helmet_03_01",
		"armor_tow_battle_leggings_03_01"
			]);
}

function handleUbeseArmor(player) {
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

function handleIthoriandefenderArmor(player) {
	spawnItems(player, [
		"armor_ithorian_defender_bicep_camo_l_04_01",
		"armor_ithorian_defender_bicep_camo_r_04_01",
		"armor_ithorian_defender_boots_camo_04_01",
		"armor_ithorian_defender_bracer_camo_l_04_01",
		"armor_ithorian_defender_bracer_camo_r_04_01",
		"armor_ithorian_defender_chest_plate_camo_04_01",
		"armor_ithorian_defender_gloves_camo_04_01",
		"armor_ithorian_defender_helmet_camo_04_01",
		"armor_ithorian_defender_leggings_camo_04_01"
			]);
}

function handleWookieeblackmtnArmor(player) {
	spawnItems(player, [
		"armor_kashyyykian_black_mtn_bicep_camo_l_04_01",
		"armor_kashyyykian_black_mtn_bicep_camo_r_04_01",
		"armor_kashyyykian_black_mtn_bracer_camo_l_04_01",
		"armor_kashyyykian_black_mtn_bracer_camo_r_04_01",
		"armor_kashyyykian_black_mtn_chest_plate_camo_04_01",
		"armor_kashyyykian_black_mtn_leggings_camo_04_01",
			]);
}

function handleDeathtrooperArmor(player) {
	spawnItems(player, [
		"armor_deathtrooper_bicep_l_04_01",
		"armor_deathtrooper_bicep_r_04_01",
		"armor_deathtrooper_boots_04_01",
		"armor_deathtrooper_bracer_l_04_01",
		"armor_deathtrooper_bracer_r_04_01",
		"armor_deathtrooper_chest_plate_04_01",
		"armor_deathtrooper_gloves_04_01",
		"armor_deathtrooper_helmet_04_01",
		"armor_deathtrooper_leggings_04_01"
			]);
}

function handleImpBattlemarineArmor(player) {
	spawnItems(player, [
		"armor_gcw_imperial_galactic_marine_bicep_l",
		"armor_gcw_imperial_galactic_marine_bicep_r",
		"armor_gcw_imperial_galactic_marine_boots",
		"armor_gcw_imperial_galactic_marine_bracer_l",
		"armor_gcw_imperial_galactic_marine_bracer_r",
		"armor_gcw_imperial_galactic_marine_chest_plate",
		"armor_gcw_imperial_galactic_marine_gloves",
		"armor_gcw_imperial_galactic_marine_helmet",
		"armor_gcw_imperial_galactic_marine_leggings"
			]);
}

function handleImpBattlewornscoutArmor(player) {
	spawnItems(player, [
		"armor_gcw_imperial_scouttrooper_bicep_l",
		"armor_gcw_imperial_scouttrooper_bicep_r",
		"armor_gcw_imperial_scouttrooper_boots",
		"armor_gcw_imperial_scouttrooper_bracer_l",
		"armor_gcw_imperial_scouttrooper_bracer_r",
		"armor_gcw_imperial_scouttrooper_chest_plate",
		"armor_gcw_imperial_scouttrooper_gloves",
		"armor_gcw_imperial_scouttrooper_helmet",
		"armor_gcw_imperial_scouttrooper_leggings"
			]);
}

function handleImpBattlewornshockArmor(player) {
	spawnItems(player, [
		"armor_gcw_imperial_shocktrooper_bicep_l",
		"armor_gcw_imperial_shocktrooper_bicep_r",
		"armor_gcw_imperial_shocktrooper_boots",
		"armor_gcw_imperial_shocktrooper_bracer_l",
		"armor_gcw_imperial_shocktrooper_bracer_r",
		"armor_gcw_imperial_shocktrooper_chest_plate",
		"armor_gcw_imperial_shocktrooper_gloves",
		"armor_gcw_imperial_shocktrooper_helmet",
		"armor_gcw_imperial_shocktrooper_leggings"
			]);
}

function handleImpBattlewornstormArmor(player) {
	spawnItems(player, [
		"armor_gcw_imperial_stormtrooper_bicep_l",
		"armor_gcw_imperial_stormtrooper_bicep_r",
		"armor_gcw_imperial_stormtrooper_boots",
		"armor_gcw_imperial_stormtrooper_bracer_l",
		"armor_gcw_imperial_stormtrooper_bracer_r",
		"armor_gcw_imperial_stormtrooper_chest_plate",
		"armor_gcw_imperial_stormtrooper_gloves",
		"armor_gcw_imperial_stormtrooper_helmet",
		"armor_gcw_imperial_stormtrooper_leggings"
			]);
}

function handleRebBattlewornassaultArmor(player) {
	spawnItems(player, [
		"armor_gcw_rebel_assault_bicep_l",
		"armor_gcw_rebel_assault_bicep_r",
		"armor_gcw_rebel_assault_boots",
		"armor_gcw_rebel_assault_bracer_l",
		"armor_gcw_rebel_assault_bracer_r",
		"armor_gcw_rebel_assault_chest_plate",
		"armor_gcw_rebel_assault_gloves",
		"armor_gcw_rebel_assault_helmet",
		"armor_gcw_rebel_assault_leggings"
			]);
}

function handleRebBattlewornbattleArmor(player) {
	spawnItems(player, [
		"armor_gcw_rebel_battle_bicep_l",
		"armor_gcw_rebel_battle_bicep_r",
		"armor_gcw_rebel_battle_boots",
		"armor_gcw_rebel_battle_bracer_l",
		"armor_gcw_rebel_battle_bracer_r",
		"armor_gcw_rebel_battle_chest_plate",
		"armor_gcw_rebel_battle_gloves",
		"armor_gcw_rebel_battle_helmet",
		"armor_gcw_rebel_battle_leggings"
			]);
}

function handleRebBattlewornmarineArmor(player) {
	spawnItems(player, [
		"armor_gcw_rebel_marine_bicep_l",
		"armor_gcw_rebel_marine_bicep_r",
		"armor_gcw_rebel_marine_boots",
		"armor_gcw_rebel_marine_bracer_l",
		"armor_gcw_rebel_marine_bracer_r",
		"armor_gcw_rebel_marine_chest_plate",
		"armor_gcw_rebel_marine_gloves",
		"armor_gcw_rebel_marine_helmet",
		"armor_gcw_rebel_marine_leggings"
			]);
}

function handleRebBattlewornspecforceArmor(player) {
	spawnItems(player, [
		"armor_gcw_rebel_specforce_bicep_l",
		"armor_gcw_rebel_specforce_bicep_r",
		"armor_gcw_rebel_specforce_boots",
		"armor_gcw_rebel_specforce_bracer_l",
		"armor_gcw_rebel_specforce_bracer_r",
		"armor_gcw_rebel_specforce_chest_plate",
		"armor_gcw_rebel_specforce_gloves",
		"armor_gcw_rebel_specforce_helmet",
		"armor_gcw_rebel_specforce_leggings"
			]);
}

function handleImpBlackcrusaderArmor(player) {
	spawnItems(player, [
		"armor_mandalorian_imperial_black_bicep_l_04_01",
		"armor_mandalorian_imperial_black_bicep_r_04_01",
		"armor_mandalorian_imperial_black_boots_04_01",
		"armor_mandalorian_imperial_black_bracer_l_04_01",
		"armor_mandalorian_imperial_black_bracer_r_04_01",
		"armor_mandalorian_imperial_black_chest_plate_04_01",
		"armor_mandalorian_imperial_black_gloves_04_01",
		"armor_mandalorian_imperial_black_helmet_04_01",
		"armor_mandalorian_imperial_black_leggings_04_01"
			]);
}

function handleRebBlackcrusaderArmor(player) {
	spawnItems(player, [
		"armor_mandalorian_rebel_black_bicep_l_04_01",
		"armor_mandalorian_rebel_black_bicep_r_04_01",
		"armor_mandalorian_rebel_black_boots_04_01",
		"armor_mandalorian_rebel_black_bracer_l_04_01",
		"armor_mandalorian_rebel_black_bracer_r_04_01",
		"armor_mandalorian_rebel_black_chest_plate_04_01",
		"armor_mandalorian_rebel_black_gloves_04_01",
		"armor_mandalorian_rebel_black_helmet_04_01",
		"armor_mandalorian_rebel_black_leggings_04_01"
			]);
}

function handleImpBlackpvpspecopsArmor(player) {
	spawnItems(player, [
		"armor_pvp_spec_ops_imperial_black_bicep_l_05_01",
		"armor_pvp_spec_ops_imperial_black_bicep_r_05_01",
		"armor_pvp_spec_ops_imperial_black_boots_05_01",
		"armor_pvp_spec_ops_imperial_black_bracer_l_05_01",
		"armor_pvp_spec_ops_imperial_black_bracer_r_05_01",
		"armor_pvp_spec_ops_imperial_black_chest_plate_orange_pad_05_01",
		"armor_pvp_spec_ops_imperial_black_gloves_05_01",
		"armor_pvp_spec_ops_imperial_black_helmet_05_01",
		"armor_pvp_spec_ops_imperial_black_leggings_05_01"
			]);
}

function handleImpWhitepvpspecopsArmor(player) {
	spawnItems(player, [
		"armor_pvp_spec_ops_imperial_white_bicep_l_05_01",
		"armor_pvp_spec_ops_imperial_white_bicep_r_05_01",
		"armor_pvp_spec_ops_imperial_white_boots_05_01",
		"armor_pvp_spec_ops_imperial_white_bracer_l_05_01",
		"armor_pvp_spec_ops_imperial_white_bracer_r_05_01",
		"armor_pvp_spec_ops_imperial_white_chest_plate_orange_pad_05_01",
		"armor_pvp_spec_ops_imperial_white_gloves_05_01",
		"armor_pvp_spec_ops_imperial_white_helmet_05_01",
		"armor_pvp_spec_ops_imperial_white_leggings_05_01"
			]);
}

function handleImpSnowtrooperArmor(player) {
	spawnItems(player, [
		"armor_snowtrooper_bicep_l",
		"armor_snowtrooper_bicep_r",
		"armor_snowtrooper_boots",
		"armor_snowtrooper_bracer_l",
		"armor_snowtrooper_bracer_r",
		"armor_snowtrooper_chest_plate",
		"armor_snowtrooper_gloves",
		"armor_snowtrooper_helmet",
		"armor_snowtrooper_leggings"
			]);
}

function handleImpForestCamoArmor(player) {
	spawnItems(player, [
		"armor_scouttrooper_bicep_camo_l_04_01",
		"armor_scouttrooper_bicep_camo_r_04_01",
		"armor_scouttrooper_boots_camo_04_01",
		"armor_scouttrooper_bracer_camo_l_04_01",
		"armor_scouttrooper_bracer_camo_r_04_01",
		"armor_scouttrooper_chest_plate_camo_04_01",
		"armor_scouttrooper_gloves_camo_04_01",
		"armor_scouttrooper_helmet_camo_04_01",
		"armor_scouttrooper_leggings_camo_04_01"
			]);
}

function handleRebForestCamoArmor(player) {
	spawnItems(player, [
		"armor_rebel_assault_bicep_camo_l_04_01",
		"armor_rebel_assault_bicep_camo_r_04_01",
		"armor_rebel_assault_boots_camo_04_01",
		"armor_rebel_assault_bracer_camo_l_04_01",
		"armor_rebel_assault_bracer_camo_r_04_01",
		"armor_rebel_assault_chest_plate_camo_04_01",
		"armor_rebel_assault_gloves_camo_04_01",
		"armor_rebel_assault_helmet_camo_04_01",
		"armor_rebel_assault_leggings_camo_04_01"
			]);
}

function handleRebGreenpvpspecopsArmor(player) {
	spawnItems(player, [
		"armor_pvp_spec_ops_rebel_black_green_bicep_l_05_01",
		"armor_pvp_spec_ops_rebel_black_green_bicep_r_05_01",
		"armor_pvp_spec_ops_rebel_black_green_boots_05_01",
		"armor_pvp_spec_ops_rebel_black_green_bracer_l_05_01",
		"armor_pvp_spec_ops_rebel_black_green_bracer_r_05_01",
		"armor_pvp_spec_ops_rebel_black_green_chest_plate_05_01",
		"armor_pvp_spec_ops_rebel_black_green_gloves_05_01",
		"armor_pvp_spec_ops_rebel_black_green_helmet_05_01",
		"armor_pvp_spec_ops_rebel_black_green_leggings_05_01"
			]);
}

function handleRebAlliancecoldArmor(player) {
	spawnItems(player, [
		"armor_rebel_snow_bicep_l",
		"armor_rebel_snow_bicep_r",
		"armor_rebel_snow_boots",
		"armor_rebel_snow_bracer_l",
		"armor_rebel_snow_bracer_r",
		"armor_rebel_snow_chest_plate",
		"armor_rebel_snow_gloves",
		"armor_rebel_snow_helmet",
		"armor_rebel_snow_leggings"
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
	var SuiEvent = Java.type("resources.sui.SuiEvent");
	
	if (eventType != SuiEvent.OK_PRESSED) {
		return;
	}
	
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
		"weapon_mandalorian_lightsaber_04_01",
		"weapon_npe_lightsaber_02_01",
		"weapon_npe_lightsaber_02_02",
		"weapon_roadmap_lightsaber_02_02"
			]);
}

function handleMelee(player) {
	spawnItems(player, [
		"weapon_tow_blasterfist_04_01",
		"weapon_tow_sword_1h_05_02",
		"weapon_tow_sword_2h_05_02",
		"weapon_tow_polearm_05_01",
		"weapon_polearm_02_01",
		"weapon_polearm_04_01",
		"weapon_magna_guard_polearm_04_01",
		"weapon_content_polearm_tier_8_03_02",
		"weapon_quest_u10_knuckler_01_02"
			]);
}

function handleRanged(player) {
	spawnItems(player, [
		"weapon_tow_pistol_flechette_05_01",
		"weapon_tow_carbine_05_01",
		"weapon_tow_rifle_05_02",
		"weapon_tow_rifle_lightning_cannon_04_01",
		"weapon_tow_heavy_rocket_launcher_05_01",
		"weapon_borvo_carbine_03_01",
		"weapon_borvo_pistol_03_01",
		"weapon_borvo_rifle_03_01",
		"weapon_content_rifle_tier_7_03_01",
		"weapon_content_pistol_tier_7_03_01",
		"weapon_content_carbine_tier_7_03_01",
		"weapon_content_carbine_talus_selonian_04_01",
		"weapon_pistol_drop_lvl40_02_01",
		"weapon_rifle_drop_lvl40_02_01",
		"weapon_gcw_heavy_pulse_cannon_03_01",
		"weapon_heavy_pvp_general_reward_06_01",
		"weapon_pistol_imperial_pvp_general_reward_06_01",
		"weapon_rifle_imperial_pvp_general_reward_06_01",
		"weapon_carbine_pvp_imperial_general_reward_06_01"
			]);
}

function handleWearables(player) {
	var SuiListBox = Java.type("resources.sui.SuiListBox");
	listBox = new SuiListBox(SuiButtons.OK_CANCEL, "Character Builder Terminal", "Select a wearable category to receive a weapon of that type.");
	
	listBox.addListItem("Backpack");
	listBox.addListItem("Equipment");
	listBox.addListItem("Jedi Robes");

	listBox.addCallback("radial/terminal/character_builder", "handleWearablesSelection");
	listBox.display(player);
}

function handleWearablesSelection(player, creature, eventType, parameters) {
	var SuiEvent = Java.type("resources.sui.SuiEvent");
	
	if (eventType != SuiEvent.OK_PRESSED) {
		return;
	}
	
	var SuiListBox = Java.type("resources.sui.SuiListBox");
	selection = SuiListBox.getSelectedRow(parameters);
	
	switch(selection) {
		case 0: handleBackpack(player); break;
		case 1: handleEquipment(player); break;
		case 2: handleRobe(player); break;
	}
}

function handleBackpack(player) {
	spawnItems(player, [
		"item_content_backpack_rsf_02_01",
		"item_empire_day_imperial_sandtrooper_backpack",
		"item_empire_day_rebel_camoflauge_backpack",
		"item_event_gmf_backpack_01",
		"item_heroic_backpack_krayt_skull_01_01",
		"item_heroic_backpack_tauntaun_skull_01_01"
			]);
}

function handleEquipment(player) {
	spawnItems(player, [
	    "item_lifeday_09_jacket_01",
	    "item_pgc_chronicle_master_robe",
	    "item_senator_robe",
	    "item_senator_wookiee_robe",
	    "item_tcg_loot_reward_series1_arc170_flightsuit",
	    "item_tcg_loot_reward_series1_black_corset_dress",
	    "item_tcg_loot_reward_series1_black_flightsuit",
	    "item_tcg_loot_reward_series1_glowing_blue_eyes",
	    "item_tcg_loot_reward_series1_glowing_red_eyes",
	    "item_tcg_loot_reward_series5_ceremonial_travel_headdress",
	    "item_tcg_loot_reward_series6_greedo_outfit",
	    "item_tcg_loot_reward_series7_gold_cape",
	    "item_tow_duster_03_01",
		"item_event_gmf_jacket_01",
		"item_event_gmf_wings_01"
	    	]);
}

function handleRobe(player) {
	spawnItems(player, [
		"item_gcw_imperial_cape_01",
		"item_gcw_imperial_jacket_01",
		"item_gcw_rebel_cape_01",
		"item_gcw_rebel_jacket_01",
		"item_jedi_robe_04_01",
		"item_jedi_robe_04_02",
		"item_jedi_robe_06_03",
		"item_jedi_robe_06_04",
		"item_jedi_robe_dark_04_04",
		"item_jedi_robe_dark_04_05",
		"item_jedi_robe_light_04_05"
			]);
}

function handleTravel(player) {
	var SuiListBox = Java.type("resources.sui.SuiListBox");
	listBox = new SuiListBox(SuiButtons.OK_CANCEL, "Character Builder Terminal", "Select a location you want to get teleported to.");
	
	listBox.addListItem("Dathomir - Quarantine Zone");
	listBox.addListItem("Endor - DWB");
	listBox.addListItem("Naboo - Weapon Development Facility");
	listBox.addListItem("Tatooine - Fort Tusken");
	listBox.addListItem("Tatooine - Krayt Graveyard");
	listBox.addListItem("Tatooine - Mos Eisley");
	listBox.addListItem("Tatooine - Squill Cave");
	
	listBox.addCallback("radial/terminal/character_builder", "handleTravelSelection");
	listBox.display(player);
}

function handleTravelSelection(player, creature, eventType, parameters) {
	var SuiEvent = Java.type("resources.sui.SuiEvent");
	
	if (eventType != SuiEvent.OK_PRESSED) {
		return;
	}
	
	var SuiListBox = Java.type("resources.sui.SuiListBox");
	selection = SuiListBox.getSelectedRow(parameters);
	
	switch(selection) {
		case 0: handleDatQz(player); break;
		case 1: handleEndDwb(player); break;
		case 2: handleNabWeaponFac(player); break;
		case 3: handleTatFortTusken(player); break;
		case 4: handleTatKraytGrave(player); break;
		case 5: handleTatMosEisley(player); break;
		case 6: handleTatSquillCave(player); break;
	}
}

function handleDatQz(player) {
	var ObjectTeleportIntent = Java.type('intents.object.ObjectTeleportIntent');
	var Location = Java.type('resources.Location');
	var Terrain = Java.type('resources.Terrain');
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(-5786, 510, -6554, Terrain.DATHOMIR)).broadcast();
}

function handleEndDwb(player) {
	var ObjectTeleportIntent = Java.type('intents.object.ObjectTeleportIntent');
	var Location = Java.type('resources.Location');
	var Terrain = Java.type('resources.Terrain');
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(-4683, 13, 4326, Terrain.ENDOR)).broadcast();
}

function handleNabWeaponFac(player) {
	var ObjectTeleportIntent = Java.type('intents.object.ObjectTeleportIntent');
	var Location = Java.type('resources.Location');
	var Terrain = Java.type('resources.Terrain');
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(-6439, 41, -3265, Terrain.NABOO)).broadcast();
}

function handleTatFortTusken(player) {
	var ObjectTeleportIntent = Java.type('intents.object.ObjectTeleportIntent');
	var Location = Java.type('resources.Location');
	var Terrain = Java.type('resources.Terrain');
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(-3941, 59, 6318, Terrain.TATOOINE)).broadcast();
}

function handleTatKraytGrave(player) {
	var ObjectTeleportIntent = Java.type('intents.object.ObjectTeleportIntent');
	var Location = Java.type('resources.Location');
	var Terrain = Java.type('resources.Terrain');
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(7380, 122, 4298, Terrain.TATOOINE)).broadcast();
}

function handleTatMosEisley(player) {
	var ObjectTeleportIntent = Java.type('intents.object.ObjectTeleportIntent');
	var Location = Java.type('resources.Location');
	var Terrain = Java.type('resources.Terrain');
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(3525, 4, -4807, Terrain.TATOOINE)).broadcast();
}

function handleTatSquillCave(player) {
	var ObjectTeleportIntent = Java.type('intents.object.ObjectTeleportIntent');
	var Location = Java.type('resources.Location');
	var Terrain = Java.type('resources.Terrain');
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(57, 152, -79, Terrain.TATOOINE)).broadcast();
}
