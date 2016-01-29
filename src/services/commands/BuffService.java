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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import intents.BuffIntent;
import intents.PlayerEventIntent;
import intents.SkillModIntent;
import network.packets.swg.zone.spatial.PlayClientEffectObjectMessage;
import resources.Buff;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.DatatableData;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.creature.CreatureObject;
import utilities.Scripts;

public class BuffService extends Service {
	
	// TODO remove buffs on respec. Listen for respec intent and remove buffs with
	// BuffData that has REMOVE_ON_RESPEC = 1
	
	// TODO group buffs
	
	// TODO particle effects
	
	// TODO decay buffs on deathblow
	
	// TODO debuffs vs buffs
	
//	private static final byte GROUP_BUFF_RANGE = 100;	
	
	private final DatatableData buffTable;
	private final DelayQueue<BuffDelayed> buffRemoval;
	private final ExecutorService executor;
	private boolean stopBuffRemover;
	private final Map<String, BuffData> dataMap;
	
	public BuffService() {
		registerForIntent(BuffIntent.TYPE);
		registerForIntent(PlayerEventIntent.TYPE);
		
		buffTable = (DatatableData) ClientFactory.getInfoFromFile("datatables/buff/buff.iff");
		buffRemoval = new DelayQueue<>();
		executor = Executors.newSingleThreadScheduledExecutor();
		dataMap = new HashMap<>();
	}
	
	@Override
	public boolean initialize() {
		loadBuffs();
		
		executor.execute(new BuffRemover());
		return super.initialize();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		switch(i.getType()) {
			case BuffIntent.TYPE:
				BuffIntent bi = (BuffIntent) i;
				
				if(bi.isRemove()) {
					handleBuffIntentRemove(bi);
				} else {
					handleBuffIntentAdd(bi);
				}
				
				break;
			case PlayerEventIntent.TYPE: handleObjectCreation((PlayerEventIntent) i); break;
		}
	}
	
	@Override
	public boolean terminate() {
		stopBuffRemover = true;
		executor.shutdown();
		
		return super.terminate();
	}
	
	private void loadBuffs() {
		for(int row = 0; row < buffTable.getRowCount(); row++) {
			dataMap.put((String) buffTable.getCell(row, 0), new BuffData(
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
					(String) buffTable.getCell(row, 20)	// particle hardpoint
			));
		} 
	}
	
	private void handleObjectCreation(PlayerEventIntent pei) {
		CreatureObject creature = pei.getPlayer().getCreatureObject();
		
		switch(pei.getEvent()) {
			case PE_FIRST_ZONE: handleFirstZone(creature); break;
			case PE_DISAPPEAR: break;
		}
	}
	
	private void handleFirstZone(CreatureObject creature) {
		for(Buff buff : creature.getBuffs()) {
			// TODO fix. Needs buffName
			manageBuff(buff, null, creature);
		}
	}
	
	private void handleBuffIntentAdd(BuffIntent bi) {
		CreatureObject receiver = bi.getReceiver();
		CreatureObject buffer = bi.getBuffer();
		String buffName = bi.getBuffName();
		
		BuffData buffData = dataMap.get(buffName);
		
		if(buffData == null)
			return;
		
		Buff buff = new Buff(buffer.getObjectId(), receiver.getPlayerObject().getPlayTime(), (int) buffData.getDefaultDuration(), buffData.getEffect1Value());
		
		sendSkillModIntent(buffData, receiver, stopBuffRemover);
		receiver.addBuff(buffName, buff);
		
		manageBuff(buff, buffName, receiver);
		
		String effectFileName = buffData.getEffectFileName();
		
		if(!effectFileName.isEmpty())
			sendClientEffectMessage(receiver, effectFileName, buffData.getParticleHardPoint());
	}
	
	private void handleBuffIntentRemove(BuffIntent bi) {
		removeBuff(bi.getReceiver(), bi.getBuffName(), false);
	}
	
	private void manageBuff(Buff buff, String buffName, CreatureObject creature) {
		// If this buff has less than or 0 seconds left, then remove it.
		if(buff.getEndTime() <= 0) {
			removeBuff(creature, buffName, true);
		} else if(buff.getDuration() >= 0) {
			// If this buff doesn't last forever, we'll schedule it for removal in the future
			buffRemoval.put(new BuffDelayed(buff, buffName, creature));
		}
	}
	
	private void removeBuff(CreatureObject creature, String buffName, boolean expired) {
		// Get the BuffData for this buff name.
		BuffData buffData = dataMap.get(buffName);
		
		if(buffData == null) {
			return;
		}
		
		Buff buff = creature.getBuffByName(buffName);
		
		// Check if this buff can be stacked
		if(buffData.getMaxStackCount() > 1 && !expired) {
			// Check if this buff has been stacked
			if(buff.getStackCount() > 1) {
				// Get the adjustment from the script
				// TODO casting exception if the script doesn't exist or returns null
				int adjustment = Scripts.invoke(buffName, "onStackChange", creature);
				
				// If it has, reduce the stack count and reset the duration.
				creature.adjustBuffStackCount(buffName, adjustment);
			}
		} else {
			// Remove skillmods
			sendSkillModIntent(buffData, creature, true);
			
			// Remove the buff from the creature
			creature.removeBuff(buffName);
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
		target.sendObserversAndSelf(new PlayClientEffectObjectMessage(effectFileName, target.getObjectId(), hardPoint, ""));
	}
	
	private class BuffRemover implements Runnable {
		@Override
		public void run() {
			while(!stopBuffRemover) {
				BuffDelayed buffToRemove = buffRemoval.poll();
				
				if(buffToRemove != null)
					removeBuff(buffToRemove.owner, buffToRemove.buffName, true);
			}
		}
	}
	
	private class BuffDelayed implements Delayed {
		
		private final Buff buff;
		private final String buffName;
		private final CreatureObject owner;
		
		private BuffDelayed(Buff buff, String buffName, CreatureObject owner) {
			this.buff = buff;
			this.buffName = buffName;
			this.owner = owner;
		}
		
		@Override
		public int compareTo(Delayed o) {
			return Long.compare(buff.getEndTime(), ((BuffDelayed) o).buff.getEndTime());
		}

		@Override
		public long getDelay(TimeUnit timeUnit) {
			return timeUnit.convert(buff.getTotalDuration() - owner.getPlayerObject().getPlayTime(), TimeUnit.MILLISECONDS);
		}
		
	}
	
	/**
	 * @author Mads
	 * Each instance of this class holds the base information
	 * for a specific buff name. Thus, we only store shared data that each {@code Buff} instance
	 * would otherwise have been holding.
	 * 
	 * Example: Instead of each {@code Buff} instance storing the max amount of times you can
	 * stack it, a shared class stores that information.
	 * 
	 * With many {@code Buff} instances in play, this will result in memory
	 * usage reduction.
	 */
	private class BuffData {
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
		
		private BuffData(int maxStackCount, String effect1Name, float effect1Value, String effect2Name, float effect2Value, String effect3Name, float effect3Value, String effect4Name, float effect4Value, String effect5Name, float effect5Value, float defaultDuration, String effectFileName, String particleHardPoint) {
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
		
	}
	
}
