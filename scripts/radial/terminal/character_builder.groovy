import com.projectswg.common.data.location.Location
import com.projectswg.common.data.location.Terrain
import intents.object.CreateStaticItemIntent
import intents.object.ObjectTeleportIntent
import resources.containers.ContainerPermissionsType;
import resources.objects.SWGObject
import resources.objects.creature.CreatureObject
import resources.player.Player
import com.projectswg.common.data.radial.RadialItem
import com.projectswg.common.data.radial.RadialOption
import resources.sui.SuiButtons
import com.projectswg.common.data.sui.SuiEvent
import resources.sui.SuiListBox
import services.objects.StaticItemService
import com.projectswg.common.debug.Log

static def getOptions(List<RadialOption> options, Player player, SWGObject target, Object... args) {
	options.add(new RadialOption(RadialItem.ITEM_USE))
	options.add(new RadialOption(RadialItem.EXAMINE))
}

static def handleSelection(Player player, SWGObject target, RadialItem selection, Object... args) {
	switch (selection) {
		case RadialItem.ITEM_USE:
			def listBox = new SuiListBox(SuiButtons.OK_CANCEL, "Character Builder Terminal", "Select a category.")

			listBox.addListItem("Armor")
			listBox.addListItem("Weapons")
			listBox.addListItem("Wearables")
			listBox.addListItem("Travel")

			listBox.addCallback("radial/terminal/character_builder", "handleCategorySelection")
			listBox.display(player)
			break
	}
}

static def handleCategorySelection(Player player, CreatureObject creature, SuiEvent eventType, Map<String, String> parameters) {
	if (eventType != SuiEvent.OK_PRESSED) {
		return
	}
	def selection = SuiListBox.getSelectedRow(parameters)

	switch (selection) {
		case 0: handleArmor(player); break
		case 1: handleWeapons(player); break
		case 2: handleWearables(player); break
		case 3: handleTravel(player); break
	}
}

static def spawnItems(Player player, List<String> items) {
	def creature = player.getCreatureObject()
	def inventory = creature.getSlottedObject("inventory")

	new CreateStaticItemIntent(creature, inventory, new StaticItemService.LootBoxHandler(creature), ContainerPermissionsType.DEFAULT, items.toArray(new String[items.size()])).broadcast()
}

static def handleArmor(Player player) {
	def listBox = new SuiListBox(SuiButtons.OK_CANCEL, "Character Builder Terminal", "Select a set of armor to receive.")

	listBox.addListItem("R.I.S. Armor")
	listBox.addListItem("Mandalorian Armor")
	listBox.addListItem("Bone Armor")
	listBox.addListItem("Chitin Armor")
	listBox.addListItem("Padded Armor")
	listBox.addListItem("Ubese Armor")
	listBox.addListItem("Ithorian Defender Armor")
	listBox.addListItem("Wookiee Black Mountain Armor")
	listBox.addListItem("Deathtrooper Armor")
	listBox.addListItem("Imperial - Galactic Marine Armor")
	listBox.addListItem("Imperial - Scout trooper Armor")
	listBox.addListItem("Imperial - Shock trooper Armor")
	listBox.addListItem("Imperial - Stormtrooper Armor")
	listBox.addListItem("Imperial - Black Crusader Armor")
	listBox.addListItem("Imperial - Black Spec Ops Armor")
	listBox.addListItem("Imperial - White Spec Ops Armor")
	listBox.addListItem("Imperial - Snow trooper Armor")
	listBox.addListItem("Imperial - Forest Camouflage Armor")
	listBox.addListItem("Rebel - Forest Camouflage Armor")
	listBox.addListItem("Rebel - Assault Armor")
	listBox.addListItem("Rebel - Battle Armor")
	listBox.addListItem("Rebel - Marine Armor")
	listBox.addListItem("Rebel - Spec Force Armor")
	listBox.addListItem("Rebel - Black Crusader Armor")
	listBox.addListItem("Rebel - Alliance Weather Cold Armor")
	listBox.addCallback("radial/terminal/character_builder", "handleArmorSelection")
	listBox.display(player)
}

static def handleArmorSelection(Player player, CreatureObject creature, SuiEvent eventType, Map<String, String> parameters) {
	if (eventType != SuiEvent.OK_PRESSED) {
		return
	}

	switch (SuiListBox.getSelectedRow(parameters)) {
		case 0: handleRisArmor(player); break
		case 1: handleMandoArmor(player); break
		case 2: handleBoneArmor(player); break
		case 3: handleChitinArmor(player); break
		case 4: handlePaddedArmor(player); break
		case 5: handleUbeseArmor(player); break
		case 6: handleIthoriandefenderArmor(player); break
		case 7: handleWookieeblackmtnArmor(player); break
		case 8: handleDeathtrooperArmor(player); break
		case 9: handleImpBattlemarineArmor(player); break
		case 10: handleImpBattlewornscoutArmor(player); break
		case 11: handleImpBattlewornshockArmor(player); break
		case 12: handleImpBattlewornstormArmor(player); break
		case 13: handleImpBlackcrusaderArmor(player); break
		case 14: handleImpBlackpvpspecopsArmor(player); break
		case 15: handleImpWhitepvpspecopsArmor(player); break
		case 16: handleImpSnowtrooperArmor(player); break
		case 17: handleImpForestCamoArmor(player); break
		case 18: handleRebForestCamoArmor(player); break
		case 19: handleRebBattlewornassaultArmor(player); break
		case 20: handleRebBattlewornbattleArmor(player); break
		case 21: handleRebBattlewornmarineArmor(player); break
		case 22: handleRebBattlewornspecforceArmor(player); break
		case 23: handleRebBlackcrusaderArmor(player); break
		case 24: handleRebAlliancecoldArmor(player); break
	}
}

static def handleRisArmor(Player player) {
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
	])
}

static def handleMandoArmor(Player player) {
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
	])
}

static def handleBoneArmor(Player player) {
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
	])
}

static def handleChitinArmor(Player player) {
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
	])
}

static def handlePaddedArmor(Player player) {
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
	])
}

static def handleUbeseArmor(Player player) {
	spawnItems(player, [
			"armor_recon_sta_lvl80_boots_02_01",
			"armor_recon_sta_lvl80_bracer_l_02_01",
			"armor_recon_sta_lvl80_bracer_r_02_01",
			"armor_recon_sta_lvl80_chest_02_01",
			"armor_recon_sta_lvl80_gloves_02_01",
			"armor_recon_sta_lvl80_helmet_02_01",
			"armor_recon_sta_lvl80_leggings_02_01"
	])
}

static def handleIthoriandefenderArmor(Player player) {
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
	])
}

static def handleWookieeblackmtnArmor(Player player) {
	spawnItems(player, [
			"armor_kashyyykian_black_mtn_bicep_camo_l_04_01",
			"armor_kashyyykian_black_mtn_bicep_camo_r_04_01",
			"armor_kashyyykian_black_mtn_bracer_camo_l_04_01",
			"armor_kashyyykian_black_mtn_bracer_camo_r_04_01",
			"armor_kashyyykian_black_mtn_chest_plate_camo_04_01",
			"armor_kashyyykian_black_mtn_leggings_camo_04_01",
	])
}

static def handleDeathtrooperArmor(Player player) {
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
	])
}

static def handleImpBattlemarineArmor(Player player) {
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
	])
}

static def handleImpBattlewornscoutArmor(Player player) {
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
	])
}

static def handleImpBattlewornshockArmor(Player player) {
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
	])
}

static def handleImpBattlewornstormArmor(Player player) {
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
	])
}

static def handleRebBattlewornassaultArmor(Player player) {
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
	])
}

static def handleRebBattlewornbattleArmor(Player player) {
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
	])
}

static def handleRebBattlewornmarineArmor(Player player) {
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
	])
}

static def handleRebBattlewornspecforceArmor(Player player) {
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
	])
}

static def handleImpBlackcrusaderArmor(Player player) {
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
	])
}

static def handleRebBlackcrusaderArmor(Player player) {
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
	])
}

static def handleImpBlackpvpspecopsArmor(Player player) {
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
	])
}

static def handleImpWhitepvpspecopsArmor(Player player) {
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
	])
}

static def handleImpSnowtrooperArmor(Player player) {
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
	])
}

static def handleImpForestCamoArmor(Player player) {
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
	])
}

static def handleRebForestCamoArmor(Player player) {
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
	])
}

static def handleRebGreenpvpspecopsArmor(Player player) {
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
	])
}

static def handleRebAlliancecoldArmor(Player player) {
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
	])
}

static def handleWeapons(Player player) {
	def listBox = new SuiListBox(SuiButtons.OK_CANCEL, "Character Builder Terminal", "Select a weapon category to receive a weapon of that type.")

	listBox.addListItem("Lightsabers")
	listBox.addListItem("Melee")
	listBox.addListItem("Ranged")

	listBox.addCallback("radial/terminal/character_builder", "handleWeaponSelection")
	listBox.display(player)
}

static
def handleWeaponSelection(Player player, CreatureObject creature, SuiEvent eventType, Map<String, String> parameters) {
	if (eventType != SuiEvent.OK_PRESSED) {
		return
	}

	def selection = SuiListBox.getSelectedRow(parameters)

	switch (selection) {
		case 0: handleLightsabers(player); break
		case 1: handleMelee(player); break
		case 2: handleRanged(player); break
	}
}

static def handleLightsabers(Player player) {
	spawnItems(player, [
			"weapon_mandalorian_lightsaber_04_01",
			"weapon_npe_lightsaber_02_01",
			"weapon_npe_lightsaber_02_02",
			"weapon_roadmap_lightsaber_02_02"
	])
}

static def handleMelee(Player player) {
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
	])
}

static def handleRanged(Player player) {
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
	])
}

static def handleWearables(Player player) {
	def listBox = new SuiListBox(SuiButtons.OK_CANCEL, "Character Builder Terminal", "Select a wearable category to receive a weapon of that type.")

	listBox.addListItem("Backpacks")
	listBox.addListItem("Bikinis")
	listBox.addListItem("Bodysuits")
	listBox.addListItem("Boots")
	listBox.addListItem("Bustiers")
	listBox.addListItem("Dress")
	listBox.addListItem("Gloves")
	listBox.addListItem("Goggles")
	listBox.addListItem("Hats")
	listBox.addListItem("Helmets")
	listBox.addListItem("Jackets")
	listBox.addListItem("Pants")
	listBox.addListItem("Robes")
	listBox.addListItem("Shirt")
	listBox.addListItem("Shoes")
	listBox.addListItem("Skirts")
	listBox.addListItem("Vest")
	listBox.addListItem("Ithorian equipment")
	listBox.addListItem("Jedi equipment")
	listBox.addListItem("Nightsister equipment")
	listBox.addListItem("Tusken Raider equipment")
	listBox.addListItem("Wookie equipment")
	listBox.addListItem("TCG")

	listBox.addCallback("radial/terminal/character_builder", "handleWearablesSelection")
	listBox.display(player)
}

static
def handleWearablesSelection(Player player, CreatureObject creature, SuiEvent eventType, Map<String, String> parameters) {

	if (eventType != SuiEvent.OK_PRESSED) {
		return
	}

	def selection = SuiListBox.getSelectedRow(parameters)

	switch (selection) {
		case 0: handleBackpack(player); break
		case 1: handleBikini(player); break
		case 2: handleBodysuit(player); break
		case 3: handleBoot(player); break
		case 4: handleBustier(player); break
		case 5: handleDress(player); break
		case 6: handleGlove(player); break
		case 7: handleGoggle(player); break
		case 8: handleHat(player); break
		case 9: handleHelmet(player); break
		case 10: handleJacket(player); break
		case 11: handlePant(player); break
		case 12: handleRobe(player); break
		case 13: handleShirt(player); break
		case 14: handleShoe(player); break
		case 15: handleSkirt(player); break
		case 16: handleVest(player); break
		case 17: handleIthorianEquipment(player); break
		case 18: handleJediEquipment(player); break
		case 19: handleNightsisterEquipment(player); break
		case 20: handleTuskenEquipment(player); break
		case 21: handleWookieeEquipment(player); break
		case 22: handleOther(player); break
	}
}

static def handleBackpack(Player player) {
	spawnItems(player, [
			"item_content_backpack_rsf_02_01",
			"item_empire_day_imperial_sandtrooper_backpack",
			"item_empire_day_rebel_camoflauge_backpack",
			"item_event_gmf_backpack_01",
			"item_heroic_backpack_krayt_skull_01_01",
			"item_heroic_backpack_tauntaun_skull_01_01"
	])
}

static def handleBikini(Player player) {
	spawnItems(player, [
			"item_clothing_bikini_01_01",
			"item_clothing_bikini_01_02",
			"item_clothing_bikini_01_03",
			"item_clothing_bikini_01_04",
			"item_clothing_bikini_leggings_01_01"
	])
}

static def handleBodysuit(Player player) {
	spawnItems(player, [
			"item_clothing_bodysuit_at_at_01_01",
			"item_clothing_bodysuit_bwing_01_01",
			"item_clothing_bodysuit_tie_fighter_01_01",
			"item_clothing_bodysuit_trando_slaver_01_01"
	])
}

static def handleBoot(Player player) {
	spawnItems(player, [
			"item_clothing_boots_01_03",
			"item_clothing_boots_01_04",
			"item_clothing_boots_01_05",
			"item_clothing_boots_01_12",
			"item_clothing_boots_01_14",
			"item_clothing_boots_01_15",
			"item_clothing_boots_01_19",
			"item_clothing_boots_01_21",
			"item_clothing_boots_01_22",
			"item_clothing_boots_01_24"
	])
}

static def handleBustier(Player player) {
	spawnItems(player, [
			"item_clothing_bustier_01_01",
			"item_clothing_bustier_01_02",
			"item_clothing_bustier_01_03"
	])
}

static def handleDress(Player player) {
	spawnItems(player, [
			"item_clothing_dress_01_05",
			"item_clothing_dress_01_06",
			"item_clothing_dress_01_07",
			"item_clothing_dress_01_08",
			"item_clothing_dress_01_09",
			"item_clothing_dress_01_10",
			"item_clothing_dress_01_11",
			"item_clothing_dress_01_12",
			"item_clothing_dress_01_13",
			"item_clothing_dress_01_14",
			"item_clothing_dress_01_15",
			"item_clothing_dress_01_16",
			"item_clothing_dress_01_18",
			"item_clothing_dress_01_19",
			"item_clothing_dress_01_23",
			"item_clothing_dress_01_26",
			"item_clothing_dress_01_27",
			"item_clothing_dress_01_29",
			"item_clothing_dress_01_30",
			"item_clothing_dress_01_31",
			"item_clothing_dress_01_32",
			"item_clothing_dress_01_33",
			"item_clothing_dress_01_34",
			"item_clothing_dress_01_35"
	])
}

static def handleGlove(Player player) {
	spawnItems(player, [
			"item_clothing_gloves_01_02",
			"item_clothing_gloves_01_03",
			"item_clothing_gloves_01_06",
			"item_clothing_gloves_01_07",
			"item_clothing_gloves_01_10",
			"item_clothing_gloves_01_11",
			"item_clothing_gloves_01_12",
			"item_clothing_gloves_01_13",
			"item_clothing_gloves_01_14"
	])
}

static def handleGoggle(Player player) {
	spawnItems(player, [
			"item_clothing_goggles_anniversary_01_01",
			"item_clothing_goggles_goggles_01_01",
			"item_clothing_goggles_goggles_01_02",
			"item_clothing_goggles_goggles_01_03",
			"item_clothing_goggles_goggles_01_04",
			"item_clothing_goggles_goggles_01_05",
			"item_clothing_goggles_goggles_01_06"
	])
}

static def handleHat(Player player) {
	spawnItems(player, [
			"item_clothing_hat_chef_01_01",
			"item_clothing_hat_chef_01_02",
			"item_clothing_hat_imp_01_01",
			"item_clothing_hat_imp_01_02",
			"item_clothing_hat_rebel_trooper_01_01",
			"item_clothing_hat_01_02",
			"item_clothing_hat_01_04",
			"item_clothing_hat_01_10",
			"item_clothing_hat_01_12",
			"item_clothing_hat_01_13",
			"item_clothing_hat_01_14",
			"item_clothing_hat_twilek_01_01",
			"item_clothing_hat_twilek_01_02",
			"item_clothing_hat_twilek_01_03",
			"item_clothing_hat_twilek_01_04",
			"item_clothing_hat_twilek_01_05"
	])
}

static def handleHelmet(Player player) {
	spawnItems(player, [
			"item_clothing_helmet_at_at_01_01",
			"item_clothing_helmet_fighter_blacksun_01_01",
			"item_clothing_helmet_fighter_imperial_01_01",
			"item_clothing_helmet_fighter_privateer_01_01",
			"item_clothing_helmet_fighter_rebel_01_01",
			"item_clothing_helmet_tie_fighter_01_01"
	])
}

static def handleJacket(Player player) {
	spawnItems(player, [
			"item_clothing_jacket_ace_imperial_01_01",
			"item_clothing_jacket_ace_privateer_01_01",
			"item_clothing_jacket_ace_rebel_01_01",
			"item_clothing_jacket_gcw_imperial_01_01",
			"item_clothing_jacket_gcw_rebel_01_01",
			"item_clothing_jacket_01_02",
			"item_clothing_jacket_01_03",
			"item_clothing_jacket_01_04",
			"item_clothing_jacket_01_05",
			"item_clothing_jacket_01_06",
			"item_clothing_jacket_01_07",
			"item_clothing_jacket_01_08",
			"item_clothing_jacket_01_09",
			"item_clothing_jacket_01_10",
			"item_clothing_jacket_01_11",
			"item_clothing_jacket_01_12",
			"item_clothing_jacket_01_13",
			"item_clothing_jacket_01_14",
			"item_clothing_jacket_01_15",
			"item_clothing_jacket_01_16",
			"item_clothing_jacket_01_17",
			"item_clothing_jacket_01_18",
			"item_clothing_jacket_01_19",
			"item_clothing_jacket_01_20",
			"item_clothing_jacket_01_21",
			"item_clothing_jacket_01_22",
			"item_clothing_jacket_01_23",
			"item_clothing_jacket_01_24",
			"item_clothing_jacket_01_25",
			"item_clothing_jacket_01_26"
	])
}

static def handlePant(Player player) {
	spawnItems(player, [
			"item_clothing_pants_01_01",
			"item_clothing_pants_01_02",
			"item_clothing_pants_01_03",
			"item_clothing_pants_01_04",
			"item_clothing_pants_01_05",
			"item_clothing_pants_01_06",
			"item_clothing_pants_01_07",
			"item_clothing_pants_01_08",
			"item_clothing_pants_01_09",
			"item_clothing_pants_01_10",
			"item_clothing_pants_01_11",
			"item_clothing_pants_01_12",
			"item_clothing_pants_01_13",
			"item_clothing_pants_01_14",
			"item_clothing_pants_01_15",
			"item_clothing_pants_01_16",
			"item_clothing_pants_01_17",
			"item_clothing_pants_01_18",
			"item_clothing_pants_01_21",
			"item_clothing_pants_01_22",
			"item_clothing_pants_01_24",
			"item_clothing_pants_01_25",
			"item_clothing_pants_01_26",
			"item_clothing_pants_01_27",
			"item_clothing_pants_01_28",
			"item_clothing_pants_01_29",
			"item_clothing_pants_01_30",
			"item_clothing_pants_01_31",
			"item_clothing_pants_01_32",
			"item_clothing_pants_01_33"
	])
}

static def handleRobe(Player player) {
	spawnItems(player, [
			"item_clothing_robe_exar_cultist_hood_down_01_01",
			"item_clothing_robe_exar_cultist_hood_up_01_01",
			"item_clothing_robe_prefect_talmont_01_01",
			"item_clothing_robe_01_01",
			"item_clothing_robe_01_04",
			"item_clothing_robe_01_05",
			"item_clothing_robe_01_12",
			"item_clothing_robe_01_18",
			"item_clothing_robe_01_27",
			"item_clothing_robe_01_32",
			"item_clothing_robe_01_33"
	])
}

static def handleShirt(Player player) {
	spawnItems(player, [
			"item_clothing_shirt_01_03",
			"item_clothing_shirt_01_04",
			"item_clothing_shirt_01_05",
			"item_clothing_shirt_01_07",
			"item_clothing_shirt_01_08",
			"item_clothing_shirt_01_09",
			"item_clothing_shirt_01_10",
			"item_clothing_shirt_01_11",
			"item_clothing_shirt_01_12",
			"item_clothing_shirt_01_13",
			"item_clothing_shirt_01_14",
			"item_clothing_shirt_01_15",
			"item_clothing_shirt_01_16",
			"item_clothing_shirt_01_24",
			"item_clothing_shirt_01_26",
			"item_clothing_shirt_01_27",
			"item_clothing_shirt_01_28",
			"item_clothing_shirt_01_30",
			"item_clothing_shirt_01_32",
			"item_clothing_shirt_01_34",
			"item_clothing_shirt_01_38",
			"item_clothing_shirt_01_42"
	])
}

static def handleShoe(Player player) {
	spawnItems(player, [
			"item_clothing_shoes_01_01",
			"item_clothing_shoes_01_02",
			"item_clothing_shoes_01_03",
			"item_clothing_shoes_01_07",
			"item_clothing_shoes_01_08",
			"item_clothing_shoes_01_09"
	])
}

static def handleSkirt(Player player) {
	spawnItems(player, [
			"item_clothing_skirt_01_03",
			"item_clothing_skirt_01_04",
			"item_clothing_skirt_01_05",
			"item_clothing_skirt_01_06",
			"item_clothing_skirt_01_07",
			"item_clothing_skirt_01_08",
			"item_clothing_skirt_01_09",
			"item_clothing_skirt_01_10",
			"item_clothing_skirt_01_11",
			"item_clothing_skirt_01_12",
			"item_clothing_skirt_01_13",
			"item_clothing_skirt_01_14"
	])
}

static def handleVest(Player player) {
	spawnItems(player, [
			"item_clothing_vest_01_01",
			"item_clothing_vest_01_02",
			"item_clothing_vest_01_03",
			"item_clothing_vest_01_04",
			"item_clothing_vest_01_05",
			"item_clothing_vest_01_06",
			"item_clothing_vest_01_09",
			"item_clothing_vest_01_10",
			"item_clothing_vest_01_11",
			"item_clothing_vest_01_15"
	])
}

static def handleIthorianEquipment(Player player) {
	spawnItems(player, [
			"item_clothing_ithorian_apron_chef_jacket_01_01",
			"item_clothing_ithorian_hat_chef_01_01",
			"item_clothing_ithorian_hat_chef_01_02",
			"item_clothing_ithorian_bodysuit_01_01",
			"item_clothing_ithorian_bodysuit_01_02",
			"item_clothing_ithorian_bodysuit_01_03",
			"item_clothing_ithorian_bodysuit_01_04",
			"item_clothing_ithorian_bodysuit_01_05",
			"item_clothing_ithorian_bodysuit_01_06",
			"item_clothing_ithorian_dress_01_02",
			"item_clothing_ithorian_dress_01_03",
			"item_clothing_ithorian_gloves_01_01",
			"item_clothing_ithorian_gloves_01_02",
			"item_clothing_ithorian_hat_01_01",
			"item_clothing_ithorian_hat_01_02",
			"item_clothing_ithorian_hat_01_03",
			"item_clothing_ithorian_hat_01_04",
			"item_clothing_ithorian_pants_01_01",
			"item_clothing_ithorian_pants_01_02",
			"item_clothing_ithorian_pants_01_03",
			"item_clothing_ithorian_pants_01_04",
			"item_clothing_ithorian_pants_01_05",
			"item_clothing_ithorian_pants_01_06",
			"item_clothing_ithorian_pants_01_07",
			"item_clothing_ithorian_pants_01_08",
			"item_clothing_ithorian_pants_01_09",
			"item_clothing_ithorian_pants_01_10",
			"item_clothing_ithorian_pants_01_11",
			"item_clothing_ithorian_pants_01_12",
			"item_clothing_ithorian_pants_01_13",
			"item_clothing_ithorian_pants_01_14",
			"item_clothing_ithorian_pants_01_15",
			"item_clothing_ithorian_pants_01_16",
			"item_clothing_ithorian_pants_01_17",
			"item_clothing_ithorian_pants_01_18",
			"item_clothing_ithorian_pants_01_19",
			"item_clothing_ithorian_pants_01_20",
			"item_clothing_ithorian_pants_01_21",
			"item_clothing_ithorian_robe_01_02",
			"item_clothing_ithorian_robe_01_03",
			"item_clothing_ithorian_shirt_01_01",
			"item_clothing_ithorian_shirt_01_02",
			"item_clothing_ithorian_shirt_01_03",
			"item_clothing_ithorian_shirt_01_04",
			"item_clothing_ithorian_shirt_01_05",
			"item_clothing_ithorian_shirt_01_06",
			"item_clothing_ithorian_shirt_01_07",
			"item_clothing_ithorian_shirt_01_08",
			"item_clothing_ithorian_shirt_01_09",
			"item_clothing_ithorian_shirt_01_10",
			"item_clothing_ithorian_shirt_01_11",
			"item_clothing_ithorian_shirt_01_12",
			"item_clothing_ithorian_shirt_01_13",
			"item_clothing_ithorian_shirt_01_14",
			"item_clothing_ithorian_skirt_01_01",
			"item_clothing_ithorian_skirt_01_02",
			"item_clothing_ithorian_skirt_01_03",
			"item_clothing_ithorian_vest_01_01",
			"item_clothing_ithorian_vest_01_02"
	])
}

static def handleJediEquipment(Player player) {
	spawnItems(player, [
			"item_gcw_imperial_cape_01",
			"item_gcw_imperial_jacket_01",
			"item_gcw_rebel_cape_01",
			"item_gcw_rebel_jacket_01",
			"item_jedi_robe_04_01",
			"item_jedi_robe_04_02",
			"item_jedi_robe_06_03",
			"item_jedi_robe_06_04",
			"item_jedi_robe_dark_03_01",
			"item_jedi_robe_dark_03_02",
			"item_jedi_robe_dark_03_03",
			"item_jedi_robe_dark_04_01",
			"item_jedi_robe_dark_04_02",
			"item_jedi_robe_dark_04_03",
			"item_jedi_robe_dark_04_04",
			"item_jedi_robe_dark_04_05",
			"item_jedi_robe_light_03_01",
			"item_jedi_robe_light_03_02",
			"item_jedi_robe_light_03_03",
			"item_jedi_robe_light_04_01",
			"item_jedi_robe_light_04_02",
			"item_jedi_robe_light_04_03",
			"item_jedi_robe_light_04_04",
			"item_jedi_robe_light_04_05"
	])
}

static def handleNightsisterEquipment(Player player) {
	spawnItems(player, [
			"item_clothing_boots_nightsister_01_01",
			"item_clothing_dress_nightsister_01_01",
			"item_clothing_hat_nightsister_01_01",
			"item_clothing_hat_nightsister_01_02",
			"item_clothing_hat_nightsister_01_03",
			"item_clothing_pants_nightsister_01_01",
			"item_clothing_pants_nightsister_01_02",
			"item_clothing_shirt_nightsister_01_01",
			"item_clothing_shirt_nightsister_01_02",
			"item_clothing_shirt_nightsister_01_03"
	])
}

static def handleTuskenEquipment(Player player) {
	spawnItems(player, [
			"item_clothing_bandolier_tusken_01_01",
			"item_clothing_bandolier_tusken_01_02",
			"item_clothing_bandolier_tusken_01_03",
			"item_clothing_boots_tusken_raider_01_01",
			"item_clothing_gloves_tusken_raider_01_01",
			"item_clothing_helmet_tusken_raider_01_01",
			"item_clothing_helmet_tusken_raider_01_02",
			"item_clothing_robe_tusken_raider_01_01",
			"item_clothing_robe_tusken_raider_01_02"
	])
}

static def handleWookieeEquipment(Player player) {
	spawnItems(player, [
			"item_clothing_wookiee_gloves_01_01",
			"item_clothing_wookiee_gloves_01_02",
			"item_clothing_wookiee_gloves_01_03",
			"item_clothing_wookiee_gloves_01_04",
			"item_clothing_wookiee_hat_01_01",
			"item_clothing_wookiee_hood_01_01",
			"item_clothing_wookiee_hood_01_02",
			"item_clothing_wookiee_hood_01_03",
			"item_clothing_wookiee_lifeday_robe_01_01",
			"item_clothing_wookiee_lifeday_robe_01_02",
			"item_clothing_wookiee_lifeday_robe_01_03",
			"item_clothing_wookiee_shirt_01_01",
			"item_clothing_wookiee_shirt_01_02",
			"item_clothing_wookiee_shirt_01_03",
			"item_clothing_wookiee_shirt_01_04",
			"item_clothing_wookiee_shirt_01_05",
			"item_clothing_wookiee_shoulder_pad_01_01",
			"item_clothing_wookiee_shoulder_pad_01_02",
			"item_clothing_wookiee_skirt_01_01",
			"item_clothing_wookiee_skirt_01_02",
			"item_clothing_wookiee_skirt_01_03",
			"item_clothing_wookiee_skirt_01_04"
	])
}

static def handleOther(Player player) {
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
	])
}

static def handleTravel(Player player) {
	def listBox = new SuiListBox(SuiButtons.OK_CANCEL, "Character Builder Terminal", "Select a location you want to get teleported to.")

	listBox.addListItem("Corellia - Stronghold")
	listBox.addListItem("Coreliia - Corsec Base")
	listBox.addListItem("Corellia - Rebel Base with X-Wings")
	listBox.addListItem("Dantooine - Force Crystal Hunter's Cave")
	listBox.addListItem("Dantooine - Jedi Temple Ruins")
	listBox.addListItem("Dantooine - The Warren")
	listBox.addListItem("Dathomir - Imperial Prison")
	listBox.addListItem("Dathomir - Nightsister Stronghold")
	listBox.addListItem("Dathomir - Nightsister vs. Singing Moutain Clan")
	listBox.addListItem("Dathomir - Quarantine Zone")
	listBox.addListItem("Endor - DWB")
	listBox.addListItem("Endor - Jinda Cave")
	listBox.addListItem("Kashyyyk - Etyyy, The Hunting Grounds")
	listBox.addListItem("Kashyyyk - Kachirho, Slaver Camp")
	listBox.addListItem("Kashyyyk - Kkowir, The Dead Forest")
	listBox.addListItem("Lok - Droid Cave")
	listBox.addListItem("Lok - Great Maze of Lok")
	listBox.addListItem("Lok - Imperial Outpost")
	listBox.addListItem("Lok - Kimogila Town")
	listBox.addListItem("Naboo - Emperor's Retreat")
	listBox.addListItem("Naboo - Weapon Development Facility")
	listBox.addListItem("Rori - Hyperdrive Research Facility")
	listBox.addListItem("Talus - Detainment Center")
	listBox.addListItem("Tatooine - Fort Tusken")
	listBox.addListItem("Tatooine - Imperial Oasis")
	listBox.addListItem("Tatooine - Krayt Graveyard")
	listBox.addListItem("Tatooine - Mos Eisley")
	listBox.addListItem("Tatooine - Mos Taike")
	listBox.addListItem("Tatooine - Squill Cave")
	listBox.addListItem("Yavin 4 - Blueleaf Temple")
	listBox.addListItem("Yavin 4 - Dark Enclave")
	listBox.addListItem("Yavin 4 - Exar Kun")
	listBox.addListItem("Yavin 4 - Geonosian Cave")
	listBox.addListItem("Yavin 4 - Light Enclave")

	listBox.addCallback("radial/terminal/character_builder", "handleTravelSelection")
	listBox.display(player)
}

static
def handleTravelSelection(Player player, CreatureObject creature, SuiEvent eventType, Map<String, String> parameters) {

	if (eventType != SuiEvent.OK_PRESSED) {
		return
	}

	def selection = SuiListBox.getSelectedRow(parameters)

	switch (selection) {

	// Planet: Corellia
		case 0: handleCorStronghold(player); break
		case 1: handleCorCorsecBase(player); break
		case 2: handleCorRebelXwingBase(player); break
	// Planet: Dantooine
		case 3: handleDanCrystalCave(player); break
		case 4: handleDanJediTemple(player); break
		case 5: handleDanWarren(player); break
	// Planet: Dathomir
		case 6: handleDatImperialPrison(player); break
		case 7: handleDatNS(player); break
		case 8: handleDatNSvsSMC(player); break
		case 9: handleDatQz(player); break
	// Planet: Endor
		case 10: handleEndDwb(player); break
		case 11: handleEndJindaCave(player); break
	// Planet: Kashyyyk
		case 12: handleKasEtyyy(player); break
		case 13: handleKasKachirho(player); break
		case 14: handleKasKkowir(player); break
	// Planet: Lok
		case 15: handleLokDroidCave(player); break
		case 16: handleLokGreatMaze(player); break
		case 17: handleLokImperialOutpost(player); break
		case 18: handleLokKimogilaTown(player); break
	// Planet: Naboo
		case 19: handleNabEmperorsRetreat(player); break
		case 20: handleNabWeaponFac(player); break
	// Planet: Rori
		case 21: handleRorHyperdriveFacility(player); break
	// Planet: Talus
		case 22: handleTalDetainmentCenter(player); break
	// Planet: Tatooine
		case 23: handleTatFortTusken(player); break
		case 24: handleTatImperialOasis(player); break
		case 25: handleTatKraytGrave(player); break
		case 26: handleTatMosEisley(player); break
		case 27: handleTatMosTaike(player); break
		case 28: handleTatSquillCave(player); break
	// Planet: Yavin 4
		case 29: handleYavBlueleafTemple(player); break
		case 30: handleYavDarkEnclave(player); break
		case 31: handleYavExarKun(player); break
		case 32: handleYavGeoCave(player); break
		case 33: handleYavLightEnclave(player); break

	}
}

// Planet: Corellia

static def handleCorStronghold(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(4735d, 26d, -5676d, Terrain.CORELLIA)).broadcast()
}

static def handleCorCorsecBase(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(5137d, 16d, 1518d, Terrain.CORELLIA)).broadcast()
}

static def handleCorRebelXwingBase(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(213d, 50d, 4533d, Terrain.CORELLIA)).broadcast()
}

// Planet: Dantooine

static def handleDanJediTemple(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(4078d, 10d, 5370d, Terrain.DANTOOINE)).broadcast()
}

static def handleDanCrystalCave(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(-6225d, 48d, 7381d, Terrain.DANTOOINE)).broadcast()
}

static def handleDanWarren(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(-564d, 1d, -3789d, Terrain.DANTOOINE)).broadcast()
}

// Planet: Dathomir

static def handleDatImperialPrison(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(-6079d, 132d, 971d, Terrain.DATHOMIR)).broadcast()
}

static def handleDatNS(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(-3989d, 124d, -10d, Terrain.DATHOMIR)).broadcast()
}

static def handleDatNSvsSMC(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(-2457d, 117d, 1530d, Terrain.DATHOMIR)).broadcast()
}

static def handleDatQz(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(-5786d, 510d, -6554d, Terrain.DATHOMIR)).broadcast()
}

// Planet: Endor

static def handleEndJindaCave(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(-1714d, 31d, -8d, Terrain.ENDOR)).broadcast()
}

static def handleEndDwb(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(-4683d, 13d, 4326d, Terrain.ENDOR)).broadcast()
}

// Planet: Kashyyyk

static def handleKasEtyyy(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(275d, 48d, 503d, Terrain.KASHYYYK_HUNTING)).broadcast()
}

static def handleKasKachirho(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(146d, 19d, 162d, Terrain.KASHYYYK_MAIN)).broadcast()
}


static def handleKasKkowir(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(-164d, 16d, -262d, Terrain.KASHYYYK_DEAD_FOREST)).broadcast()
}

// Planet: Lok

static def handleLokDroidCave(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(3331d, 105d, -4912d, Terrain.LOK)).broadcast()
}

static def handleLokGreatMaze(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(3848d, 62d, -464d, Terrain.LOK)).broadcast()
}

static def handleLokImperialOutpost(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(-1914d, 11d, -3299d, Terrain.LOK)).broadcast()
}

static def handleLokKimogilaTown(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(-70d, 42d, 2769d, Terrain.LOK)).broadcast()
}

// Planet: Naboo

static def handleNabEmperorsRetreat(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(2535d, 295d, -3887d, Terrain.NABOO)).broadcast()
}

static def handleNabWeaponFac(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(-6439d, 41d, -3265d, Terrain.NABOO)).broadcast()
}

// Planet: Rori

static def handleRorHyperdriveFacility(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(-1211d, 98d, 4552d, Terrain.RORI)).broadcast()
}

// Planet: Talus

static def handleTalDetainmentCenter(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(4958d, 449d, -5983d, Terrain.TALUS)).broadcast()
}

// Planet: Tatooine

static def handleTatFortTusken(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(-3941d, 59d, 6318d, Terrain.TATOOINE)).broadcast()
}

static def handleTatKraytGrave(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(7380d, 122d, 4298d, Terrain.TATOOINE)).broadcast()
}

static def handleTatMosEisley(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(3525d, 4d, -4807d, Terrain.TATOOINE)).broadcast()
}

static def handleTatMosTaike(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(3684d, 7d, 2357d, Terrain.TATOOINE)).broadcast()
}

static def handleTatSquillCave(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(57d, 152d, -79d, Terrain.TATOOINE)).broadcast()
}

static def handleTatImperialOasis(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(-5458d, 10d, 2601d, Terrain.TATOOINE)).broadcast()
}

// Planet: Yavin 4

static def handleYavBlueleafTemple(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(-947d, 86d, -2131d, Terrain.YAVIN4)).broadcast()
}

static def handleYavExarKun(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(4928d, 103d, 5587d, Terrain.YAVIN4)).broadcast()
}

static def handleYavDarkEnclave(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(5107d, 81d, 301d, Terrain.YAVIN4)).broadcast()
}

static def handleYavLightEnclave(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(-5575d, 87d, 4902d, Terrain.YAVIN4)).broadcast()
}

static def handleYavGeoCave(Player player) {
	new ObjectTeleportIntent(player.getCreatureObject(), new Location(-6485d, 83d, -446d, Terrain.YAVIN4)).broadcast()
}