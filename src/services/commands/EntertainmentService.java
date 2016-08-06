/***********************************************************************************
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
package services.commands;

import intents.DanceIntent;
import intents.PlayerEventIntent;
import intents.chat.ChatBroadcastIntent;
import intents.experience.ExperienceIntent;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import resources.Posture;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.DatatableData;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.creature.CreatureObject;
import resources.server_info.Log;

/**
 *
 * @author Mads
 */
public class EntertainmentService extends Service {

	// TODO: when performing, make NPCs in a radius of x look towards the player (?) and clap. When they stop, turn back (?) and stop clapping
	private static final byte XP_CYCLE_RATE = 10;
	
	private final Map<String, Integer> danceMap;	// dance performanceNames mapped to danceId
	private final Map<CreatureObject, Future<?>> performerMap;
	private final ScheduledExecutorService executorService;

	public EntertainmentService() {
		danceMap = new HashMap<>();
		performerMap = new HashMap<>();	// TODO synchronize access?
		executorService = Executors.newSingleThreadScheduledExecutor();
		registerForIntent(DanceIntent.TYPE);
		registerForIntent(PlayerEventIntent.TYPE);
	}

	@Override
	public boolean initialize() {
		DatatableData performanceTable = (DatatableData) ClientFactory.getInfoFromFile("datatables/performance/performance.iff");

		for (int i = 0; i < performanceTable.getRowCount(); i++) {
			String requiredDance = (String) performanceTable.getCell(i, 4);
			
			// Load the dances only. Music is currently unsupported.
			if (!requiredDance.isEmpty()) {
				danceMap.put((String) performanceTable.getCell(i, 0), (int) performanceTable.getCell(i, 5));	// performanceName, danceVisualId
			}
		}

		return super.initialize();
	}

	@Override
	public boolean terminate() {
		executorService.shutdownNow();
		return super.terminate();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		switch (i.getType()) {
			case DanceIntent.TYPE:
				handleDanceIntent((DanceIntent) i);
				break;
			case PlayerEventIntent.TYPE:
				handlePlayerEventIntent((PlayerEventIntent) i);
				break;
		}
	}

	private void handleDanceIntent(DanceIntent i) {
		CreatureObject dancer = i.getCreatureObject();
		String danceName = i.getDanceName();

		if (i.isStartDance()) {
			// This intent wants the creature to start dancing
			if (dancer.isPerforming()) {
				new ChatBroadcastIntent(dancer.getOwner(), "@performance:already_performing_self").broadcast();
			} else if (danceMap.containsKey(danceName)) {
				// The dance name is valid.
				if (dancer.hasAbility("startDance+" + danceName)) {
					startDancing(dancer, "dance_" + danceMap.get(danceName));
				} else {
					// This creature doesn't have the ability to perform this dance.
					new ChatBroadcastIntent(dancer.getOwner(), "@performance:dance_lack_skill_self").broadcast();
				}
			} else {
				// This dance name is invalid
				new ChatBroadcastIntent(dancer.getOwner(), "@performance:dance_unknown_self").broadcast();
			}
		} else {
			// This intent wants the creature to stop dancing
			stopDancing(dancer);
		}
	}
	
	private void handlePlayerEventIntent(PlayerEventIntent i) {
		switch(i.getEvent()) {
			case PE_LOGGED_OUT:
				// Don't keep giving them XP if they log out
				CreatureObject creature = i.getPlayer().getCreatureObject();
				
				if(creature.getPosture().equals(Posture.SKILL_ANIMATING)) {
					cancelExperienceTask(creature);
				}
				
				break;
			case PE_ZONE_IN_SERVER: 
				// We need to check if they're dancing in order to start giving them XP
				creature = i.getPlayer().getCreatureObject();
				
				if(creature.getPosture().equals(Posture.SKILL_ANIMATING)) {
					scheduleExperienceTask(creature);
				}
				
				break;
		}
	}

	
	/**
	 * Checks if the {@code CreatureObject} is a Novice Entertainer.
	 * @param performer
	 * @return true if {@code performer} is a Novice Entertainer and false if not
	 */
	private boolean isEntertainer(CreatureObject performer) {
		return performer.hasSkill("class_entertainer_phase1_novice");	// First entertainer skillbox
	}
	
	private void scheduleExperienceTask(CreatureObject performer) {
		Log.d(this, "Scheduled %s to receive XP every %d seconds", performer, XP_CYCLE_RATE);
		synchronized(performerMap) {
			performerMap.put(performer, executorService.scheduleAtFixedRate(new EntertainerExperience(performer), XP_CYCLE_RATE, XP_CYCLE_RATE, TimeUnit.SECONDS));
		}
	}
	
	private void cancelExperienceTask(CreatureObject performer) {
		Log.d(this, "%s no longer receives XP every %d seconds", performer, XP_CYCLE_RATE);
		synchronized (performerMap) {
			Future<?> future = performerMap.remove(performer);
			
			// TODO null check?
			// TODO use return result?
			future.cancel(false);	// Running tasks are allowed to finish.
		}
	}
	
	private void startDancing(CreatureObject dancer, String danceId) {
		dancer.setAnimation(danceId);
		dancer.setPerformanceId(0);	// 0 - anything else will make it look like we're playing music
		dancer.setPerforming(true);
		dancer.setPosture(Posture.SKILL_ANIMATING);
		
		// Only entertainers get XP
		if(isEntertainer(dancer))
			scheduleExperienceTask(dancer);
		
		new ChatBroadcastIntent(dancer.getOwner(), "@performance:dance_start_self").broadcast();
	}

	private void stopDancing(CreatureObject dancer) {
		if (dancer.isPerforming()) {
			dancer.setPerforming(false);
			dancer.setPosture(Posture.UPRIGHT);
			dancer.setPerformanceCounter(0);
			dancer.setAnimation("");
			
			// Non-entertainers don't receive XP - ignore them
			if(isEntertainer(dancer))
				cancelExperienceTask(dancer);
			
			new ChatBroadcastIntent(dancer.getOwner(), "@performance:dance_stop_self").broadcast();
		} else {
			new ChatBroadcastIntent(dancer.getOwner(), "@performance:dance_not_performing").broadcast();
		}
	}

	private class EntertainerExperience implements Runnable {

		private final CreatureObject performer;
		
		private EntertainerExperience(CreatureObject performer) {
			this.performer = performer;
		}
		
		@Override
		public void run() {
			int xpGained = 123;	// TODO: This depends on the performance!
			new ExperienceIntent(performer, "entertainer", xpGained).broadcast();
		}
		
	}
	
}
