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
package com.projectswg.holocore.services.gameplay.gcw;

import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.encodables.tangible.PvpFaction;
import com.projectswg.common.data.encodables.tangible.PvpStatus;
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage;
import com.projectswg.common.network.packets.swg.zone.PlayMusicMessage;
import com.projectswg.holocore.intents.gameplay.combat.CreatureKilledIntent;
import com.projectswg.holocore.intents.gameplay.gcw.faction.CivilWarPointIntent;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureDifficulty;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

/**
 * Responsible for granting Galactic Civil War points in PvP scenarios.
 */
public class CivilWarPvpService extends Service {
	
	@IntentHandler
	private void handleCreatureKilledIntent(CreatureKilledIntent cki) {
		CreatureObject corpseCreature = cki.getCorpse();
		CreatureObject killerCreature = cki.getKiller();
		PvpFaction killerFaction = killerCreature.getPvpFaction();
		
		if (!killerCreature.isPlayer()) {
			return;
		}
		
		boolean specialForces = corpseCreature.getPvpStatus() == PvpStatus.SPECIALFORCES;
		
		if (!isFactionEligible(killerFaction, corpseCreature.getPvpFaction())) {
			return;
		}
		
		PlayerObject killerPlayer = killerCreature.getPlayerObject();
		
		byte multiplier = makeMultiplier(specialForces, corpseCreature.isPlayer());
		int base = baseForDifficulty(corpseCreature.getDifficulty());
		int granted = pointsGranted(base, multiplier);
		ProsePackage prose;
		
		if (specialForces) {
			// Increment kill counter
			killerPlayer.setPvpKills(killerPlayer.getPvpKills() + 1);
			
			// Determine which effect and sound to play
			String effectFile;
			String soundFile;
			
			if (killerFaction == PvpFaction.REBEL) {
				effectFile = "clienteffect/holoemote_rebel.cef";
				soundFile = "sound/music_themequest_victory_rebel.snd";
			} else {
				effectFile = "clienteffect/holoemote_imperial.cef";
				soundFile = "sound/music_themequest_victory_imperial.snd";
			}
			
			// PvP GCW point system message
			prose = new ProsePackage("StringId", new StringId("gcw", "gcw_rank_pvp_kill_point_grant"), "DI", granted, "TT", corpseCreature
					.getObjectName());
			
			// Send visual effect to killer and everyone around
			killerCreature.sendObservers(new PlayClientEffectObjectMessage(effectFile, "head", killerCreature.getObjectId(), ""));
			
			// Send sound to just to the killer
			killerCreature.sendSelf(new PlayMusicMessage(0, soundFile, 0, false));
		} else {
			// NPC GCW point system message
			prose = new ProsePackage(new StringId("gcw", "gcw_rank_generic_point_grant"), "DI", granted);
		}
		
		// Increment GCW point counter
		CivilWarPointIntent.broadcast(killerPlayer, granted, prose);
	}
	
	boolean isFactionEligible(PvpFaction killerFaction, PvpFaction corpseFaction) {
		return killerFaction != PvpFaction.NEUTRAL && corpseFaction != PvpFaction.NEUTRAL && killerFaction != corpseFaction;
	}
	
	byte makeMultiplier(boolean specialForces, boolean player) {
		byte multiplier = 1;
		
		if (specialForces) {
			multiplier += 1;
		}
		
		if (player) {
			multiplier += 18;
		}
		
		return multiplier;
	}
	
	int baseForDifficulty(CreatureDifficulty difficulty) {
		switch (difficulty) {
			case NORMAL:
				return 5;
			case ELITE:
				return 10;
			case BOSS:
				return 15;
			default:
				throw new IllegalArgumentException("Unhandled CreatureDifficulty: " + difficulty);
		}
	}
	
	int pointsGranted(int base, byte multiplier) {
		return base * multiplier;
	}
	
}
