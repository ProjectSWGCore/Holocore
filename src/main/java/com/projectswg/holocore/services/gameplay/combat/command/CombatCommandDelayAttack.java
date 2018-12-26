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

package com.projectswg.holocore.services.gameplay.combat.command;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage;
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.global.commands.CombatCommand;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.log.Log;

enum CombatCommandDelayAttack implements CombatCommandHitType {
	INSTANCE;
	
	private final ScheduledThreadPool executor;
	
	CombatCommandDelayAttack() {
		this.executor = new ScheduledThreadPool(2, "combat-command-delay-attack-%d");
	}
	
	@Override
	public void initialize() {
		executor.start();
	}
	
	@Override
	public void terminate() {
		executor.stop();
		executor.awaitTermination(1000);
	}
	
	@Override
	public void handle(CreatureObject source, SWGObject target, CombatCommand combatCommand, String arguments) {
		String[] argSplit = arguments.split(" ");
		Location eggLocation;
		SWGObject eggParent;
		
		switch (combatCommand.getEggPosition()) {
			case LOCATION:
				if (argSplit[0].equals("a") || argSplit[0].equals("c")) {    // is "c" in free-targeting mode
					eggLocation = source.getLocation();
				} else {
					eggLocation = new Location(Float.parseFloat(argSplit[0]), Float.parseFloat(argSplit[1]), Float.parseFloat(argSplit[2]), source.getTerrain());
				}
				
				eggParent = source.getParent();
				break;
			default:
				Log.w("Unrecognised delay egg position %s from command %s - defaulting to SELF", combatCommand.getEggPosition(), combatCommand.getName());
			case SELF:
				eggLocation = source.getLocation();
				eggParent = source.getParent();
				break;
			case TARGET:
				eggLocation = target.getLocation();
				eggParent = target.getParent();
				break;
		}
		
		// Spawn delay egg object
		String eggTemplate = combatCommand.getDelayAttackEggTemplate();
		SWGObject delayEgg = eggTemplate.endsWith("generic_egg_small.iff") ? null : ObjectCreator.createObjectFromTemplate(eggTemplate);
		
		if (delayEgg != null) {
			delayEgg.moveToContainer(eggParent, eggLocation);
			ObjectCreatedIntent.broadcast(delayEgg);
		}
		
		long interval = (long) (combatCommand.getInitialDelayAttackInterval() * 1000);
		executor.execute(interval, () -> delayEggLoop(delayEgg, source, target, combatCommand, 1));
	}
	
	private void delayEggLoop(final SWGObject delayEgg, final CreatureObject source, final SWGObject target, final CombatCommand combatCommand, final int currentLoop) {
		String delayAttackParticle = combatCommand.getDelayAttackParticle();
		
		// Show particle effect to everyone observing the delay egg, if one is defined
		if (delayEgg != null && !delayAttackParticle.isEmpty())
			delayEgg.sendObservers(new PlayClientEffectObjectMessage(delayAttackParticle, "", delayEgg.getObjectId(), ""));
		
		// Handle the attack of this loop
		CombatCommandAttack.INSTANCE.handle(source, target, delayEgg, combatCommand);
		
		if (currentLoop < combatCommand.getDelayAttackLoops()) {
			// Recursively schedule another loop if that wouldn't exceed the amount of loops we need to perform
			long interval = (long) (combatCommand.getDelayAttackInterval() * 1000);
			executor.execute(interval, () -> delayEggLoop(delayEgg, source, target, combatCommand, currentLoop + 1));
		} else if (delayEgg != null) {
			// The delayed attack has ended - destroy the egg
			new DestroyObjectIntent(delayEgg).broadcast();
		}
	}
}
