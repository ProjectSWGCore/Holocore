/************************************************************************************
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.projectswg.common.concurrency.PswgScheduledThreadPool;
import com.projectswg.common.data.CRC;
import com.projectswg.common.debug.Log;
import com.projectswg.common.network.packets.swg.zone.object_controller.CommandTimer;

import resources.commands.Command;
import resources.objects.creature.CreatureObject;
import services.commands.CommandLauncher.EnqueuedCommand;

public class CommandCooldownHandler {
	
	private final Map<CreatureObject, Set<String>>		cooldownMap;
	private final PswgScheduledThreadPool				cooldownThread;
	
	public CommandCooldownHandler() {
		this.cooldownMap = new HashMap<>();
		this.cooldownThread = new PswgScheduledThreadPool(1, "command-launcher-cooldown-thread");
	}
	
	public void start() {
		cooldownThread.start();
	}
	
	public void stop() {
		cooldownThread.stop();
	}
	
	public boolean startCooldowns(CreatureObject creature, EnqueuedCommand enqueued) {
		synchronized (cooldownMap) {
			Command command = enqueued.getCommand();
			String cooldownGroup1 = command.getCooldownGroup();
			String cooldownGroup2 = command.getCooldownGroup2();
			float cooldownTime1 = command.getCooldownTime();
			float cooldownTime2 = command.getCooldownTime2();
			if (!addValidCooldowns(creature, cooldownGroup1, cooldownGroup2))
				return false;
			if (isValidCooldownGroup(cooldownGroup1))
				startCooldownGroup(creature, enqueued, cooldownGroup1, cooldownTime1);
			if (isValidCooldownGroup(cooldownGroup2))
				startCooldownGroup(creature, enqueued, cooldownGroup2, cooldownTime2);
			return true;
		}
	}
	
	private boolean isValidCooldownGroup(String cooldownGroup) {
		return !cooldownGroup.isEmpty() && !cooldownGroup.equals("defaultCooldownGroup");
	}
	
	private void startCooldownGroup(CreatureObject creature, EnqueuedCommand enqueued, String cooldownGroup, float cooldownTime) {
		CommandTimer commandTimer = new CommandTimer(creature.getObjectId());
		commandTimer.setCooldownGroupCrc(CRC.getCrc(cooldownGroup));
		commandTimer.setCooldownMax(cooldownTime);
		commandTimer.setCommandNameCrc(enqueued.getCommand().getCrc());
		commandTimer.setSequenceId(enqueued.getRequest().getCounter());
		creature.sendSelf(commandTimer);
		
		cooldownThread.execute((long) (cooldownTime * 1000), () -> removeCooldown(creature, cooldownGroup));
	}
	
	private boolean addValidCooldowns(CreatureObject creature, String ... cooldownGroups) {
		synchronized (cooldownMap) {
			Set<String> cooldowns = cooldownMap.get(creature);
			
			if (cooldowns == null) {
				// This is the first time they're using a cooldown command
				cooldowns = new HashSet<>();
				cooldownMap.put(creature, cooldowns);
			}
			
			for (String cooldownGroup : cooldownGroups) {
				if (isValidCooldownGroup(cooldownGroup) && cooldowns.contains(cooldownGroup))
					return false;
			}
			for (String cooldownGroup : cooldownGroups) {
				cooldowns.add(cooldownGroup);
			}
			return true;
		}
	}
	
	private void removeCooldown(CreatureObject creature, String cooldownGroup) {
		synchronized (cooldownMap) {
			Set<String> cooldownGroups = cooldownMap.get(creature);
			if (cooldownGroups.remove(cooldownGroup)) {
				
			} else {
				Log.w("%s doesn't have cooldown group %s!", creature, cooldownGroup);
			}
		}
	}
	
}
