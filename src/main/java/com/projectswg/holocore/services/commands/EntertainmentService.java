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
package com.projectswg.holocore.services.commands;

import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.data.swgfile.visitors.DatatableData;
import com.projectswg.common.network.packets.swg.zone.object_controller.Animation;
import com.projectswg.holocore.intents.DanceIntent;
import com.projectswg.holocore.intents.FlourishIntent;
import com.projectswg.holocore.intents.PlayerEventIntent;
import com.projectswg.holocore.intents.WatchIntent;
import com.projectswg.holocore.intents.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.experience.ExperienceIntent;
import com.projectswg.holocore.intents.player.PlayerTransformedIntent;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import com.projectswg.holocore.resources.player.Player;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Mads
 */
public class EntertainmentService extends Service {
	
	// TODO: when performing, make NPCs in a radius of x look towards the player (?) and clap. When they stop, turn back (?) and stop clapping
	private static final byte XP_CYCLE_RATE = 10;
	private static final byte WATCH_RADIUS = 20;
	
	private final Map<String, PerformanceData> performanceMap;    // performance names mapped to performance data
	private final Map<Long, Performance> performerMap;
	private final Map<String, String> danceMap;    // Maps performance ID to performance name
	private final ScheduledExecutorService executorService;
	
	public EntertainmentService() {
		performanceMap = new HashMap<>();
		performerMap = new HashMap<>();    // TODO synchronize access?
		danceMap = new HashMap<>();
		executorService = Executors.newSingleThreadScheduledExecutor();
	}
	
	@Override
	public boolean initialize() {
		DatatableData performanceTable = (DatatableData) ClientFactory.getInfoFromFile("datatables/performance/performance.iff");
		
		for (int i = 0; i < performanceTable.getRowCount(); i++) {
			String requiredDance = (String) performanceTable.getCell(i, 4);
			
			// Load the dances only. Music is currently unsupported.
			if (!requiredDance.isEmpty()) {
				String performanceName = (String) performanceTable.getCell(i, 0);
				String performanceNumber = String.valueOf(performanceTable.getCell(i, 5));    // danceVisualId
				PerformanceData performanceData = new PerformanceData(performanceNumber, (int) performanceTable.getCell(i, 10));    // flourishXpMod
				
				performanceMap.put(performanceName, performanceData);    // Map the name to the performance data
				danceMap.put(performanceNumber, performanceName);    // Map the dance ID to a performance name!
			}
		}
		
		return super.initialize();
	}
	
	@Override
	public boolean terminate() {
		executorService.shutdownNow();
		return super.terminate();
	}
	
	@IntentHandler
	private void handleDanceIntent(DanceIntent di) {
		CreatureObject dancer = di.getCreatureObject();
		String danceName = di.getDanceName();
		
		if (di.isStartDance()) {
			// This intent wants the creature to start dancing
			// If we're changing dance, allow them to do so
			boolean changeDance = di.isChangeDance();
			
			if (!changeDance && dancer.isPerforming()) {
				new SystemMessageIntent(dancer.getOwner(), "@performance:already_performing_self").broadcast();
			} else if (performanceMap.containsKey(danceName)) {
				// The dance name is valid.
				if (dancer.hasAbility("startDance+" + danceName)) {
					
					if (changeDance) {    // If they're changing dance, we just need to change their animation.
						changeDance(dancer, danceName);
					} else {    // Otherwise, they should begin performing now
						startDancing(dancer, danceName);
					}
				} else {
					// This creature doesn't have the ability to perform this dance.
					new SystemMessageIntent(dancer.getOwner(), "@performance:dance_lack_skill_self").broadcast();
				}
			} else {
				// This dance name is invalid
				new SystemMessageIntent(dancer.getOwner(), "@performance:dance_unknown_self").broadcast();
			}
		} else {
			// This intent wants the creature to stop dancing
			stopDancing(dancer);
		}
	}
	
	@IntentHandler
	private void handlePlayerEventIntent(PlayerEventIntent pei) {
		CreatureObject creature = pei.getPlayer().getCreatureObject();
		
		if (creature == null)
			return;
		
		switch (pei.getEvent()) {
			case PE_LOGGED_OUT:
				// Don't keep giving them XP if they log out
				if (isEntertainer(creature) && creature.getPosture().equals(Posture.SKILL_ANIMATING)) {
					cancelExperienceTask(creature);
				}
				
				break;
			case PE_ZONE_IN_SERVER:
				// We need to check if they're dancing in order to start giving them XP
				if (isEntertainer(creature) && creature.getPosture().equals(Posture.SKILL_ANIMATING)) {
					scheduleExperienceTask(creature, danceMap.get(creature.getAnimation().replace("dance_", "")));
				}
				
				break;
			case PE_DISAPPEAR:
				// If a spectator disappears, they need to stop watching and be removed from the audience
				long performerId = creature.getPerformanceListenTarget();
				
				if (performerId != 0 && performerMap.get(performerId).removeSpectator(creature)) {
					stopWatching(creature, false);
				}
				
				// If a performer disappears, the audience needs to be cleared
				// They're also removed from the map of active performers.
				if (isEntertainer(creature) && creature.isPerforming()) {
					performerMap.get(creature.getObjectId()).clearSpectators();
					performerMap.remove(creature.getObjectId());
				}
				break;
			default:
				break;
		}
	}
	
	@IntentHandler
	private void handleFlourishIntent(FlourishIntent fi) {
		Player performer = fi.getPerformer();
		CreatureObject performerObject = performer.getCreatureObject();
		
		if (performerObject.getPerformanceCounter() != 0)
			return;
		
		performerObject.setPerformanceCounter(1);
		
		// Send the flourish animation to the owner of the creature and owners of creatures observing
		performerObject.sendObservers(new Animation(performerObject.getObjectId(), fi.getFlourishName()));
		new SystemMessageIntent(performer, "@performance:flourish_perform").broadcast();
	}
	
	@IntentHandler
	private void handleWatchIntent(WatchIntent wi) {
		SWGObject target = wi.getTarget();
		
		if (target instanceof CreatureObject) {
			CreatureObject actor = wi.getActor();
			CreatureObject creature = (CreatureObject) target;
			Player actorOwner = actor.getOwner();
			
			if (!isEntertainer(creature)) {
				// We can't watch non-entertainers - do nothing
				return;
			}
			
			if (creature.isPlayer()) {
				if (creature.isPerforming()) {
					Performance performance = performerMap.get(creature.getObjectId());
					
					if (wi.isStartWatch()) {
						if (performance.addSpectator(actor)) {
							startWatching(actor, creature);
						}
					} else {
						if (performance.removeSpectator(actor)) {
							stopWatching(actor, true);
						}
					}
				} else {
					// While this is a valid target for watching, the target is currently not performing.
					new SystemMessageIntent(actorOwner, new ProsePackage(new StringId("performance", "dance_watch_not_dancing"), "TT", creature.getObjectName())).broadcast();
				}
			} else {
				// You can't watch NPCs, regardless of whether they're dancing or not
				new SystemMessageIntent(actorOwner, "@performance:dance_watch_npc").broadcast();
			}
		}
	}
	
	@IntentHandler
	private void handlePlayerTransformedIntent(PlayerTransformedIntent pti) {
		CreatureObject movedPlayer = pti.getPlayer();
		long performanceListenTarget = movedPlayer.getPerformanceListenTarget();
		
		if (performanceListenTarget != 0) {
			// They're watching a performer!
			
			Performance performance = performerMap.get(performanceListenTarget);
			
			if (performance == null) {
				Log.e("Couldn't perform range check on %s, because there was no performer with object ID %d", movedPlayer, performanceListenTarget);
				return;
			}
			
			CreatureObject performer = performance.getPerformer();
			
			Location performerLocation = performer.getWorldLocation();
			Location movedPlayerLocation = pti.getPlayer().getWorldLocation();    // Ziggy: The newLocation in PlayerTransformedIntent isn't the world location, which is what we need here
			
			if (!movedPlayerLocation.isWithinDistance(performerLocation, WATCH_RADIUS)) {
				// They moved out of the defined range! Make them stop watching
				if (performance.removeSpectator(movedPlayer)) {
					stopWatching(movedPlayer, true);
				} else {
					Log.w("%s ran out of range of %s, but couldn't stop watching because they weren't watching in the first place", movedPlayer, performer);
				}
			}
		}
		
	}
	
	/**
	 * Checks if the {@code CreatureObject} is a Novice Entertainer.
	 *
	 * @param performer
	 * @return true if {@code performer} is a Novice Entertainer and false if not
	 */
	private boolean isEntertainer(CreatureObject performer) {
		return performer.hasSkill("class_entertainer_phase1_novice");    // First entertainer skillbox
	}
	
	private void scheduleExperienceTask(CreatureObject performer, String performanceName) {
		Log.d("Scheduled %s to receive XP every %d seconds", performer, XP_CYCLE_RATE);
		synchronized (performerMap) {
			long performerId = performer.getObjectId();
			Future<?> future = executorService.scheduleAtFixedRate(new EntertainerExperience(performer), XP_CYCLE_RATE, XP_CYCLE_RATE, TimeUnit.SECONDS);
			
			// If they went LD but came back before disappearing
			if (performerMap.containsKey(performerId)) {
				Performance performance = performerMap.get(performerId);
				performance.setFuture(future);
			} else {
				performerMap.put(performer.getObjectId(), new Performance(performer, future, performanceName));
			}
		}
	}
	
	private void cancelExperienceTask(CreatureObject performer) {
		Log.d("%s no longer receives XP every %d seconds", performer, XP_CYCLE_RATE);
		synchronized (performerMap) {
			Performance performance = performerMap.get(performer.getObjectId());
			
			if (performance == null) {
				Log.e("Couldn't cancel experience task for %s because they weren't found in performerMap", performer);
				return;
			}
			
			Future<?> future = performance.getFuture();
			
			// TODO null check?
			// TODO use return result?
			future.cancel(false);    // Running tasks are allowed to finish.
		}
	}
	
	private void startDancing(CreatureObject dancer, String danceName) {
		dancer.setAnimation("dance_" + performanceMap.get(danceName).getPerformanceId());
		dancer.setPerformanceId(0);    // 0 - anything else will make it look like we're playing music
		dancer.setPerformanceCounter(0);
		dancer.setPerforming(true);
		dancer.setPosture(Posture.SKILL_ANIMATING);
		
		// Only entertainers get XP
		if (isEntertainer(dancer))
			scheduleExperienceTask(dancer, danceName);
		
		new SystemMessageIntent(dancer.getOwner(), "@performance:dance_start_self").broadcast();
	}
	
	private void stopDancing(CreatureObject dancer) {
		if (dancer.isPerforming()) {
			dancer.setPerforming(false);
			dancer.setPosture(Posture.UPRIGHT);
			dancer.setPerformanceCounter(0);
			dancer.setAnimation("");
			
			// Non-entertainers don't receive XP and have no audience - ignore them
			if (isEntertainer(dancer)) {
				cancelExperienceTask(dancer);
				performerMap.remove(dancer.getObjectId()).clearSpectators();
			}
			
			new SystemMessageIntent(dancer.getOwner(), "@performance:dance_stop_self").broadcast();
		} else {
			new SystemMessageIntent(dancer.getOwner(), "@performance:dance_not_performing").broadcast();
		}
	}
	
	private void changeDance(CreatureObject dancer, String newPerformanceName) {
		performerMap.get(dancer.getObjectId()).setPerformanceName(newPerformanceName);
		dancer.setAnimation("dance_" + performanceMap.get(newPerformanceName).getPerformanceId());
	}
	
	private void startWatching(CreatureObject actor, CreatureObject creature) {
		actor.setMoodAnimation("entertained");
		new SystemMessageIntent(actor.getOwner(), new ProsePackage(new StringId("performance", "dance_watch_self"), "TT", creature.getObjectName())).broadcast();
		actor.setPerformanceListenTarget(creature.getObjectId());
	}
	
	private void stopWatching(CreatureObject actor, boolean displaySystemMessage) {
		actor.setMoodAnimation("");
		if (displaySystemMessage)
			new SystemMessageIntent(actor.getOwner(), "@performance:dance_watch_stop_self").broadcast();
		actor.setPerformanceListenTarget(0);
	}
	
	private class Performance {
		
		private final CreatureObject performer;
		private final Set<CreatureObject> audience;
		private Future<?> future;
		private String performanceName;
		
		public Performance(CreatureObject performer, Future<?> future, String performanceName) {
			this.performer = performer;
			this.future = future;
			this.performanceName = performanceName;
			audience = new HashSet<>();
		}
		
		public CreatureObject getPerformer() {
			return performer;
		}
		
		public Future<?> getFuture() {
			return future;
		}
		
		public void setFuture(Future<?> future) {
			this.future = future;
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
		
		public void clearSpectators() {
			audience.forEach(spectator -> stopWatching(spectator, true));
			audience.clear();
		}
		
		public void setPerformanceName(String performanceName) {
			this.performanceName = performanceName;
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
			Performance performance = performerMap.get(performer.getObjectId());
			
			if (performance == null) {
				Log.e("Performer %s wasn't in performermap", performer);
				return;
			}
			
			String performanceName = performance.getPerformanceName();
			PerformanceData performanceData = performanceMap.get(performanceName);
			int flourishXpMod = performanceData.getFlourishXpMod();
			int performanceCounter = performer.getPerformanceCounter();
			int xpGained = performanceCounter * flourishXpMod;
			
			if (xpGained > 0) {
				new ExperienceIntent(performer, "entertainer", xpGained).broadcast();
				performer.setPerformanceCounter(performanceCounter - 1);
			}
		}
		
	}
	
}
