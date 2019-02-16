/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.support.global.zone.creation;

import com.projectswg.common.data.customization.CustomizationString;
import com.projectswg.common.data.encodables.tangible.PvpFlag;
import com.projectswg.common.data.encodables.tangible.Race;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.network.packets.swg.login.creation.ClientCreateCharacter;
import com.projectswg.holocore.intents.gameplay.player.experience.skills.GrantSkillIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.TerrainZoneInsertionLoader.ZoneInsertion;
import com.projectswg.holocore.resources.support.global.player.AccessLevel;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import com.projectswg.holocore.resources.support.objects.swg.player.Profession;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponType;
import com.projectswg.holocore.services.support.objects.ObjectStorageService.BuildingLookup;
import me.joshlarson.jlcommon.utilities.Arguments;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;

public class CharacterCreation {
	
	private final Player player;
	private final ClientCreateCharacter create;
	
	public CharacterCreation(Player player, ClientCreateCharacter create) {
		this.player = player;
		this.create = create;
	}
	
	public CreatureObject createCharacter(AccessLevel accessLevel, ZoneInsertion info) {
		Race			race		= Race.getRaceByFile(create.getRace());
		CreatureObject	creatureObj	= createCreature(race.getFilename(), info);
		PlayerObject	playerObj	= createPlayer(creatureObj);
		
		setCreatureObjectValues(creatureObj);
		setPlayerObjectValues(playerObj);
		createHair(creatureObj, create.getHair(), create.getHairCustomization());
		createStarterClothing(creatureObj, create.getRace(), create.getClothes());
		playerObj.setAdminTag(accessLevel);
		
		ObjectCreatedIntent.broadcast(playerObj);
		ObjectCreatedIntent.broadcast(creatureObj);
		return creatureObj;
	}
	
	@NotNull
	private CreatureObject createCreature(String template, ZoneInsertion info) {
		if (!info.getBuildingId().isEmpty())
			return createCreatureBuilding(template, info);
		SWGObject obj = ObjectCreator.createObjectFromTemplate(template);
		assert obj instanceof CreatureObject;
		obj.setPersisted(true);
		obj.setLocation(generateRandomLocation(info));
		return (CreatureObject) obj;
	}
	
	@NotNull
	private CreatureObject createCreatureBuilding(String template, ZoneInsertion info) {
		BuildingObject building = BuildingLookup.getBuildingByTag(info.getBuildingId());
		Arguments.validate(building != null, String.format("Invalid building: %s", info.getBuildingId()));
		
		CellObject cell = building.getCellByName(info.getCell());
		Arguments.validate(cell != null, String.format("Invalid cell! Cell does not exist: %s  Building: %s", info.getCell(), building));
		
		SWGObject obj = ObjectCreator.createObjectFromTemplate(template);
		assert obj instanceof CreatureObject;
		obj.moveToContainer(cell, generateRandomLocation(info));
		return (CreatureObject) obj;
	}
	
	@NotNull
	private PlayerObject createPlayer(CreatureObject creatureObj) {
		SWGObject obj = ObjectCreator.createObjectFromTemplate("object/player/shared_player.iff");
		assert obj instanceof PlayerObject;
		obj.moveToContainer(creatureObj);
		return (PlayerObject) obj;
	}
	
	@NotNull
	private TangibleObject createTangible(SWGObject container, String template) {
		SWGObject obj = ObjectCreator.createObjectFromTemplate(template);
		assert obj instanceof TangibleObject;
		obj.moveToContainer(container);
		new ObjectCreatedIntent(obj).broadcast();
		return (TangibleObject) obj;
	}
	
	/** Creates an object with default world visibility */
	@NotNull
	private TangibleObject createDefaultObject(SWGObject container, String template) {
		return createTangible(container, template);
	}
	
	/** Creates an object with inventory-level world visibility (only the owner) */
	@NotNull
	private TangibleObject createInventoryObject(SWGObject container, String template) {
		return createTangible(container, template);
	}
	
	private void createHair(CreatureObject creatureObj, String hair, CustomizationString customization) {
		if (hair.isEmpty())
			return;
		TangibleObject hairObj = createDefaultObject(creatureObj, ClientFactory.formatToSharedFile(hair));
		hairObj.setAppearanceData(customization);
	}
	
	private void setCreatureObjectValues(CreatureObject creatureObj) {
		creatureObj.setRace(Race.getRaceByFile(create.getRace()));
		creatureObj.setAppearanceData(create.getCharCustomization());
		creatureObj.setHeight(create.getHeight());
		creatureObj.setObjectName(create.getName());
		creatureObj.setPvpFlags(PvpFlag.PLAYER);
		creatureObj.setVolume(0x000F4240);
		creatureObj.setBankBalance(1000);
		creatureObj.setCashBalance(100);
		new GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, create.getStartingPhase(), creatureObj, true).broadcast();
		new GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "species_" + creatureObj.getRace().getSpecies(), creatureObj, true).broadcast();
		
		WeaponObject defWeapon = (WeaponObject) createInventoryObject(creatureObj, "object/weapon/melee/unarmed/shared_unarmed_default_player.iff");
		defWeapon.setMaxRange(5);
		defWeapon.setType(WeaponType.UNARMED);
		defWeapon.setAttackSpeed(1);
		defWeapon.setMinDamage(50);
		defWeapon.setMaxDamage(100);
		creatureObj.setEquippedWeapon(defWeapon);
		createDefaultObject(creatureObj, "object/tangible/inventory/shared_character_inventory.iff");
		createInventoryObject(creatureObj, "object/tangible/datapad/shared_character_datapad.iff");
		createInventoryObject(creatureObj, "object/tangible/inventory/shared_appearance_inventory.iff");
		createInventoryObject(creatureObj, "object/tangible/bank/shared_character_bank.iff");
		createInventoryObject(creatureObj, "object/tangible/mission_bag/shared_mission_bag.iff");
	}
	
	private void setPlayerObjectValues(PlayerObject playerObj) {
		playerObj.setProfession(Profession.getProfessionFromClient(create.getProfession()));
		playerObj.setBornDate(Instant.now());
		playerObj.setAccount(player.getUsername());
	}
	
	private void createStarterClothing(CreatureObject creature, String race, String clothing) {
		List<String> items = DataLoader.playerStartClothing().getClothing(Race.getRaceByFile(race), clothing);
		if (items != null) {
			for (String itemTemplate : items) {
				createDefaultObject(creature, itemTemplate);
			}
		}
		
		SWGObject inventory = creature.getSlottedObject("inventory");
		assert inventory != null : "inventory is not defined";
		createDefaultObject(inventory, "object/tangible/npe/shared_npe_uniform_box.iff");
	}
	
	private static Location generateRandomLocation(ZoneInsertion info) {
		return Location.builder()
				.setTerrain(info.getTerrain())
				.setX(info.getX() + (Math.random()-.5) * info.getRadius())
				.setY(info.getY())
				.setZ(info.getZ() + (Math.random()-.5) * info.getRadius())
				.setOrientationX(0)
				.setOrientationY(0)
				.setOrientationZ(0)
				.setOrientationW(1)
				.build();
	}
	
}
