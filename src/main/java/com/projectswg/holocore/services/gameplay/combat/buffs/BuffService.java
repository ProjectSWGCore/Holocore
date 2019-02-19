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
package com.projectswg.holocore.services.gameplay.combat.buffs;

import com.projectswg.common.data.CRC;
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage;
import com.projectswg.holocore.ProjectSWG;
import com.projectswg.holocore.intents.gameplay.combat.CreatureKilledIntent;
import com.projectswg.holocore.intents.gameplay.combat.buffs.BuffIntent;
import com.projectswg.holocore.intents.gameplay.player.experience.skills.SkillModIntent;
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.data.server_info.loader.BuffLoader.BuffInfo;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.creature.Buff;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.concurrency.BasicScheduledThread;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

public class BuffService extends Service {
	
	/*
	 * TODO allow removal of buffs with BuffInfo where PLAYER_REMOVABLE == 1
	 * TODO remove buffs on respec. Listen for respec event and remove buffs with BuffInfo where
	 *      REMOVE_ON_RESPEC == 1
	 * TODO remove group buff(s) from receiver when distance between caster and receiver is 100m.
	 *      Perform same check upon zoning in. Skillmod1 effect name is "group"
	 */
	
	private final BasicScheduledThread timerCheckThread;
	private final Set<CreatureObject> monitored;
	
	public BuffService() {
		timerCheckThread = new BasicScheduledThread("buff-timer-check", this::checkBuffTimers);
		monitored = new HashSet<>();
	}
	
	@Override
	public boolean start() {
		timerCheckThread.startWithFixedRate(1000, 1000);
		return super.start();
	}
	
	@Override
	public boolean stop() {
		timerCheckThread.stop();
		return super.stop();
	}
	
	private void checkBuffTimers() {
		List<CreatureObject> creatures;
		synchronized (monitored) {
			creatures = new ArrayList<>(monitored);
		}
		for (CreatureObject creature : creatures) {
			removeAllBuffs(creature, creature.getBuffEntries(buff -> isBuffExpired(creature, buff)));
		}
	}
	
	@IntentHandler
	private void handleBuffIntent(BuffIntent bi) {
		BuffInfo buffData = getBuff(bi.getBuffName());
		Objects.requireNonNull(buffData, "No known buff: " + bi.getBuffName());
		assert buffData.getName().equalsIgnoreCase(bi.getBuffName()) : "BuffIntent name ["+bi.getBuffName()+"] does not match BuffInfo name ["+buffData.getName()+ ']';
		if (bi.isRemove()) {
			removeBuff(bi.getReceiver(), buffData, false);
		} else {
			addBuff(bi.getReceiver(), buffData, bi.getBuffer());
		}
	}
	
	@IntentHandler
	private void handlePlayerEventIntent(PlayerEventIntent pei) {
		CreatureObject creature = pei.getPlayer().getCreatureObject();
		
		switch (pei.getEvent()) {
			case PE_FIRST_ZONE:
				handleFirstZone(creature);
				break;
			case PE_DISAPPEAR:
				handleDisappear(creature);
				break;
			default:
				break;
		}
	}
	
	private void handleFirstZone(CreatureObject creature) {
		if (isCreatureBuffed(creature)) {
			addToMonitored(creature);
		}
	}
	
	private void handleDisappear(CreatureObject creature) {
		removeFromMonitored(creature);
		removeAllBuffs(creature, creature.getBuffEntries(buff -> !isBuffPersistent(buff)));
	}
	
	@IntentHandler
	private void handleCreatureKilledIntent(CreatureKilledIntent cki) {
		CreatureObject corpse = cki.getCorpse();
		
		if (cki.getKiller().isPlayer()) {
			// PvP death - decay durations of certain buffs
			corpse.getBuffEntries(buff -> isBuffDecayable(corpse, buff)).forEach(buff -> decayDuration(corpse, buff));
		} else {
			// PvE death - remove certain buffs
			removeAllBuffs(corpse, corpse.getBuffEntries(this::isBuffRemovedOnDeath));
		}
	}
	
	private void addToMonitored(CreatureObject creature) {
		synchronized (monitored) {
			monitored.add(creature);
		}
	}
	
	private void removeFromMonitored(CreatureObject creature) {
		synchronized (monitored) {
			monitored.remove(creature);
		}
	}
	
	private int calculatePlayTime(CreatureObject creature) {
		if (creature.isPlayer()) {
			creature.getPlayerObject().updatePlayTime();
			return creature.getPlayerObject().getPlayTime();
		}
		
		// NPC time is based on the current galactic time because NPCs don't have playTime
		return (int)ProjectSWG.getGalacticTime();
	}
	
	private boolean isBuffExpired(CreatureObject creature, Buff buff) {
		return buff.getDuration() >= 0 && calculatePlayTime(creature) >= buff.getEndTime();
	}
	
	private boolean isBuffDecayable(CreatureObject creature, Buff buff) {
		return !isBuffExpired(creature, buff) && getBuff(buff).isDecayOnPvpDeath();
	}
	
	private boolean isBuffPersistent(Buff buff) {
		return getBuff(buff).isPersistent();
	}
	
	private boolean isBuffRemovedOnDeath(Buff buff) {
		return getBuff(buff).isRemoveOnDeath();
	}
	
	private boolean isBuffInfinite(BuffInfo buffData) {
		return buffData.getDuration() < 0;
	}
	
	private boolean isCreatureBuffed(CreatureObject creature) {
		return creature.getBuffEntries(buff -> !isBuffInfinite(getBuff(buff))).count() > 0;
	}
	
	private void decayDuration(CreatureObject creature, Buff buff) {
		int newDuration = (int) (buff.getDuration() * 0.10);	// Duration decays with 10%
		
		creature.setBuffDuration(new CRC(buff.getCrc()), buff.getStartTime(), newDuration);
	}
	
	private void removeAllBuffs(CreatureObject creature, Stream<Buff> buffStream) {
		buffStream.forEach(buff -> removeBuff(creature, getBuff(buff), true));
	}
	
	private void addBuff(CreatureObject receiver, @NotNull BuffInfo buffData, CreatureObject buffer) {
		String groupName = buffData.getGroup1();
		Optional<Buff> groupBuff = receiver.getBuffEntries(buff -> groupName.equals(getBuff(buff).getGroup1())).findAny();
		
		int applyTime = calculatePlayTime(receiver);
		
		if (groupBuff.isPresent()) {
			Buff buff = groupBuff.get();
			
			if (buff.getCrc() == buffData.getCrc()) {
				if (isBuffInfinite(buffData)) {
					removeBuff(receiver, buffData, true);
				} else {
					// TODO skillmods influencing stack increment
					checkStackCount(receiver, buff, applyTime, 1);
				}
			} else {
				BuffInfo oldBuff = getBuff(buff);
				if (buffData.getPriority() >= oldBuff.getPriority()) {
					removeBuff(receiver, oldBuff, true);
					applyBuff(receiver, buffer, buffData, applyTime);
				}
			}
		} else {
			applyBuff(receiver, buffer, buffData, applyTime);
		}
	}
	
	private void removeBuff(CreatureObject creature, @NotNull BuffInfo buffData, boolean expired) {
		Optional<Buff> optionalEntry = creature.getBuffEntries(buff -> buff.getCrc() == buffData.getCrc()).findAny();
		if (!optionalEntry.isPresent())
			return; // Obique: Used to be an assertion, however if a service sends the removal after it expires it would assert - so I just removed it.
		
		Buff buff = optionalEntry.get();
		if (buffData.getMaxStackCount() > 1 && !expired && buff.getStackCount() > 1) {
			checkStackCount(creature, buff, calculatePlayTime(creature), buffData.getMaxStackCount());
		} else {
			Buff removedBuff = creature.removeBuff(new CRC(buff.getCrc()));
			Objects.requireNonNull(removedBuff, "Buff must exist if being removed");
			
			checkSkillMods(buffData, creature, -removedBuff.getStackCount());
			checkCallback(buffData, creature);
		}
		
		if (!isCreatureBuffed(creature)) {
			removeFromMonitored(creature);
		}
	}
	
	private void checkStackCount(CreatureObject receiver, Buff buff, int applyTime, int stackMod) {
		BuffInfo buffData = getBuff(buff);
		
		Objects.requireNonNull(buffData, "No known buff: " + buff.getCrc());
		// If it's the same buff, we need to check for stacks
		int maxStackCount = buffData.getMaxStackCount();
		
		if (maxStackCount < 2) {
			removeBuff(receiver, buffData, true);
			applyBuff(receiver, receiver, buffData, applyTime);
			return;
		}
		
		if (stackMod + buff.getStackCount() > maxStackCount) {
			stackMod = maxStackCount;
		}
		
		CRC crc = new CRC(buff.getCrc());
		receiver.adjustBuffStackCount(crc, stackMod);
		checkSkillMods(buffData, receiver, stackMod);
		
		// If the stack count was incremented, also renew the duration
		if (stackMod > 0) {
			receiver.setBuffDuration(crc, applyTime, (int) buffData.getDuration());
		}
	}
	
	private void applyBuff(CreatureObject receiver, CreatureObject buffer, BuffInfo buffData, int applyTime) {
		// TODO stack counts upon add/remove need to be defined on a per-buff basis due to skillmod influence. Scripts might not be a bad idea.
		int stackCount = 1;
		int buffDuration = (int) buffData.getDuration();
		{
			Player bufferPlayer = buffer.getOwner();
			String bufferUsername = bufferPlayer == null ? "NULL" : bufferPlayer.getUsername();
			StandardLog.onPlayerTrace(this, receiver, "received buff '%s' from %s/%s; applyTime: %d, buffDuration: %d", buffData.getName(), bufferUsername, buffer.getObjectName(), applyTime, buffDuration);
		}
		Buff buff = new Buff(buffData.getCrc(), applyTime + buffDuration, (float) buffData.getEffectValue(0), buffDuration, buffer.getObjectId(), stackCount);
		
		checkSkillMods(buffData, receiver, 1);
		receiver.addBuff(buff);
		
		sendParticleEffect(buffData.getParticle(), receiver, buffData.getParticleHardpoint());
		sendParticleEffect(buffData.getStanceParticle(), receiver, buffData.getParticleHardpoint());
		
		addToMonitored(receiver);
	}
	
	private void sendParticleEffect(String effectFileName, CreatureObject receiver, String hardPoint) {
		if (!effectFileName.isEmpty()) {
			receiver.sendObservers(new PlayClientEffectObjectMessage(effectFileName, hardPoint, receiver.getObjectId(), ""));
		}
	}
	
	private void checkCallback(BuffInfo buffData, CreatureObject creature) {
		String callback = buffData.getCallback();
		
		if (callback.equals("none")) {
			return;
		}
		
		if (DataLoader.buffs().containsBuff(callback)) {
			addBuff(creature, getBuff(callback), creature);
		}
	}
	
	private void checkSkillMods(BuffInfo buffData, CreatureObject creature, int valueFactor) {
		/*
		 * TODO Check effectName == "group". If yes, every group member within 100m range (maybe
		 *      just the ones aware of the buffer) receive the buff. Once outside range, buff needs
		 *      removal
		 */
		for (int i = 0; i < buffData.getEffects(); i++) {
			if (buffData.getEffectName(i) != null)
				sendSkillModIntent(creature, buffData.getEffectName(i), buffData.getEffectValue(i), valueFactor);
		}
	}
	
	private void sendSkillModIntent(CreatureObject creature, String effectName, double effectValue, int valueFactor) {
		if (!effectName.isEmpty())
			new SkillModIntent(effectName, 0, (int) effectValue * valueFactor, creature).broadcast();
	}
	
	@Nullable
	private BuffInfo getBuff(String name) {
		return DataLoader.buffs().getBuff(name);
	}
	
	@Nullable
	private BuffInfo getBuff(@NotNull Buff buff) {
		return DataLoader.buffs().getBuff(buff.getCrc());
	}
	
}
