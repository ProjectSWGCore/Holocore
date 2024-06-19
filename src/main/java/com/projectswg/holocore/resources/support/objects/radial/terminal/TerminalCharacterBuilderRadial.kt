/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package com.projectswg.holocore.resources.support.objects.radial.terminal

import com.projectswg.common.data.location.Location
import com.projectswg.common.data.location.Terrain
import com.projectswg.common.data.radial.RadialItem
import com.projectswg.common.data.radial.RadialOption
import com.projectswg.common.data.sui.SuiEvent
import com.projectswg.holocore.intents.gameplay.player.experience.GrantSkillIntent
import com.projectswg.holocore.intents.support.objects.CreateStaticItemIntent
import com.projectswg.holocore.intents.support.objects.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.global.zone.sui.SuiListBox
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.radial.RadialHandlerInterface
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.services.support.objects.ObjectStorageService
import com.projectswg.holocore.services.support.objects.items.StaticItemService.SystemMessageHandler

class TerminalCharacterBuilderRadial : RadialHandlerInterface {
	override fun getOptions(options: MutableCollection<RadialOption>, player: Player, target: SWGObject) {
		options.add(RadialOption.create(RadialItem.ITEM_USE))
		options.add(RadialOption.createSilent(RadialItem.EXAMINE))
	}

	override fun handleSelection(player: Player, target: SWGObject, selection: RadialItem) {
		if (selection == RadialItem.ITEM_USE) {
			SuiListBox().run {
				title = "Character Builder Terminal"
				prompt = "Select a category"

				addListItem("TRAVEL - Fast Travel Locations")
				addListItem("SKILLS - Grant skillboxes")
				addListItem("SKILLS - Unlock Force Sensitive")
				addListItem("ITEMS - Armor")
				addListItem("ITEMS - Weapons")
				addListItem("ITEMS - Wearables")
				addListItem("ITEMS - Vehicles")
				addListItem("ITEMS - Tools")
				addListItem("Credits")

				addCallback(SuiEvent.OK_PRESSED, "handleCategorySelection") { _: SuiEvent, parameters: Map<String, String> -> handleCategorySelection(player, parameters) }
				display(player)
			}
		}
	}

	companion object {
		private fun handleCategorySelection(player: Player, parameters: Map<String, String>) {
			val selection = SuiListBox.getSelectedRow(parameters)

			when (selection) {
				0 -> handleTravel(player)
				1 -> handleSkillsGrantBoxes(player)
				2 -> handleSkillsUnlockForceSensitive(player)
				3 -> handleArmor(player)
				4 -> handleWeapons(player)
				5 -> handleWearables(player)
				6 -> handleVehicles(player)
				7 -> handleTools(player)
				8 -> handleCredits(player)
			}
		}

		private fun handleSkillsGrantBoxes(player: Player) {
			SuiListBox().run {
				title = "Character Builder Terminal"
				prompt = "Select a skill you want to learn"

				addListItem("Social - Entertainer (Master)")
				addListItem("Social - Dancer (Novice)")
				addListItem("Melee - Brawler (Master)")
				addListItem("Melee - Fencer (Novice)")
				addListItem("Melee - Pikeman (Novice)")
				addListItem("Melee - Swordsman (Novice)")
				addListItem("Melee - Teras Kasi (Novice)")
				addListItem("Ranged - Marksman (Master)")
				addListItem("Ranged - Pistoleer (Novice)")
				addListItem("Ranged - Carbineer (Novice)")
				addListItem("Ranged - Rifleman (Novice)")
				addListItem("Ranged - Commando (Novice)")
				addListItem("Science - Medic (Master)")
				addListItem("Science - Combat Medic (Master)")
				addListItem("Science - Doctor (Master)")
				addListItem("Force Sensitive - Dark Jedi ranks (Ranks)")
				addListItem("Force Sensitive - Light Jedi ranks (Ranks)")
				addListItem("Force Sensitive - Master Force Defender (Master)")
				addListItem("Force Sensitive - Master Force Enhancer (Master)")
				addListItem("Force Sensitive - Master Force Healing (Master)")
				addListItem("Force Sensitive - Lightsaber Master (Master)")
				addListItem("Force Sensitive - Master Force Wielder (Master)")
				addListItem("Force Sensitive - Combat Prowess Master (Master)")
				addListItem("Force Sensitive - Crafting Mastery (Master)")
				addListItem("Force Sensitive - Enhanced Reflexes Master (Master)")
				addListItem("Force Sensitive - Heightened Senses Master (Master)")

				addCallback(SuiEvent.OK_PRESSED, "handleSkillsSelection") { _: SuiEvent, parameters: Map<String, String> -> handleSkillsSelection(player, parameters) }
				display(player)
			}
		}

		private fun handleSkillsSelection(player: Player, parameters: Map<String, String>) {
			val selection = SuiListBox.getSelectedRow(parameters)

			when (selection) {
				0  -> handleMasterEntertainer(player)
				1  -> handleNoviceDancer(player)
				2  -> handleMasterBrawler(player)
				3  -> handleNoviceFencer(player)
				4  -> handleNovicePikeman(player)
				5  -> handleNoviceSwordsman(player)
				6  -> handleNoviceTerasKasi(player)
				7  -> handleMasterMarksman(player)
				8  -> handleNovicePistoleer(player)
				9  -> handleNoviceCarbineer(player)
				10 -> handleNoviceRifleman(player)
				11 -> handleNoviceCommando(player)
				12 -> handleMasterMedic(player)
				13 -> handleMasterCombatMedic(player)
				14 -> handleMasterDoctor(player)
				15 -> handleJedi_1(player)
				16 -> handleJedi_2(player)
				17 -> handleJedi_3(player)
				18 -> handleJedi_4(player)
				19 -> handleJedi_5(player)
				20 -> handleJedi_6(player)
				21 -> handleJedi_7(player)
				22 -> handleJedi_8(player)
				23 -> handleJedi_9(player)
				24 -> handleJedi_10(player)
				25 -> handleJedi_11(player)
			}
		}

		private fun handleMasterEntertainer(player: Player) {
			val creatureObject = player.creatureObject
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "social_entertainer_master", creatureObject, true).broadcast()
		}

		private fun handleNoviceDancer(player: Player) {
			val creatureObject = player.creatureObject
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "social_dancer_novice", creatureObject, true).broadcast()
		}

		private fun handleMasterBrawler(player: Player) {
			val creatureObject = player.creatureObject
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "combat_brawler_master", creatureObject, true).broadcast()
		}

		private fun handleNoviceFencer(player: Player) {
			val creatureObject = player.creatureObject
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "combat_1hsword_novice", creatureObject, true).broadcast()
		}

		private fun handleNovicePikeman(player: Player) {
			val creatureObject = player.creatureObject
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "combat_polearm_novice", creatureObject, true).broadcast()
		}

		private fun handleNoviceSwordsman(player: Player) {
			val creatureObject = player.creatureObject
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "combat_2hsword_novice", creatureObject, true).broadcast()
		}

		private fun handleNoviceTerasKasi(player: Player) {
			val creatureObject = player.creatureObject
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "combat_unarmed_novice", creatureObject, true).broadcast()
		}

		private fun handleMasterMarksman(player: Player) {
			val creatureObject = player.creatureObject
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "combat_marksman_master", creatureObject, true).broadcast()
		}

		private fun handleNovicePistoleer(player: Player) {
			val creatureObject = player.creatureObject
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "combat_pistol_novice", creatureObject, true).broadcast()
		}

		private fun handleNoviceCarbineer(player: Player) {
			val creatureObject = player.creatureObject
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "combat_carbine_novice", creatureObject, true).broadcast()
		}

		private fun handleNoviceRifleman(player: Player) {
			val creatureObject = player.creatureObject
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "combat_rifleman_novice", creatureObject, true).broadcast()
		}

		private fun handleNoviceCommando(player: Player) {
			val creatureObject = player.creatureObject
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "combat_commando_novice", creatureObject, true).broadcast()
		}

		private fun handleMasterMedic(player: Player) {
			val creatureObject = player.creatureObject
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "science_medic_master", creatureObject, true).broadcast()
		}

		private fun handleMasterCombatMedic(player: Player) {
			val creatureObject = player.creatureObject
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "science_combatmedic_master", creatureObject, true).broadcast()
		}

		private fun handleMasterDoctor(player: Player) {
			val creatureObject = player.creatureObject
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "science_doctor_master", creatureObject, true).broadcast()
		}

		private fun handleJedi_1(player: Player) {
			val creatureObject = player.creatureObject
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "force_rank_dark_master", creatureObject, true).broadcast()
		}

		private fun handleJedi_2(player: Player) {
			val creatureObject = player.creatureObject
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "force_rank_light_master", creatureObject, true).broadcast()
		}

		private fun handleJedi_3(player: Player) {
			val creatureObject = player.creatureObject
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "force_discipline_defender_novice", creatureObject, true).broadcast()
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "force_discipline_defender_master", creatureObject, true).broadcast()
		}

		private fun handleJedi_4(player: Player) {
			val creatureObject = player.creatureObject
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "force_discipline_enhancements_novice", creatureObject, true).broadcast()
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "force_discipline_enhancements_master", creatureObject, true).broadcast()
		}

		private fun handleJedi_5(player: Player) {
			val creatureObject = player.creatureObject
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "force_discipline_healing_novice", creatureObject, true).broadcast()
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "force_discipline_healing_master", creatureObject, true).broadcast()
		}

		private fun handleJedi_6(player: Player) {
			val creatureObject = player.creatureObject
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "force_discipline_light_saber_novice", creatureObject, true).broadcast()
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "force_discipline_light_saber_master", creatureObject, true).broadcast()
		}

		private fun handleJedi_7(player: Player) {
			val creatureObject = player.creatureObject
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "force_discipline_powers_novice", creatureObject, true).broadcast()
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "force_discipline_powers_master", creatureObject, true).broadcast()
		}

		private fun handleJedi_8(player: Player) {
			val creatureObject = player.creatureObject
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "force_sensitive_combat_prowess_novice", creatureObject, true).broadcast()
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "force_sensitive_combat_prowess_master", creatureObject, true).broadcast()
		}

		private fun handleJedi_9(player: Player) {
			val creatureObject = player.creatureObject
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "force_sensitive_crafting_mastery_novice", creatureObject, true).broadcast()
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "force_sensitive_crafting_mastery_master", creatureObject, true).broadcast()
		}

		private fun handleJedi_10(player: Player) {
			val creatureObject = player.creatureObject
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "force_sensitive_enhanced_reflexes_novice", creatureObject, true).broadcast()
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "force_sensitive_enhanced_reflexes_master", creatureObject, true).broadcast()
		}

		private fun handleJedi_11(player: Player) {
			val creatureObject = player.creatureObject
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "force_sensitive_heightened_senses_novice", creatureObject, true).broadcast()
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "force_sensitive_heightened_senses_master", creatureObject, true).broadcast()
		}

		private fun handleSkillsUnlockForceSensitive(player: Player) {
			val creatureObject = player.creatureObject
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "force_title_jedi_rank", creatureObject, true).broadcast()
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "force_title_jedi_rank_01", creatureObject, true).broadcast()
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "force_title_jedi_rank_02", creatureObject, true).broadcast()
			spawnItems(
				player, "item_color_crystal_02_28", "item_power_crystal_04_19", "item_power_crystal_04_19", "item_power_crystal_04_19", "item_power_crystal_04_19", "item_color_crystal_02_00", "item_color_crystal_02_01", "item_color_crystal_02_02", "item_color_crystal_02_03", "item_color_crystal_02_04", "item_color_crystal_02_05", "item_color_crystal_02_06", "item_color_crystal_02_07", "item_color_crystal_02_08", "item_color_crystal_02_09", "item_color_crystal_02_10", "item_color_crystal_02_11", "weapon_cl30_1h_ls", "weapon_cl40_1h_ls", "weapon_cl50_1h_ls", "weapon_cl60_1h_ls", "weapon_cl70_1h_ls", "weapon_cl80_1h_ls", "weapon_cl30_2h_ls", "weapon_cl40_2h_ls", "weapon_cl50_2h_ls", "weapon_cl60_2h_ls", "weapon_cl70_2h_ls", "weapon_cl80_2h_ls", "weapon_cl30_polearm_ls", "weapon_cl40_polearm_ls", "weapon_cl50_polearm_ls", "weapon_cl60_polearm_ls", "weapon_cl70_polearm_ls", "weapon_cl80_polearm_ls"
			)
		}

		private fun spawnItems(player: Player, vararg items: String) {
			val creature = player.creatureObject
			val inventory = creature.getSlottedObject("inventory")

			CreateStaticItemIntent(creature, inventory, SystemMessageHandler(creature), *items).broadcast()
		}

		private fun handleCredits(player: Player) {
			val creatureObject = player.creatureObject
			val oneMillion = 1000000
			creatureObject.setCashBalance(oneMillion.toLong())
			creatureObject.setBankBalance(oneMillion.toLong())
		}

		private fun handleArmor(player: Player) {
			SuiListBox().run {
				title = "Character Builder Terminal"
				prompt = "Select a set of armor to receive."

				addListItem("Assault Armor - Basic")
				addListItem("Assault Armor - Standard")
				addListItem("Assault Armor - Advanced")
				addListItem("Battle Armor - Basic")
				addListItem("Battle Armor - Standard")
				addListItem("Battle Armor - Advanced")
				addListItem("Recon Armor - Basic")
				addListItem("Recon Armor - Standard")
				addListItem("Recon Armor - Advanced")
				addListItem("Special Armor - RIS")
				addListItem("Special Armor - Mandalorian")
				addListItem("Special Armor - Ithorian")
				addListItem("Special Armor - Wookiee")
				addCallback(SuiEvent.OK_PRESSED, "handleArmorSelection") { _: SuiEvent, parameters: Map<String, String> -> handleArmorSelection(player, parameters) }
				display(player)
			}
		}

		private fun handleArmorSelection(player: Player, parameters: Map<String, String>) {
			when (SuiListBox.getSelectedRow(parameters)) {
				0  -> handleAssaultArmorBasic(player)
				1  -> handleAssaultArmorStandard(player)
				2  -> handleAssaultArmorAdvanced(player)
				3  -> handleBattleArmorBasic(player)
				4  -> handleBattleArmorStandard(player)
				5  -> handleBattleArmorAdvanced(player)
				6  -> handleReconArmorBasic(player)
				7  -> handleReconArmorStandard(player)
				8  -> handleReconArmorAdvanced(player)
				9  -> handleRISArmor(player)
				10 -> handleMandalorianArmor(player)
				11 -> handleIthorianArmor(player)
				12 -> handleWookieeArmor(player)
			}
		}

		private fun handleAssaultArmorBasic(player: Player) {
			spawnItems(
				player, "armor_assault_agi_lvl20_bicep_l_02_01", "armor_assault_agi_lvl20_bicep_r_02_01", "armor_assault_agi_lvl20_boots_02_01", "armor_assault_agi_lvl20_bracer_l_02_01", "armor_assault_agi_lvl20_bracer_r_02_01", "armor_assault_agi_lvl20_chest_02_01", "armor_assault_agi_lvl20_gloves_02_01", "armor_assault_agi_lvl20_helmet_02_01", "armor_assault_agi_lvl20_leggings_02_01"
			)
		}

		private fun handleAssaultArmorStandard(player: Player) {
			spawnItems(
				player, "armor_assault_agi_lvl50_bicep_l_02_01", "armor_assault_agi_lvl50_bicep_r_02_01", "armor_assault_agi_lvl50_boots_02_01", "armor_assault_agi_lvl50_bracer_l_02_01", "armor_assault_agi_lvl50_bracer_r_02_01", "armor_assault_agi_lvl50_chest_02_01", "armor_assault_agi_lvl50_gloves_02_01", "armor_assault_agi_lvl50_helmet_02_01", "armor_assault_agi_lvl50_leggings_02_01"
			)
		}

		private fun handleAssaultArmorAdvanced(player: Player) {
			spawnItems(
				player, "armor_assault_agi_lvl80_bicep_l_02_01", "armor_assault_agi_lvl80_bicep_r_02_01", "armor_assault_agi_lvl80_boots_02_01", "armor_assault_agi_lvl80_bracer_l_02_01", "armor_assault_agi_lvl80_bracer_r_02_01", "armor_assault_agi_lvl80_chest_02_01", "armor_assault_agi_lvl80_gloves_02_01", "armor_assault_agi_lvl80_helmet_02_01", "armor_assault_agi_lvl80_leggings_02_01"
			)
		}

		private fun handleBattleArmorBasic(player: Player) {
			spawnItems(
				player, "armor_battle_agi_lvl20_bicep_l_02_01", "armor_battle_agi_lvl20_bicep_r_02_01", "armor_battle_agi_lvl20_boots_02_01", "armor_battle_agi_lvl20_bracer_l_02_01", "armor_battle_agi_lvl20_bracer_r_02_01", "armor_battle_agi_lvl20_chest_02_01", "armor_battle_agi_lvl20_gloves_02_01", "armor_battle_agi_lvl20_helmet_02_01", "armor_battle_agi_lvl20_leggings_02_01"
			)
		}

		private fun handleBattleArmorStandard(player: Player) {
			spawnItems(
				player, "armor_battle_agi_lvl50_bicep_l_02_01", "armor_battle_agi_lvl50_bicep_r_02_01", "armor_battle_agi_lvl50_boots_02_01", "armor_battle_agi_lvl50_bracer_l_02_01", "armor_battle_agi_lvl50_bracer_r_02_01", "armor_battle_agi_lvl50_chest_02_01", "armor_battle_agi_lvl50_gloves_02_01", "armor_battle_agi_lvl50_helmet_02_01", "armor_battle_agi_lvl50_leggings_02_01"
			)
		}

		private fun handleBattleArmorAdvanced(player: Player) {
			spawnItems(
				player, "armor_battle_agi_lvl80_bicep_l_02_01", "armor_battle_agi_lvl80_bicep_r_02_01", "armor_battle_agi_lvl80_boots_02_01", "armor_battle_agi_lvl80_bracer_l_02_01", "armor_battle_agi_lvl80_bracer_r_02_01", "armor_battle_agi_lvl80_chest_02_01", "armor_battle_agi_lvl80_gloves_02_01", "armor_battle_agi_lvl80_helmet_02_01", "armor_battle_agi_lvl80_leggings_02_01"
			)
		}

		private fun handleReconArmorBasic(player: Player) {
			spawnItems(
				player, "armor_recon_agi_lvl20_bicep_l_02_01", "armor_recon_agi_lvl20_bicep_r_02_01", "armor_recon_agi_lvl20_boots_02_01", "armor_recon_agi_lvl20_bracer_l_02_01", "armor_recon_agi_lvl20_bracer_r_02_01", "armor_recon_agi_lvl20_chest_02_01", "armor_recon_agi_lvl20_gloves_02_01", "armor_recon_agi_lvl20_helmet_02_01", "armor_recon_agi_lvl20_leggings_02_01"
			)
		}

		private fun handleReconArmorStandard(player: Player) {
			spawnItems(
				player, "armor_recon_agi_lvl50_bicep_l_02_01", "armor_recon_agi_lvl50_bicep_r_02_01", "armor_recon_agi_lvl50_boots_02_01", "armor_recon_agi_lvl50_bracer_l_02_01", "armor_recon_agi_lvl50_bracer_r_02_01", "armor_recon_agi_lvl50_chest_02_01", "armor_recon_agi_lvl50_gloves_02_01", "armor_recon_agi_lvl50_helmet_02_01", "armor_recon_agi_lvl50_leggings_02_01"
			)
		}

		private fun handleReconArmorAdvanced(player: Player) {
			spawnItems(
				player, "armor_recon_agi_lvl80_bicep_l_02_01", "armor_recon_agi_lvl80_bicep_r_02_01", "armor_recon_agi_lvl80_boots_02_01", "armor_recon_agi_lvl80_bracer_l_02_01", "armor_recon_agi_lvl80_bracer_r_02_01", "armor_recon_agi_lvl80_chest_02_01", "armor_recon_agi_lvl80_gloves_02_01", "armor_recon_agi_lvl80_helmet_02_01", "armor_recon_agi_lvl80_leggings_02_01"
			)
		}


		private fun handleMandalorianArmor(player: Player) {
			spawnItems(
				player, "armor_mandalorian_bicep_l", "armor_mandalorian_bicep_r", "armor_mandalorian_bracer_l", "armor_mandalorian_bracer_r", "armor_mandalorian_chest_plate", "armor_mandalorian_gloves", "armor_mandalorian_helmet", "armor_mandalorian_leggings", "armor_mandalorian_shoes"
			)
		}


		private fun handleRISArmor(player: Player) {
			spawnItems(
				player, "armor_ris_bicep_l", "armor_ris_bicep_r", "armor_ris_boots", "armor_ris_bracer_l", "armor_ris_bracer_r", "armor_ris_chest_plate", "armor_ris_gloves", "armor_ris_helmet", "armor_ris_leggings"
			)
		}

		private fun handleIthorianArmor(player: Player) {
			spawnItems(
				player, "armor_ithorian_recon_bicep_l", "armor_ithorian_recon_bicep_r", "armor_ithorian_recon_boots", "armor_ithorian_recon_bracer_l", "armor_ithorian_recon_bracer_r", "armor_ithorian_recon_chest", "armor_ithorian_recon_gloves", "armor_ithorian_recon_helmet", "armor_ithorian_recon_leggings", "armor_ithorian_battle_bicep_l", "armor_ithorian_battle_bicep_r", "armor_ithorian_battle_boots", "armor_ithorian_battle_bracer_l", "armor_ithorian_battle_bracer_r", "armor_ithorian_battle_chest", "armor_ithorian_battle_leggings", "armor_ithorian_battle_gloves", "armor_ithorian_battle_helmet", "armor_ithorian_assault_bicep_l", "armor_ithorian_assault_bicep_r", "armor_ithorian_assault_boots", "armor_ithorian_assault_bracer_l", "armor_ithorian_assault_bracer_r", "armor_ithorian_assault_chest", "armor_ithorian_assault_gloves", "armor_ithorian_assault_helmet", "armor_ithorian_assault_leggings"
			)
		}

		private fun handleWookieeArmor(player: Player) {
			spawnItems(
				player, "armor_wookiee_recon_bicep_l", "armor_wookiee_recon_bicep_r", "armor_wookiee_recon_bracer_l", "armor_wookiee_recon_bracer_r", "armor_wookiee_recon_chest", "armor_wookiee_recon_leggings", "armor_wookiee_battle_bicep_l", "armor_wookiee_battle_bicep_r", "armor_wookiee_battle_bracer_l", "armor_wookiee_battle_bracer_r", "armor_wookiee_battle_chest", "armor_wookiee_battle_leggings", "armor_wookiee_assault_bicep_l", "armor_wookiee_assault_bicep_r", "armor_wookiee_assault_bracer_l", "armor_wookiee_assault_bracer_r", "armor_wookiee_assault_chest", "armor_wookiee_assault_leggings"
			)
		}

		private fun handleWeapons(player: Player) {
			SuiListBox().run {
				title = "Character Builder Terminal"
				prompt = "Select a weapon category to receive a weapon of that type."

				addListItem("CL  1 - Melee/Ranged")
				addListItem("CL 10 - Melee/Ranged")
				addListItem("CL 20 - Melee/Ranged")
				addListItem("CL 30 - Melee/Ranged")
				addListItem("CL 40 - Melee/Ranged")
				addListItem("CL 50 - Melee/Ranged")

				addCallback(SuiEvent.OK_PRESSED, "handleWeaponSelection") { _: SuiEvent, parameters: Map<String, String> -> handleWeaponSelection(player, parameters) }
				display(player)
			}
		}

		private fun handleWeaponSelection(player: Player, parameters: Map<String, String>) {
			val selection = SuiListBox.getSelectedRow(parameters)

			when (selection) {
				0 -> handlecl1(player)
				1 -> handlecl10(player)
				2 -> handlecl20(player)
				3 -> handlecl30(player)
				4 -> handlecl40(player)
				5 -> handlecl50(player)
			}
		}


		private fun handlecl1(player: Player) {
			spawnItems(
				player, "weapon_cl1_1h", "weapon_cl1_2h", "weapon_cl1_carbine", "weapon_cl1_heavy", "weapon_cl1_pistol", "weapon_cl1_polearm", "weapon_cl1_rifle", "weapon_cl1_unarmed"
			)
		}

		private fun handlecl10(player: Player) {
			spawnItems(
				player, "weapon_cl10_1h", "weapon_cl10_2h", "weapon_cl10_carbine", "weapon_cl10_heavy", "weapon_cl10_pistol", "weapon_cl10_polearm", "weapon_cl10_rifle", "weapon_cl10_unarmed"
			)
		}

		private fun handlecl20(player: Player) {
			spawnItems(
				player, "weapon_cl20_1h", "weapon_cl20_2h", "weapon_cl20_carbine", "weapon_cl20_heavy", "weapon_cl20_pistol", "weapon_cl20_polearm", "weapon_cl20_rifle", "weapon_cl20_unarmed"
			)
		}

		private fun handlecl30(player: Player) {
			spawnItems(
				player, "weapon_cl30_1h", "weapon_cl30_2h", "weapon_cl30_carbine", "weapon_cl30_heavy", "weapon_cl30_pistol", "weapon_cl30_polearm", "weapon_cl30_rifle", "weapon_cl30_unarmed"
			)
		}

		private fun handlecl40(player: Player) {
			spawnItems(
				player, "weapon_cl40_1h", "weapon_cl40_2h", "weapon_cl40_carbine", "weapon_cl40_heavy", "weapon_cl40_pistol", "weapon_cl40_polearm", "weapon_cl40_rifle", "weapon_cl40_unarmed"
			)
		}

		private fun handlecl50(player: Player) {
			spawnItems(
				player, "weapon_cl50_1h", "weapon_cl50_2h", "weapon_cl50_carbine", "weapon_cl50_heavy", "weapon_cl50_pistol", "weapon_cl50_polearm", "weapon_cl50_rifle", "weapon_cl50_unarmed"
			)
		}

		private fun handleWearables(player: Player) {
			SuiListBox().run {
				title = "Character Builder Terminal"
				prompt = "Select a wearable category to receive a weapon of that type."

				addListItem("Backpacks")
				addListItem("Bikinis")
				addListItem("Bodysuits")
				addListItem("Boots")
				addListItem("Bustiers")
				addListItem("Dress")
				addListItem("Gloves")
				addListItem("Goggles")
				addListItem("Hats")
				addListItem("Helmets")
				addListItem("Jackets")
				addListItem("Pants")
				addListItem("Robes")
				addListItem("Shirt")
				addListItem("Shoes")
				addListItem("Skirts")
				addListItem("Vest")
				addListItem("Ithorian equipment")
				addListItem("Nightsister equipment")
				addListItem("Tusken Raider equipment")
				addListItem("Wookie equipment")

				addCallback(SuiEvent.OK_PRESSED, "handleWearablesSelection") { _: SuiEvent, parameters: Map<String, String> -> handleWearablesSelection(player, parameters) }
				display(player)
			}
		}

		private fun handleWearablesSelection(player: Player, parameters: Map<String, String>) {
			val selection = SuiListBox.getSelectedRow(parameters)

			when (selection) {
				0  -> handleBackpack(player)
				1  -> handleBikini(player)
				2  -> handleBodysuit(player)
				3  -> handleBoot(player)
				4  -> handleBustier(player)
				5  -> handleDress(player)
				6  -> handleGlove(player)
				7  -> handleGoggle(player)
				8  -> handleHat(player)
				9  -> handleHelmet(player)
				10 -> handleJacket(player)
				11 -> handlePant(player)
				12 -> handleRobe(player)
				13 -> handleShirt(player)
				14 -> handleShoe(player)
				15 -> handleSkirt(player)
				16 -> handleVest(player)
				17 -> handleIthorianEquipment(player)
				18 -> handleNightsisterEquipment(player)
				19 -> handleTuskenEquipment(player)
				20 -> handleWookieeEquipment(player)
			}
		}

		private fun handleBackpack(player: Player) {
			spawnItems(
				player, "item_clothing_backpack_agi_lvl1_02_01", "item_clothing_backpack_con_lvl1_02_01", "item_clothing_backpack_lck_lvl1_02_01", "item_clothing_backpack_pre_lvl1_02_01"
			)
		}

		private fun handleBikini(player: Player) {
			spawnItems(
				player, "item_clothing_bikini_01_01", "item_clothing_bikini_01_02", "item_clothing_bikini_01_03", "item_clothing_bikini_01_04", "item_clothing_bikini_leggings_01_01"
			)
		}

		private fun handleBodysuit(player: Player) {
			spawnItems(
				player, "item_clothing_bodysuit_at_at_01_01", "item_clothing_bodysuit_bwing_01_01", "item_clothing_bodysuit_tie_fighter_01_01", "item_clothing_bodysuit_trando_slaver_01_01"
			)
		}

		private fun handleBoot(player: Player) {
			spawnItems(
				player, "item_clothing_boots_01_03", "item_clothing_boots_01_04", "item_clothing_boots_01_05", "item_clothing_boots_01_12", "item_clothing_boots_01_14", "item_clothing_boots_01_15", "item_clothing_boots_01_19", "item_clothing_boots_01_21", "item_clothing_boots_01_22"
			)
		}

		private fun handleBustier(player: Player) {
			spawnItems(
				player, "item_clothing_bustier_01_01", "item_clothing_bustier_01_02", "item_clothing_bustier_01_03"
			)
		}

		private fun handleDress(player: Player) {
			spawnItems(
				player, "item_clothing_dress_01_05", "item_clothing_dress_01_06", "item_clothing_dress_01_07", "item_clothing_dress_01_08", "item_clothing_dress_01_09", "item_clothing_dress_01_10", "item_clothing_dress_01_11", "item_clothing_dress_01_12", "item_clothing_dress_01_13", "item_clothing_dress_01_14", "item_clothing_dress_01_15", "item_clothing_dress_01_16", "item_clothing_dress_01_18", "item_clothing_dress_01_19", "item_clothing_dress_01_23", "item_clothing_dress_01_26", "item_clothing_dress_01_27", "item_clothing_dress_01_29", "item_clothing_dress_01_30", "item_clothing_dress_01_31", "item_clothing_dress_01_32", "item_clothing_dress_01_33", "item_clothing_dress_01_34", "item_clothing_dress_01_35"
			)
		}

		private fun handleGlove(player: Player) {
			spawnItems(
				player, "item_clothing_gloves_01_02", "item_clothing_gloves_01_03", "item_clothing_gloves_01_06", "item_clothing_gloves_01_07", "item_clothing_gloves_01_10", "item_clothing_gloves_01_11", "item_clothing_gloves_01_12", "item_clothing_gloves_01_13", "item_clothing_gloves_01_14"
			)
		}

		private fun handleGoggle(player: Player) {
			spawnItems(
				player, "item_clothing_goggles_goggles_01_01", "item_clothing_goggles_goggles_01_02", "item_clothing_goggles_goggles_01_03", "item_clothing_goggles_goggles_01_04", "item_clothing_goggles_goggles_01_05", "item_clothing_goggles_goggles_01_06"
			)
		}

		private fun handleHat(player: Player) {
			spawnItems(
				player, "item_clothing_hat_chef_01_01", "item_clothing_hat_chef_01_02", "item_clothing_hat_imp_01_01", "item_clothing_hat_imp_01_02", "item_clothing_hat_rebel_trooper_01_01", "item_clothing_hat_01_02", "item_clothing_hat_01_04", "item_clothing_hat_01_10", "item_clothing_hat_01_12", "item_clothing_hat_01_13", "item_clothing_hat_01_14", "item_clothing_hat_twilek_01_01", "item_clothing_hat_twilek_01_02", "item_clothing_hat_twilek_01_03", "item_clothing_hat_twilek_01_04", "item_clothing_hat_twilek_01_05"
			)
		}

		private fun handleHelmet(player: Player) {
			spawnItems(
				player, "item_clothing_helmet_at_at_01_01", "item_clothing_helmet_fighter_blacksun_01_01", "item_clothing_helmet_fighter_imperial_01_01", "item_clothing_helmet_fighter_privateer_01_01", "item_clothing_helmet_fighter_rebel_01_01", "item_clothing_helmet_tie_fighter_01_01"
			)
		}

		private fun handleJacket(player: Player) {
			spawnItems(
				player, "item_clothing_jacket_01_02", "item_clothing_jacket_01_03", "item_clothing_jacket_01_04", "item_clothing_jacket_01_05", "item_clothing_jacket_01_06", "item_clothing_jacket_01_07", "item_clothing_jacket_01_08", "item_clothing_jacket_01_09", "item_clothing_jacket_01_10", "item_clothing_jacket_01_11", "item_clothing_jacket_01_12", "item_clothing_jacket_01_13", "item_clothing_jacket_01_14", "item_clothing_jacket_01_15", "item_clothing_jacket_01_16", "item_clothing_jacket_01_17", "item_clothing_jacket_01_18", "item_clothing_jacket_01_19", "item_clothing_jacket_01_20", "item_clothing_jacket_01_21", "item_clothing_jacket_01_22", "item_clothing_jacket_01_23", "item_clothing_jacket_01_24", "item_clothing_jacket_01_25", "item_clothing_jacket_01_26"
			)
		}

		private fun handlePant(player: Player) {
			spawnItems(
				player, "item_clothing_pants_01_01", "item_clothing_pants_01_02", "item_clothing_pants_01_03", "item_clothing_pants_01_04", "item_clothing_pants_01_05", "item_clothing_pants_01_06", "item_clothing_pants_01_07", "item_clothing_pants_01_08", "item_clothing_pants_01_09", "item_clothing_pants_01_10", "item_clothing_pants_01_11", "item_clothing_pants_01_12", "item_clothing_pants_01_13", "item_clothing_pants_01_14", "item_clothing_pants_01_15", "item_clothing_pants_01_16", "item_clothing_pants_01_17", "item_clothing_pants_01_18", "item_clothing_pants_01_21", "item_clothing_pants_01_22", "item_clothing_pants_01_24", "item_clothing_pants_01_25", "item_clothing_pants_01_26", "item_clothing_pants_01_27", "item_clothing_pants_01_28", "item_clothing_pants_01_29", "item_clothing_pants_01_30", "item_clothing_pants_01_31", "item_clothing_pants_01_32", "item_clothing_pants_01_33"
			)
		}

		private fun handleRobe(player: Player) {
			spawnItems(
				player, "item_clothing_robe_01_01", "item_clothing_robe_01_04", "item_clothing_robe_01_05", "item_clothing_robe_01_12", "item_clothing_robe_01_18", "item_clothing_robe_01_27", "item_clothing_robe_01_32", "item_clothing_robe_01_33"
			)
		}

		private fun handleShirt(player: Player) {
			spawnItems(
				player, "item_clothing_shirt_01_03", "item_clothing_shirt_01_04", "item_clothing_shirt_01_05", "item_clothing_shirt_01_07", "item_clothing_shirt_01_08", "item_clothing_shirt_01_09", "item_clothing_shirt_01_10", "item_clothing_shirt_01_11", "item_clothing_shirt_01_12", "item_clothing_shirt_01_13", "item_clothing_shirt_01_14", "item_clothing_shirt_01_15", "item_clothing_shirt_01_16", "item_clothing_shirt_01_24", "item_clothing_shirt_01_26", "item_clothing_shirt_01_27", "item_clothing_shirt_01_28", "item_clothing_shirt_01_30", "item_clothing_shirt_01_32", "item_clothing_shirt_01_34", "item_clothing_shirt_01_38", "item_clothing_shirt_01_42"
			)
		}

		private fun handleShoe(player: Player) {
			spawnItems(
				player, "item_clothing_shoes_01_01", "item_clothing_shoes_01_02", "item_clothing_shoes_01_03", "item_clothing_shoes_01_07", "item_clothing_shoes_01_08", "item_clothing_shoes_01_09"
			)
		}

		private fun handleSkirt(player: Player) {
			spawnItems(
				player, "item_clothing_skirt_01_03", "item_clothing_skirt_01_04", "item_clothing_skirt_01_05", "item_clothing_skirt_01_06", "item_clothing_skirt_01_07", "item_clothing_skirt_01_08", "item_clothing_skirt_01_09", "item_clothing_skirt_01_10", "item_clothing_skirt_01_11", "item_clothing_skirt_01_12", "item_clothing_skirt_01_13", "item_clothing_skirt_01_14"
			)
		}

		private fun handleVest(player: Player) {
			spawnItems(
				player, "item_clothing_vest_01_01", "item_clothing_vest_01_02", "item_clothing_vest_01_03", "item_clothing_vest_01_04", "item_clothing_vest_01_05", "item_clothing_vest_01_06", "item_clothing_vest_01_09", "item_clothing_vest_01_10", "item_clothing_vest_01_11", "item_clothing_vest_01_15"
			)
		}

		private fun handleIthorianEquipment(player: Player) {
			spawnItems(
				player,
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
			)
		}

		private fun handleNightsisterEquipment(player: Player) {
			spawnItems(
				player, "item_clothing_boots_nightsister_01_01", "item_clothing_dress_nightsister_01_01", "item_clothing_hat_nightsister_01_01", "item_clothing_hat_nightsister_01_02", "item_clothing_hat_nightsister_01_03", "item_clothing_pants_nightsister_01_01", "item_clothing_pants_nightsister_01_02", "item_clothing_shirt_nightsister_01_01", "item_clothing_shirt_nightsister_01_02", "item_clothing_shirt_nightsister_01_03"
			)
		}

		private fun handleTuskenEquipment(player: Player) {
			spawnItems(
				player, "item_clothing_bandolier_tusken_01_01", "item_clothing_bandolier_tusken_01_02", "item_clothing_bandolier_tusken_01_03", "item_clothing_boots_tusken_raider_01_01", "item_clothing_gloves_tusken_raider_01_01", "item_clothing_helmet_tusken_raider_01_01", "item_clothing_helmet_tusken_raider_01_02", "item_clothing_robe_tusken_raider_01_01", "item_clothing_robe_tusken_raider_01_02"
			)
		}

		private fun handleWookieeEquipment(player: Player) {
			spawnItems(
				player, "item_clothing_wookiee_gloves_01_01", "item_clothing_wookiee_gloves_01_02", "item_clothing_wookiee_gloves_01_03", "item_clothing_wookiee_gloves_01_04", "item_clothing_wookiee_hat_01_01", "item_clothing_wookiee_hood_01_01", "item_clothing_wookiee_hood_01_02", "item_clothing_wookiee_hood_01_03", "item_clothing_wookiee_lifeday_robe_01_01", "item_clothing_wookiee_lifeday_robe_01_02", "item_clothing_wookiee_lifeday_robe_01_03", "item_clothing_wookiee_shirt_01_01", "item_clothing_wookiee_shirt_01_02", "item_clothing_wookiee_shirt_01_03", "item_clothing_wookiee_shirt_01_04", "item_clothing_wookiee_shoulder_pad_01_01", "item_clothing_wookiee_shoulder_pad_01_02", "item_clothing_wookiee_skirt_01_01", "item_clothing_wookiee_skirt_01_02", "item_clothing_wookiee_skirt_01_03", "item_clothing_wookiee_skirt_01_04"
			)
		}

		private fun handleTravel(player: Player) {
			SuiListBox().run {
				title = "Character Builder Terminal"
				prompt = "Select a location you want to get teleported to."

				addListItem("Corellia - Stronghold")
				addListItem("Coreliia - Corsec Base")
				addListItem("Dantooine - Force Crystal Hunter's Cave")
				addListItem("Dantooine - Jedi Temple Ruins")
				addListItem("Dantooine - The Warren")
				addListItem("Dathomir - Imperial Prison")
				addListItem("Dathomir - Nightsister Stronghold")
				addListItem("Dathomir - Nightsister vs. Singing Moutain Clan")
				addListItem("Endor - DWB")
				addListItem("Endor - Jinda Cave")
				addListItem("Kashyyyk - Etyyy, The Hunting Grounds")
				addListItem("Kashyyyk - Kachirho, Slaver Camp")
				addListItem("Kashyyyk - Kkowir, The Dead Forest")
				addListItem("Kashyyyk - Rryatt Trail, 1")
				addListItem("Kashyyyk - Rryatt Trail, 2")
				addListItem("Kashyyyk - Rryatt Trail, 3")
				addListItem("Kashyyyk - Rryatt Trail, 4")
				addListItem("Kashyyyk - Rryatt Trail, 5")
				addListItem("Kashyyyk - Slaver")
				addListItem("Lok - Droid Cave")
				addListItem("Lok - Great Maze of Lok")
				addListItem("Lok - Imperial Outpost")
				addListItem("Lok - Kimogila Town")
				addListItem("Mustafar - Mensix Mining Facility")
				addListItem("Naboo - Emperor's Retreat")
				addListItem("Rori - Hyperdrive Research Facility")
				addListItem("Talus - Detainment Center")
				addListItem("Tatooine - Fort Tusken")
				addListItem("Tatooine - Imperial Oasis")
				addListItem("Tatooine - Krayt Graveyard")
				addListItem("Tatooine - Mos Eisley")
				addListItem("Tatooine - Mos Taike")
				addListItem("Tatooine - Squill Cave")
				addListItem("Yavin 4 - Blueleaf Temple")
				addListItem("Yavin 4 - Dark Enclave")
				addListItem("Yavin 4 - Geonosian Cave")
				addListItem("Yavin 4 - Light Enclave")
				addListItem("[INSTANCE] - Myyyydril Cave")
				addListItem("[INSTANCE] - Avatar Platform (EASY)")
				addListItem("[INSTANCE] - Avatar Platform (MEDIUM)")
				addListItem("[INSTANCE] - Avatar Platform (HARD)")
				addListItem("[INSTANCE] - Mustafar Jedi Challenge (EASY)")
				addListItem("[INSTANCE] - Mustafar Jedi Challenge (MEDIUM)")
				addListItem("[INSTANCE] - Mustafar Jedi Challenge (HARD)")
				addListItem("[INVASION] - Droid Army")

				addCallback(SuiEvent.OK_PRESSED, "handleTravelSelection") { _: SuiEvent, parameters: Map<String, String> -> handleTravelSelection(player, parameters) }
				display(player)
			}
		}

		private fun handleTravelSelection(player: Player, parameters: Map<String, String>) {
			val selection = SuiListBox.getSelectedRow(parameters)

			when (selection) {
				0  -> handleCorStronghold(player)
				1  -> handleCorCorsecBase(player)
				2  -> handleDanCrystalCave(player)
				3  -> handleDanJediTemple(player)
				4  -> handleDanWarren(player)
				5  -> handleDatImperialPrison(player)
				6  -> handleDatNS(player)
				7  -> handleDatNSvsSMC(player)
				8  -> handleEndDwb(player)
				9  -> handleEndJindaCave(player)
				10 -> handleKasEtyyy(player)
				11 -> handleKasKachirho(player)
				12 -> handleKasKkowir(player)
				13 -> handleKasRryatt1(player)
				14 -> handleKasRryatt2(player)
				15 -> handleKasRryatt3(player)
				16 -> handleKasRryatt4(player)
				17 -> handleKasRryatt5(player)
				18 -> handleKasSlaver(player)
				19 -> handleLokDroidCave(player)
				20 -> handleLokGreatMaze(player)
				21 -> handleLokImperialOutpost(player)
				22 -> handleLokKimogilaTown(player)
				23 -> handleMusMensix(player)
				24 -> handleNabEmperorsRetreat(player)
				25 -> handleRorHyperdriveFacility(player)
				26 -> handleTalDetainmentCenter(player)
				27 -> handleTatFortTusken(player)
				28 -> handleTatImperialOasis(player)
				29 -> handleTatKraytGrave(player)
				30 -> handleTatMosEisley(player)
				31 -> handleTatMosTaike(player)
				32 -> handleTatSquillCave(player)
				33 -> handleYavBlueleafTemple(player)
				34 -> handleYavDarkEnclave(player)
				35 -> handleYavGeoCave(player)
				36 -> handleYavLightEnclave(player)
				37 -> handleInstanceMyyydrilCave(player)
				38 -> handleInstanceAvatarPlatformEasy(player)
				39 -> handleInstanceAvatarPlatformMedium(player)
				40 -> handleInstanceAvatarPlatformHard(player)
				41 -> handleInstanceMusJediEasy(player)
				42 -> handleInstanceMusJediMedium(player)
				43 -> handleInstanceMusJediHard(player)
				44 -> handleInvasionMusDroidArmy(player)
			}
		}

		// Planet: Corellia
		private fun handleCorStronghold(player: Player) {
			teleportTo(player, 4735.0, 26.0, -5676.0, Terrain.CORELLIA)
		}

		private fun handleCorCorsecBase(player: Player) {
			teleportTo(player, 5137.0, 16.0, 1518.0, Terrain.CORELLIA)
		}

		// Planet: Dantooine
		private fun handleDanJediTemple(player: Player) {
			teleportTo(player, 4078.0, 10.0, 5370.0, Terrain.DANTOOINE)
		}

		private fun handleDanCrystalCave(player: Player) {
			teleportTo(player, -6225.0, 48.0, 7381.0, Terrain.DANTOOINE)
		}

		private fun handleDanWarren(player: Player) {
			teleportTo(player, -564.0, 1.0, -3789.0, Terrain.DANTOOINE)
		}

		// Planet: Dathomir
		private fun handleDatImperialPrison(player: Player) {
			teleportTo(player, -6079.0, 132.0, 971.0, Terrain.DATHOMIR)
		}

		private fun handleDatNS(player: Player) {
			teleportTo(player, -3989.0, 124.0, -10.0, Terrain.DATHOMIR)
		}

		private fun handleDatNSvsSMC(player: Player) {
			teleportTo(player, -2457.0, 117.0, 1530.0, Terrain.DATHOMIR)
		}

		// Planet: Endor
		private fun handleEndJindaCave(player: Player) {
			teleportTo(player, -1714.0, 31.0, -8.0, Terrain.ENDOR)
		}

		private fun handleEndDwb(player: Player) {
			teleportTo(player, -4683.0, 13.0, 4326.0, Terrain.ENDOR)
		}

		// Planet: Kashyyyk
		private fun handleKasEtyyy(player: Player) {
			teleportTo(player, 275.0, 48.0, 503.0, Terrain.KASHYYYK_HUNTING)
		}

		private fun handleKasKachirho(player: Player) {
			teleportTo(player, 146.0, 19.0, 162.0, Terrain.KASHYYYK_MAIN)
		}

		private fun handleKasKkowir(player: Player) {
			teleportTo(player, -164.0, 16.0, -262.0, Terrain.KASHYYYK_DEAD_FOREST)
		}

		private fun handleKasRryatt1(player: Player) {
			teleportTo(player, 534.0, 173.0, 82.0, Terrain.KASHYYYK_RRYATT_TRAIL)
		}

		private fun handleKasRryatt2(player: Player) {
			teleportTo(player, 1422.0, 70.0, 722.0, Terrain.KASHYYYK_RRYATT_TRAIL)
		}

		private fun handleKasRryatt3(player: Player) {
			teleportTo(player, 2526.0, 182.0, -278.0, Terrain.KASHYYYK_RRYATT_TRAIL)
		}

		private fun handleKasRryatt4(player: Player) {
			teleportTo(player, 768.0, 141.0, -439.0, Terrain.KASHYYYK_RRYATT_TRAIL)
		}

		private fun handleKasRryatt5(player: Player) {
			teleportTo(player, 2495.0, -24.0, -924.0, Terrain.KASHYYYK_RRYATT_TRAIL)
		}

		private fun handleKasSlaver(player: Player) {
			teleportTo(player, 561.8, 22.8, 1552.8, Terrain.KASHYYYK_NORTH_DUNGEONS)
		}

		// Planet: Lok
		private fun handleLokDroidCave(player: Player) {
			teleportTo(player, 3331.0, 105.0, -4912.0, Terrain.LOK)
		}

		private fun handleLokGreatMaze(player: Player) {
			teleportTo(player, 3848.0, 62.0, -464.0, Terrain.LOK)
		}

		private fun handleLokImperialOutpost(player: Player) {
			teleportTo(player, -1914.0, 11.0, -3299.0, Terrain.LOK)
		}

		private fun handleLokKimogilaTown(player: Player) {
			teleportTo(player, -70.0, 42.0, 2769.0, Terrain.LOK)
		}

		// Planet: Mustafar
		private fun handleMusMensix(player: Player) {
			teleportTo(player, -2489.0, 230.0, 1621.0, Terrain.MUSTAFAR)
		}

		// Planet: Naboo
		private fun handleNabEmperorsRetreat(player: Player) {
			teleportTo(player, 2535.0, 295.0, -3887.0, Terrain.NABOO)
		}

		// Planet: Rori
		private fun handleRorHyperdriveFacility(player: Player) {
			teleportTo(player, -1211.0, 98.0, 4552.0, Terrain.RORI)
		}

		// Planet: Talus
		private fun handleTalDetainmentCenter(player: Player) {
			teleportTo(player, 4958.0, 449.0, -5983.0, Terrain.TALUS)
		}

		// Planet: Tatooine
		private fun handleTatFortTusken(player: Player) {
			teleportTo(player, -3941.0, 59.0, 6318.0, Terrain.TATOOINE)
		}

		private fun handleTatKraytGrave(player: Player) {
			teleportTo(player, 7380.0, 122.0, 4298.0, Terrain.TATOOINE)
		}

		private fun handleTatMosEisley(player: Player) {
			teleportTo(player, 3525.0, 4.0, -4807.0, Terrain.TATOOINE)
		}

		private fun handleTatMosTaike(player: Player) {
			teleportTo(player, 3684.0, 7.0, 2357.0, Terrain.TATOOINE)
		}

		private fun handleTatSquillCave(player: Player) {
			teleportTo(player, 57.0, 152.0, -79.0, Terrain.TATOOINE)
		}

		private fun handleTatImperialOasis(player: Player) {
			teleportTo(player, -5458.0, 10.0, 2601.0, Terrain.TATOOINE)
		}

		// Planet: Yavin 4
		private fun handleYavBlueleafTemple(player: Player) {
			teleportTo(player, -947.0, 86.0, -2131.0, Terrain.YAVIN4)
		}

		private fun handleYavDarkEnclave(player: Player) {
			teleportTo(player, 5107.0, 81.0, 301.0, Terrain.YAVIN4)
		}

		private fun handleYavLightEnclave(player: Player) {
			teleportTo(player, -5575.0, 87.0, 4902.0, Terrain.YAVIN4)
		}

		private fun handleYavGeoCave(player: Player) {
			teleportTo(player, -6485.0, 83.0, -446.0, Terrain.YAVIN4)
		}

		// Dungeons:
		private fun handleInstanceMyyydrilCave(player: Player) {
			teleportTo(player, "kas_pob_myyydril_1", 1, -5.2, -1.3, -5.3)
		}

		private fun handleInstanceAvatarPlatformEasy(player: Player) {
			teleportTo(player, "kas_pob_avatar_1", 1, 103.2, 0.1, 21.7)
		}

		private fun handleInstanceAvatarPlatformMedium(player: Player) {
			teleportTo(player, "kas_pob_avatar_2", 1, 103.2, 0.1, 21.7)
		}

		private fun handleInstanceAvatarPlatformHard(player: Player) {
			teleportTo(player, "kas_pob_avatar_3", 1, 103.2, 0.1, 21.7)
		}

		private fun handleInstanceMusJediEasy(player: Player) {
			teleportTo(player, 2209.8, 74.8, 6410.2, Terrain.MUSTAFAR)
		}

		private fun handleInstanceMusJediMedium(player: Player) {
			teleportTo(player, 2195.1, 74.8, 4990.40, Terrain.MUSTAFAR)
		}

		private fun handleInstanceMusJediHard(player: Player) {
			teleportTo(player, 2190.5, 74.8, 3564.8, Terrain.MUSTAFAR)
		}

		private fun handleInvasionMusDroidArmy(player: Player) {
			teleportTo(player, 4908.0, 24.0, 6046.0, Terrain.MUSTAFAR)
		}


		private fun teleportTo(player: Player, x: Double, y: Double, z: Double, terrain: Terrain) {
			player.creatureObject.moveToContainer(null, Location(x, y, z, terrain))
		}

		private fun teleportTo(player: Player, buildoutTag: String, cellNumber: Int, x: Double, y: Double, z: Double) {
			val building = checkNotNull(ObjectStorageService.BuildingLookup.getBuildingByTag(buildoutTag)) { "building does not exist" }
			val cell = checkNotNull(building.getCellByNumber(cellNumber)) { "cell does not exist" }
			player.creatureObject.moveToContainer(cell, Location(x, y, z, building.terrain))
		}

		private fun teleportTo(player: Player, buildoutTag: String, cellName: String, x: Double, y: Double, z: Double) {
			val building = checkNotNull(ObjectStorageService.BuildingLookup.getBuildingByTag(buildoutTag)) { "building does not exist" }
			val cell = checkNotNull(building.getCellByName(cellName)) { "cell does not exist" }
			player.creatureObject.moveToContainer(cell, Location(x, y, z, building.terrain))
		}


		private fun handleVehicles(player: Player) {
			val items = arrayOf(
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
			)
			for (item in items) {
				val deed = ObjectCreator.createObjectFromTemplate(item)
				deed.moveToContainer(player.creatureObject.inventory)
				ObjectCreatedIntent(deed).broadcast()
			}
		}

		private fun handleTools(player: Player) {
			SuiListBox().run {
				title = "Character Builder Terminal"
				prompt = "Select a tool to acquire."

				addListItem("Gas Pocket Survey Device")
				addListItem("Chemical Survey Device")
				addListItem("Flora Survey Tool")
				addListItem("Mineral Survey Device")
				addListItem("Water Survey Device")
				addListItem("Wind Current Surveying Tool")

				addCallback(SuiEvent.OK_PRESSED, "handleToolsSelection") { _: SuiEvent, parameters: Map<String, String> -> handleToolsSelection(player, parameters) }
				display(player)
			}
		}

		private fun handleToolsSelection(player: Player, parameters: Map<String, String>) {
			val selection = SuiListBox.getSelectedRow(parameters)

			when (selection) {
				0 -> handleGas(player)
				1 -> handleChemical(player)
				2 -> handleFlora(player)
				3 -> handleMineral(player)
				4 -> handleWater(player)
				5 -> handleWind(player)
			}
		}

		private fun handleGas(player: Player) {
			spawnItems(
				player, "survey_tool_gas"
			)
		}

		private fun handleChemical(player: Player) {
			spawnItems(
				player, "survey_tool_liquid"
			)
		}

		private fun handleFlora(player: Player) {
			spawnItems(
				player, "survey_tool_lumber"
			)
		}

		private fun handleMineral(player: Player) {
			spawnItems(
				player, "survey_tool_mineral"
			)
		}

		private fun handleWater(player: Player) {
			spawnItems(
				player, "survey_tool_moisture"
			)
		}

		private fun handleWind(player: Player) {
			spawnItems(
				player, "survey_tool_wind"
			)
		}
	}
}