package com.projectswg.holocore.resources.support.objects.radial.terminal;

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
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.services.support.objects.ObjectStorageService;
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

				listBox.addListItem("TRAVEL - Fast Travel Locations");
				listBox.addListItem("ITEMS - Armor");
				listBox.addListItem("ITEMS - Weapons");
				listBox.addListItem("ITEMS - Wearables");
				listBox.addListItem("ITEMS - Tools");
				listBox.addListItem("ITEMS - Vehicles");
				listBox.addListItem("Credits");

				listBox.addCallback(SuiEvent.OK_PRESSED, "handleCategorySelection", (event, parameters) -> handleCategorySelection(player, parameters));
				listBox.display(player);
				break;
			}
		}
	}

	private static void handleCategorySelection(Player player, Map<String, String> parameters) {
		int selection = SuiListBox.getSelectedRow(parameters);

		switch (selection) {
			case 0: handleTravel(player); break;
			case 1: handleArmor(player); break;
			case 2: handleWeapons(player); break;
			case 3: handleWearables(player); break;
			case 4: handleTools(player); break;
			case 5: handleVehicles(player); break;
			case 6: handleCredits(player); break;
		}
	}

	private static void spawnItems(Player player, String ... items) {
		CreatureObject creature = player.getCreatureObject();
		SWGObject inventory = creature.getSlottedObject("inventory");

		new CreateStaticItemIntent(creature, inventory, new StaticItemService.LootBoxHandler(creature), items).broadcast();
	}

	private static void handleCredits(Player player) {
		CreatureObject creatureObject = player.getCreatureObject();
		int oneMillion = 1_000_000;
		creatureObject.setCashBalance(oneMillion);
		creatureObject.setBankBalance(oneMillion);
	}

	private static void handleArmor(Player player) {
		SuiListBox listBox = new SuiListBox(SuiButtons.OK_CANCEL, "Character Builder Terminal", "Select a set of armor to receive.");

		listBox.addListItem("Composite Armor");
		listBox.addListItem("Chitin Armor");
		listBox.addListItem("Padded Armor");
		listBox.addListItem("Bone Armor");
		listBox.addListItem("Mandalorian Armor");
		listBox.addListItem("Tantel Armor");
		listBox.addListItem("Ubese Armor");
		listBox.addListItem("R.I.S. Armor");
		listBox.addListItem("Ithorian Armor Sets");
		listBox.addListItem("Wookiee Armor Sets");
		listBox.addCallback(SuiEvent.OK_PRESSED, "handleArmorSelection", (event, parameters) -> handleArmorSelection(player, parameters));
		listBox.display(player);
	}

	private static void handleArmorSelection(Player player, Map<String, String> parameters) {
		switch (SuiListBox.getSelectedRow(parameters)) {
			case 0: handleCompositeArmor(player); break;
			case 1: handleChitinArmor(player); break;
			case 2: handlePaddedArmor(player); break;
			case 3: handleBoneArmor(player); break;
			case 4: handleMandalorianArmor(player); break;
			case 5: handleTantelArmor(player); break;
			case 6: handleUbeseArmor(player); break;
			case 7: handleRISArmor(player); break;
			case 8: handleIthorianArmorSets(player); break;
			case 9: handleWookieeArmorSets(player); break;
		}
	}

	private static void handleCompositeArmor(Player player) {
		spawnItems(player,
				"armor_composite_lvl1_bicep_l",
				"armor_composite_lvl1_bicep_r",
				"armor_composite_lvl1_boots",
				"armor_composite_lvl1_bracer_l",
				"armor_composite_lvl1_bracer_r",
				"armor_composite_lvl1_chest",
				"armor_composite_lvl1_gloves",
				"armor_composite_lvl1_helmet",
				"armor_composite_lvl1_leggings"
		);
	}

	private static void handleChitinArmor(Player player) {
		spawnItems(player,
				"armor_chitin_lvl1_bicep_l",
				"armor_chitin_lvl1_bicep_r",
				"armor_chitin_lvl1_boots",
				"armor_chitin_lvl1_bracer_l",
				"armor_chitin_lvl1_bracer_r",
				"armor_chitin_lvl1_chest",
				"armor_chitin_lvl1_gloves",
				"armor_chitin_lvl1_helmet",
				"armor_chitin_lvl1_leggings"
		);
	}

	private static void handlePaddedArmor(Player player) {
		spawnItems(player,
				"armor_padded_lvl1_bicep_l",
				"armor_padded_lvl1_bicep_r",
				"armor_padded_lvl1_boots",
				"armor_padded_lvl1_bracer_l",
				"armor_padded_lvl1_bracer_r",
				"armor_padded_lvl1_chest",
				"armor_padded_lvl1_gloves",
				"armor_padded_lvl1_helmet",
				"armor_padded_lvl1_leggings"
		);
	}

	private static void handleBoneArmor(Player player) {
		spawnItems(player,
				"armor_bone_lvl1_bicep_l",
				"armor_bone_lvl1_bicep_r",
				"armor_bone_lvl1_boots",
				"armor_bone_lvl1_bracer_l",
				"armor_bone_lvl1_bracer_r",
				"armor_bone_lvl1_chest",
				"armor_bone_lvl1_gloves",
				"armor_bone_lvl1_helmet",
				"armor_bone_lvl1_leggings"
		);
	}

	private static void handleMandalorianArmor(Player player) {
		spawnItems(player,
				"armor_mandalorian_lvl1_bicep_l",
				"armor_mandalorian_lvl1_bicep_r",
				"armor_mandalorian_lvl1_bracer_l",
				"armor_mandalorian_lvl1_bracer_r",
				"armor_mandalorian_lvl1_chest_plate",
				"armor_mandalorian_lvl1_gloves",
				"armor_mandalorian_lvl1_helmet",
				"armor_mandalorian_lvl1_leggings",
				"armor_mandalorian_lvl1_shoes"
		);
	}

	private static void handleTantelArmor(Player player) {
		spawnItems(player,
				"armor_tantel_lvl1_bicep_l",
				"armor_tantel_lvl1_bicep_r",
				"armor_tantel_lvl1_boots",
				"armor_tantel_lvl1_bracer_l",
				"armor_tantel_lvl1_bracer_r",
				"armor_tantel_lvl1_chest",
				"armor_tantel_lvl1_gloves",
				"armor_tantel_lvl1_helmet",
				"armor_tantel_lvl1_leggings"
		);
	}

	private static void handleUbeseArmor(Player player) {
		spawnItems(player,
				"armor_ubese_lvl1_boots",
				"armor_ubese_lvl1_bracer_l",
				"armor_ubese_lvl1_bracer_r",
				"armor_ubese_lvl1_chest",
				"armor_ubese_lvl1_gloves",
				"armor_ubese_lvl1_helmet",
				"armor_ubese_lvl1_leggings"
		);
	}

	private static void handleRISArmor(Player player) {
		spawnItems(player,
				"armor_ris_lvl1_bicep_l",
				"armor_ris_lvl1_bicep_r",
				"armor_ris_lvl1_boots",
				"armor_ris_lvl1_bracer_l",
				"armor_ris_lvl1_bracer_r",
				"armor_ris_lvl1_chest_plate",
				"armor_ris_lvl1_gloves",
				"armor_ris_lvl1_helmet",
				"armor_ris_lvl1_leggings"
		);
	}

	private static void handleIthorianArmorSets(Player player) {
		spawnItems(player,
				"armor_ithorian_recon_lvl1_bicep_l",
				"armor_ithorian_recon_lvl1_bicep_r",
				"armor_ithorian_recon_lvl1_boots",
				"armor_ithorian_recon_lvl1_bracer_l",
				"armor_ithorian_recon_lvl1_bracer_r",
				"armor_ithorian_recon_lvl1_chest",
				"armor_ithorian_recon_lvl1_gloves",
				"armor_ithorian_recon_lvl1_helmet",
				"armor_ithorian_recon_lvl1_leggings",
				"armor_ithorian_battle_lvl1_bicep_l",
				"armor_ithorian_battle_lvl1_bicep_r",
				"armor_ithorian_battle_lvl1_boots",
				"armor_ithorian_battle_lvl1_bracer_l",
				"armor_ithorian_battle_lvl1_bracer_r",
				"armor_ithorian_battle_lvl1_chest",
				"armor_ithorian_battle_lvl1_leggings",
				"armor_ithorian_battle_lvl1_gloves",
				"armor_ithorian_battle_lvl1_helmet",
				"armor_ithorian_assault_lvl1_bicep_l",
				"armor_ithorian_assault_lvl1_bicep_r",
				"armor_ithorian_assault_lvl1_boots",
				"armor_ithorian_assault_lvl1_bracer_l",
				"armor_ithorian_assault_lvl1_bracer_r",
				"armor_ithorian_assault_lvl1_chest",
				"armor_ithorian_assault_lvl1_gloves",
				"armor_ithorian_assault_lvl1_helmet",
				"armor_ithorian_assault_lvl1_leggings"
		);
	}

	private static void handleWookieeArmorSets(Player player) {
		spawnItems(player,
				"armor_wookiee_recon_lvl1_bicep_l",
				"armor_wookiee_recon_lvl1_bicep_r",
				"armor_wookiee_recon_lvl1_bracer_l",
				"armor_wookiee_recon_lvl1_bracer_r",
				"armor_wookiee_recon_lvl1_chest",
				"armor_wookiee_recon_lvl1_leggings",
				"armor_wookiee_battle_lvl1_bicep_l",
				"armor_wookiee_battle_lvl1_bicep_r",
				"armor_wookiee_battle_lvl1_bracer_l",
				"armor_wookiee_battle_lvl1_bracer_r",
				"armor_wookiee_battle_lvl1_chest",
				"armor_wookiee_battle_lvl1_leggings",
				"armor_wookiee_assault_lvl1_bicep_l",
				"armor_wookiee_assault_lvl1_bicep_r",
				"armor_wookiee_assault_lvl1_bracer_l",
				"armor_wookiee_assault_lvl1_bracer_r",
				"armor_wookiee_assault_lvl1_chest",
				"armor_wookiee_assault_lvl1_leggings"
		);
	}

	private static void handleWeapons(Player player) {
		SuiListBox listBox = new SuiListBox(SuiButtons.OK_CANCEL, "Character Builder Terminal", "Select a weapon category to receive a weapon of that type.");

		listBox.addListItem("Lightsaber");
		listBox.addListItem("Melee");
		listBox.addListItem("Ranged");

		listBox.addCallback(SuiEvent.OK_PRESSED, "handleWeaponSelection", (event, parameters) -> handleWeaponSelection(player, parameters));
		listBox.display(player);
	}

	private static void handleWeaponSelection(Player player, Map<String, String> parameters) {
		int selection = SuiListBox.getSelectedRow(parameters);

		switch (selection) {
			case 0: handleLightsaber(player); break;
			case 1: handleMelee(player); break;
			case 2: handleRanged(player); break;

		}
	}

	private static void handleLightsaber(Player player) {
		spawnItems(player,
				"test_low_polearm_saber",
				"test_low_1h_saber",
				"test_low_2h_saber",
				"test_medium_polearm_saber",
				"test_medium_1h_saber",
				"test_medium_2h_saber",
				"test_high_polearm_saber",
				"test_high_1h_saber",
				"test_high_2h_saber",
				"item_color_crystal_02_16",	// Bane's Heart
				"item_color_crystal_02_19",	// B'nar's Sacrifice
				"item_color_crystal_02_20",	// Windu's Guile
				"item_color_crystal_02_28",	// Kenobi's Legacy
				"item_color_crystal_02_29",	// Sunrider's Destiny
				"item_power_crystal_04_01",	// Power crystal
				"item_power_crystal_04_04",	// Power crystal
				"item_power_crystal_04_07",	// Power crystal
				"item_power_crystal_04_09",	// Power crystal
				"item_power_crystal_04_20"	// Power crystal
		);
	}

	private static void handleMelee(Player player) {
		spawnItems(player,
				"test_low_polearm",
				"test_low_unarmed",
				"test_low_1h",
				"test_low_2h",
				"test_medium_polearm",
				"test_medium_unarmed",
				"test_medium_1h",
				"test_medium_2h",
				"test_high_polearm",
				"test_high_unarmed",
				"test_high_1h",
				"test_high_2h"
		);
	}

	private static void handleRanged(Player player) {
		spawnItems(player,
				"test_low_pistol",
				"test_low_carbine",
				"test_low_rifle",
				"test_low_heavy",
				"test_medium_pistol",
				"test_medium_carbine",
				"test_medium_rifle",
				"test_medium_heavy",
				"test_high_pistol",
				"test_high_carbine",
				"test_high_rifle",
				"test_high_heavy"
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
			case 18: handleJediRobes(player); break;
			case 19: handleNightsisterEquipment(player); break;
			case 20: handleTuskenEquipment(player); break;
			case 21: handleWookieeEquipment(player); break;
		}
	}

	private static void handleBackpack(Player player) {
		spawnItems(player,
				"item_clothing_backpack_agi_lvl1_02_01",
				"item_clothing_backpack_con_lvl1_02_01",
				"item_clothing_backpack_lck_lvl1_02_01",
				"item_clothing_backpack_pre_lvl1_02_01"
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
				"item_clothing_boots_01_22"
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

	private static void handleJediRobes(Player player) {
		spawnItems(player,
				"item_jedi_robe_dark_03_01",
				"item_jedi_robe_light_03_01",
				"item_jedi_robe_04_01",
				"item_jedi_robe_04_02",
				"item_jedi_robe_dark_03_02",
				"item_jedi_robe_light_03_02",
				"item_jedi_robe_04_03",
				"item_jedi_robe_04_04",
				"item_jedi_robe_dark_03_03",
				"item_jedi_robe_dark_04_01",
				"item_jedi_robe_light_03_03",
				"item_jedi_robe_light_04_01",
				"item_jedi_robe_dark_04_02",
				"item_jedi_robe_dark_04_03",
				"item_jedi_robe_light_04_02",
				"item_jedi_robe_light_04_03",
				"item_jedi_robe_06_01",
				"item_jedi_robe_06_02",
				"item_jedi_robe_06_03",
				"item_jedi_robe_06_04",
				"item_jedi_robe_06_05",
				"item_jedi_robe_06_06",
				"item_jedi_robe_dark_04_04",
				"item_jedi_robe_light_04_04",
				"item_jedi_robe_dark_04_05",
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
				"item_clothing_wookiee_shoulder_pad_01_01",
				"item_clothing_wookiee_shoulder_pad_01_02",
				"item_clothing_wookiee_skirt_01_01",
				"item_clothing_wookiee_skirt_01_02",
				"item_clothing_wookiee_skirt_01_03",
				"item_clothing_wookiee_skirt_01_04"
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
				"survey_tool_liquid",
				"survey_tool_lumber",
				"survey_tool_mineral",
				"survey_tool_moisture",
				"survey_tool_solar",
				"survey_tool_wind"
		);
	}

	private static void handleTravel(Player player) {
		SuiListBox listBox = new SuiListBox(SuiButtons.OK_CANCEL, "Character Builder Terminal", "Select a location you want to get teleported to.");

		listBox.addListItem("Corellia - Stronghold");
		listBox.addListItem("Coreliia - Corsec Base");
		listBox.addListItem("Dantooine - Force Crystal Hunter's Cave");
		listBox.addListItem("Dantooine - Jedi Temple Ruins");
		listBox.addListItem("Dantooine - The Warren");
		listBox.addListItem("Dathomir - Imperial Prison");
		listBox.addListItem("Dathomir - Nightsister Stronghold");
		listBox.addListItem("Dathomir - Nightsister vs. Singing Moutain Clan");
		listBox.addListItem("Endor - DWB");
		listBox.addListItem("Endor - Jinda Cave");
		listBox.addListItem("Kashyyyk - Etyyy, The Hunting Grounds");
		listBox.addListItem("Kashyyyk - Kachirho, Slaver Camp");
		listBox.addListItem("Kashyyyk - Kkowir, The Dead Forest");
		listBox.addListItem("Kashyyyk - Rryatt Trail, 1");
		listBox.addListItem("Kashyyyk - Rryatt Trail, 2");
		listBox.addListItem("Kashyyyk - Rryatt Trail, 3");
		listBox.addListItem("Kashyyyk - Rryatt Trail, 4");
		listBox.addListItem("Kashyyyk - Rryatt Trail, 5");
		listBox.addListItem("Kashyyyk - Slaver");
		listBox.addListItem("Lok - Droid Cave");
		listBox.addListItem("Lok - Great Maze of Lok");
		listBox.addListItem("Lok - Imperial Outpost");
		listBox.addListItem("Lok - Kimogila Town");
		listBox.addListItem("Mustafar - Mensix Mining Facility");
		listBox.addListItem("Naboo - Emperor's Retreat");
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
		listBox.addListItem("Yavin 4 - Geonosian Cave");
		listBox.addListItem("Yavin 4 - Light Enclave");
		listBox.addListItem("[INSTANCE] - Myyyydril Cave");
		listBox.addListItem("[INSTANCE] - Avatar Platform (EASY)");
		listBox.addListItem("[INSTANCE] - Avatar Platform (MEDIUM)");
		listBox.addListItem("[INSTANCE] - Avatar Platform (HARD)");
		listBox.addListItem("[INSTANCE] - Mustafar Jedi Challenge (EASY)");
		listBox.addListItem("[INSTANCE] - Mustafar Jedi Challenge (MEDIUM)");
		listBox.addListItem("[INSTANCE] - Mustafar Jedi Challenge (HARD)");
		listBox.addListItem("[INVASION] - Droid Army");

		listBox.addCallback(SuiEvent.OK_PRESSED, "handleTravelSelection", (event, parameters) -> handleTravelSelection(player, parameters));
		listBox.display(player);
	}

	private static void handleTravelSelection(Player player, Map<String, String> parameters) {
		int selection = SuiListBox.getSelectedRow(parameters);

		switch (selection) {

			// Planet: Corellia
			case 0: handleCorStronghold(player); break;
			case 1: handleCorCorsecBase(player); break;
			// Planet: Dantooine
			case 2: handleDanCrystalCave(player); break;
			case 3: handleDanJediTemple(player); break;
			case 4: handleDanWarren(player); break;
			// Planet: Dathomir
			case 5: handleDatImperialPrison(player); break;
			case 6: handleDatNS(player); break;
			case 7: handleDatNSvsSMC(player); break;
			// Planet: Endor
			case 8: handleEndDwb(player); break;
			case 9: handleEndJindaCave(player); break;
			// Planet: Kashyyyk
			case 10: handleKasEtyyy(player); break;
			case 11: handleKasKachirho(player); break;
			case 12: handleKasKkowir(player); break;
			case 13: handleKasRryatt1(player); break;
			case 14: handleKasRryatt2(player); break;
			case 15: handleKasRryatt3(player); break;
			case 16: handleKasRryatt4(player); break;
			case 17: handleKasRryatt5(player); break;
			case 18: handleKasSlaver(player); break;
			// Planet: Lok
			case 19: handleLokDroidCave(player); break;
			case 20: handleLokGreatMaze(player); break;
			case 21: handleLokImperialOutpost(player); break;
			case 22: handleLokKimogilaTown(player); break;
			// Planet: Mustafar
			case 23: handleMusMensix(player); break;
			// Planet: Naboo
			case 24: handleNabEmperorsRetreat(player); break;
			// Planet: Rori
			case 25: handleRorHyperdriveFacility(player); break;
			// Planet: Talus
			case 26: handleTalDetainmentCenter(player); break;
			// Planet: Tatooine
			case 27: handleTatFortTusken(player); break;
			case 28: handleTatImperialOasis(player); break;
			case 29: handleTatKraytGrave(player); break;
			case 30: handleTatMosEisley(player); break;
			case 31: handleTatMosTaike(player); break;
			case 32: handleTatSquillCave(player); break;
			// Planet: Yavin 4
			case 33: handleYavBlueleafTemple(player); break;
			case 34: handleYavDarkEnclave(player); break;
			case 35: handleYavGeoCave(player); break;
			case 36: handleYavLightEnclave(player); break;
			// Dungeons:
			case 37: handleInstanceMyyydrilCave(player); break;
			case 38: handleInstanceAvatarPlatformEasy(player); break;
			case 39: handleInstanceAvatarPlatformMedium(player); break;
			case 40: handleInstanceAvatarPlatformHard(player); break;
			// Planet: Mustafar Jedi Challenge
			case 41: handleInstanceMusJediEasy(player); break;
			case 42: handleInstanceMusJediMedium(player); break;
			case 43: handleInstanceMusJediHard(player); break;
			// Invasion:
			case 44: handleInvasionMusDroidArmy(player); break;

		}
	}

// Planet: Corellia

	private static void handleCorStronghold(Player player) {
		teleportTo(player, 4735d, 26d, -5676d, Terrain.CORELLIA);
	}
	private static void handleCorCorsecBase(Player player) {
		teleportTo(player, 5137d, 16d, 1518d, Terrain.CORELLIA);
	}

// Planet: Dantooine

	private static void handleDanJediTemple(Player player) {
		teleportTo(player, 4078d, 10d, 5370d, Terrain.DANTOOINE);
	}
	private static void handleDanCrystalCave(Player player) {
		teleportTo(player, -6225d, 48d, 7381d, Terrain.DANTOOINE);
	}
	private static void handleDanWarren(Player player) {
		teleportTo(player, -564d, 1d, -3789d, Terrain.DANTOOINE);
	}

// Planet: Dathomir

	private static void handleDatImperialPrison(Player player) {teleportTo(player, -6079d, 132d, 971d, Terrain.DATHOMIR);}
	private static void handleDatNS(Player player) {
		teleportTo(player, -3989d, 124d, -10d, Terrain.DATHOMIR);
	}
	private static void handleDatNSvsSMC(Player player) {
		teleportTo(player, -2457d, 117d, 1530d, Terrain.DATHOMIR);
	}

// Planet: Endor

	private static void handleEndJindaCave(Player player) {
		teleportTo(player, -1714d, 31d, -8d, Terrain.ENDOR);
	}
	private static void handleEndDwb(Player player) {
		teleportTo(player, -4683d, 13d, 4326d, Terrain.ENDOR);
	}

// Planet: Kashyyyk

	private static void handleKasEtyyy(Player player) {
		teleportTo(player, 275d, 48d, 503d, Terrain.KASHYYYK_HUNTING);
	}
	private static void handleKasKachirho(Player player) {
		teleportTo(player, 146d, 19d, 162d, Terrain.KASHYYYK_MAIN);
	}
	private static void handleKasKkowir(Player player) {teleportTo(player, -164d, 16d, -262d, Terrain.KASHYYYK_DEAD_FOREST);}
	private static void handleKasRryatt1(Player player) {teleportTo(player, 534d, 173d, 82d, Terrain.KASHYYYK_RRYATT_TRAIL);}
	private static void handleKasRryatt2(Player player) {teleportTo(player, 1422d, 70d, 722d, Terrain.KASHYYYK_RRYATT_TRAIL);}
	private static void handleKasRryatt3(Player player) {teleportTo(player, 2526d, 182d, -278d, Terrain.KASHYYYK_RRYATT_TRAIL);}
	private static void handleKasRryatt4(Player player) {teleportTo(player, 768d, 141d, -439d, Terrain.KASHYYYK_RRYATT_TRAIL);}
	private static void handleKasRryatt5(Player player) {teleportTo(player, 2495d, -24d, -924d, Terrain.KASHYYYK_RRYATT_TRAIL);}
	private static void handleKasSlaver(Player player) {teleportTo(player, 561.8d, 22.8d, 1552.8d, Terrain.KASHYYYK_NORTH_DUNGEONS);}

// Planet: Lok

	private static void handleLokDroidCave(Player player) {
		teleportTo(player, 3331d, 105d, -4912d, Terrain.LOK);
	}
	private static void handleLokGreatMaze(Player player) {
		teleportTo(player, 3848d, 62d, -464d, Terrain.LOK);
	}
	private static void handleLokImperialOutpost(Player player) {
		teleportTo(player, -1914d, 11d, -3299d, Terrain.LOK);
	}
	private static void handleLokKimogilaTown(Player player) {
		teleportTo(player, -70d, 42d, 2769d, Terrain.LOK);
	}

// Planet: Mustafar

	private static void handleMusMensix(Player player) {
		teleportTo(player, -2489d, 230d, 1621d, Terrain.MUSTAFAR);
	}

	// Planet: Naboo

	private static void handleNabEmperorsRetreat(Player player) {
		teleportTo(player, 2535d, 295d, -3887d, Terrain.NABOO);
	}

// Planet: Rori

	private static void handleRorHyperdriveFacility(Player player) {teleportTo(player, -1211d, 98d, 4552d, Terrain.RORI);}

// Planet: Talus

	private static void handleTalDetainmentCenter(Player player) {teleportTo(player, 4958d, 449d, -5983d, Terrain.TALUS);}

// Planet: Tatooine

	private static void handleTatFortTusken(Player player) {
		teleportTo(player, -3941d, 59d, 6318d, Terrain.TATOOINE);
	}
	private static void handleTatKraytGrave(Player player) {
		teleportTo(player, 7380d, 122d, 4298d, Terrain.TATOOINE);
	}
	private static void handleTatMosEisley(Player player) {
		teleportTo(player, 3525d, 4d, -4807d, Terrain.TATOOINE);
	}
	private static void handleTatMosTaike(Player player) {
		teleportTo(player, 3684d, 7d, 2357d, Terrain.TATOOINE);
	}
	private static void handleTatSquillCave(Player player) {
		teleportTo(player, 57d, 152d, -79d, Terrain.TATOOINE);
	}
	private static void handleTatImperialOasis(Player player) {
		teleportTo(player, -5458d, 10d, 2601d, Terrain.TATOOINE);
	}

// Planet: Yavin 4

	private static void handleYavBlueleafTemple(Player player) {
		teleportTo(player, -947d, 86d, -2131d, Terrain.YAVIN4);
	}
	private static void handleYavDarkEnclave(Player player) {
		teleportTo(player, 5107d, 81d, 301d, Terrain.YAVIN4);
	}
	private static void handleYavLightEnclave(Player player) {
		teleportTo(player, -5575d, 87d, 4902d, Terrain.YAVIN4);
	}
	private static void handleYavGeoCave(Player player) {teleportTo(player, -6485d, 83d, -446d, Terrain.YAVIN4); }

// Dungeons:

	private static void handleInstanceMyyydrilCave(Player player) {teleportTo(player, "kas_pob_myyydril_1", 1, -5.2, -1.3, -5.3);}
	private static void handleInstanceAvatarPlatformEasy(Player player) {teleportTo(player, "kas_pob_avatar_1",  1, 103.2, 0.1, 21.7);}
	private static void handleInstanceAvatarPlatformMedium(Player player) {teleportTo(player, "kas_pob_avatar_2",  1, 103.2, 0.1, 21.7);}
	private static void handleInstanceAvatarPlatformHard(Player player) {teleportTo(player, "kas_pob_avatar_3",  1, 103.2, 0.1, 21.7);}
	private static void handleInstanceMusJediEasy(Player player) {teleportTo(player, 2209.8d, 74.8d, 6410.2d, Terrain.MUSTAFAR);	}
	private static void handleInstanceMusJediMedium(Player player) {teleportTo(player, 2195.1d, 74.8d, 4990.40d, Terrain.MUSTAFAR);	}
	private static void handleInstanceMusJediHard(Player player) {teleportTo(player, 2190.5d, 74.8d, 3564.8d, Terrain.MUSTAFAR);	}
	private static void handleInvasionMusDroidArmy(Player player) {teleportTo(player, 4908d, 24d, 6046d, Terrain.MUSTAFAR);}


	private static void teleportTo(Player player, double x, double y, double z, Terrain terrain) {
		player.getCreatureObject().moveToContainer(null, new Location(x, y, z, terrain));
	}

	private static void teleportTo(Player player, String buildoutTag, int cellNumber, double x, double y, double z) {
		BuildingObject building = ObjectStorageService.BuildingLookup.getBuildingByTag(buildoutTag);
		assert building != null : "building does not exist";
		CellObject cell = building.getCellByNumber(cellNumber);
		assert cell != null : "cell does not exist";
		player.getCreatureObject().moveToContainer(cell, new Location(x, y, z, building.getTerrain()));
	}

	private static void teleportTo(Player player, String buildoutTag, String cellName, double x, double y, double z) {
		BuildingObject building = ObjectStorageService.BuildingLookup.getBuildingByTag(buildoutTag);
		assert building != null : "building does not exist";
		CellObject cell = building.getCellByName(cellName);
		assert cell != null : "cell does not exist";
		player.getCreatureObject().moveToContainer(cell, new Location(x, y, z, building.getTerrain()));
	}


	private static void handleVehicles(Player player) {
		String [] items = new String[]{
				"object/tangible/deed/vehicle_deed/shared_barc_speeder_deed.iff",
				"object/tangible/deed/vehicle_deed/shared_landspeeder_ab1_deed.iff",
				"object/tangible/deed/vehicle_deed/shared_landspeeder_av21_deed.iff",
				"object/tangible/deed/vehicle_deed/shared_landspeeder_desert_skiff_deed.iff",
				"object/tangible/deed/vehicle_deed/shared_landspeeder_lava_skiff_deed.iff",
				"object/tangible/deed/vehicle_deed/shared_landspeeder_usv5_deed.iff",
				"object/tangible/deed/vehicle_deed/shared_landspeeder_v35_deed.iff",
				"object/tangible/deed/vehicle_deed/shared_speederbike_swoop_deed.iff",
				"object/tangible/deed/vehicle_deed/shared_landspeeder_xp38_deed.iff",
				"object/tangible/deed/vehicle_deed/shared_landspeeder_tantive4_deed.iff",
				"object/tangible/deed/vehicle_deed/shared_jetpack_deed.iff",
		};
		for (String item : items) {
			SWGObject deed = ObjectCreator.createObjectFromTemplate(item);
			deed.moveToContainer(player.getCreatureObject().getInventory());
			ObjectCreatedIntent.broadcast(deed);
		}
	}
}
