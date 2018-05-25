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
package com.projectswg.holocore.resources.support.objects.swg.custom;

import com.projectswg.holocore.resources.support.data.server_info.loader.spawn.StaticSpawnLoader.PatrolFormation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PatrolGroup {
	
	private final List<AIObject> objects;
	private final int count;
	private final PatrolFormation formation;
	
	public PatrolGroup(int count, PatrolFormation formation) {
		this.objects = new ArrayList<>();
		this.count = count;
		this.formation = formation;
	}
	
	public int getCount() {
		return count;
	}
	
	public PatrolFormation getFormation() {
		return formation;
	}
	
	public List<AIObject> getObjects() {
		return Collections.unmodifiableList(objects);
	}
	
	public void addObject(AIObject object) {
		this.objects.add(object);
	}
	
}
