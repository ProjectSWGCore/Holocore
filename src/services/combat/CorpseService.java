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

import intents.PlayerEventIntent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import intents.combat.CreatureKilledIntent;
import intents.object.DestroyObjectIntent;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
import resources.server_info.Log;
import utilities.ThreadUtilities;

/**
 * The {@code CorpseService} removes corpses from the world a while after
 * they've died. It also lets players clone at a cloning facility.
 * @author mads
 */
public final class CorpseService extends Service {
	
	private final ScheduledExecutorService executor;
	// TODO Map terrain to all cloning facilities for that terrain
	// TODO cloning facilities with factional restrictions
	
	public CorpseService() {
		executor = Executors.newSingleThreadScheduledExecutor(ThreadUtilities.newThreadFactory("corpse-service"));
		registerForIntent(CreatureKilledIntent.TYPE);
		// TODO register for PlayerEventIntent
		// TODO register for ObjectCreatedIntent
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
			case PlayerEventIntent.TYPE: handlePlayerEventIntent((PlayerEventIntent) i); break;
		}
	}
	
	private void handleCreatureKilledIntent(CreatureKilledIntent i) {
		CreatureObject killedCreature = i.getKilledCreature();
		
		if(killedCreature.isPlayer()) {
			// TODO show cloning system message
			// TODO show cloning SUI window, with all possible facilities to clone at
			// TODO after 30 minutes, close the SUI window and force them to clone at the nearest cloning facility
			// TODO if the SUI window is closed by the player, make sure it reappears
		} else {
			// This is a NPC - schedule corpse for deletion
			executor.schedule(() -> deleteCorpse(killedCreature), 120, TimeUnit.SECONDS);
		}
	}
	
	private void handlePlayerEventIntent(PlayerEventIntent i) {
		switch(i.getEvent()) {
			case PE_DISAPPEAR: handleDisappearedPlayer(i.getPlayer()); break;
		}
	}
	
	private void handleDisappearedPlayer(Player player) {
		CreatureObject disappearedCreature = player.getCreatureObject();
		
		switch(disappearedCreature.getPosture()) {
			case DEAD:
				// If a player is dead when they disappear, we force them to clone at the nearest facility
				// TODO cancel currently scheduled forced clone
				// TODO force clone
				break;
		}
	}
	
	/**
	 * Only used for NPCs!
	 * @param creatureCorpse non-player creature to delete from the world
	 */
	private void deleteCorpse(CreatureObject creatureCorpse) {
		if(creatureCorpse.isPlayer()) {
			Log.e(this, "Cannot delete the corpse of a player!", creatureCorpse);
		} else {
			new DestroyObjectIntent(creatureCorpse).broadcast();
			Log.i(this, "Corpse of NPC %s was deleted from the world", creatureCorpse);
		}
	}
	
}