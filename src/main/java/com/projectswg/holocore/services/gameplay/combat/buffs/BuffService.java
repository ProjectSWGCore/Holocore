/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
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
import com.projectswg.holocore.resources.support.data.server_info.loader.BuffLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.BuffLoader.BuffInfo;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.player.PlayerEvent;
import com.projectswg.holocore.resources.support.objects.swg.creature.Buff;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class BuffService extends Service {
	
	
	private final ScheduledThreadPool timerCheckThread;
	private final Map<String, BuffCallback> callbackMap;
	private final BuffLoader buffs = DataLoader.Companion.buffs();

	public BuffService() {
		timerCheckThread = new ScheduledThreadPool(1, "buff-timer-check");
		callbackMap = new HashMap<>();
		registerCallbacks();
	}
	
	private void registerCallbacks() {
		callbackMap.put("removeBurstRun", new RemoveBurstRunBuffCallback());
	}
	
	@Override
	public boolean start() {
		timerCheckThread.start();
		return super.start();
	}
	
	@Override
	public boolean stop() {
		timerCheckThread.stop();
		return super.stop();
	}
	
	@IntentHandler
	private void handleBuffIntent(BuffIntent bi) {
		String buffName = bi.getBuffName();
		CRC buffCrc = new CRC(CRC.getCrc(buffName.toLowerCase(Locale.ENGLISH)));
		CreatureObject buffer = bi.getBuffer();
		CreatureObject receiver = bi.getReceiver();

		if (bi.isRemove()) {
			removeBuff(receiver, buffCrc);
		} else {
			addBuff(receiver, buffCrc, buffer);
		}
	}
	
	@IntentHandler
	private void handleCreatureKilledIntent(CreatureKilledIntent cki) {
		removeAllBuffs(cki.getCorpse());
	}
	
	@IntentHandler
	private void handlePlayerEventIntent(PlayerEventIntent intent) {
		PlayerEvent event = intent.getEvent();
		
		if (event == PlayerEvent.PE_ZONE_IN_SERVER) {
			removeExpiredBuffs(intent.getPlayer().getCreatureObject());
		}
	}
	
	private void removeExpiredBuffs(CreatureObject creatureObject) {
		Collection<CRC> crcsForExpiredBuffs = creatureObject.getBuffs().entrySet().stream()
				.filter(entry -> {
					Buff buff = entry.getValue();
					return isBuffExpired(creatureObject, buff);
				}).map(Map.Entry::getKey)
				.toList();

		crcsForExpiredBuffs.forEach(buffCrc -> removeBuff(creatureObject, buffCrc));
	}
	
	private int calculatePlayTime(CreatureObject creature) {
		if (creature.isPlayer()) {
			creature.getPlayerObject().updatePlayTime();
			return creature.getPlayerObject().getPlayTime();
		}
		
		// NPC time is based on the current galactic time because NPCs don't have playTime
		return (int) ProjectSWG.getGalacticTime();
	}
	
	private boolean isBuffExpired(CreatureObject creature, Buff buff) {
		return calculatePlayTime(creature) >= buff.getEndTime();
	}
	
	private boolean isBuffInfinite(BuffInfo buffData) {
		return buffData.getDuration() < 0;
	}
	
	private void removeAllBuffs(CreatureObject creature) {
		Set<CRC> buffCrcs = creature.getBuffs().keySet();
		buffCrcs.forEach(buffCrc -> removeBuff(creature, buffCrc));
	}
	
	private void addBuff(CreatureObject receiver, @NotNull CRC buffCrc, CreatureObject buffer) {
		BuffInfo buffData = buffs.getBuff(buffCrc);
		String groupName = buffData.getGroup1();
		Optional<CRC> optGroupBuffCrc = receiver.getBuffs().keySet().stream()
				.map(buffCrc1 -> buffs.getBuff(buffCrc1)).filter(buff -> {
					CRC candidateBuffCrc = buff.getCrc();
					BuffInfo candidateBuff = buffs.getBuff(candidateBuffCrc);
					String candidateBuffGroup1Name = candidateBuff.getGroup1();

					return groupName.equals(candidateBuffGroup1Name);
				})
				.map(BuffInfo::getCrc)
				.findAny();
		
		if (optGroupBuffCrc.isPresent()) {
			CRC groupBuffCrc = optGroupBuffCrc.get();
			boolean sameBuff = groupBuffCrc.equals(buffCrc);
			
			if (sameBuff) {
				if (isBuffInfinite(buffData)) {
					removeBuff(receiver, buffCrc);
				} else {
					// Reset timer
					removeBuff(receiver, buffCrc);
					applyBuff(receiver, buffer, buffData);
				}
			} else {
				BuffInfo oldBuff = buffs.getBuff(groupBuffCrc);
				if (buffData.getPriority() >= oldBuff.getPriority()) {
					removeBuff(receiver, groupBuffCrc);
					applyBuff(receiver, buffer, buffData);
				}
			}
		} else {
			applyBuff(receiver, buffer, buffData);
		}
	}
	
	private void removeBuff(CreatureObject creature, @NotNull CRC buffCrc) {
		if (!creature.getBuffs().containsKey(buffCrc))
			return; // Obique: Used to be an assertion, however if a service sends the removal after it expires it would assert - so I just removed it.

		BuffInfo buffData = buffs.getBuff(buffCrc);
		if (buffData == null) {
			StandardLog.onPlayerError(this, creature, "unable to remove buff '%s' as it could not be looked up", buffCrc.getString());
			return;
		}
		Buff removedBuff = creature.removeBuff(buffCrc);
		Objects.requireNonNull(removedBuff, "Buff must exist if being removed");
		StandardLog.onPlayerTrace(this, creature, "buff '%s' was removed", buffCrc.getString());

		checkBuffEffects(buffData, creature, false);
		checkCallback(buffData, creature);
	}
	
	private void applyBuff(CreatureObject receiver, CreatureObject buffer, BuffInfo buffData) {
		int applyTime = calculatePlayTime(receiver);
		int buffDuration = (int) buffData.getDuration();
		int endTime = applyTime + buffDuration;
		
		{
			Player bufferPlayer = buffer.getOwner();
			String bufferUsername = bufferPlayer == null ? "NULL" : bufferPlayer.getUsername();
			StandardLog.onPlayerTrace(this, receiver, "received buff '%s' from %s/%s; applyTime: %d, buffDuration: %d", buffData.getName(), bufferUsername, buffer.getObjectName(), applyTime, buffDuration);
		}

		checkBuffEffects(buffData, receiver, true);
		receiver.addBuff(buffData.getCrc(), new Buff(endTime));

		sendParticleEffect(buffData.getParticle(), receiver, "");

		scheduleBuffExpirationCheck(receiver, buffData);
	}

	private void scheduleBuffExpirationCheck(CreatureObject receiver, BuffInfo buffData) {
		timerCheckThread.execute(1000L, () -> {
			Buff buff = receiver.getBuffs().get(buffData.getCrc());
			if (buff == null) {
				return;
			}
			if (isBuffExpired(receiver, buff)) {
				removeBuff(receiver, buffData.getCrc());
			} else {
				scheduleBuffExpirationCheck(receiver, buffData);
			}
		});
	}

	private void sendParticleEffect(String effectFileName, CreatureObject receiver, String hardPoint) {
		if (effectFileName != null && !effectFileName.isEmpty()) {
			receiver.sendObservers(new PlayClientEffectObjectMessage(effectFileName, hardPoint, receiver.getObjectId(), ""));
		}
	}
	
	private void checkCallback(BuffInfo buffData, CreatureObject creature) {
		String callback = buffData.getCallback();
		
		if (callbackMap.containsKey(callback)) {
			BuffCallback buffCallback = callbackMap.get(callback);
			buffCallback.execute(creature);
		}
	}
	
	private void checkBuffEffects(BuffInfo buffData, CreatureObject creature, boolean add) {
		for (int i = 0; i < 5; i++)
			checkBuffEffect(creature, buffData.getEffectName(i), buffData.getEffectValue(i), add);
	}
	
	private void checkBuffEffect(CreatureObject creature, String effectName, double effectValue, boolean add) {
		if (effectName != null &&  !effectName.isEmpty()) {
			if (DataLoader.Companion.commands().isCommand(effectName) && effectValue == 1.0) {
				// This effect is an ability
				if (add) {
					// Buff is being added. Grant the ability.
					creature.addCommand(effectName);
				} else {
					// Buff is being removed. Remove the ability.
					creature.removeCommand(effectName);
				}
			} else {
				// This effect is a skill mod
				if (add) {
					new SkillModIntent(effectName, 0, (int) effectValue, creature).broadcast();
				} else {
					new SkillModIntent(effectName, 0, (int) -effectValue, creature).broadcast();
				}
			}
		}
	}

}
