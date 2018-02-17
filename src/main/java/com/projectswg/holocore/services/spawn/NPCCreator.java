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
package com.projectswg.holocore.services.spawn;

import java.util.Random;

import com.projectswg.common.data.encodables.tangible.PvpFaction;
import com.projectswg.common.data.encodables.tangible.PvpFlag;
import com.projectswg.common.data.encodables.tangible.PvpStatus;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Location.LocationBuilder;

import com.projectswg.holocore.intents.FactionIntent;
import com.projectswg.holocore.intents.object.ObjectCreatedIntent;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import com.projectswg.holocore.resources.objects.custom.AIObject;
import com.projectswg.holocore.resources.objects.custom.LoiterAIObject;
import com.projectswg.holocore.resources.objects.custom.PatrolAIObject;
import com.projectswg.holocore.resources.objects.custom.RandomAIObject;
import com.projectswg.holocore.resources.objects.custom.TurningAIObject;
import com.projectswg.holocore.resources.objects.tangible.OptionFlag;
import com.projectswg.holocore.resources.objects.tangible.TangibleObject;
import com.projectswg.holocore.resources.spawn.Spawner;
import com.projectswg.holocore.resources.spawn.Spawner.SpawnerFlag;
import com.projectswg.holocore.services.objects.ObjectCreator;

class NPCCreator {
	
	private static final Random RANDOM = new Random();
	
	public static long createNPC(Spawner spawner) {
		AIObject object = createNPCFromBehavior(spawner);
		
		object.setLocation(behaviorLocation(spawner));
		object.setObjectName(spawner.getCreatureName());
		object.setLevel(spawner.getCombatLevel());
		object.setDifficulty(spawner.getCreatureDifficulty());
		object.setMaxHealth(spawner.getMaxHealth());
		object.setHealth(spawner.getMaxHealth());
		object.setMaxAction(spawner.getMaxAction());
		object.setAction(spawner.getMaxAction());
		object.setMoodAnimation(spawner.getMoodAnimation());
		object.setCreatureId(spawner.getCreatureId());
		object.setSpeed(spawner.getMovementSpeed());
		setFlags(object, spawner.getSpawnerFlag());
		setNPCFaction(object, spawner.getFaction(), spawner.isSpecForce());
		
		object.moveToContainer(spawner.getSpawnerObject().getParent());
		ObjectCreatedIntent.broadcast(object);
		return object.getObjectId();
	}
	
	private static AIObject createNPCFromBehavior(Spawner spawner) {
		String template = spawner.getRandomIffTemplate();
		switch (spawner.getAIBehavior()) {
			case LOITER: {
				LoiterAIObject object = ObjectCreator.createObjectFromTemplate(template, LoiterAIObject.class);
				object.setLoiterRadius(spawner.getFloatRadius());
				return object;
			}
			case PATROL: {
				PatrolAIObject object = ObjectCreator.createObjectFromTemplate(template, PatrolAIObject.class);
				if (spawner.getPatrolRoute() != null) {
					object.setPatrolWaypoints(spawner.getPatrolRoute());
				}
				return object;
			}
			case TURN:
				return ObjectCreator.createObjectFromTemplate(template, TurningAIObject.class);
			case IDLE:
			default:
				return ObjectCreator.createObjectFromTemplate(template, RandomAIObject.class);
		}
	}
	
	private static void setFlags(CreatureObject creature, SpawnerFlag flags) {
		switch (flags) {
			case AGGRESSIVE:
				creature.setPvpFlags(PvpFlag.AGGRESSIVE);
				creature.addOptionFlags(OptionFlag.AGGRESSIVE);
			case ATTACKABLE:
				creature.setPvpFlags(PvpFlag.ATTACKABLE);
				creature.addOptionFlags(OptionFlag.HAM_BAR);
				break;
			case INVULNERABLE:
				creature.addOptionFlags(OptionFlag.INVULNERABLE);
				break;
		}
	}

	private static void setNPCFaction(TangibleObject object, PvpFaction faction, boolean specForce) {
		if (faction == null) {
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
		
		switch (spawner.getAIBehavior()) {
			case LOITER:
				// Random location within float radius of spawner and 
				int floatRadius = spawner.getFloatRadius();
				int offsetX = randomBetween(0, floatRadius);
				int offsetZ = randomBetween(0, floatRadius);
				
				spawner.setFloatRadius(floatRadius);
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
	
}
