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
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import intents.BuffIntent;
import intents.PlayerEventIntent;
import intents.SkillModIntent;
import intents.combat.CreatureKilledIntent;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
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
	
	// TODO allow removal of buffs with BuffData where PLAYER_REMOVABLE == 1
	// TODO remove buffs on respec. Listen for respec event and remove buffs with BuffData where REMOVE_ON_RESPEC == 1
	// TODO remove group buff(s) from receiver when distance between caster and receiver is 100m. Perform same check upon zoning in
	
	private final ScheduledExecutorService executor;
	private final Set<CreatureObject> monitored;
	private final Map<CRC, BuffData> dataMap;	// All CRCs are lower-cased buff names!
	
	public BuffService() {
		registerForIntent(BuffIntent.TYPE);
		registerForIntent(PlayerEventIntent.TYPE);
		
		monitored = new HashSet<>();
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
		synchronized (monitored) {
			executor.scheduleAtFixedRate(() -> monitored.parallelStream().forEach(creature -> checkBuffTimers(creature)), 1, 1, TimeUnit.SECONDS);
		}
		
		return super.start();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		switch(i.getType()) {
			case BuffIntent.TYPE: handleBuffIntent((BuffIntent) i); break;
			case PlayerEventIntent.TYPE: handlePlayerEventIntent((PlayerEventIntent) i); break;
			case CreatureKilledIntent.TYPE: handleCreatureKilledIntent((CreatureKilledIntent) i); break;
		}
	}
	
	@Override
	public boolean stop() {
		executor.shutdown();
		
		return super.stop();
	}
	
	private void checkBuffTimers(CreatureObject creature) {
		creature.getBuffEntries(buffEntry -> isBuffExpired(buffEntry.getValue()))
				.forEach(buffEntry -> removeBuff(creature, buffEntry.getKey(), true));
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
		int stanceParticle = buffTable.getColumnFromName("STANCE_PARTICLE");
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
					(String) buffTable.getCell(row, stanceParticle),
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
		if (hasBuffs(creature)) {
			synchronized (monitored) {
				monitored.add(creature);
			}
		}
	}
	
	private void handleCreatureKilledIntent(CreatureKilledIntent i) {
		CreatureObject corpse = i.getCorpse();
		
		if (!monitored.contains(corpse)) {
			return;
		}
		
		synchronized (monitored) {
			// TODO remove buffs where REMOVE_ON_DEATH == 1
			
			if (corpse.isPlayer()) {
				corpse.getBuffEntries(buffEntry -> isBuffDecayable(buffEntry))
						.forEach(buffEntry -> decayDuration(corpse, buffEntry));
			}
		}
	}
	
	private void handleDisappear(CreatureObject creature) {
		synchronized (monitored) {
			monitored.remove(creature);
			
			// Buffs that aren't persistable should be removed at this point
			creature.getBuffEntries(buffEntry -> isBuffPersistent(buffEntry.getKey()))
					.forEach(buffEntry ->  removeBuff(creature, buffEntry.getKey(), true));
		}
	}
	
	private boolean isBuffExpired(Buff buff) {
		return buff.getDuration() >= 0 && System.currentTimeMillis() / 1000 >= buff.getEndTime();
	}
	
	private boolean isBuffDecayable(Entry<CRC, Buff> buffEntry) {
		// DECAY_ON_PVP_DEATH == 1
		return !isBuffExpired(buffEntry.getValue()) /*&& dataMap.get(buffEntry.getKey()).isDecayable()*/;
	}
	
	private boolean isBuffPersistent(CRC crc) {
//		IS_PERSISTENT == 0
		return false;
	}
	
	private boolean hasBuffs(CreatureObject creature) {
		return creature.getBuffEntries(buffEntry -> true).count() > 0;
	}
	
	private void decayDuration(CreatureObject creature, Entry<CRC, Buff> buffEntry) {
		int currentDuration = buffEntry.getValue().getDuration();
		int newDuration = (int) (currentDuration * 0.10);	// Duration decays with 10%
		
		creature.setBuffDuration(buffEntry.getKey(), creature.getPlayerObject().getPlayTime(), newDuration);
	}
	
	private int calculatePlayTime(CreatureObject creature) {
		return creature.isPlayer() ? creature.getPlayerObject().getPlayTime() : 0;
	}
	
	private void addBuff(CRC newCrc, CreatureObject receiver, CreatureObject buffer) {
		BuffData buffData = dataMap.get(newCrc);
		
		if (buffData == null) {
			Log.e(this, "Could not add %s to %s - buff data does not exist", newCrc, receiver);
			return;
		}
		
		String groupName = buffData.getGroupName();
		Optional<Entry<CRC, Buff>> groupBuff = receiver.getBuffEntries(buffEntry -> groupName.equals(dataMap.get(buffEntry.getKey()).getGroupName())).findAny();
		
		if (receiver.isPlayer()) {
			receiver.getPlayerObject().updatePlayTime();
		}
		
		int playTime = calculatePlayTime(receiver);
		
		if (groupBuff.isPresent()) {
			Entry<CRC, Buff> buffEntry = groupBuff.get();
			CRC oldCrc = buffEntry.getKey();
			
			if (oldCrc.equals(newCrc)) {
				// TODO skillmods influencing stack increment
				checkStackCount(receiver, buffData, buffEntry, playTime, 1);
			} else if (buffData.getGroupPriority() >= dataMap.get(oldCrc).getGroupPriority()) {
				removeBuff(receiver, oldCrc, true);
				applyBuff(receiver, buffer, buffData, playTime, newCrc);
			}
		} else {
			applyBuff(receiver, buffer, buffData, playTime, newCrc);
		}
	}
	
	private void checkStackCount(CreatureObject receiver, BuffData buffData, Entry<CRC, Buff> buffEntry, int playTime, int stackMod) {
		// If it's the same buff, we need to check for stacks
		int maxStackCount = buffData.getMaxStackCount();

		if (maxStackCount < 2) {
			return;
		}

		int currentStacks = buffEntry.getValue().getStackCount();

		if (stackMod + currentStacks > maxStackCount) {
			stackMod = maxStackCount;
		}

		CRC crc = buffEntry.getKey();
		
		receiver.adjustBuffStackCount(crc, stackMod);
		checkSkillMods(buffData, receiver, stackMod);

		// If the stack count was incremented, also renew the duration
		if (stackMod > 0) {
			receiver.setBuffDuration(crc, playTime, (int) buffData.getDefaultDuration());
		}
	}
	
	private void applyBuff(CreatureObject receiver, CreatureObject buffer, BuffData buffData, int playTime, CRC crc) {
		// TODO stack counts upon add/remove need to be defined on a per-buff basis due to skillmod influence. Scripts might not be a bad idea.
		int stackCount = 1;
		int buffDuration = (int) buffData.getDefaultDuration();
		Buff buff = new Buff(playTime + buffDuration, buffData.getEffect1Value(), buffDuration, buffer.getObjectId(), stackCount);

		checkSkillMods(buffData, receiver, 1);
		receiver.addBuff(crc, buff);
		
		sendParticleEffect(buffData.getEffectFileName(), receiver, buffData.getParticleHardPoint());
		sendParticleEffect(buffData.getStanceParticle(), receiver, buffData.getParticleHardPoint());
	}
	
	private void sendParticleEffect(String effectFileName, CreatureObject receiver, String hardPoint) {
		if (!effectFileName.isEmpty()) {
			receiver.sendObserversAndSelf(new PlayClientEffectObjectMessage(effectFileName, hardPoint, receiver.getObjectId()));
		}
	}
	
	private void removeBuff(CreatureObject creature, CRC buffCrc, boolean expired) {
		BuffData buffData = dataMap.get(buffCrc);
		
		if (buffData == null) {
			Log.e(this, "Could not remove %s from %s - buff data for it does not exist", buffCrc, creature);
			return;
		}
		
		Optional<Entry<CRC, Buff>> optionalEntry = creature.getBuffEntries(buffEntry -> buffEntry.getKey().equals(buffCrc)).findAny();
		
		if (!optionalEntry.isPresent()) {
			// It's hard for us to remove a buff that the creature doesn't have...
			return;
		}
		
		Entry<CRC, Buff> buffEntry = optionalEntry.get();
		Buff buff = buffEntry.getValue();
		int maxStackCount = buffData.getMaxStackCount();
		int currentStacks = buff.getStackCount();
		
		if (maxStackCount > 1 && !expired && currentStacks > 1) {
			checkStackCount(creature, buffData, buffEntry, calculatePlayTime(creature), maxStackCount);
		} else {
			Buff removedBuff = creature.removeBuff(buffCrc);
			
			if (removedBuff == null) {
				return;
			}
			
			// Remove skillmods
			checkSkillMods(buffData, creature, -removedBuff.getStackCount());
			
			String callback = buffData.getCallback();
			
			if(callback.equals("none"))
				return;
			
			CRC callbackCrc = new CRC(callback.toLowerCase(Locale.ENGLISH));
			
			if (dataMap.containsKey(callbackCrc)) {
				// Apply the callback buff
				addBuff(callbackCrc, creature, creature);
			} else {
				// Call the callback command script
				// TODO
			}
			
			// If they have no more expirable buffs, we can stop monitoring them
			if (hasBuffs(creature)) {
				synchronized (monitored) {
					monitored.remove(creature);
				}
			}
		}
	}
	
	private void checkSkillMods(BuffData buffData, CreatureObject creature, int valueFactor) {
		sendSkillModIntent(creature, buffData.getEffect1Name(), buffData.getEffect1Value(), valueFactor);
		sendSkillModIntent(creature, buffData.getEffect2Name(), buffData.getEffect2Value(), valueFactor);
		sendSkillModIntent(creature, buffData.getEffect3Name(), buffData.getEffect3Value(), valueFactor);
		sendSkillModIntent(creature, buffData.getEffect4Name(), buffData.getEffect4Value(), valueFactor);
		sendSkillModIntent(creature, buffData.getEffect5Name(), buffData.getEffect5Value(), valueFactor);
	}
	
	private void sendSkillModIntent(CreatureObject creature, String effectName, float effectValue, int valueFactor) {
		if (!effectName.isEmpty())
			new SkillModIntent(effectName, 0, (int) effectValue * valueFactor, creature).broadcast();
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
		private final String stanceParticle;
		private final String callback;
		
		private BuffData(String groupName, int groupPriority, int maxStackCount, String effect1Name, float effect1Value, String effect2Name, float effect2Value, String effect3Name, float effect3Value, String effect4Name, float effect4Value, String effect5Name, float effect5Value, float defaultDuration, String effectFileName, String particleHardPoint, String stanceParticle, String callback) {
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
			this.stanceParticle = stanceParticle;
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

		public String getStanceParticle() {
			return stanceParticle;
		}

		public String getCallback() {
			return callback;
		}
		
	}
	
}
