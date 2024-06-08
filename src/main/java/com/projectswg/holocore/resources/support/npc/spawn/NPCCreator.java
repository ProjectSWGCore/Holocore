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
package com.projectswg.holocore.resources.support.npc.spawn;

import com.projectswg.common.data.encodables.tangible.PvpFlag;
import com.projectswg.common.data.encodables.tangible.PvpStatus;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Location.LocationBuilder;
import com.projectswg.common.data.objects.GameObjectType;
import com.projectswg.holocore.intents.gameplay.gcw.UpdateFactionIntent;
import com.projectswg.holocore.intents.gameplay.gcw.UpdateFactionStatusIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.NpcEquipmentLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData;
import com.projectswg.holocore.resources.support.data.server_info.loader.combat.FactionLoader.Faction;
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcStatLoader.DetailNpcStatInfo;
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcStatLoader.NpcStatInfo;
import com.projectswg.holocore.resources.support.npc.ai.NpcLoiterMode;
import com.projectswg.holocore.resources.support.npc.ai.NpcPatrolMode;
import com.projectswg.holocore.resources.support.npc.ai.NpcTurningMode;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.ObjectCreator.ObjectCreationException;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureDifficulty;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.OptionFlag;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponClass;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponType;
import me.joshlarson.jlcommon.control.IntentChain;
import me.joshlarson.jlcommon.log.Log;
import me.joshlarson.jlcommon.utilities.Arguments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class NPCCreator {
	
	public static Collection<AIObject> createAllNPCs(Spawner spawner) {
		Arguments.validate(spawner.getMinLevel() <= spawner.getMaxLevel(), "min level must be less than max level");
		int amount = spawner.getAmount();
		Collection<AIObject> npcs = new ArrayList<>();
		
		for (int i = 0; i < amount; i++) {
			npcs.add(createSingleNpc(spawner));
		}
		
		return npcs;
	}
	
	public static AIObject createSingleNpc(Spawner spawner) {
		int combatLevel = ThreadLocalRandom.current().nextInt(spawner.getMinLevel(), spawner.getMaxLevel()+1);
		AIObject object = ObjectCreator.createObjectFromTemplate(spawner.getRandomIffTemplate(), AIObject.class);

		Long equipmentId = spawner.getEquipmentId();
		if (equipmentId != null) {
			addEquipmentItemsToNpc(object, equipmentId);
		}
		
		NpcStatInfo npcStats = DataLoader.Companion.npcStats().getNpcStats(combatLevel);
		DetailNpcStatInfo detailNpcStat = getDetailedNpcStats(npcStats, spawner.getDifficulty());
		object.setSpawner(spawner);
		object.systemMove(spawner.getEgg().getParent(), behaviorLocation(spawner));
		object.setObjectName(spawner.getName());
		object.setLevel(combatLevel);
		object.setDifficulty(spawner.getDifficulty());
		object.setMaxHealth(detailNpcStat.getHealth());
		object.setHealth(detailNpcStat.getHealth());
		object.setMaxAction(detailNpcStat.getAction());
		object.setAction(detailNpcStat.getAction());
		object.setMoodAnimation(spawner.getMood());
		object.setCreatureId(spawner.getNpcId());
		object.setWalkSpeed(spawner.getMovementSpeed());
		object.setHeight(getScale(spawner));
		
		int def = detailNpcStat.getDef();
		int toHit = detailNpcStat.getToHit();
		for (WeaponClass weaponClass : WeaponClass.values()) {
			object.adjustSkillmod(weaponClass.getDefenseSkillMod(), def, 0);
			object.adjustSkillmod(weaponClass.getAccuracySkillMod(), toHit, 0);
		}
		
		int hue = spawner.getHue();
		if (hue != 0) {
			// No reason to add color customization if the value is default anyways
			object.putCustomization("/private/index_color_1", hue);
		}
		
		// Assign weapons
		try {
			spawner.getDefaultWeapon().stream().map(w -> createWeapon(detailNpcStat, w)).filter(Objects::nonNull).forEach(object::addDefaultWeapon);
			spawner.getThrownWeapon().stream().map(w -> createWeapon(detailNpcStat, w)).filter(Objects::nonNull).forEach(object::addThrownWeapon);
			List<WeaponObject> defaultWeapons = object.getDefaultWeapons();
			if (!defaultWeapons.isEmpty())
				object.setEquippedWeapon(defaultWeapons.get(ThreadLocalRandom.current().nextInt(defaultWeapons.size())));
		} catch (Throwable t) {
			Log.w(t);
		}
		
		switch (spawner.getBehavior()) {
			case LOITER:
				object.setDefaultMode(new NpcLoiterMode(object, spawner.getLoiterRadius()));
				break;
			case TURN:
				object.setDefaultMode(new NpcTurningMode(object));
				break;
			case PATROL:
				object.setDefaultMode(new NpcPatrolMode(object, spawner.getPatrolRoute() == null ? List.of() : spawner.getPatrolRoute()));
				break;
			default:
				break;
		}
		setFlags(object, spawner);
		setNPCFaction(object, spawner.getFaction(), spawner.isSpecForce());
		
		spawner.addNPC(object);
		ObjectCreatedIntent.broadcast(object);
		return object;
	}

	private static void addEquipmentItemsToNpc(AIObject object, Long equipmentId) {
		NpcEquipmentLoader.NpcEquipmentInfo equipmentInfo = ServerData.INSTANCE.getNpcEquipment().getEquipmentInfo(equipmentId);

		if (equipmentInfo != null) {
			addEquipmentToSlot(equipmentInfo.getLeftHandTemplate(), "hold_l", object);
			addEquipmentToSlot(equipmentInfo.getRightHandTemplate(), "hold_r", object);
		}
	}

	private static void addEquipmentToSlot(String objectTemplate, String slotName, AIObject object) {
		if (!objectTemplate.isBlank()) {
			SWGObject rightHandObject = ObjectCreator.createObjectFromTemplate(objectTemplate);
			rightHandObject.moveToSlot(object, slotName, 4);
		}
	}

	private static void setFlags(AIObject object, Spawner spawner) {
		switch (spawner.getSpawnerFlag()) {
			case AGGRESSIVE:
				object.setPvpFlags(PvpFlag.CAN_ATTACK_YOU);
				object.addOptionFlags(OptionFlag.AGGRESSIVE);
			case ATTACKABLE:
				object.setPvpFlags(PvpFlag.YOU_CAN_ATTACK);
				object.addOptionFlags(OptionFlag.HAM_BAR);
				break;
			case INVULNERABLE:
				object.addOptionFlags(OptionFlag.INVULNERABLE);
				break;
			case QUEST:
				object.addOptionFlags(OptionFlag.INVULNERABLE);
				object.addOptionFlags(OptionFlag.INTERESTING);
				break;
		}
	}

	private static void setNPCFaction(TangibleObject object, Faction faction, boolean specForce) {
		if (specForce) {
			IntentChain.broadcastChain(
					new UpdateFactionIntent(object, faction),
					new UpdateFactionStatusIntent(object, PvpStatus.SPECIALFORCES)
			);
		} else {
			new UpdateFactionIntent(object, faction).broadcast();
		}
	}
	
	private static double getScale(Spawner spawner) {
		double scaleMin = spawner.getScaleMin();
		double scaleMax = spawner.getScaleMax();
		if (scaleMin == scaleMax) {
			// Min and max are the same. Using either of them is fine.
			return scaleMin;
		} else {
			// There's a gap between min and max. Let's generate a random number between them (both inclusive)
			return ThreadLocalRandom.current().nextDouble(scaleMin, scaleMax + 0.1d);	// +0.1 to make scaleMax inclusive
		}
	}
	
	private static Location behaviorLocation(Spawner spawner) {
		LocationBuilder builder = Location.builder(spawner.getLocation());
		
		switch (spawner.getBehavior()) {
			case LOITER: {
				// Random location within float radius of spawner
				double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
				double distance = ThreadLocalRandom.current().nextDouble(spawner.getLoiterRadius());
				int offsetX = (int) (Math.cos(angle) * distance);
				int offsetZ = (int) (Math.sin(angle) * distance);
				
				builder.translatePosition(offsetX, 0, offsetZ);
				
				// Doesn't break here - LOITER NPCs also have TURN behavior
			}
			case TURN: {
				// Random heading when spawned
				int randomHeading = randomBetween(0, 360);    // Can't use negative numbers as minimum
				builder.setHeading(randomHeading);
				break;
			}
			default:
				break;
		}
		if (spawner.getBuildingId().isEmpty() || spawner.getBuildingId().endsWith("_world"))
			builder.setY(DataLoader.Companion.terrains().getHeight(builder));
		
		return builder.build();
	}
	
	private static DetailNpcStatInfo getDetailedNpcStats(NpcStatInfo npcStats, CreatureDifficulty difficulty) {
		switch (difficulty) {
			case NORMAL:
			default:
				return npcStats.getNormalDetailStat();
			case ELITE:
				return npcStats.getEliteDetailStat();
			case BOSS:
				return npcStats.getBossDetailStat();
		}
	}
	
	/**
	 * Generates a random number between from (inclusive) and to (inclusive)
	 * @param from a positive minimum value
	 * @param to maximum value, which is larger than the minimum value
	 * @return a random number between the two, both inclusive
	 */
	private static int randomBetween(int from, int to) {
		return ThreadLocalRandom.current().nextInt((to - from) + 1) + from;
	}
	
	private static WeaponObject createWeapon(DetailNpcStatInfo detailNpcStat, String template) {
		try {
			WeaponObject weapon = (WeaponObject) ObjectCreator.createObjectFromTemplate(template);
			WeaponType weaponType = getWeaponType(weapon.getGameObjectType());
			
			weapon.setMinDamage((int) (detailNpcStat.getDamagePerSecond() * 2 * 0.90));
			weapon.setMaxDamage(detailNpcStat.getDamagePerSecond() * 2);
			int range = DataLoader.Companion.npcWeaponRanges().getWeaponRange(template);
			if (range == -1)
				Log.w("Failed to load weapon range for: %s", template);
			weapon.setMinRange(range);
			weapon.setMaxRange(range);
			weapon.setType(weaponType);
			// TODO set damage type, since all NPC weapons shouldn't deal kinetic damage
			return weapon;
		} catch (ObjectCreationException e) {
			Log.w("Weapon template does not exist: %s", template);
			return null;
		}
	}
	
	/**
	 * Somewhat accurate way of determining a WeaponType based on a GameObjectType.
	 * Problem is that GOT_WEAPON_RANGED_RIFLE can be both a rifle and a heavy weapon, but we assume it's a rifle since NPCs don't use heavy weapons.
	 *
	 * @param weaponObjectType to determine a WeaponType based on
	 * @return {@code WeaponType} that was determined from the given {@code weaponObjectType} param
	 */
	private static WeaponType getWeaponType(GameObjectType weaponObjectType) {
		switch (weaponObjectType) {
			case GOT_WEAPON_HEAVY_MINE:		return WeaponType.HEAVY;
			case GOT_WEAPON_HEAVY_MISC:		return WeaponType.HEAVY;
			case GOT_WEAPON_HEAVY_SPECIAL:	return WeaponType.HEAVY;
			case GOT_WEAPON_MELEE_1H:		return WeaponType.ONE_HANDED_MELEE;
			case GOT_WEAPON_MELEE_2H:		return WeaponType.TWO_HANDED_MELEE;
			case GOT_WEAPON_MELEE_MISC:		return WeaponType.ONE_HANDED_MELEE;
			case GOT_WEAPON_MELEE_POLEARM:	return WeaponType.POLEARM_MELEE;
			case GOT_WEAPON_RANGED_CARBINE:	return WeaponType.CARBINE;
			case GOT_WEAPON_RANGED_PISTOL:	return WeaponType.PISTOL;
			case GOT_WEAPON_RANGED_RIFLE:	return WeaponType.RIFLE;
			case GOT_WEAPON_RANGED_THROWN:	return WeaponType.THROWN;
		}
		return WeaponType.UNARMED;
	}
	
}
