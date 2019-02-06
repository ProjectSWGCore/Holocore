/***********************************************************************************
 * Copyright (c) 2019 /// Project SWG /// www.projectswg.com                       *
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

package com.projectswg.holocore.intents.gameplay.player.experience.expertise;

import com.projectswg.holocore.resources.support.data.server_info.loader.ExpertiseLoader.ExpertiseInfo;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.control.Intent;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class RequestExpertiseIntent extends Intent {
	
	private final @NotNull CreatureObject creature;
	private final @NotNull List<@NotNull ExpertiseInfo> expertise;
	
	public RequestExpertiseIntent(@NotNull CreatureObject creature, @NotNull List<@NotNull ExpertiseInfo> expertise) {
		this.creature = creature;
		this.expertise = Collections.unmodifiableList(expertise);
	}
	
	@NotNull
	public CreatureObject getCreature() {
		return creature;
	}
	
	@NotNull
	public List<@NotNull ExpertiseInfo> getExpertise() {
		return expertise;
	}
	
	public static void broadcast(@NotNull CreatureObject creature, @NotNull List<@NotNull ExpertiseInfo> expertise) {
		new RequestExpertiseIntent(creature, expertise).broadcast();
	}
	
	public static void broadcast(@NotNull CreatureObject creature, @NotNull ExpertiseInfo expertise) {
		new RequestExpertiseIntent(creature, List.of(expertise)).broadcast();
	}
	
}
