package com.projectswg.holocore.resources.support.objects.radial.terminal;

import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.data.radial.RadialItem;
import com.projectswg.common.data.radial.RadialOption;
import com.projectswg.common.data.sui.SuiEvent;
import com.projectswg.holocore.intents.support.objects.items.CreateStaticItemIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiButtons;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiListBox;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.radial.RadialHandlerInterface;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import com.projectswg.holocore.services.support.objects.items.StaticItemService;

import java.util.Collection;
import java.util.Map;

public class TerminalCharacterBuilderRadial implements RadialHandlerInterface {
	
	public TerminalCharacterBuilderRadial() {
		
	}
	
	@Override
	public void getOptions(Collection<RadialOption> options, Player player, SWGObject target) {
		options.add(RadialOption.create(RadialItem.ITEM_USE));
		options.add(RadialOption.createSilent(RadialItem.EXAMINE));
	}
	
	@Override
	public void handleSelection(Player player, SWGObject target, RadialItem selection) {
		switch (selection) {
			case ITEM_USE: {
				SuiListBox listBox = new SuiListBox(SuiButtons.OK_CANCEL, "Character Builder Terminal", "Select a category.");
				
				listBox.addListItem("Armor");
				listBox.addListItem("Weapons");
				listBox.addListItem("Wearables");
				listBox.addListItem("Tools");
				listBox.addListItem("Travel");
				listBox.addListItem("Vehicles");
				listBox.addListItem("Powerups");
				listBox.addListItem("Heroic Jewelry Sets");
				
				listBox.addCallback(SuiEvent.OK_PRESSED, "handleCategorySelection", (event, parameters) -> handleCategorySelection(player, parameters));
				listBox.display(player);
				break;
			}
		}
	}
	
	private static void handleCategorySelection(Player player, Map<String, String> parameters) {
		int selection = SuiListBox.getSelectedRow(parameters);
		
		switch (selection) {
			case 0: handleArmor(player); break;
			case 1: handleWeapons(player); break;
			case 2: handleWearables(player); break;
			case 3: handleTools(player); break;
			case 4: handleTravel(player); break;
			case 5: handleVehicles(player); break;
			case 6: handlePowerups(player); break;
			case 7: handleJewelrySets(player); break;
		}
	}
	
	private static void spawnItems(Player player, String ... items) {
		CreatureObject creature = player.getCreatureObject();
		SWGObject inventory = creature.getSlottedObject("inventory");
		
		new CreateStaticItemIntent(creature, inventory, new StaticItemService.LootBoxHandler(creature), items).broadcast();
	}
	
	private static void handleArmor(Player player) {
		SuiListBox listBox = new SuiListBox(SuiButtons.OK_CANCEL, "Character Builder Terminal", "Select a set of armor to receive.");
		
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
		listBox.addCallback(SuiEvent.OK_PRESSED, "handleArmorSelection", (event, parameters) -> handleArmorSelection(player, parameters));
		listBox.display(player);
	}
	
	private static void handleArmorSelection(Player player, Map<String, String> parameters) {
		switch (SuiListBox.getSelectedRow(parameters)) {
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
	
	private static void handleRisArmor(Player player) {
		spawnItems(player, 
				"armor_ris_bicep_l",
				"armor_ris_bicep_r",
				"armor_ris_boots",
				"armor_ris_bracer_l",
				"armor_ris_bracer_r",
				"armor_ris_chest_plate",
				"armor_ris_gloves",
				"armor_ris_helmet",
				"armor_ris_leggings"
		);
	}
	
	private static void handleMandoArmor(Player player) {
		spawnItems(player, 
				"armor_mandalorian_bicep_l",
				"armor_mandalorian_bicep_r",
				"armor_mandalorian_bracer_l",
				"armor_mandalorian_bracer_r",
				"armor_mandalorian_chest_plate",
				"armor_mandalorian_gloves",
				"armor_mandalorian_helmet",
				"armor_mandalorian_leggings",
				"armor_mandalorian_shoes"
		);
	}
	
	private static void handleBoneArmor(Player player) {
		spawnItems(player, 
				"armor_bone_bicep_l_02_01",
				"armor_bone_bicep_r_02_01",
				"armor_bone_boots_02_01",
				"armor_bone_bracer_l_02_01",
				"armor_bone_bracer_r_02_01",
				"armor_bone_chest_02_01",
				"armor_bone_gloves_02_01",
				"armor_bone_helmet_02_01",
				"armor_bone_leggings_02_01"
		);
	}
	
	private static void handleChitinArmor(Player player) {
		spawnItems(player, 
				"armor_assault_sta_lvl80_bicep_l_02_01",
				"armor_assault_sta_lvl80_bicep_r_02_01",
				"armor_assault_sta_lvl80_boots_02_01",
				"armor_assault_sta_lvl80_bracer_l_02_01",
				"armor_assault_sta_lvl80_bracer_r_02_01",
				"armor_assault_sta_lvl80_chest_02_01",
				"armor_assault_sta_lvl80_gloves_02_01",
				"armor_assault_sta_lvl80_helmet_02_01",
				"armor_assault_sta_lvl80_leggings_02_01"
		);
	}
	
	private static void handlePaddedArmor(Player player) {
		spawnItems(player, 
				"armor_tow_battle_bicep_l_03_01",
				"armor_tow_battle_bicep_r_03_01",
				"armor_tow_battle_boots_03_01",
				"armor_tow_battle_bracer_l_03_01",
				"armor_tow_battle_bracer_r_03_01",
				"armor_tow_battle_chest_03_01",
				"armor_tow_battle_gloves_03_01",
				"armor_tow_battle_helmet_03_01",
				"armor_tow_battle_leggings_03_01"
		);
	}
	
	private static void handleUbeseArmor(Player player) {
		spawnItems(player, 
				"armor_recon_sta_lvl80_boots_02_01",
				"armor_recon_sta_lvl80_bracer_l_02_01",
				"armor_recon_sta_lvl80_bracer_r_02_01",
				"armor_recon_sta_lvl80_chest_02_01",
				"armor_recon_sta_lvl80_gloves_02_01",
				"armor_recon_sta_lvl80_helmet_02_01",
				"armor_recon_sta_lvl80_leggings_02_01"
		);
	}
	
	private static void handleIthoriandefenderArmor(Player player) {
		spawnItems(player, 
				"armor_ithorian_defender_bicep_camo_l_04_01",
				"armor_ithorian_defender_bicep_camo_r_04_01",
				"armor_ithorian_defender_boots_camo_04_01",
				"armor_ithorian_defender_bracer_camo_l_04_01",
				"armor_ithorian_defender_bracer_camo_r_04_01",
				"armor_ithorian_defender_chest_plate_camo_04_01",
				"armor_ithorian_defender_gloves_camo_04_01",
				"armor_ithorian_defender_helmet_camo_04_01",
				"armor_ithorian_defender_leggings_camo_04_01"
		);
	}
	
	private static void handleWookieeblackmtnArmor(Player player) {
		spawnItems(player, 
				"armor_kashyyykian_black_mtn_bicep_camo_l_04_01",
				"armor_kashyyykian_black_mtn_bicep_camo_r_04_01",
				"armor_kashyyykian_black_mtn_bracer_camo_l_04_01",
				"armor_kashyyykian_black_mtn_bracer_camo_r_04_01",
				"armor_kashyyykian_black_mtn_chest_plate_camo_04_01",
				"armor_kashyyykian_black_mtn_leggings_camo_04_01"
		);
	}
	
	private static void handleDeathtrooperArmor(Player player) {
		spawnItems(player, 
				"armor_deathtrooper_bicep_l_04_01",
				"armor_deathtrooper_bicep_r_04_01",
				"armor_deathtrooper_boots_04_01",
				"armor_deathtrooper_bracer_l_04_01",
				"armor_deathtrooper_bracer_r_04_01",
				"armor_deathtrooper_chest_plate_04_01",
				"armor_deathtrooper_gloves_04_01",
				"armor_deathtrooper_helmet_04_01",
				"armor_deathtrooper_leggings_04_01"
		);
	}
	
	private static void handleImpBattlemarineArmor(Player player) {
		spawnItems(player, 
				"armor_gcw_imperial_galactic_marine_bicep_l",
				"armor_gcw_imperial_galactic_marine_bicep_r",
				"armor_gcw_imperial_galactic_marine_boots",
				"armor_gcw_imperial_galactic_marine_bracer_l",
				"armor_gcw_imperial_galactic_marine_bracer_r",
				"armor_gcw_imperial_galactic_marine_chest_plate",
				"armor_gcw_imperial_galactic_marine_gloves",
				"armor_gcw_imperial_galactic_marine_helmet",
				"armor_gcw_imperial_galactic_marine_leggings"
		);
	}
	
	private static void handleImpBattlewornscoutArmor(Player player) {
		spawnItems(player, 
				"armor_gcw_imperial_scouttrooper_bicep_l",
				"armor_gcw_imperial_scouttrooper_bicep_r",
				"armor_gcw_imperial_scouttrooper_boots",
				"armor_gcw_imperial_scouttrooper_bracer_l",
				"armor_gcw_imperial_scouttrooper_bracer_r",
				"armor_gcw_imperial_scouttrooper_chest_plate",
				"armor_gcw_imperial_scouttrooper_gloves",
				"armor_gcw_imperial_scouttrooper_helmet",
				"armor_gcw_imperial_scouttrooper_leggings"
		);
	}
	
	private static void handleImpBattlewornshockArmor(Player player) {
		spawnItems(player, 
				"armor_gcw_imperial_shocktrooper_bicep_l",
				"armor_gcw_imperial_shocktrooper_bicep_r",
				"armor_gcw_imperial_shocktrooper_boots",
				"armor_gcw_imperial_shocktrooper_bracer_l",
				"armor_gcw_imperial_shocktrooper_bracer_r",
				"armor_gcw_imperial_shocktrooper_chest_plate",
				"armor_gcw_imperial_shocktrooper_gloves",
				"armor_gcw_imperial_shocktrooper_helmet",
				"armor_gcw_imperial_shocktrooper_leggings"
		);
	}
	
	private static void handleImpBattlewornstormArmor(Player player) {
		spawnItems(player, 
				"armor_gcw_imperial_stormtrooper_bicep_l",
				"armor_gcw_imperial_stormtrooper_bicep_r",
				"armor_gcw_imperial_stormtrooper_boots",
				"armor_gcw_imperial_stormtrooper_bracer_l",
				"armor_gcw_imperial_stormtrooper_bracer_r",
				"armor_gcw_imperial_stormtrooper_chest_plate",
				"armor_gcw_imperial_stormtrooper_gloves",
				"armor_gcw_imperial_stormtrooper_helmet",
				"armor_gcw_imperial_stormtrooper_leggings"
		);
	}
	
	private static void handleRebBattlewornassaultArmor(Player player) {
		spawnItems(player, 
				"armor_gcw_rebel_assault_bicep_l",
				"armor_gcw_rebel_assault_bicep_r",
				"armor_gcw_rebel_assault_boots",
				"armor_gcw_rebel_assault_bracer_l",
				"armor_gcw_rebel_assault_bracer_r",
				"armor_gcw_rebel_assault_chest_plate",
				"armor_gcw_rebel_assault_gloves",
				"armor_gcw_rebel_assault_helmet",
				"armor_gcw_rebel_assault_leggings"
		);
	}
	
	private static void handleRebBattlewornbattleArmor(Player player) {
		spawnItems(player, 
				"armor_gcw_rebel_battle_bicep_l",
				"armor_gcw_rebel_battle_bicep_r",
				"armor_gcw_rebel_battle_boots",
				"armor_gcw_rebel_battle_bracer_l",
				"armor_gcw_rebel_battle_bracer_r",
				"armor_gcw_rebel_battle_chest_plate",
				"armor_gcw_rebel_battle_gloves",
				"armor_gcw_rebel_battle_helmet",
				"armor_gcw_rebel_battle_leggings"
		);
	}
	
	private static void handleRebBattlewornmarineArmor(Player player) {
		spawnItems(player, 
				"armor_gcw_rebel_marine_bicep_l",
				"armor_gcw_rebel_marine_bicep_r",
				"armor_gcw_rebel_marine_boots",
				"armor_gcw_rebel_marine_bracer_l",
				"armor_gcw_rebel_marine_bracer_r",
				"armor_gcw_rebel_marine_chest_plate",
				"armor_gcw_rebel_marine_gloves",
				"armor_gcw_rebel_marine_helmet",
				"armor_gcw_rebel_marine_leggings"
		);
	}
	
	private static void handleRebBattlewornspecforceArmor(Player player) {
		spawnItems(player, 
				"armor_gcw_rebel_specforce_bicep_l",
				"armor_gcw_rebel_specforce_bicep_r",
				"armor_gcw_rebel_specforce_boots",
				"armor_gcw_rebel_specforce_bracer_l",
				"armor_gcw_rebel_specforce_bracer_r",
				"armor_gcw_rebel_specforce_chest_plate",
				"armor_gcw_rebel_specforce_gloves",
				"armor_gcw_rebel_specforce_helmet",
				"armor_gcw_rebel_specforce_leggings"
		);
	}
	
	private static void handleImpBlackcrusaderArmor(Player player) {
		spawnItems(player, 
				"armor_mandalorian_imperial_black_bicep_l_04_01",
				"armor_mandalorian_imperial_black_bicep_r_04_01",
				"armor_mandalorian_imperial_black_boots_04_01",
				"armor_mandalorian_imperial_black_bracer_l_04_01",
				"armor_mandalorian_imperial_black_bracer_r_04_01",
				"armor_mandalorian_imperial_black_chest_plate_04_01",
				"armor_mandalorian_imperial_black_gloves_04_01",
				"armor_mandalorian_imperial_black_helmet_04_01",
				"armor_mandalorian_imperial_black_leggings_04_01"
		);
	}
	
	private static void handleRebBlackcrusaderArmor(Player player) {
		spawnItems(player, 
				"armor_mandalorian_rebel_black_bicep_l_04_01",
				"armor_mandalorian_rebel_black_bicep_r_04_01",
				"armor_mandalorian_rebel_black_boots_04_01",
				"armor_mandalorian_rebel_black_bracer_l_04_01",
				"armor_mandalorian_rebel_black_bracer_r_04_01",
				"armor_mandalorian_rebel_black_chest_plate_04_01",
				"armor_mandalorian_rebel_black_gloves_04_01",
				"armor_mandalorian_rebel_black_helmet_04_01",
				"armor_mandalorian_rebel_black_leggings_04_01"
		);
	}
	
	private static void handleImpBlackpvpspecopsArmor(Player player) {
		spawnItems(player, 
				"armor_pvp_spec_ops_imperial_black_bicep_l_05_01",
				"armor_pvp_spec_ops_imperial_black_bicep_r_05_01",
				"armor_pvp_spec_ops_imperial_black_boots_05_01",
				"armor_pvp_spec_ops_imperial_black_bracer_l_05_01",
				"armor_pvp_spec_ops_imperial_black_bracer_r_05_01",
				"armor_pvp_spec_ops_imperial_black_chest_plate_orange_pad_05_01",
				"armor_pvp_spec_ops_imperial_black_gloves_05_01",
				"armor_pvp_spec_ops_imperial_black_helmet_05_01",
				"armor_pvp_spec_ops_imperial_black_leggings_05_01"
		);
	}
	
	private static void handleImpWhitepvpspecopsArmor(Player player) {
		spawnItems(player, 
				"armor_pvp_spec_ops_imperial_white_bicep_l_05_01",
				"armor_pvp_spec_ops_imperial_white_bicep_r_05_01",
				"armor_pvp_spec_ops_imperial_white_boots_05_01",
				"armor_pvp_spec_ops_imperial_white_bracer_l_05_01",
				"armor_pvp_spec_ops_imperial_white_bracer_r_05_01",
				"armor_pvp_spec_ops_imperial_white_chest_plate_orange_pad_05_01",
				"armor_pvp_spec_ops_imperial_white_gloves_05_01",
				"armor_pvp_spec_ops_imperial_white_helmet_05_01",
				"armor_pvp_spec_ops_imperial_white_leggings_05_01"
		);
	}
	
	private static void handleImpSnowtrooperArmor(Player player) {
		spawnItems(player, 
				"armor_snowtrooper_bicep_l",
				"armor_snowtrooper_bicep_r",
				"armor_snowtrooper_boots",
				"armor_snowtrooper_bracer_l",
				"armor_snowtrooper_bracer_r",
				"armor_snowtrooper_chest_plate",
				"armor_snowtrooper_gloves",
				"armor_snowtrooper_helmet",
				"armor_snowtrooper_leggings"
		);
	}
	
	private static void handleImpForestCamoArmor(Player player) {
		spawnItems(player, 
				"armor_scouttrooper_bicep_camo_l_04_01",
				"armor_scouttrooper_bicep_camo_r_04_01",
				"armor_scouttrooper_boots_camo_04_01",
				"armor_scouttrooper_bracer_camo_l_04_01",
				"armor_scouttrooper_bracer_camo_r_04_01",
				"armor_scouttrooper_chest_plate_camo_04_01",
				"armor_scouttrooper_gloves_camo_04_01",
				"armor_scouttrooper_helmet_camo_04_01",
				"armor_scouttrooper_leggings_camo_04_01"
		);
	}
	
	private static void handleRebForestCamoArmor(Player player) {
		spawnItems(player, 
				"armor_rebel_assault_bicep_camo_l_04_01",
				"armor_rebel_assault_bicep_camo_r_04_01",
				"armor_rebel_assault_boots_camo_04_01",
				"armor_rebel_assault_bracer_camo_l_04_01",
				"armor_rebel_assault_bracer_camo_r_04_01",
				"armor_rebel_assault_chest_plate_camo_04_01",
				"armor_rebel_assault_gloves_camo_04_01",
				"armor_rebel_assault_helmet_camo_04_01",
				"armor_rebel_assault_leggings_camo_04_01"
		);
	}
	
	private static void handleRebGreenpvpspecopsArmor(Player player) {
		spawnItems(player, 
				"armor_pvp_spec_ops_rebel_black_green_bicep_l_05_01",
				"armor_pvp_spec_ops_rebel_black_green_bicep_r_05_01",
				"armor_pvp_spec_ops_rebel_black_green_boots_05_01",
				"armor_pvp_spec_ops_rebel_black_green_bracer_l_05_01",
				"armor_pvp_spec_ops_rebel_black_green_bracer_r_05_01",
				"armor_pvp_spec_ops_rebel_black_green_chest_plate_05_01",
				"armor_pvp_spec_ops_rebel_black_green_gloves_05_01",
				"armor_pvp_spec_ops_rebel_black_green_helmet_05_01",
				"armor_pvp_spec_ops_rebel_black_green_leggings_05_01"
		);
	}
	
	private static void handleRebAlliancecoldArmor(Player player) {
		spawnItems(player, 
				"armor_rebel_snow_bicep_l",
				"armor_rebel_snow_bicep_r",
				"armor_rebel_snow_boots",
				"armor_rebel_snow_bracer_l",
				"armor_rebel_snow_bracer_r",
				"armor_rebel_snow_chest_plate",
				"armor_rebel_snow_gloves",
				"armor_rebel_snow_helmet",
				"armor_rebel_snow_leggings"
		);
	}
	
	private static void handleWeapons(Player player) {
		SuiListBox listBox = new SuiListBox(SuiButtons.OK_CANCEL, "Character Builder Terminal", "Select a weapon category to receive a weapon of that type.");
		
		listBox.addListItem("Lightsabers");
		listBox.addListItem("Melee");
		listBox.addListItem("Ranged");
		
		listBox.addCallback(SuiEvent.OK_PRESSED, "handleWeaponSelection", (event, parameters) -> handleWeaponSelection(player, parameters));
		listBox.display(player);
	}
	
	private static void handleWeaponSelection(Player player, Map<String, String> parameters) {
		int selection = SuiListBox.getSelectedRow(parameters);
		
		switch (selection) {
			case 0: handleLightsabers(player); break;
			case 1: handleMelee(player); break;
			case 2: handleRanged(player); break;
		}
	}
	
	private static void handleLightsabers(Player player) {
		spawnItems(player, 
				"weapon_mandalorian_lightsaber_04_01",
				"weapon_npe_lightsaber_02_01",
				"weapon_npe_lightsaber_02_02",
				"weapon_roadmap_lightsaber_02_02"
		);
	}
	
	private static void handleMelee(Player player) {
		spawnItems(player, 
				"weapon_tow_blasterfist_04_01",
				"weapon_tow_sword_1h_05_02",
				"weapon_tow_sword_2h_05_02",
				"weapon_tow_polearm_05_01",
				"weapon_polearm_02_01",
				"weapon_polearm_04_01",
				"weapon_magna_guard_polearm_04_01",
				"weapon_content_polearm_tier_8_03_02",
				"weapon_quest_u10_knuckler_01_02"
		);
	}
	
	private static void handleRanged(Player player) {
		spawnItems(player, 
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
		);
	}
	
	private static void handleWearables(Player player) {
		SuiListBox listBox = new SuiListBox(SuiButtons.OK_CANCEL, "Character Builder Terminal", "Select a wearable category to receive a weapon of that type.");
		
		listBox.addListItem("Backpacks");
		listBox.addListItem("Bikinis");
		listBox.addListItem("Bodysuits");
		listBox.addListItem("Boots");
		listBox.addListItem("Bustiers");
		listBox.addListItem("Dress");
		listBox.addListItem("Gloves");
		listBox.addListItem("Goggles");
		listBox.addListItem("Hats");
		listBox.addListItem("Helmets");
		listBox.addListItem("Jackets");
		listBox.addListItem("Pants");
		listBox.addListItem("Robes");
		listBox.addListItem("Shirt");
		listBox.addListItem("Shoes");
		listBox.addListItem("Skirts");
		listBox.addListItem("Vest");
		listBox.addListItem("Ithorian equipment");
		listBox.addListItem("Jedi equipment");
		listBox.addListItem("Nightsister equipment");
		listBox.addListItem("Tusken Raider equipment");
		listBox.addListItem("Wookie equipment");
		listBox.addListItem("TCG");

		listBox.addCallback(SuiEvent.OK_PRESSED, "handleWearablesSelection", (event, parameters) -> handleWearablesSelection(player, parameters));
		listBox.display(player);
	}
	
	private static void handleWearablesSelection(Player player, Map<String, String> parameters) {
		int selection = SuiListBox.getSelectedRow(parameters);
		
		switch (selection) {
			case 0: handleBackpack(player); break;
			case 1: handleBikini(player); break;
			case 2: handleBodysuit(player); break;
			case 3: handleBoot(player); break;
			case 4: handleBustier(player); break;
			case 5: handleDress(player); break;
			case 6: handleGlove(player); break;
			case 7: handleGoggle(player); break;
			case 8: handleHat(player); break;
			case 9: handleHelmet(player); break;
			case 10: handleJacket(player); break;
			case 11: handlePant(player); break;
			case 12: handleRobe(player); break;
			case 13: handleShirt(player); break;
			case 14: handleShoe(player); break;
			case 15: handleSkirt(player); break;
			case 16: handleVest(player); break;
			case 17: handleIthorianEquipment(player); break;
			case 18: handleJediEquipment(player); break;
			case 19: handleNightsisterEquipment(player); break;
			case 20: handleTuskenEquipment(player); break;
			case 21: handleWookieeEquipment(player); break;
			case 22: handleOther(player); break;
		}
	}
	
	private static void handleBackpack(Player player) {
		spawnItems(player, 
				"item_content_backpack_rsf_02_01",
				"item_empire_day_imperial_sandtrooper_backpack",
				"item_empire_day_rebel_camoflauge_backpack",
				"item_event_gmf_backpack_01",
				"item_heroic_backpack_krayt_skull_01_01",
				"item_heroic_backpack_tauntaun_skull_01_01"
		);
	}
	
	private static void handleBikini(Player player) {
		spawnItems(player, 
				"item_clothing_bikini_01_01",
				"item_clothing_bikini_01_02",
				"item_clothing_bikini_01_03",
				"item_clothing_bikini_01_04",
				"item_clothing_bikini_leggings_01_01"
		);
	}
	
	private static void handleBodysuit(Player player) {
		spawnItems(player, 
				"item_clothing_bodysuit_at_at_01_01",
				"item_clothing_bodysuit_bwing_01_01",
				"item_clothing_bodysuit_tie_fighter_01_01",
				"item_clothing_bodysuit_trando_slaver_01_01"
		);
	}
	
	private static void handleBoot(Player player) {
		spawnItems(player, 
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
		);
	}
	
	private static void handleBustier(Player player) {
		spawnItems(player, 
				"item_clothing_bustier_01_01",
				"item_clothing_bustier_01_02",
				"item_clothing_bustier_01_03"
		);
	}
	
	private static void handleDress(Player player) {
		spawnItems(player, 
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
		);
	}
	
	private static void handleGlove(Player player) {
		spawnItems(player, 
				"item_clothing_gloves_01_02",
				"item_clothing_gloves_01_03",
				"item_clothing_gloves_01_06",
				"item_clothing_gloves_01_07",
				"item_clothing_gloves_01_10",
				"item_clothing_gloves_01_11",
				"item_clothing_gloves_01_12",
				"item_clothing_gloves_01_13",
				"item_clothing_gloves_01_14"
		);
	}
	
	private static void handleGoggle(Player player) {
		spawnItems(player, 
				"item_clothing_goggles_anniversary_01_01",
				"item_clothing_goggles_goggles_01_01",
				"item_clothing_goggles_goggles_01_02",
				"item_clothing_goggles_goggles_01_03",
				"item_clothing_goggles_goggles_01_04",
				"item_clothing_goggles_goggles_01_05",
				"item_clothing_goggles_goggles_01_06"
		);
	}
	
	private static void handleHat(Player player) {
		spawnItems(player, 
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
		);
	}
	
	private static void handleHelmet(Player player) {
		spawnItems(player, 
				"item_clothing_helmet_at_at_01_01",
				"item_clothing_helmet_fighter_blacksun_01_01",
				"item_clothing_helmet_fighter_imperial_01_01",
				"item_clothing_helmet_fighter_privateer_01_01",
				"item_clothing_helmet_fighter_rebel_01_01",
				"item_clothing_helmet_tie_fighter_01_01"
		);
	}
	
	private static void handleJacket(Player player) {
		spawnItems(player, 
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
		);
	}
	
	private static void handlePant(Player player) {
		spawnItems(player, 
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
		);
	}
	
	private static void handleRobe(Player player) {
		spawnItems(player, 
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
		);
	}
	
	private static void handleShirt(Player player) {
		spawnItems(player, 
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
		);
	}
	
	private static void handleShoe(Player player) {
		spawnItems(player, 
				"item_clothing_shoes_01_01",
				"item_clothing_shoes_01_02",
				"item_clothing_shoes_01_03",
				"item_clothing_shoes_01_07",
				"item_clothing_shoes_01_08",
				"item_clothing_shoes_01_09"
		);
	}
	
	private static void handleSkirt(Player player) {
		spawnItems(player, 
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
		);
	}
	
	private static void handleVest(Player player) {
		spawnItems(player, 
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
		);
	}
	
	private static void handleIthorianEquipment(Player player) {
		spawnItems(player, 
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
		);
	}
	
	private static void handleJediEquipment(Player player) {
		spawnItems(player, 
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
		);
	}
	
	private static void handleNightsisterEquipment(Player player) {
		spawnItems(player, 
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
		);
	}
	
	private static void handleTuskenEquipment(Player player) {
		spawnItems(player, 
				"item_clothing_bandolier_tusken_01_01",
				"item_clothing_bandolier_tusken_01_02",
				"item_clothing_bandolier_tusken_01_03",
				"item_clothing_boots_tusken_raider_01_01",
				"item_clothing_gloves_tusken_raider_01_01",
				"item_clothing_helmet_tusken_raider_01_01",
				"item_clothing_helmet_tusken_raider_01_02",
				"item_clothing_robe_tusken_raider_01_01",
				"item_clothing_robe_tusken_raider_01_02"
		);
	}
	
	private static void handleWookieeEquipment(Player player) {
		spawnItems(player, 
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
		);
	}
	
	private static void handleOther(Player player) {
		spawnItems(player, 
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
		);
	}
	
	private static void handleTools(Player player) {
		SuiListBox listBox = new SuiListBox(SuiButtons.OK_CANCEL, "Character Builder Terminal", "Select the tools you want to receive.");
		
		listBox.addListItem("Survey Tools");
		
		listBox.addCallback(SuiEvent.OK_PRESSED, "handleToolsSelection", (event, parameters) -> handleToolsSelection(player, parameters));
		listBox.display(player);
	}
	
	private static void handleToolsSelection(Player player, Map<String, String> parameters) {
		int selection = SuiListBox.getSelectedRow(parameters);
		
		switch (selection) {
			case 0: handleSurveyTools(player); break;
		}
	}
	
	private static void handleSurveyTools(Player player) {
		spawnItems(player, 
				"survey_tool_gas",
				"survey_tool_inorganic",
				"survey_tool_liquid",
				"survey_tool_lumber",
				"survey_tool_mineral",
				"survey_tool_moisture",
				"survey_tool_organic",
				"survey_tool_solar",
				"survey_tool_wind"
		);
	}
	
	private static void handleTravel(Player player) {
		SuiListBox listBox = new SuiListBox(SuiButtons.OK_CANCEL, "Character Builder Terminal", "Select a location you want to get teleported to.");
		
		listBox.addListItem("Corellia - Stronghold");
		listBox.addListItem("Coreliia - Corsec Base");
		listBox.addListItem("Corellia - Rebel Base with X-Wings");
		listBox.addListItem("Dantooine - Force Crystal Hunter's Cave");
		listBox.addListItem("Dantooine - Jedi Temple Ruins");
		listBox.addListItem("Dantooine - The Warren");
		listBox.addListItem("Dathomir - Imperial Prison");
		listBox.addListItem("Dathomir - Nightsister Stronghold");
		listBox.addListItem("Dathomir - Nightsister vs. Singing Moutain Clan");
		listBox.addListItem("Dathomir - Quarantine Zone");
		listBox.addListItem("Endor - DWB");
		listBox.addListItem("Endor - Jinda Cave");
		listBox.addListItem("Kashyyyk - Etyyy, The Hunting Grounds");
		listBox.addListItem("Kashyyyk - Kachirho, Slaver Camp");
		listBox.addListItem("Kashyyyk - Kkowir, The Dead Forest");
		listBox.addListItem("Lok - Droid Cave");
		listBox.addListItem("Lok - Great Maze of Lok");
		listBox.addListItem("Lok - Imperial Outpost");
		listBox.addListItem("Lok - Kimogila Town");
		listBox.addListItem("Naboo - Emperor's Retreat");
		listBox.addListItem("Naboo - Weapon Development Facility");
		listBox.addListItem("Rori - Hyperdrive Research Facility");
		listBox.addListItem("Talus - Detainment Center");
		listBox.addListItem("Tatooine - Fort Tusken");
		listBox.addListItem("Tatooine - Imperial Oasis");
		listBox.addListItem("Tatooine - Krayt Graveyard");
		listBox.addListItem("Tatooine - Mos Eisley");
		listBox.addListItem("Tatooine - Mos Taike");
		listBox.addListItem("Tatooine - Squill Cave");
		listBox.addListItem("Yavin 4 - Blueleaf Temple");
		listBox.addListItem("Yavin 4 - Dark Enclave");
		listBox.addListItem("Yavin 4 - Exar Kun");
		listBox.addListItem("Yavin 4 - Geonosian Cave");
		listBox.addListItem("Yavin 4 - Light Enclave");
		listBox.addListItem("Character Farm - (Static) AI Turret vs. AI NPCs");
		listBox.addListItem("Character Farm - (Loiter) AI Faction vs. AI Faction");
		listBox.addListItem("Character Farm - (Patrol) AI vs. AI");

		listBox.addCallback(SuiEvent.OK_PRESSED, "handleTravelSelection", (event, parameters) -> handleTravelSelection(player, parameters));
		listBox.display(player);
	}
	
	private static void handleTravelSelection(Player player, Map<String, String> parameters) {
		int selection = SuiListBox.getSelectedRow(parameters);
		
		switch (selection) {
		
		// Planet: Corellia
			case 0: handleCorStronghold(player); break;
			case 1: handleCorCorsecBase(player); break;
			case 2: handleCorRebelXwingBase(player); break;
		// Planet: Dantooine
			case 3: handleDanCrystalCave(player); break;
			case 4: handleDanJediTemple(player); break;
			case 5: handleDanWarren(player); break;
		// Planet: Dathomir
			case 6: handleDatImperialPrison(player); break;
			case 7: handleDatNS(player); break;
			case 8: handleDatNSvsSMC(player); break;
			case 9: handleDatQz(player); break;
		// Planet: Endor
			case 10: handleEndDwb(player); break;
			case 11: handleEndJindaCave(player); break;
		// Planet: Kashyyyk
			case 12: handleKasEtyyy(player); break;
			case 13: handleKasKachirho(player); break;
			case 14: handleKasKkowir(player); break;
		// Planet: Lok
			case 15: handleLokDroidCave(player); break;
			case 16: handleLokGreatMaze(player); break;
			case 17: handleLokImperialOutpost(player); break;
			case 18: handleLokKimogilaTown(player); break;
		// Planet: Naboo
			case 19: handleNabEmperorsRetreat(player); break;
			case 20: handleNabWeaponFac(player); break;
		// Planet: Rori
			case 21: handleRorHyperdriveFacility(player); break;
		// Planet: Talus
			case 22: handleTalDetainmentCenter(player); break;
		// Planet: Tatooine
			case 23: handleTatFortTusken(player); break;
			case 24: handleTatImperialOasis(player); break;
			case 25: handleTatKraytGrave(player); break;
			case 26: handleTatMosEisley(player); break;
			case 27: handleTatMosTaike(player); break;
			case 28: handleTatSquillCave(player); break;
		// Planet: Yavin 4
			case 29: handleYavBlueleafTemple(player); break;
			case 30: handleYavDarkEnclave(player); break;
			case 31: handleYavExarKun(player); break;
			case 32: handleYavGeoCave(player); break;
			case 33: handleYavLightEnclave(player); break;
		// Planet: Character Farm
			case 34: handleChfStatic(player); break;
			case 35: handleChfLoiter(player); break;
			case 36: handleChfPatrol(player); break;


		}
	}

// Planet: Corellia
	
	private static void handleCorStronghold(Player player) {
		player.getCreatureObject().moveToLocation(new Location(4735d, 26d, -5676d, Terrain.CORELLIA));
	}
	
	private static void handleCorCorsecBase(Player player) {
		player.getCreatureObject().moveToLocation(new Location(5137d, 16d, 1518d, Terrain.CORELLIA));
	}
	
	private static void handleCorRebelXwingBase(Player player) {
		player.getCreatureObject().moveToLocation(new Location(213d, 50d, 4533d, Terrain.CORELLIA));
	}

// Planet: Dantooine
	
	private static void handleDanJediTemple(Player player) {
		player.getCreatureObject().moveToLocation(new Location(4078d, 10d, 5370d, Terrain.DANTOOINE));
	}
	
	private static void handleDanCrystalCave(Player player) {
		player.getCreatureObject().moveToLocation(new Location(-6225d, 48d, 7381d, Terrain.DANTOOINE));
	}
	
	private static void handleDanWarren(Player player) {
		player.getCreatureObject().moveToLocation(new Location(-564d, 1d, -3789d, Terrain.DANTOOINE));
	}

// Planet: Dathomir
	
	private static void handleDatImperialPrison(Player player) {
		player.getCreatureObject().moveToLocation(new Location(-6079d, 132d, 971d, Terrain.DATHOMIR));
	}
	
	private static void handleDatNS(Player player) {
		player.getCreatureObject().moveToLocation(new Location(-3989d, 124d, -10d, Terrain.DATHOMIR));
	}
	
	private static void handleDatNSvsSMC(Player player) {
		player.getCreatureObject().moveToLocation(new Location(-2457d, 117d, 1530d, Terrain.DATHOMIR));
	}
	
	private static void handleDatQz(Player player) {
		player.getCreatureObject().moveToLocation(new Location(-5786d, 510d, -6554d, Terrain.DATHOMIR));
	}

// Planet: Endor
	
	private static void handleEndJindaCave(Player player) {
		player.getCreatureObject().moveToLocation(new Location(-1714d, 31d, -8d, Terrain.ENDOR));
	}
	
	private static void handleEndDwb(Player player) {
		player.getCreatureObject().moveToLocation(new Location(-4683d, 13d, 4326d, Terrain.ENDOR));
	}

// Planet: Kashyyyk
	
	private static void handleKasEtyyy(Player player) {
		player.getCreatureObject().moveToLocation(new Location(275d, 48d, 503d, Terrain.KASHYYYK_HUNTING));
	}
	
	private static void handleKasKachirho(Player player) {
		player.getCreatureObject().moveToLocation(new Location(146d, 19d, 162d, Terrain.KASHYYYK_MAIN));
	}
	
	
	private static void handleKasKkowir(Player player) {
		player.getCreatureObject().moveToLocation(new Location(-164d, 16d, -262d, Terrain.KASHYYYK_DEAD_FOREST));
	}

// Planet: Lok
	
	private static void handleLokDroidCave(Player player) {
		player.getCreatureObject().moveToLocation(new Location(3331d, 105d, -4912d, Terrain.LOK));
	}
	
	private static void handleLokGreatMaze(Player player) {
		player.getCreatureObject().moveToLocation(new Location(3848d, 62d, -464d, Terrain.LOK));
	}
	
	private static void handleLokImperialOutpost(Player player) {
		player.getCreatureObject().moveToLocation(new Location(-1914d, 11d, -3299d, Terrain.LOK));
	}
	
	private static void handleLokKimogilaTown(Player player) {
		player.getCreatureObject().moveToLocation(new Location(-70d, 42d, 2769d, Terrain.LOK));
	}

// Planet: Naboo
	
	private static void handleNabEmperorsRetreat(Player player) {
		player.getCreatureObject().moveToLocation(new Location(2535d, 295d, -3887d, Terrain.NABOO));
	}
	
	private static void handleNabWeaponFac(Player player) {
		player.getCreatureObject().moveToLocation(new Location(-6439d, 41d, -3265d, Terrain.NABOO));
	}

// Planet: Rori
	
	private static void handleRorHyperdriveFacility(Player player) {
		player.getCreatureObject().moveToLocation(new Location(-1211d, 98d, 4552d, Terrain.RORI));
	}

// Planet: Talus
	
	private static void handleTalDetainmentCenter(Player player) {
		player.getCreatureObject().moveToLocation(new Location(4958d, 449d, -5983d, Terrain.TALUS));
	}

// Planet: Tatooine
	
	private static void handleTatFortTusken(Player player) {
		player.getCreatureObject().moveToLocation(new Location(-3941d, 59d, 6318d, Terrain.TATOOINE));
	}
	
	private static void handleTatKraytGrave(Player player) {
		player.getCreatureObject().moveToLocation(new Location(7380d, 122d, 4298d, Terrain.TATOOINE));
	}
	
	private static void handleTatMosEisley(Player player) {
		player.getCreatureObject().moveToLocation(new Location(3525d, 4d, -4807d, Terrain.TATOOINE));
	}
	
	private static void handleTatMosTaike(Player player) {
		player.getCreatureObject().moveToLocation(new Location(3684d, 7d, 2357d, Terrain.TATOOINE));
	}
	
	private static void handleTatSquillCave(Player player) {
		player.getCreatureObject().moveToLocation(new Location(57d, 152d, -79d, Terrain.TATOOINE));
	}
	
	private static void handleTatImperialOasis(Player player) {
		player.getCreatureObject().moveToLocation(new Location(-5458d, 10d, 2601d, Terrain.TATOOINE));
	}

// Planet: Yavin 4
	
	private static void handleYavBlueleafTemple(Player player) {
		player.getCreatureObject().moveToLocation(new Location(-947d, 86d, -2131d, Terrain.YAVIN4));
	}
	
	private static void handleYavExarKun(Player player) {
		player.getCreatureObject().moveToLocation(new Location(4928d, 103d, 5587d, Terrain.YAVIN4));
	}
	
	private static void handleYavDarkEnclave(Player player) {
		player.getCreatureObject().moveToLocation(new Location(5107d, 81d, 301d, Terrain.YAVIN4));
	}
	
	private static void handleYavLightEnclave(Player player) {
		player.getCreatureObject().moveToLocation(new Location(-5575d, 87d, 4902d, Terrain.YAVIN4));
	}
	
	private static void handleYavGeoCave(Player player) {
		player.getCreatureObject().moveToLocation(new Location(-6485d, 83d, -446d, Terrain.YAVIN4));
	}
// Planet: Character Farm

	private static void handleChfStatic(Player player) {
		player.getCreatureObject().moveToLocation(new Location(550d, 0d, 0d, Terrain.CHARACTER_FARM));
	}

	private static void handleChfLoiter(Player player) {
		player.getCreatureObject().moveToLocation(new Location(15d, 0d, 0d, Terrain.CHARACTER_FARM));
	}

	private static void handleChfPatrol(Player player) {
		player.getCreatureObject().moveToLocation(new Location(1000d, 0d, 0d, Terrain.CHARACTER_FARM));
	}

	private static void handleVehicles(Player player) {
		String [] items = new String[]{
			"object/tangible/deed/vehicle_deed/shared_barc_speeder_deed.iff",
			"object/tangible/deed/vehicle_deed/shared_mustafar_panning_droid.iff",
			"object/tangible/deed/vehicle_deed/shared_landspeeder_ab1_deed.iff",
			"object/tangible/deed/vehicle_deed/shared_landspeeder_av21_deed.iff",
			"object/tangible/deed/vehicle_deed/shared_landspeeder_desert_skiff_deed.iff",
			"object/tangible/deed/vehicle_deed/shared_landspeeder_lava_skiff_deed.iff",
			"object/tangible/deed/vehicle_deed/shared_landspeeder_usv5_deed.iff",
			"object/tangible/deed/vehicle_deed/shared_landspeeder_v35_deed.iff",
			"object/tangible/deed/vehicle_deed/shared_speederbike_swoop_deed.iff",
			"object/tangible/deed/vehicle_deed/shared_landspeeder_xp38_deed.iff",
			"object/tangible/deed/vehicle_deed/shared_landspeeder_tantive4_deed.iff"
		};
		for (String item : items) {
			SWGObject deed = ObjectCreator.createObjectFromTemplate(item);
			deed.moveToContainer(player.getCreatureObject().getInventory());
			ObjectCreatedIntent.broadcast(deed);
		}
	}
	
	private static void handlePowerups(Player player) {
		spawnPowerup(player, "object/tangible/loot/generic_usable/shared_copper_battery_usuable.iff", "item_reverse_engineering_powerup_armor_02_01","constitution_modified", "100");	// Breastplate
		spawnPowerup(player, "object/tangible/loot/generic_usable/shared_chassis_blueprint_usuable.iff", "item_reverse_engineering_powerup_clothing_02_01", "constitution_modified", "100");	// Shirt
		spawnPowerup(player, "object/tangible/loot/generic_usable/shared_scope_weapon_generic.iff", "item_reverse_engineering_powerup_weapon_02_01", "constitution_modified", "100");	// Weapon
	}
	
	private static void handleJewelrySets(Player player) {
		SuiListBox listBox = new SuiListBox(SuiButtons.OK_CANCEL, "Character Builder Terminal", "Select a set of jewelry to receive.");
		
		listBox.addListItem("Heroism set");
		
		listBox.addCallback(SuiEvent.OK_PRESSED, "handleSetSelection", (event, parameters) -> handleJewelrySelection(player, parameters));
		listBox.display(player);
	}

	private static void handleJewelrySelection(Player player, Map<String, String> parameters) {
		int selection = SuiListBox.getSelectedRow(parameters);

		switch (selection) {
			case 0: handleHeroism(player); break;

		}
	}

	private static void handleHeroism(Player player) {
		spawnItems(player,
				"item_band_set_hero_01_01",
				"item_bracelet_l_set_hero_01_01",
				"item_bracelet_r_set_hero_01_01",
				"item_necklace_set_hero_01_01",
				"item_ring_set_hero_01_01"
		);
	}
	
	private static void spawnPowerup(Player player, String template, String stfKey, String modifier, String value) {
		TangibleObject powerup = (TangibleObject) ObjectCreator.createObjectFromTemplate(template);
		powerup.setStf("static_item_n", stfKey);
		powerup.setDetailStf(new StringId("static_item_d", stfKey));
		
		powerup.addAttribute("@stat_n:" + modifier, value);
		
		CreatureObject creature = player.getCreatureObject();
		SWGObject inventory = creature.getSlottedObject("inventory");
		
		powerup.setCounter(250);
		powerup.moveToContainer(inventory);
		
		ObjectCreatedIntent.broadcast(powerup);
	}
	
}
