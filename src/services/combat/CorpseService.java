/************************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
 *                                                                                  *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
 * Our goal is to create an emulator which will provide a server for players to     *
 * continue playing a game similar to the one they used to play. We are basing      *
 * it on the final publish of the game prior to end-game events.                    *
 *                                                                                  *
 * This file is part of Holocore.                                                   *
 *                                                                                  *
 * -------------------------------------------------------------------------------- *
 *                                                                                  *
 * Holocore is free software: you can redistribute it and/or modify                 *
 * it under the terms of the GNU Affero General Public License as                   *
 * published by the Free Software Foundation, either version 3 of the               *
 * License, or (at your option) any later version.                                  *
 *                                                                                  *
 * Holocore is distributed in the hope that it will be useful,                      *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
 * GNU Affero General Public License for more details.                              *
 *                                                                                  *
 * You should have received a copy of the GNU Affero General Public License         *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
 *                                                                                  *
 ***********************************************************************************/
package services.combat;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import intents.combat.CreatureKilledIntent;
import intents.object.DestroyObjectIntent;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.creature.CreatureObject;
import utilities.ThreadUtilities;

/**
 * The {@code CorpseService} removes corpses from the world a while after
 * they've died. It also lets players clone at a cloning facility.
 * @author mads
 */
public class CorpseService extends Service {
	
	private final ScheduledExecutorService executor;
	
	public CorpseService() {
		executor = Executors.newSingleThreadScheduledExecutor(ThreadUtilities.newThreadFactory("corpse-service"));
		registerForIntent(CreatureKilledIntent.TYPE);
	}
	
	@Override
	public boolean terminate() {
		executor.shutdown();
		return super.terminate();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		switch(i.getType()) {
			case CreatureKilledIntent.TYPE: handleCreatureKilledIntent((CreatureKilledIntent) i); break;
		}
	}
	
	private void handleCreatureKilledIntent(CreatureKilledIntent i) {
		CreatureObject killedCreature = i.getKilledCreature();
		
		if(killedCreature.isPlayer()) {
			// TODO show cloning system message
			// TODO show cloning SUI window, with all possible facilities to clone at
			// TODO after 30 minutes, force them to clone at the nearest cloning facility
		} else {
			// Schedule corpse for removal
			executor.schedule(() -> new DestroyObjectIntent(killedCreature).broadcast(), 60, TimeUnit.SECONDS);
		}
	}
	
}