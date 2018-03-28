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

package com.projectswg.holocore.services.faction;

import com.projectswg.common.concurrency.PswgScheduledThreadPool;
import com.projectswg.common.control.Service;
import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.encodables.tangible.PvpFaction;
import com.projectswg.common.data.encodables.tangible.PvpStatus;
import com.projectswg.common.debug.Log;
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage;
import com.projectswg.common.network.packets.swg.zone.PlayMusicMessage;
import com.projectswg.holocore.intents.CivilWarPointIntent;
import com.projectswg.holocore.intents.FactionIntent;
import com.projectswg.holocore.intents.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.combat.CreatureKilledIntent;
import com.projectswg.holocore.intents.object.DestroyObjectIntent;
import com.projectswg.holocore.intents.object.ObjectCreatedIntent;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.creature.CreatureDifficulty;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import com.projectswg.holocore.resources.objects.player.PlayerObject;
import com.projectswg.holocore.resources.objects.tangible.TangibleObject;
import com.projectswg.holocore.resources.server_info.SdbLoader;
import com.projectswg.holocore.resources.server_info.StandardLog;

import java.io.File;
import java.io.IOException;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class CivilWarService extends Service {
	
	private static final int IMPERIAL_INDEX = 0;
	private static final int REBEL_INDEX = 1;
	
	private final Map<Integer, String[]> rankAbilities;
	private final Set<PlayerObject> playerObjects;
	private final PswgScheduledThreadPool threadPool;
	private final DayOfWeek updateWeekDay;
	private final LocalTime updateTime;
	private final ZoneOffset updateOffset;
	private int rankEpoch;
	
	CivilWarService() {
		rankAbilities = new HashMap<>();
		playerObjects = ConcurrentHashMap.newKeySet();
		threadPool = new PswgScheduledThreadPool(1, "civil-war-service");
		// Rank update time is the night between Thursday and Friday at 00:00 UTC
		updateWeekDay = DayOfWeek.FRIDAY;
		updateTime = LocalTime.MIDNIGHT;
		updateOffset = OffsetDateTime.now().getOffset();
		
		loadRankAbilities();
		
		registerForIntent(CivilWarPointIntent.class, this::handleCivilWarPointIntent);
		registerForIntent(CreatureKilledIntent.class, this::handleCreatureKilledIntent);
		registerForIntent(DestroyObjectIntent.class, this::handleDestroyObjectIntent);
		registerForIntent(FactionIntent.class, this::handleFactionIntent);
		registerForIntent(ObjectCreatedIntent.class, this::handleObjectCreatedIntent);
	}
	
	@Override
	public boolean initialize() {
		threadPool.start();
		scheduleRankUpdate();
		
		return super.initialize();
	}
	
	@Override
	public boolean terminate() {
		threadPool.stop(); // To stop more threads from running
		threadPool.awaitTermination(1000); // Waits for them to complete executing
		
		return super.terminate();
	}
	
	int leftoverPoints(float progress, int points) {
		return (int) ((progress - 100) / 100 * (float) points);
	}
	
	boolean isRankDown(float oldProgress, float newProgress) {
		return oldProgress + newProgress < 0;
	}
	
	int nextUpdateTime(LocalDate now) {
		LocalDate nextUpdateDate = now.with(TemporalAdjusters.next(updateWeekDay));
		
		return (int) nextUpdateDate.toEpochSecond(updateTime, updateOffset);
	}
	
	boolean isDecayRank(int rank) {
		return rank >= 7;    // Lieutenant and above has decay
	}
	
	float rankProgress(float currentProgress, float decay, int currentRank, int points) {
		float progress = currentProgress - decay;
		
		progress += (float) points / (currentRank * 100.0f);
		
		return progress;
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
	
	private void loadRankAbilities() {
		String what = "rank abilities";
		long startTime = StandardLog.onStartLoad(what);
		
		try (SdbLoader.SdbResultSet set = SdbLoader.load(new File("serverdata/gcw/abilities.sdb"))) {
			while (set.next()) {
				int rank = (int) set.getInt("rank");
				String imperial = set.getText("imperial");
				String rebel = set.getText("rebel");
				String[] abilities = new String[2];
				
				abilities[IMPERIAL_INDEX] = imperial;
				abilities[REBEL_INDEX] = rebel;
				
				rankAbilities.put(rank, abilities);
			}
		} catch (IOException e) {
			Log.e(e);
		}
		
		StandardLog.onEndLoad(rankAbilities.size(), what, startTime);
	}
	
	private void scheduleRankUpdate() {
		LocalDate nowDate = LocalDate.now();
		LocalTime nowTime = LocalTime.now();
		int nowEpoch = (int) nowDate.toEpochSecond(nowTime, updateOffset);
		rankEpoch = nextUpdateTime(nowDate);    // Is in the future
		
		int delay = (rankEpoch - nowEpoch) * 1000;    // Must be milliseconds
		
		Log.d("GCW update occurs in: %d minutes", (rankEpoch - nowEpoch) / 60);
		
		threadPool.execute(delay, this::updateRanks);
		playerObjects.forEach(this::updateTimer);
	}
	
	private void updateTimer(PlayerObject playerObject) {
		playerObject.setGcwNextUpdate(rankEpoch);
	}
	
	private void changeRank(PlayerObject playerObject, int newRank) {
		assert (newRank < 1 || newRank > 12);
		
		int oldRank = playerObject.getCurrentRank();
		
		assert (oldRank > 1);
		
		int highestRank = Math.max(oldRank, newRank);
		int lowestRank = Math.min(oldRank, newRank);
		CreatureObject creature = (CreatureObject) playerObject.getParent();
		PvpFaction faction = creature.getPvpFaction();
		int abilityIndex = faction == PvpFaction.IMPERIAL ? IMPERIAL_INDEX : REBEL_INDEX;
		
		playerObject.setCurrentRank(newRank);
		
		if (oldRank > newRank) {
			// They've been demoted
			for (int rank = highestRank; rank >= lowestRank; rank--) {
				if (!isDecayRank(rank)) {
					break;
				}
				
				String ability = rankAbilities.get(rank)[abilityIndex];
				
				creature.removeAbility(ability);
			}
		} else if (oldRank < newRank) {
			// They've been promoted
			for (int rank = lowestRank; rank <= highestRank; rank++) {
				if (!isDecayRank(rank)) {
					continue;
				}
				
				String ability = rankAbilities.get(rank)[abilityIndex];
				
				creature.addAbility(ability);
			}
		}
	}
	
	private void moveToLifetime(PlayerObject playerObject, int points) {
		int kills = playerObject.getPvpKills();
		
		// Reset current kills and points
		playerObject.setGcwPoints(0);
		playerObject.setPvpKills(0);
		
		// Add current stats to lifetime stats
		playerObject.setLifetimeGcwPoints(playerObject.getLifetimeGcwPoints() + points);
		playerObject.setLifetimePvpKills(playerObject.getLifetimePvpKills() + kills);
	}
	
	private void updateRank(PlayerObject playerObject) {
		int currentRank = playerObject.getCurrentRank();
		float oldProgress = playerObject.getRankProgress();
		int points = playerObject.getGcwPoints();
		float decay = 0;
		
		if (isDecayRank(currentRank)) {
			decay = currentRank;
		}
		
		float newProgress = rankProgress(oldProgress, decay, currentRank, points);
		
		if (newProgress >= 100) {
			int promotion = playerObject.getCurrentRank() + 1;
			
			if (promotion > 12) {	// 12 is the max rank
				playerObject.setRankProgress(99.99F);
				moveToLifetime(playerObject, points);
				return;
			}
			
			changeRank(playerObject, promotion);
			
			// Leftover points carry over
			int leftoverPoints = leftoverPoints(newProgress, points);
			
			// Calculate progress within the new rank using leftover points
			float nextRankProgress = rankProgress(newProgress, 0, promotion, leftoverPoints);
			
			if (nextRankProgress >= 100) {
				// They've ranked up and can rank up again
				updateRank(playerObject);
				return;
			} else {
				// They've ranked up, but cannot rank up again
				playerObject.setRankProgress(nextRankProgress);
			}
			
		} else if (newProgress > 0) {
			// Set their new progress
			playerObject.setRankProgress(newProgress);
		} else if (newProgress < 0 && isRankDown(oldProgress, newProgress)) {
			int demotion = playerObject.getCurrentRank() - 1;
			
			if (demotion < 1) {	// 1 is the minimum rank
				playerObject.setRankProgress(0);
				return;
			}
			
			changeRank(playerObject, demotion);
			
			// Push their progress backwards in the new rank
			int leftoverPoints = leftoverPoints(newProgress, points);
			
			// Calculate progress within the new rank using leftover points
			float nextRankProgress = 100 - rankProgress(newProgress, 0, demotion, leftoverPoints);
			
			if (nextRankProgress <= 0) {
				// They've ranked down and can rank down again
				updateRank(playerObject);
				moveToLifetime(playerObject, points);
				return;
			} else {
				// They've ranked down, but cannot rank down again
				playerObject.setRankProgress(nextRankProgress);
			}
		}
		
		moveToLifetime(playerObject, points);
	}
	
	private void updateRanks() {
		playerObjects.forEach(playerObject -> {
			if (playerObject.getCurrentRank() > 0)
				updateRank(playerObject);
		});
		
		scheduleRankUpdate();    // Schedule next rank update
	}
	
	private void grantPoints(ProsePackage prose, PlayerObject receiver, int points) {
		receiver.setGcwPoints(receiver.getGcwPoints() + points);
		SystemMessageIntent.broadcastPersonal(receiver.getOwner(), prose);
	}
	
	private void handleCivilWarPointIntent(CivilWarPointIntent cwpi) {
		int points = cwpi.getPoints();
		PlayerObject receiver = cwpi.getReceiver();
		ProsePackage prose = new ProsePackage(new StringId("gcw", "gcw_rank_generic_point_grant"), "DI", points);
		
		grantPoints(prose, receiver, points);
	}
	
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
			killerCreature.sendObserversAndSelf(new PlayClientEffectObjectMessage(effectFile, "head", killerCreature.getObjectId(), ""));
			
			// Send sound to just to the killer
			killerCreature.sendSelf(new PlayMusicMessage(0, soundFile, 0, false));
		} else {
			// NPC GCW point system message
			prose = new ProsePackage(new StringId("gcw", "gcw_rank_generic_point_grant"), "DI", granted);
		}
		
		// Increment GCW point counter
		grantPoints(prose, killerPlayer, granted);
	}
	
	private void handleFactionIntent(FactionIntent fi) {
		if (fi.getUpdateType() != FactionIntent.FactionIntentType.FACTIONUPDATE) {
			return;
		}
		
		TangibleObject target = fi.getTarget();
		
		if (!(target instanceof CreatureObject)) {
			return;
		}
		
		CreatureObject creature = (CreatureObject) target;
		
		if (!creature.isPlayer()) {
			// NPCs don't have factional ranks
			return;
		}
		
		PlayerObject playerObject = creature.getPlayerObject();
		
		if (fi.getNewFaction() == PvpFaction.NEUTRAL) {
			// They've left the imperials or rebels and must have rank removed
			playerObject.setCurrentRank(0);
			
			int points = playerObject.getGcwPoints();
			
			moveToLifetime(playerObject, points);
		} else {
			// They've joined the imperials or rebels and become Privates
			playerObject.setCurrentRank(1);
		}
	}
	
	private void handleDestroyObjectIntent(DestroyObjectIntent doi) {
		SWGObject object = doi.getObject();
		
		if (!(object instanceof PlayerObject)) {
			return;
		}
		
		playerObjects.remove(object);
	}
	
	private void handleObjectCreatedIntent(ObjectCreatedIntent oci) {
		SWGObject object = oci.getObject();
		
		if (!(object instanceof PlayerObject)) {
			return;
		}
		
		PlayerObject playerObject = (PlayerObject) object;
		
		// Let's make sure the timer's displayed for them
		updateTimer(playerObject);
		
		playerObjects.add(playerObject);
	}
	
}
