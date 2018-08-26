/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of PSWGCommon.                                                *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * PSWGCommon is free software: you can redistribute it and/or modify              *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * PSWGCommon is distributed in the hope that it will be useful,                   *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with PSWGCommon.  If not, see <http://www.gnu.org/licenses/>.             *
 ***********************************************************************************/

package com.projectswg.holocore.intents.gameplay.world.travel.pet;

import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.intangible.IntangibleObject;
import me.joshlarson.jlcommon.control.Intent;
import org.jetbrains.annotations.NotNull;

public class PetDeviceStoreIntent extends Intent {
	
	private final CreatureObject creature;
	private final IntangibleObject controlDevice;
	
	public PetDeviceStoreIntent(@NotNull CreatureObject creature, @NotNull IntangibleObject controlDevice) {
		this.creature = creature;
		this.controlDevice = controlDevice;
	}
	
	@NotNull
	public CreatureObject getCreature() {
		return creature;
	}
	
	@NotNull
	public IntangibleObject getControlDevice() {
		return controlDevice;
	}
	
	public static void broadcast(@NotNull CreatureObject creature, @NotNull IntangibleObject controlDevice) {
		new PetDeviceStoreIntent(creature, controlDevice).broadcast();
	}
	
}
