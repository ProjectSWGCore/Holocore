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
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

public class BuffService extends Service {
	
	
	private final ScheduledThreadPool timerCheckThread;
	private final Map<String, BuffCallback> callbackMap;
	
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
		BuffInfo buffData = getBuff(buffName);

		Objects.requireNonNull(buffData, "No known buff: " + buffName);
		assert buffData.getName().equals(buffName) : "BuffIntent name ["+ buffName +"] does not match BuffData name ["+buffData.getName()+ ']';
		CreatureObject buffer = bi.getBuffer();
		CreatureObject receiver = bi.getReceiver();

		if (bi.isRemove()) {
			removeBuff(receiver, buffData);
		} else {
			addBuff(receiver, buffData, buffer);
		}
	}
	
	@IntentHandler
	private void handleCreatureKilledIntent(CreatureKilledIntent cki) {
		CreatureObject corpse = cki.getCorpse();
		
		// All buffs are removed upon death
		removeAllBuffs(corpse, corpse.getBuffEntries(buff -> true));
	}
	
	@IntentHandler
	private void handlePlayerEventIntent(PlayerEventIntent intent) {
		PlayerEvent event = intent.getEvent();
		
		if (event == PlayerEvent.PE_ZONE_IN_SERVER) {
			removeExpiredBuffs(intent);
		}
	}
	
	private void removeExpiredBuffs(PlayerEventIntent intent) {
		Player player = intent.getPlayer();
		CreatureObject creatureObject = player.getCreatureObject();
		Stream<Buff> expiredBuffs = creatureObject.getBuffEntries(buff -> isBuffExpired(creatureObject, buff));
		expiredBuffs.forEach(expiredBuff -> removeBuff(creatureObject, getBuff(expiredBuff)));
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
	
	private void removeAllBuffs(CreatureObject creature, Stream<Buff> buffStream) {
		buffStream.forEach(buff -> removeBuff(creature, getBuff(buff)));
	}
	
	private void addBuff(CreatureObject receiver, @NotNull BuffInfo buffData, CreatureObject buffer) {
		String groupName = buffData.getGroup1();
		Optional<Buff> groupBuff = receiver.getBuffEntries(buff -> groupName.equals(getBuff(buff).getGroup1())).findAny();
		
		if (groupBuff.isPresent()) {
			Buff buff = groupBuff.get();
			
			if (buff.getCrc() == buffData.getCrc()) {
				if (isBuffInfinite(buffData)) {
					removeBuff(receiver, buffData);
				}
			} else {
				BuffInfo oldBuff = getBuff(buff);
				if (buffData.getPriority() >= oldBuff.getPriority()) {
					removeBuff(receiver, oldBuff);
					applyBuff(receiver, buffer, buffData);
				}
			}
		} else {
			applyBuff(receiver, buffer, buffData);
		}
	}
	
	private void removeBuff(CreatureObject creature, @NotNull BuffInfo buffData) {
		Optional<Buff> optionalEntry = creature.getBuffEntries(buff -> buff.getCrc() == buffData.getCrc()).findAny();
		if (!optionalEntry.isPresent())
			return; // Obique: Used to be an assertion, however if a service sends the removal after it expires it would assert - so I just removed it.
		
		Buff buff = optionalEntry.get();
		Buff removedBuff = creature.removeBuff(new CRC(buff.getCrc()));
		Objects.requireNonNull(removedBuff, "Buff must exist if being removed");

		checkBuffEffects(buffData, creature, false);
		checkCallback(buffData, creature);
	}
	
	private void applyBuff(CreatureObject receiver, CreatureObject buffer, BuffInfo buffData) {
		int applyTime = calculatePlayTime(receiver);
		int buffDuration = (int) buffData.getDuration();
		
		{
			Player bufferPlayer = buffer.getOwner();
			String bufferUsername = bufferPlayer == null ? "NULL" : bufferPlayer.getUsername();
			StandardLog.onPlayerTrace(this, receiver, "received buff '%s' from %s/%s; applyTime: %d, buffDuration: %d", buffData.getName(), bufferUsername, buffer.getObjectName(), applyTime, buffDuration);
		}
		Buff buff = new Buff(buffData.getCrc(), applyTime + buffDuration);

		checkBuffEffects(buffData, receiver, true);
		receiver.addBuff(buff);

		sendParticleEffect(buffData.getParticle(), receiver, "");

		timerCheckThread.execute(buffDuration * 1000L, () -> removeBuff(receiver, buffData));
	}
	
	private void sendParticleEffect(String effectFileName, CreatureObject receiver, String hardPoint) {
		if (effectFileName != null && !effectFileName.isEmpty()) {
			receiver.sendObservers(new PlayClientEffectObjectMessage(effectFileName, hardPoint, receiver.getObjectId(), ""));
		}
	}
	
	private void checkCallback(BuffInfo buffData, CreatureObject creature) {
		String callback = buffData.getCallback();
		
		if (callback.equals("none")) {
			return;
		}
		
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
	
	@Nullable
	private BuffInfo getBuff(String name) {
		return DataLoader.Companion.buffs().getBuff(name);
	}
	
	@Nullable
	private BuffInfo getBuff(@NotNull Buff buff) {
		return DataLoader.Companion.buffs().getBuff(buff.getCrc());
	}
	
}
