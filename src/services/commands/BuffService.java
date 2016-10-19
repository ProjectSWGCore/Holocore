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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import intents.BuffIntent;
import intents.PlayerEventIntent;
import intents.SkillModIntent;
import java.util.Locale;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Stream;
import network.packets.swg.zone.PlayClientEffectObjectMessage;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.DatatableData;
import resources.common.CRC;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.creature.Buff;
import resources.objects.creature.CreatureObject;
import resources.server_info.Log;
import utilities.ThreadUtilities;

public class BuffService extends Service {
	
	// TODO allow removal of non-debuffs by right-clicking them
	// TODO remove buffs on respec. Listen for respec event and remove buffs with BuffData where REMOVE_ON_RESPEC == 1
	// TODO remove group buff(s) from receiver when distance between caster and receiver is 100m. Perform same check upon zoning in
	// TODO on deathblow, decay buffs with BuffData where DECAY_ON_PVP_DEATH == 1
	
	private final ScheduledExecutorService executor;
	private final Map<CreatureObject, DelayQueue<BuffDelayed>> buffRemoval;
	private final Map<CRC, BuffData> dataMap;	// All CRCs are lower-cased buff names!
	
	public BuffService() {
		registerForIntent(BuffIntent.TYPE);
		registerForIntent(PlayerEventIntent.TYPE);
		
		buffRemoval = new HashMap<>();
		dataMap = new HashMap<>();
		executor = Executors.newSingleThreadScheduledExecutor(ThreadUtilities.newThreadFactory("buff-service"));
	}
	
	@Override
	public boolean initialize() {
		long startTime = System.nanoTime();
		Log.i(this, "Loading buffs...");
		loadBuffs();
		Log.i(this, "Finished loading %d buffs in %fms", dataMap.size(), (System.nanoTime() - startTime) / 1E6);
		return super.initialize();
	}

	@Override
	public boolean start() {
		// Polls buff queues every second
		executor.scheduleAtFixedRate(() -> buffRemoval.values().forEach(buffQueue -> checkBuffQueue(buffQueue)), 1, 1, TimeUnit.SECONDS);
		
		return super.start();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		switch(i.getType()) {
			case BuffIntent.TYPE: handleBuffIntent((BuffIntent) i); break;
			case PlayerEventIntent.TYPE: handlePlayerEventIntent((PlayerEventIntent) i); break;
		}
	}
	
	@Override
	public boolean stop() {
		executor.shutdown();
		
		return super.stop();
	}
	
	private void checkBuffQueue(DelayQueue<BuffDelayed> buffQueue) {
		BuffDelayed buffToRemove = buffQueue.poll();
		
		if (buffToRemove != null) {
			removeBuff(buffToRemove.getCreature(), buffToRemove.getBuffCrc(), true);
		}
	}
	
	private void loadBuffs() {
		DatatableData buffTable = (DatatableData) ClientFactory.getInfoFromFile("datatables/buff/buff.iff");
		
		int group1 = buffTable.getColumnFromName("GROUP1");
		int priority = buffTable.getColumnFromName("PRIORITY");
		int maxStacks = buffTable.getColumnFromName("MAX_STACKS");
		int effect1Param = buffTable.getColumnFromName("EFFECT1_PARAM");
		int effect1Value = buffTable.getColumnFromName("EFFECT1_VALUE");
		int effect2Param = buffTable.getColumnFromName("EFFECT2_PARAM");
		int effect2Value = buffTable.getColumnFromName("EFFECT2_VALUE");
		int effect3Param = buffTable.getColumnFromName("EFFECT3_PARAM");
		int effect3Value = buffTable.getColumnFromName("EFFECT3_VALUE");
		int effect4Param = buffTable.getColumnFromName("EFFECT4_PARAM");
		int effect4Value = buffTable.getColumnFromName("EFFECT4_VALUE");
		int effect5Param = buffTable.getColumnFromName("EFFECT5_PARAM");
		int effect5Value = buffTable.getColumnFromName("EFFECT5_VALUE");
		int duration = buffTable.getColumnFromName("DURATION");
		int particle = buffTable.getColumnFromName("PARTICLE");
		int particleHardpoint = buffTable.getColumnFromName("PARTICLE_HARDPOINT");
		int callback = buffTable.getColumnFromName("CALLBACK");
		
		for(int row = 0; row < buffTable.getRowCount(); row++) {
			dataMap.put(new CRC(((String) buffTable.getCell(row, 0)).toLowerCase(Locale.ENGLISH)), new BuffData(
					(String) buffTable.getCell(row, group1),
					(int) buffTable.getCell(row, priority),
					(int) buffTable.getCell(row, maxStacks),
					(String) buffTable.getCell(row, effect1Param),
					(float) buffTable.getCell(row, effect1Value),
					(String) buffTable.getCell(row, effect2Param),
					(float) buffTable.getCell(row, effect2Value),
					(String) buffTable.getCell(row, effect3Param),
					(float) buffTable.getCell(row, effect3Value),
					(String) buffTable.getCell(row, effect4Param),
					(float) buffTable.getCell(row, effect4Value),
					(String) buffTable.getCell(row, effect5Param),
					(float) buffTable.getCell(row, effect5Value),
					(float) buffTable.getCell(row, duration),
					(String) buffTable.getCell(row, particle),
					(String) buffTable.getCell(row, particleHardpoint),
					(String) buffTable.getCell(row, callback)
			));
		} 
	}
	
	private void handleBuffIntent(BuffIntent i) {
		CRC buffCrc = new CRC(i.getBuffName().toLowerCase(Locale.ENGLISH));
		
		if (i.isRemove()) {
			removeBuff(i.getReceiver(), buffCrc, false);
		} else {
			addBuff(buffCrc, i.getReceiver(), i.getBuffer());
		}
	}
	
	private void handlePlayerEventIntent(PlayerEventIntent pei) {
		CreatureObject creature = pei.getPlayer().getCreatureObject();
		
		switch(pei.getEvent()) {
			case PE_FIRST_ZONE: handleFirstZone(creature); break;
			case PE_DISAPPEAR: handleDisappear(creature); break;	
		}
	}
	
	private void handleFirstZone(CreatureObject creature) {
		creature.getBuffs().forEach((crc, buff) -> manageBuff(buff, crc, creature));
	}
	
	private void handleDisappear(CreatureObject creature) {
		synchronized(buffRemoval) {
			buffRemoval.remove(creature);
		}
	}
	
	private void addBuff(CRC buffCrc, CreatureObject receiver, CreatureObject buffer) {
		BuffData buffData = dataMap.get(buffCrc);
		
		if (buffData == null) {
			Log.e(this, "Could not add %s to %s - buff data for it does not exist", buffCrc, receiver);
			return;
		}
		
		String groupName = buffData.getGroupName();

		// Let's see if they have any buffs in this group already
		Stream<CRC> buffsToRemove = receiver.getBuffs().keySet().stream().filter(candidate -> checkGroup(groupName, candidate, buffData));
		long groupBuffCount = buffsToRemove.count();
		
		// If not, let's just stop here
		if (groupBuffCount <= 0) {
			return;
		} else if (groupBuffCount > 1) {
			// Only one buff per group should be possible
			Log.e(this, "%s had multiple buffs from the same group %s!", receiver, groupName);
		}

		buffsToRemove.forEach(crc -> removeBuff(receiver, crc, false));

		// The client-side timer hinges on the playTime of the PlayerObject.
		// We therefore must update it to current time, so the timer starts from full duration
		receiver.getPlayerObject().updatePlayTime();
		
        // TODO stack counts upon add/remove probably need to be defined on a per-buff basis due to skillmod influence.
		int stackCount = 1;
		int buffDuration = (int) buffData.getDefaultDuration();
		Buff buff = new Buff(receiver.getPlayerObject().getPlayTime() + buffDuration, buffData.getEffect1Value(), buffDuration, buffer.getObjectId(), stackCount);
		
		sendSkillModIntent(buffData, receiver, false);
		receiver.addBuff(buffCrc, buff);
		manageBuff(buff, buffCrc, receiver);
		
		String effectFileName = buffData.getEffectFileName();
		
		if (!effectFileName.isEmpty())
			receiver.sendObserversAndSelf(new PlayClientEffectObjectMessage(effectFileName, buffData.getParticleHardPoint(), receiver.getObjectId()));
	}
	
	private boolean checkGroup(String groupName, CRC candidate, BuffData existingBuffData) {
		BuffData candidateData = dataMap.get(candidate);
		
		return candidateData.getGroupName().equals(groupName) && candidateData.getGroupPriority() >= existingBuffData.getGroupPriority();
	}
	
	private void manageBuff(Buff buff, CRC buffCrc, CreatureObject creature) {
		if (buff.getEndTime() <= 0) {
			// If this buff has less than or 0 seconds left, then remove it.
			removeBuff(creature, buffCrc, true);
		} else if(buff.getDuration() >= 0) {
			// If this buff doesn't last forever or hasn't expired, we'll schedule it for removal in the future
			synchronized (buffRemoval) {
				DelayQueue<BuffDelayed> buffQueue = buffRemoval.get(creature);

				if(buffQueue == null) {
					buffQueue = new DelayQueue<>();
					buffRemoval.put(creature, buffQueue);
				}

				buffQueue.add(new BuffDelayed(buff, buffCrc, creature));
			}
		}
	}
	
	private void removeBuff(CreatureObject creature, CRC buffCrc, boolean expired) {
		BuffData buffData = dataMap.get(buffCrc);
		
		if (buffData == null) {
			Log.e(this, "Could not remove %s from %s - buff data for it does not exist", buffCrc, creature);
			return;
		}
		
		Buff buff = creature.getBuffByCrc(buffCrc);
		
		// Check if this buff can be stacked
		if (buffData.getMaxStackCount() > 1 && !expired) {
			// Check if this buff has been stacked
			if(buff.getStackCount() > 1) {				
				// If it has, reduce the stack count and reset the duration.
				// TODO NGE: buffs can, based on skillmods, adjust with different values
				creature.adjustBuffStackCount(buffCrc, -1);
			}
		} else {
			// Remove skillmods
			sendSkillModIntent(buffData, creature, true);
			
			// Remove the buff from the creature
			creature.removeBuff(buffCrc);
			
			String callback = buffData.getCallback();
			
			if(callback.equals("none"))
				return;
			
			CRC callbackCrc = new CRC(callback.toLowerCase(Locale.ENGLISH));
			if(dataMap.containsKey(callbackCrc)) {
				// Apply the callback buff
				addBuff(callbackCrc, creature, creature);
			} else {
				// Call the callback command script
				// TODO
			}
		}
	}
	
	private void sendSkillModIntent(BuffData buffData, CreatureObject creature, boolean remove) {
		String effect1Name = buffData.getEffect1Name();
		String effect2Name = buffData.getEffect2Name();
		String effect3Name = buffData.getEffect3Name();
		String effect4Name = buffData.getEffect4Name();
		String effect5Name = buffData.getEffect5Name();
		
		int valueFactor = remove ? -1 : 1;
		
		if (!effect1Name.isEmpty())
			new SkillModIntent(effect1Name, 0, (int) buffData.getEffect1Value() * valueFactor, creature).broadcast();
		if (!effect2Name.isEmpty())
			new SkillModIntent(effect2Name, 0, (int) buffData.getEffect2Value() * valueFactor, creature).broadcast();
		if (!effect3Name.isEmpty())
			new SkillModIntent(effect3Name, 0, (int) buffData.getEffect3Value() * valueFactor, creature).broadcast();
		if (!effect4Name.isEmpty())
			new SkillModIntent(effect4Name, 0, (int) buffData.getEffect4Value() * valueFactor, creature).broadcast();
		if (!effect5Name.isEmpty())
			new SkillModIntent(effect5Name, 0, (int) buffData.getEffect5Value() * valueFactor, creature).broadcast();
	}
	
	private static class BuffDelayed implements Delayed {
		
		private final Buff buff;
		private final CRC buffCrc;
		private final CreatureObject creature;
		
		private BuffDelayed(Buff buff, CRC buffCrc, CreatureObject creature) {
			this.buff = buff;
			this.buffCrc = buffCrc;
			this.creature = creature;
		}
		
		@Override
		public int compareTo(Delayed o) {
			return Integer.compare(buff.getEndTime(), ((BuffDelayed) o).buff.getEndTime());
		}

		@Override
		public long getDelay(TimeUnit timeUnit) {
			return timeUnit.convert(buff.getEndTime() - System.currentTimeMillis() / 1000, TimeUnit.MILLISECONDS);
		}

		public Buff getBuff() {
			return buff;
		}

		public CRC getBuffCrc() {
			return buffCrc;
		}

		public CreatureObject getCreature() {
			return creature;
		}
		
	}
	
	/**
	 * @author Mads
	 * Each instance of this class holds the base information
	 * for a specific buff name.
	 * 
	 * Example: Instead of each {@code Buff} instance storing the max amount of
	 * times you can stack it, a shared class stores that information.
	 * 
	 * With many {@code Buff} instances in play, this will result in a noticeable
	 * memory usage reduction.
	 */
	private static class BuffData {
		
		private final String groupName;
		private final int groupPriority;
		private final int maxStackCount;
		private final String effect1Name;
		private final float effect1Value;
		private final String effect2Name;
		private final float effect2Value;
		private final String effect3Name;
		private final float effect3Value;
		private final String effect4Name;
		private final float effect4Value;
		private final String effect5Name;
		private final float effect5Value;
		private final float defaultDuration;
		private final String effectFileName;
		private final String particleHardPoint;
		private final String callback;
		
		private BuffData(String groupName, int groupPriority, int maxStackCount, String effect1Name, float effect1Value, String effect2Name, float effect2Value, String effect3Name, float effect3Value, String effect4Name, float effect4Value, String effect5Name, float effect5Value, float defaultDuration, String effectFileName, String particleHardPoint, String callback) {
			this.groupName = groupName;
			this.groupPriority = groupPriority;
			this.maxStackCount = maxStackCount;
			this.effect1Name = effect1Name;
			this.effect1Value = effect1Value;
			this.effect2Name = effect2Name;
			this.effect2Value = effect2Value;
			this.effect3Name = effect3Name;
			this.effect3Value = effect3Value;
			this.effect4Name = effect4Name;
			this.effect4Value = effect4Value;
			this.effect5Name = effect5Name;
			this.effect5Value = effect5Value;
			this.defaultDuration = defaultDuration;
			this.effectFileName = effectFileName;
			this.particleHardPoint = particleHardPoint;
			this.callback = callback;
		}

		public String getGroupName() {
			return groupName;
		}

		public int getGroupPriority() {
			return groupPriority;
		}

		private int getMaxStackCount() {
			return maxStackCount;
		}

		private String getEffect1Name() {
			return effect1Name;
		}

		private float getEffect1Value() {
			return effect1Value;
		}

		private String getEffect2Name() {
			return effect2Name;
		}

		private float getEffect2Value() {
			return effect2Value;
		}

		private String getEffect3Name() {
			return effect3Name;
		}

		private float getEffect3Value() {
			return effect3Value;
		}

		private String getEffect4Name() {
			return effect4Name;
		}

		private float getEffect4Value() {
			return effect4Value;
		}

		private String getEffect5Name() {
			return effect5Name;
		}

		private float getEffect5Value() {
			return effect5Value;
		}

		private float getDefaultDuration() {
			return defaultDuration;
		}

		private String getEffectFileName() {
			return effectFileName;
		}

		private String getParticleHardPoint() {
			return particleHardPoint;
		}

		public String getCallback() {
			return callback;
		}
		
	}
	
}
