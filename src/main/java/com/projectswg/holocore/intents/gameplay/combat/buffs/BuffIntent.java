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
package com.projectswg.holocore.intents.gameplay.combat.buffs;

import com.projectswg.holocore.resources.support.data.server_info.loader.BuffLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.BuffLoader.BuffInfo;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.control.Intent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BuffIntent extends Intent {
	
	private String buffName;
	private BuffInfo buffData;
	private final CreatureObject buffer, receiver;
	private final boolean remove;
	
	public BuffIntent(@NotNull String buffName, @NotNull CreatureObject buffer, @NotNull CreatureObject receiver, boolean remove) {
		this.buffName = buffName;
		this.buffer = buffer;
		this.receiver = receiver;
		this.remove = remove;
	}
	
	public BuffIntent(@NotNull BuffInfo buffData, @NotNull CreatureObject buffer, @NotNull CreatureObject receiver, boolean remove) {
		this.buffData = buffData;
		this.buffer = buffer;
		this.receiver = receiver;
		this.remove = remove;
	}
	
	@NotNull
	public CreatureObject getReceiver() {
		return receiver;
	}
	
	@NotNull
	public CreatureObject getBuffer() {
		return buffer;
	}
	
	@Nullable
	public String getBuffName() {
		return buffName;
	}
	
	@Nullable
	public BuffLoader.BuffInfo getBuffData() {
		return buffData;
	}
	
	public boolean isRemove() {
		return remove;
	}
	
	public static void broadcast(@NotNull String buffName, @NotNull CreatureObject buffer, @NotNull CreatureObject receiver, boolean remove) {
		new BuffIntent(buffName, buffer, receiver, remove).broadcast();
	}
	
	public static void broadcast(@NotNull BuffInfo buffData, @NotNull CreatureObject buffer, @NotNull CreatureObject receiver, boolean remove) {
		new BuffIntent(buffData, buffer, receiver, remove).broadcast();
	}
	
}
