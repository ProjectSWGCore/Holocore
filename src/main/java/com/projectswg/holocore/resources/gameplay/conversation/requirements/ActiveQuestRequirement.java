/***********************************************************************************
 * Copyright (c) 2021 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.gameplay.conversation.requirements;

import com.projectswg.holocore.resources.gameplay.conversation.model.Requirement;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;

import java.util.Collection;

public class ActiveQuestRequirement implements Requirement {
	
	private final String questName;
	private final boolean active;
	private final Integer task;
	
	public ActiveQuestRequirement(String questName, boolean active, Integer task) {
		this.questName = questName;
		this.task = task;
		this.active = active;
	}
	
	@Override
	public boolean test(Player player) {
		PlayerObject playerObject = player.getPlayerObject();
		
		if (!playerObject.isQuestInJournal(questName)) {
			return !active;
		}
		
		if (playerObject.isQuestComplete(questName)) {
			return !active;
		}
		
		if (task != null) {
			Collection<Integer> questActiveTasks = playerObject.getQuestActiveTasks(questName);
			boolean taskActive = questActiveTasks.contains(task);
			
			return taskActive == active;
		}

		return active;
	}
	
	public String getQuestName() {
		return questName;
	}
	
	public Integer getTask() {
		return task;
	}
	
	public boolean isActive() {
		return active;
	}
}
