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
package com.projectswg.holocore.resources.support.global.zone.creation

import com.projectswg.common.data.customization.CustomizationString
import com.projectswg.common.data.encodables.tangible.PvpFlag
import com.projectswg.common.data.encodables.tangible.Race
import com.projectswg.common.data.location.Location
import com.projectswg.common.data.swgfile.ClientFactory
import com.projectswg.common.network.packets.swg.login.creation.ClientCreateCharacter
import com.projectswg.holocore.intents.gameplay.player.experience.GrantSkillIntent
import com.projectswg.holocore.intents.support.objects.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader.Companion.playerStartClothing
import com.projectswg.holocore.resources.support.data.server_info.loader.TerrainZoneInsertionLoader.ZoneInsertion
import com.projectswg.holocore.resources.support.global.player.AccessLevel
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.DefaultWeaponFactory.createDefaultWeapon
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject
import com.projectswg.holocore.services.support.objects.ObjectStorageService.BuildingLookup
import me.joshlarson.jlcommon.utilities.Arguments
import java.time.Instant

class CharacterCreation(private val player: Player, private val create: ClientCreateCharacter) {
	fun createCharacter(accessLevel: AccessLevel?, info: ZoneInsertion): CreatureObject {
		val race = Race.getRaceByFile(create.race)
		val creatureObj = createCreature(race.filename, info)
		val playerObj = createPlayer(creatureObj)

		setCreatureObjectValues(creatureObj, race)
		setPlayerObjectValues(playerObj, race)
		createHair(creatureObj, create.hair, create.hairCustomization)
		createStarterClothing(creatureObj, race, create.clothes)
		playerObj.setAdminTag(accessLevel!!)
		playerObj.biography = create.biography

		ObjectCreatedIntent(playerObj).broadcast()
		ObjectCreatedIntent(creatureObj).broadcast()
		return creatureObj
	}

	private fun createCreature(template: String, info: ZoneInsertion): CreatureObject {
		if (info.buildingId.isNotEmpty()) return createCreatureBuilding(template, info)
		val obj = ObjectCreator.createObjectFromTemplate(template)
		assert(obj is CreatureObject)
		obj.isPersisted = true
		obj.location = generateRandomLocation(info)
		return obj as CreatureObject
	}

	private fun createCreatureBuilding(template: String, info: ZoneInsertion): CreatureObject {
		val building = BuildingLookup.getBuildingByTag(info.buildingId)
		Arguments.validate(building != null, String.format("Invalid building: %s", info.buildingId))

		val cell = building!!.getCellByName(info.cell)
		Arguments.validate(cell != null, String.format("Invalid cell! Cell does not exist: %s  Building: %s", info.cell, building))

		val obj = ObjectCreator.createObjectFromTemplate(template)
		assert(obj is CreatureObject)
		obj.moveToContainer(cell, generateRandomLocation(info))
		return obj as CreatureObject
	}

	private fun createPlayer(creatureObj: CreatureObject): PlayerObject {
		val obj = ObjectCreator.createObjectFromTemplate("object/player/shared_player.iff")
		assert(obj is PlayerObject)
		obj.moveToContainer(creatureObj)
		return obj as PlayerObject
	}

	private fun createTangible(container: SWGObject, template: String): TangibleObject {
		val obj = ObjectCreator.createObjectFromTemplate(template)
		assert(obj is TangibleObject)
		obj.moveToContainer(container)
		ObjectCreatedIntent(obj).broadcast()
		return obj as TangibleObject
	}

	/** Creates an object with default world visibility  */
	private fun createDefaultObject(container: SWGObject, template: String): TangibleObject {
		return createTangible(container, template)
	}

	/** Creates an object with inventory-level world visibility (only the owner)  */
	private fun createInventoryObject(container: SWGObject, template: String) {
		createTangible(container, template)
	}

	private fun createHair(creatureObj: CreatureObject, hair: String, customization: CustomizationString) {
		if (hair.isEmpty()) return
		val hairObj = createDefaultObject(creatureObj, ClientFactory.formatToSharedFile(hair))
		hairObj.appearanceData = customization
	}

	private fun setCreatureObjectValues(creatureObj: CreatureObject, race: Race) {
		creatureObj.race = race
		creatureObj.appearanceData = create.charCustomization
		creatureObj.height = create.height.toDouble()
		creatureObj.objectName = create.name
		creatureObj.setPvpFlags(PvpFlag.PLAYER)
		creatureObj.volume = 0x000F4240
		creatureObj.setBankBalance(100000)
		creatureObj.setCashBalance(1000)

		// New characters are Novices in all basic professions in the Combat Upgrade
		GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "species_" + creatureObj.race.species, creatureObj, true).broadcast()
		GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "social_entertainer_novice", creatureObj, true).broadcast()
		GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "outdoors_scout_novice", creatureObj, true).broadcast()
		GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "science_medic_novice", creatureObj, true).broadcast()
		GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "crafting_artisan_novice", creatureObj, true).broadcast()
		GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "combat_brawler_novice", creatureObj, true).broadcast()
		GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "combat_marksman_novice", creatureObj, true).broadcast()


		// Everyone can Burst Run
		creatureObj.addCommand("burstrun")

		val languages = languagesSkillsForRace(creatureObj.race)

		for (language in languages) {
			GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, language, creatureObj, true).broadcast()
		}

		val defaultWeapon = createDefaultWeapon(creatureObj)
		creatureObj.equippedWeapon = defaultWeapon
		createDefaultObject(creatureObj, "object/tangible/inventory/shared_character_inventory.iff")
		createInventoryObject(creatureObj, "object/tangible/datapad/shared_character_datapad.iff")
		createInventoryObject(creatureObj, "object/tangible/bank/shared_character_bank.iff")
		createInventoryObject(creatureObj, "object/tangible/mission_bag/shared_mission_bag.iff")
		createItem(creatureObj.inventory, "object/tangible/instrument/shared_slitherhorn.iff")
	}

	private fun createDefaultWeapon(creatureObj: CreatureObject): WeaponObject {
		val defWeapon = createDefaultWeapon()
		defWeapon.moveToContainer(creatureObj)
		return defWeapon
	}

	private fun setPlayerObjectValues(playerObj: PlayerObject, race: Race) {
		playerObj.setBornDate(Instant.now())
		playerObj.account = player.username
		playerObj.languageId = defaultLanguageForRace(race)
	}

	private fun createStarterClothing(creature: CreatureObject, race: Race, clothing: String) {
		val items = playerStartClothing().getClothing(race, clothing)
		if (items != null) {
			for (itemTemplate in items) {
				createItem(creature, itemTemplate)
			}
		}
	}

	private fun createItem(container: SWGObject, itemTemplate: String) {
		val item = createDefaultObject(container, itemTemplate)
		item.volume = 1
	}

	private fun languagesSkillsForRace(race: Race): Collection<String> {
		val languages: MutableCollection<String> = HashSet()

		languages.add("social_language_basic_comprehend") // Anyone can comprehend Galactic Basic
		languages.add("social_language_wookiee_comprehend") // Anyone can comprehend Shyriiwook

		when (race) {
			Race.HUMAN_MALE, Race.HUMAN_FEMALE           -> languages.add("social_language_basic_speak")
			Race.BOTHAN_MALE, Race.BOTHAN_FEMALE         -> {
				languages.add("social_language_basic_speak")
				languages.add("social_language_bothan_speak")
				languages.add("social_language_bothan_comprehend")
			}

			Race.ITHORIAN_MALE, Race.ITHORIAN_FEMALE     -> {
				languages.add("social_language_basic_speak")
				languages.add("social_language_ithorian_speak")
				languages.add("social_language_ithorian_comprehend")
			}

			Race.TWILEK_MALE, Race.TWILEK_FEMALE         -> {
				languages.add("social_language_basic_speak")
				languages.add("social_language_lekku_comprehend")
				languages.add("social_language_lekku_speak")
				languages.add("social_language_twilek_comprehend")
				languages.add("social_language_twilek_speak")
			}

			Race.MONCAL_MALE, Race.MONCAL_FEMALE         -> {
				languages.add("social_language_basic_speak")
				languages.add("social_language_moncalamari_comprehend")
				languages.add("social_language_moncalamari_speak")
			}

			Race.RODIAN_MALE, Race.RODIAN_FEMALE         -> {
				languages.add("social_language_basic_speak")
				languages.add("social_language_rodian_comprehend")
				languages.add("social_language_rodian_speak")
			}

			Race.SULLUSTAN_MALE, Race.SULLUSTAN_FEMALE   -> {
				languages.add("social_language_basic_speak")
				languages.add("social_language_sullustan_comprehend")
				languages.add("social_language_sullustan_speak")
			}

			Race.TRANDOSHAN_MALE, Race.TRANDOSHAN_FEMALE -> {
				languages.add("social_language_basic_speak")
				languages.add("social_language_trandoshan_comprehend")
				languages.add("social_language_trandoshan_speak")
			}

			Race.WOOKIEE_MALE, Race.WOOKIEE_FEMALE       -> languages.add("social_language_wookiee_speak")

			else                                         -> {}
		}
		return languages
	}

	companion object {
		private fun defaultLanguageForRace(race: Race): Int {
			return when (race) {
				Race.WOOKIEE_MALE, Race.WOOKIEE_FEMALE -> 5
				else                                   -> 1
			}
		}

		private fun generateRandomLocation(info: ZoneInsertion): Location {
			return Location.builder().setTerrain(info.terrain).setX(info.x + (Math.random() - .5) * info.radius).setY(info.y).setZ(info.z + (Math.random() - .5) * info.radius).setOrientationX(0.0).setOrientationY(0.0).setOrientationZ(0.0).setOrientationW(1.0).build()
		}
	}
}