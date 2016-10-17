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
import java.util.concurrent.ScheduledExecutorService;
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
	
	// TODO buff slots A, B, C and D
	// TODO remove buffs on respec. Listen for respec event and remove buffs with BuffData where REMOVE_ON_RESPEC == 1
	// TODO remove group buff(s) from receiver when distance between caster and receiver is 100m. Perform same check upon zoning in
	// TODO decay buffs on deathblow
	
	private final ScheduledExecutorService executor;
	private final Map<CreatureObject, DelayQueue<BuffDelayed>> buffRemoval;
	private final Map<CRC, BuffData> dataMap;
	
	public BuffService() {
		registerForIntent(BuffIntent.TYPE);
		registerForIntent(PlayerEventIntent.TYPE);
		
		buffRemoval = new HashMap<>();
		dataMap = new HashMap<>();
		executor = Executors.newSingleThreadScheduledExecutor(ThreadUtilities.newThreadFactory("buff-service"));
	}
	
	@Override
	public boolean initialize() {
		long startTime = System.currentTimeMillis();
		Log.i(this, "Loading buffs...");
		loadBuffs();
		Log.i(this, "Finished loading buffs in %dms", System.currentTimeMillis() - startTime);
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
		
		for(int row = 0; row < buffTable.getRowCount(); row++) {
			dataMap.put(new CRC((String) buffTable.getCell(row, 0)), new BuffData(
					(int) buffTable.getCell(row, 28),	// max stacks
					(String) buffTable.getCell(row, 7),	// effect1
					(float) buffTable.getCell(row, 8),	// value1
					(String) buffTable.getCell(row, 9),	// effect2
					(float) buffTable.getCell(row, 10),	// value2
					(String) buffTable.getCell(row, 11),	// effect3
					(float) buffTable.getCell(row, 12),	// value3
					(String) buffTable.getCell(row, 13),	// effect4
					(float) buffTable.getCell(row, 14),	// value4
					(String) buffTable.getCell(row, 15),	// effect5
					(float) buffTable.getCell(row, 16),	// value5
					(float) buffTable.getCell(row, 6),	// default duration
					(String) buffTable.getCell(row, 19),	// particle effect
					(String) buffTable.getCell(row, 20),	// particle hardpoint
					(String) buffTable.getCell(row, 18)	// Callback
			));
		} 
	}
	
	private void handleBuffIntent(BuffIntent i) {
		if (i.isRemove()) {
			removeBuff(i.getReceiver(), new CRC(i.getBuffName()), false);
		} else {
			addBuff(new CRC(i.getBuffName()), i.getReceiver(), i.getBuffer());
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
		buffRemoval.remove(creature);
	}
	
	private void addBuff(CRC buffCrc, CreatureObject receiver, CreatureObject buffer) {
		BuffData buffData = dataMap.get(buffCrc);
		
		if(buffData == null) {
			Log.e(this, "Could not add %s to %s - buff data for it does not exist", receiver, buffCrc);
			return;
		}
		
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
		
		if(!effectFileName.isEmpty())
			sendClientEffectMessage(receiver, effectFileName, buffData.getParticleHardPoint());
	}
	
	private void manageBuff(Buff buff, CRC buffCrc, CreatureObject creature) {
		if(buff.getEndTime() <= 0) {
			// If this buff has less than or 0 seconds left, then remove it.
			removeBuff(creature, buffCrc, true);
		} else if(buff.getDuration() >= 0) {
			// If this buff doesn't last forever or hasn't expired, we'll schedule it for removal in the future
			DelayQueue<BuffDelayed> buffQueue = buffRemoval.get(creature);
			
			if(buffQueue == null) {
				buffQueue = new DelayQueue<>();
				buffRemoval.put(creature, buffQueue);
			}
			
			buffQueue.add(new BuffDelayed(buff, buffCrc, creature));
		}
	}
	
	private void removeBuff(CreatureObject creature, CRC buffCrc, boolean expired) {
		BuffData buffData = dataMap.get(buffCrc);
		
		if(buffData == null) {
			Log.e(this, "Could not remove %s from %s - buff data for it does not exist", buffCrc, creature);
			return;
		}
		
		Buff buff = creature.getBuffByCrc(buffCrc);
		
		// Check if this buff can be stacked
		if(buffData.getMaxStackCount() > 1 && !expired) {
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
			
			if(callback.isEmpty())
				return;
			
			CRC callbackCrc = new CRC(callback);
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
		
		if(!effect1Name.isEmpty())
			new SkillModIntent(effect1Name, 0, (int) buffData.getEffect1Value() * valueFactor, creature).broadcast();
		if(!effect2Name.isEmpty())
			new SkillModIntent(effect2Name, 0, (int) buffData.getEffect2Value() * valueFactor, creature).broadcast();
		if(!effect3Name.isEmpty())
			new SkillModIntent(effect3Name, 0, (int) buffData.getEffect3Value() * valueFactor, creature).broadcast();
		if(!effect4Name.isEmpty())
			new SkillModIntent(effect4Name, 0, (int) buffData.getEffect4Value() * valueFactor, creature).broadcast();
		if(!effect5Name.isEmpty())
			new SkillModIntent(effect5Name, 0, (int) buffData.getEffect5Value() * valueFactor, creature).broadcast();
	}
	
	private void sendClientEffectMessage(CreatureObject target, String effectFileName, String hardPoint) {
		target.sendObserversAndSelf(new PlayClientEffectObjectMessage(effectFileName, hardPoint, target.getObjectId()));
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
		
		private BuffData(int maxStackCount, String effect1Name, float effect1Value, String effect2Name, float effect2Value, String effect3Name, float effect3Value, String effect4Name, float effect4Value, String effect5Name, float effect5Value, float defaultDuration, String effectFileName, String particleHardPoint, String callback) {
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
