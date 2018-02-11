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
package com.projectswg.holocore.services.player.creation;

import java.util.Calendar;

import com.projectswg.common.data.encodables.tangible.PvpFlag;
import com.projectswg.common.data.encodables.tangible.Race;
import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.data.swgfile.visitors.ProfTemplateData;
import com.projectswg.common.debug.Assert;
import com.projectswg.common.debug.Log;
import com.projectswg.common.network.packets.swg.login.creation.ClientCreateCharacter;

import com.projectswg.holocore.intents.experience.GrantSkillIntent;
import com.projectswg.holocore.intents.object.ObjectCreatedIntent;
import com.projectswg.holocore.resources.containers.ContainerPermissionsType;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.building.BuildingObject;
import com.projectswg.holocore.resources.objects.cell.CellObject;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import com.projectswg.holocore.resources.objects.player.PlayerObject;
import com.projectswg.holocore.resources.objects.tangible.TangibleObject;
import com.projectswg.holocore.resources.objects.weapon.WeaponObject;
import com.projectswg.holocore.resources.objects.weapon.WeaponType;
import com.projectswg.holocore.resources.player.AccessLevel;
import com.projectswg.holocore.services.objects.ObjectCreator;
import com.projectswg.holocore.services.objects.ObjectManager;
import com.projectswg.holocore.services.player.TerrainZoneInsertion.SpawnInformation;

public class CharacterCreation {
	
	private final ObjectManager objManager;
	private final ProfTemplateData templateData;
	private final ClientCreateCharacter create;
	
	public CharacterCreation(ObjectManager objManager, ProfTemplateData templateData, ClientCreateCharacter create) {
		this.objManager = objManager;
		this.templateData = templateData;
		this.create = create;
	}
	
	public CreatureObject createCharacter(AccessLevel accessLevel, SpawnInformation info) {
		Race			race		= Race.getRaceByFile(create.getRace());
		CreatureObject	creatureObj	= createCreature(race.getFilename(), info);
		if (creatureObj == null)
			return null;
		PlayerObject	playerObj	= createPlayer(creatureObj, "object/player/shared_player.iff");
		Assert.notNull(playerObj);
		
		setCreatureObjectValues(creatureObj);
		setPlayerObjectValues(playerObj);
		createHair(creatureObj, create.getHair(), create.getHairCustomization());
		createStarterClothing(creatureObj, create.getRace());
		playerObj.setAdminTag(accessLevel);
		new ObjectCreatedIntent(creatureObj).broadcast();
		return creatureObj;
	}
	
	private CreatureObject createCreature(String template, SpawnInformation info) {
		if (info.building)
			return createCreatureBuilding(template, info);
		SWGObject obj = ObjectCreator.createObjectFromTemplate(template);
		Assert.test(obj instanceof CreatureObject);
		obj.setLocation(info.location);
		return (CreatureObject) obj;
	}
	
	private CreatureObject createCreatureBuilding(String template, SpawnInformation info) {
		SWGObject parent = objManager.getObjectById(info.buildingId);
		if (parent == null || !(parent instanceof BuildingObject)) {
			Log.e("Invalid parent! Either null or not a building: %s  BUID: %d", parent, info.buildingId);
			return null;
		}
		CellObject cell = ((BuildingObject) parent).getCellByName(info.cell);
		if (cell == null) {
			Log.e("Invalid cell! Cell does not exist: %s  B-Template: %s  BUID: %d", info.cell, parent.getTemplate(), info.buildingId);
			return null;
		}
		SWGObject obj = ObjectCreator.createObjectFromTemplate(template);
		Assert.test(obj instanceof CreatureObject);
		obj.setLocation(info.location);
		obj.moveToContainer(cell);
		return (CreatureObject) obj;
	}
	
	private PlayerObject createPlayer(CreatureObject creatureObj, String template) {
		SWGObject obj = ObjectCreator.createObjectFromTemplate(template);
		Assert.test(obj instanceof PlayerObject);
		obj.moveToContainer(creatureObj);
		new ObjectCreatedIntent(obj).broadcast();
		return (PlayerObject) obj;
	}
	
	private TangibleObject createTangible(SWGObject container, ContainerPermissionsType type, String template) {
		SWGObject obj = ObjectCreator.createObjectFromTemplate(template);
		Assert.test(obj instanceof TangibleObject);
		obj.setContainerPermissions(type);
		obj.moveToContainer(container);
		new ObjectCreatedIntent(obj).broadcast();
		return (TangibleObject) obj;
	}
	
	/** Creates an object with default world visibility */
	private TangibleObject createDefaultObject(SWGObject container, String template) {
		return createTangible(container, ContainerPermissionsType.DEFAULT, template);
	}
	
	/** Creates an object with inventory-level world visibility (only the owner) */
	private TangibleObject createInventoryObject(SWGObject container, String template) {
		return createTangible(container, ContainerPermissionsType.INVENTORY, template);
	}
	
	private void createHair(CreatureObject creatureObj, String hair, byte [] customization) {
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
		playerObj.setProfession(create.getProfession());
		Calendar date = Calendar.getInstance();
		playerObj.setBornDate(date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1, date.get(Calendar.DAY_OF_MONTH));
	}
	
	private void createStarterClothing(CreatureObject creature, String race) {
		for (String template : templateData.getItems(ClientFactory.formatToSharedFile(race))) {
			createDefaultObject(creature, template);
		}
		
		SWGObject inventory = creature.getSlottedObject("inventory");
		Assert.notNull(inventory);
		createDefaultObject(inventory, "object/tangible/npe/shared_npe_uniform_box.iff");
	}
	
}
