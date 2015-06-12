/*******************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com
 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.
 * Our goal is to create an emulator which will provide a server for players to
 * continue playing a game similar to the one they used to play. We are basing
 * it on the final publish of the game prior to end-game events.
 *
 * This file is part of Holocore.
 *
 * --------------------------------------------------------------------------------
 *
 * Holocore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Holocore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/

package resources.objects.tangible;

import java.util.EnumSet;

public enum OptionFlag {
	ON_OFF(0x00000001),
	VENDOR(0x00000002),
	INSURED(0x00000004),
	CONVERSABLE(0x00000008),
	HIBERNATING(0x00000010),
	MAGIC_ITEM(0x00000020),
	AGGRESSIVE(0x00000040),
	HAM_BAR(0x00000080),
	INVULNERABLE(0x00000100),
	DISABLED(0x00000200),
	UNINSURABLE(0x00000400),
	INTERESTING(0x00000800),
	MOUNT(0x00001000),
	CRAFTED(0x00002000),
	WINGS_OPENED(0x00004000),
	SPACE_INTERESTING(0x00008000),
	DOCKING(0x00010000),    // JTL?
	DESTROYING(0x00020000), // JTL?
	COMMABLE(0x00040000),
	DOCKABLE(0x00080000),
	EJECT(0x00100000),
	INSPECTABLE(0x00200000),
	TRANSFERABLE(0x00400000),
	SHOW_FLIGHT_TUTORIAL(0x00800000),
	SPACE_COMBAT_MUSIC(0x01000000),
	ENCOUNTER_LOCKED(0x02000000),
	SPAWNED_CREATURE(0x04000000),
	HOLIDAY_INTERESTING(0x08000000),
	LOCKED(0x10000000);

	int flag;

	OptionFlag(int flag) {
		this.flag = flag;
	}

	public int getFlag() {
		return flag;
	}

	public static EnumSet<OptionFlag> toEnumSet(int flags) {
		EnumSet<OptionFlag> enumSet = EnumSet.noneOf(OptionFlag.class);
		for (OptionFlag optionFlag : OptionFlag.values()) {
			if ((flags & optionFlag.getFlag()) == optionFlag.getFlag())
				enumSet.add(optionFlag);
		}

		return enumSet;
	}
}
