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
import intents.FlourishIntent;
import intents.PlayerEventIntent;
import intents.WatchIntent;
import intents.chat.ChatBroadcastIntent;
import intents.experience.ExperienceIntent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import network.packets.swg.zone.object_controller.Animation;
import resources.Posture;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.DatatableData;
import resources.control.Intent;
import resources.control.Service;
import resources.encodables.ProsePackage;
import resources.encodables.StringId;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
import resources.server_info.Log;

/**
 *
 * @author Mads
 */
public class EntertainmentService extends Service {

	// TODO: when performing, make NPCs in a radius of x look towards the player (?) and clap. When they stop, turn back (?) and stop clapping
	private static final byte XP_CYCLE_RATE = 10;
	
	private final Map<String, PerformanceData> performanceMap;	// performance names mapped to performance data
	private final Map<CreatureObject, Performance> performerMap;
	private final Map<String, String> danceMap;	// Maps performance ID to performance name
	private final ScheduledExecutorService executorService;

	public EntertainmentService() {
		performanceMap = new HashMap<>();
		performerMap = new HashMap<>();	// TODO synchronize access?
		danceMap = new HashMap<>();
		executorService = Executors.newSingleThreadScheduledExecutor();
		registerForIntent(DanceIntent.TYPE);
		registerForIntent(PlayerEventIntent.TYPE);
		registerForIntent(FlourishIntent.TYPE);
		registerForIntent(WatchIntent.TYPE);
	}

	@Override
	public boolean initialize() {
		DatatableData performanceTable = (DatatableData) ClientFactory.getInfoFromFile("datatables/performance/performance.iff");

		for (int i = 0; i < performanceTable.getRowCount(); i++) {
			String requiredDance = (String) performanceTable.getCell(i, 4);
			
			// Load the dances only. Music is currently unsupported.
			if (!requiredDance.isEmpty()) {
				String performanceName = (String) performanceTable.getCell(i, 0);
				String performanceNumber = String.valueOf(performanceTable.getCell(i, 5));	// danceVisualId
				PerformanceData performanceData = new PerformanceData(
						performanceNumber,
						(int) performanceTable.getCell(i, 10));	// flourishXpMod
				
				performanceMap.put(performanceName, performanceData);	// Map the name to the performance data
				danceMap.put(performanceNumber, performanceName);	// Map the dance ID to a performance name!
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
			case FlourishIntent.TYPE:
				handleFlourishIntent((FlourishIntent) i);
				break;
			case WatchIntent.TYPE:
				handleWatchIntent((WatchIntent) i);
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
			} else if (performanceMap.containsKey(danceName)) {
				// The dance name is valid.
				if (dancer.hasAbility("startDance+" + danceName)) {
					startDancing(dancer, danceName);
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
		CreatureObject creature = i.getPlayer().getCreatureObject();
		
		// Null creatures or non-entertainers are ignored!
		if(creature == null || !isEntertainer(creature))
			return;
		
		switch(i.getEvent()) {
			case PE_LOGGED_OUT:
				// Don't keep giving them XP if they log out
				if(creature.getPosture().equals(Posture.SKILL_ANIMATING)) {
					cancelExperienceTask(creature);
				}
				
				break;
			case PE_ZONE_IN_SERVER: 
				// We need to check if they're dancing in order to start giving them XP
				if(creature.getPosture().equals(Posture.SKILL_ANIMATING)) {
					scheduleExperienceTask(creature, danceMap.get(creature.getAnimation().replace("dance_", "")));
				}
				
				break;
		}
	}

	private void handleFlourishIntent(FlourishIntent i) {
		Player performer = i.getPerformer();
		CreatureObject performerObject = performer.getCreatureObject();
		
		// TODO performance counter check
		performerObject.setPerformanceCounter(performerObject.getPerformanceCounter() + 1);
		
		// Send the flourish animation to the owner of the creature and owners of creatures observing
		performerObject.sendObserversAndSelf(new Animation(performerObject.getObjectId(), i.getFlourishName()));
		new ChatBroadcastIntent(performer, "@performance:flourish_perform").broadcast();
	}
	
	private void handleWatchIntent(WatchIntent i) {
		SWGObject target = i.getTarget();
		
		if(target instanceof CreatureObject) {
			CreatureObject actor = i.getActor();
			CreatureObject creature = (CreatureObject) target;
			Player actorOwner = actor.getOwner();
			
			if(!isEntertainer(creature)) {
				// We can't watch non-entetainers - do nothing
				return;
			}
			
			if(creature.isPlayer()) {
				if(creature.isPerforming()) {
					Performance performance = performerMap.get(creature);
					
					if(performance.addSpectator(actor)) {
						actor.setMoodAnimation("entertained");
						new ChatBroadcastIntent(actorOwner, new ProsePackage(new StringId("performance", "dance_watch_self"), "TT", creature.getName())).broadcast();
						actor.setPerformanceListenTarget(target.getObjectId());
					}
				} else {
					// While this is a valid target for watching, the target is currently not performing.
					new ChatBroadcastIntent(actorOwner, new ProsePackage(new StringId("performance", "dance_watch_not_dancing"), "TT", creature.getName())).broadcast();
				}
			} else {
				// You can't watch NPCs, regardless of whether they're dancing or not
				new ChatBroadcastIntent(actorOwner, "@performance:dance_watch_npc").broadcast();
			}
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
	
	private void scheduleExperienceTask(CreatureObject performer, String performanceName) {
		Log.d(this, "Scheduled %s to receive XP every %d seconds", performer, XP_CYCLE_RATE);
		synchronized(performerMap) {
			Future<?> future = executorService.scheduleAtFixedRate(new EntertainerExperience(performer), XP_CYCLE_RATE, XP_CYCLE_RATE, TimeUnit.SECONDS);
			performerMap.put(performer, new Performance(future, performanceName));
		}
	}
	
	private void cancelExperienceTask(CreatureObject performer) {
		Log.d(this, "%s no longer receives XP every %d seconds", performer, XP_CYCLE_RATE);
		synchronized (performerMap) {
			Performance performance = performerMap.remove(performer);
			
			if(performance == null) {
				Log.e(this, "Couldn't cancel experience task for %s because they weren't found in performerMap", performer);
				return;
			}
			
			Future<?> future = performance.getFuture();
			
			// TODO null check?
			// TODO use return result?
			future.cancel(false);	// Running tasks are allowed to finish.
		}
	}
	
	private void startDancing(CreatureObject dancer, String danceName) {
		dancer.setAnimation("dance_" + performanceMap.get(danceName).getPerformanceId());
		dancer.setPerformanceId(0);	// 0 - anything else will make it look like we're playing music
		dancer.setPerforming(true);
		dancer.setPosture(Posture.SKILL_ANIMATING);
		
		// Only entertainers get XP
		if(isEntertainer(dancer))
			scheduleExperienceTask(dancer, danceName);
		
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

	private class Performance {
		private final Future<?> future;
		private final String performanceName;
		private final Set<CreatureObject> audience;

		public Performance(Future<?> future, String performanceName) {
			this.future = future;
			this.performanceName = performanceName;
			audience = new HashSet<>();
		}

		public Future<?> getFuture() {
			return future;
		}

		public String getPerformanceName() {
			return performanceName;
		}
		
		public boolean addSpectator(CreatureObject spectator) {
			return audience.add(spectator);
		}
		
		public boolean removeSpectator(CreatureObject spectator) {
			return audience.remove(spectator);
		}
		
	}
	
	/**
	 * Data pulled from the performance.iff table
	 */
	private class PerformanceData {
		private final String performanceId;
		private final int flourishXpMod;

		public PerformanceData(String performanceId, int flourishXpMod) {
			this.performanceId = performanceId;
			this.flourishXpMod = flourishXpMod;
		}

		public String getPerformanceId() {
			return performanceId;
		}

		public int getFlourishXpMod() {
			return flourishXpMod;
		}
		
	}
	
	private class EntertainerExperience implements Runnable {

		private final CreatureObject performer;
		
		private EntertainerExperience(CreatureObject performer) {
			this.performer = performer;
		}
		
		@Override
		public void run() {
			Performance performance = performerMap.get(performer);
			
			if(performance == null) {
				Log.e("EntertainerExperience", "Performer %s wasn't in performermap", performer);
				return;
			}
			
			String performanceName = performance.getPerformanceName();
			PerformanceData performanceData = performanceMap.get(performanceName);
			int flourishXpMod = performanceData.getFlourishXpMod();
			int performanceCounter = performer.getPerformanceCounter();
			int xpGained = (int) (performanceCounter * (flourishXpMod * 3.8));
			
			if(xpGained > 0) {
				new ExperienceIntent(performer, "entertainer", xpGained).broadcast();
				performer.setPerformanceCounter(performanceCounter - 1);
			}
		}
		
	}
	
}
