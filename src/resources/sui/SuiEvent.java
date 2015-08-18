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

package resources.sui;

/**
 * Created by Waverunner on 8/14/2015
 */
public enum SuiEvent {
	NONE(0),
	BUTTON(1),
	CHECKBOX(2),
	ENABLE_DISABLE(3),
	GENERIC(4),
	SLIDER(5),
	TAB_PANE(6),
	TEXTBOX(7),
	VISIBILITY_CHANGED(8),
	OK_PRESSED(9),
	CANCEL_PRESSED(10);

	private int value;

	SuiEvent(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static SuiEvent valueOf(int value) {
		switch(value) {
			case 0: return NONE;
			case 1: return BUTTON;
			case 2: return CHECKBOX;
			case 3: return ENABLE_DISABLE;
			case 4: return GENERIC;
			case 5: return SLIDER;
			case 6: return TAB_PANE;
			case 7: return TEXTBOX;
			case 8: return VISIBILITY_CHANGED;
			case 9: return OK_PRESSED;
			case 10: return CANCEL_PRESSED;
			default: return NONE;
		}
	}
}
