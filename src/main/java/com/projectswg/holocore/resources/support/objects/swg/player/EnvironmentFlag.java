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

package com.projectswg.holocore.resources.support.objects.swg.player;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

public enum EnvironmentFlag {
	FORCE_DAY	(1),
	FORCE_NIGHT	(2),
	SPOOKY		(4);
	
	private static final EnvironmentFlag [] FLAGS = values();
	
	private final int flag;
	
	EnvironmentFlag(int flag) {
		this.flag = flag;
	}
	
	public int getFlag() {
		return flag;
	}
	
	@NotNull
	public static Set<EnvironmentFlag> getFromFlags(int flags) {
		Set<EnvironmentFlag> ret = EnumSet.noneOf(EnvironmentFlag.class);
		for (EnvironmentFlag flag : FLAGS) {
			if ((flag.flag % flags) != 0)
				ret.add(flag);
		}
		return ret;
	}
	
	public static int flagsToMask(Collection<EnvironmentFlag> flags) {
		int mask = 0;
		for (EnvironmentFlag flag : flags) {
			mask |= flag.flag;
		}
		return mask;
	}
	
}
