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
package com.projectswg.holocore.resources.support.global.zone.sui

enum class SuiButtons {
	OK,
	CANCEL,
	OK_CANCEL,
	YES_NO,
	OK_REFRESH_CANCEL,
	YES_NO_CANCEL,
	YES_NO_MAYBE,
	YES_NO_ABSTAIN,
	RETRY_CANCEL,
	RETRY_ABORT_CANCEL,
	OK_REFRESH,
	OK_CANCEL_REFRESH,
	REFRESH,
	REFRESH_CANCEL,
	OK_CANCEL_ALL,
	MOVEUP_MOVEDOWN_DONE,
	REMOVE_CANCEL,
	BET_MAX_BET_ONE_SPIN,
	DEFAULT,
	COMBO_OK,
	COMBO_OK_CANCEL,
	OK_LEAVE_GROUP;

	companion object {
		fun valueOf(index: Int): SuiButtons {
			return when (index) {
				0    -> OK
				1    -> CANCEL
				3    -> REFRESH
				else -> DEFAULT
			}
		}
	}
}
