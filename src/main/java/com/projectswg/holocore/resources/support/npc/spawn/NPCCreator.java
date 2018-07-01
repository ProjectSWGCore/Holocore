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
package com.projectswg.holocore.resources.support.npc.spawn;

import com.projectswg.common.data.encodables.tangible.PvpFaction;
import com.projectswg.common.data.encodables.tangible.PvpFlag;
import com.projectswg.common.data.encodables.tangible.PvpStatus;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Location.LocationBuilder;
import com.projectswg.holocore.intents.gameplay.gcw.faction.FactionIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.npc.ai.NpcCombatMode;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.ObjectCreator.ObjectCreationException;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import com.projectswg.holocore.resources.support.npc.ai.NpcLoiterMode;
import com.projectswg.holocore.resources.support.npc.ai.NpcPatrolMode;
import com.projectswg.holocore.resources.support.npc.ai.NpcTurningMode;
import com.projectswg.holocore.resources.support.objects.swg.tangible.OptionFlag;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject;
import me.joshlarson.jlcommon.log.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class NPCCreator {
	
	private static final Random RANDOM = new Random();
	
	public static long createNPC(Spawner spawner) {
		AIObject object = ObjectCreator.createObjectFromTemplate(spawner.getRandomIffTemplate(), AIObject.class);
		
		object.setLocation(behaviorLocation(spawner));
		object.setObjectName(spawner.getName());
		object.setLevel(spawner.getCombatLevel());
		object.setDifficulty(spawner.getDifficulty());
		object.setMaxHealth(spawner.getHealth());
		object.setHealth(spawner.getHealth());
		object.setMaxAction(spawner.getAction());
		object.setAction(spawner.getAction());
		object.setMoodAnimation(spawner.getMood());
		object.setCreatureId(spawner.getNpcId());
		object.setWalkSpeed(spawner.getMovementSpeed());
		
		// Assign weapons
		try {
			spawner.getPrimaryWeapons().stream().map(w -> createWeapon(spawner, w)).filter(Objects::nonNull).forEach(object::addPrimaryWeapon);
			spawner.getSecondaryWeapons().stream().map(w -> createWeapon(spawner, w)).filter(Objects::nonNull).forEach(object::addSecondaryWeapon);
			List<WeaponObject> primaryWeapons = object.getPrimaryWeapons();
			if (!primaryWeapons.isEmpty())
				object.setEquippedWeapon(primaryWeapons.get(RANDOM.nextInt(primaryWeapons.size())));
		} catch (Throwable t) {
			Log.w(t);
		}
		
		switch (spawner.getBehavior()) {
			case LOITER:
				object.setDefaultMode(new NpcLoiterMode(spawner.getLoiterRadius()));
				break;
			case TURN:
				object.setDefaultMode(new NpcTurningMode());
				break;
			case PATROL:
				object.setDefaultMode(new NpcPatrolMode(spawner.getPatrolRoute() == null ? new ArrayList<>() : spawner.getPatrolRoute()));
				break;
			default:
				break;
		}
		setFlags(object, spawner);
		setNPCFaction(object, spawner.getFaction(), spawner.isSpecForce());
		
		object.systemMove(spawner.getEgg().getParent());
		ObjectCreatedIntent.broadcast(object);
		return object.getObjectId();
	}
	
	private static void setFlags(AIObject creature, Spawner spawner) {
		switch (spawner.getSpawnerFlag()) {
			case AGGRESSIVE:
				creature.setPvpFlags(PvpFlag.AGGRESSIVE);
				creature.addOptionFlags(OptionFlag.AGGRESSIVE);
			case ATTACKABLE:
				creature.setPvpFlags(PvpFlag.ATTACKABLE);
				creature.addOptionFlags(OptionFlag.HAM_BAR);
				creature.setCombatMode(new NpcCombatMode(spawner));
				break;
			case INVULNERABLE:
				creature.addOptionFlags(OptionFlag.INVULNERABLE);
				break;
		}
	}

	private static void setNPCFaction(TangibleObject object, PvpFaction faction, boolean specForce) {
		if (faction == PvpFaction.NEUTRAL) {
			return;
		}

		// Clear any existing flags that mark them as attackable
		object.clearPvpFlags(PvpFlag.ATTACKABLE, PvpFlag.AGGRESSIVE);
		object.removeOptionFlags(OptionFlag.AGGRESSIVE);

		new FactionIntent(object, faction).broadcast();

		if (specForce) {
			new FactionIntent(object, PvpStatus.SPECIALFORCES).broadcast();
		}
	}
	
	private static Location behaviorLocation(Spawner spawner) {
		LocationBuilder builder = Location.builder(spawner.getLocation());
		
		switch (spawner.getBehavior()) {
			case LOITER:
				// Random location within float radius of spawner and 
				int floatRadius = spawner.getLoiterRadius();
				int offsetX = randomBetween(0, floatRadius);
				int offsetZ = randomBetween(0, floatRadius);
				
				builder.translatePosition(offsetX, 0, offsetZ);
	
				// Doesn't break here - LOITER NPCs also have TURN behavior
			case TURN:
				// Random heading when spawned
				int randomHeading = randomBetween(0, 360);	// Can't use negative numbers as minimum
				builder.setHeading(randomHeading);
				break;
			default:
				break;
		}
		
		return builder.build();
	}
	
	/**
	 * Generates a random number between from (inclusive) and to (inclusive)
	 * @param from a positive minimum value
	 * @param to maximum value, which is larger than the minimum value
	 * @return a random number between the two, both inclusive
	 */
	private static int randomBetween(int from, int to) {
		return RANDOM.nextInt((to - from) + 1) + from;
	}
	
	private static WeaponObject createWeapon(Spawner spawner, String template) {
		try {
			WeaponObject weapon = (WeaponObject) ObjectCreator.createObjectFromTemplate(template);
			weapon.setMinDamage(spawner.getDamagePerSecond());
			weapon.setMaxDamage(spawner.getDamagePerSecond());
			int range = DataLoader.npcWeaponRanges().getWeaponRange(template);
			if (range == -1)
				Log.w("Failed to load weapon range for: %s", template);
			weapon.setMinRange(range);
			weapon.setMaxRange(range);
			return weapon;
		} catch (ObjectCreationException e) {
			Log.w("Weapon template does not exist: %s", template);
			return null;
		}
	}
	
}
