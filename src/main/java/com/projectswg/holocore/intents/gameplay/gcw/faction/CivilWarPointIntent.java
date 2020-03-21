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
package com.projectswg.holocore.intents.gameplay.gcw.faction;

import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import me.joshlarson.jlcommon.control.Intent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class CivilWarPointIntent extends Intent {
	
	private final PlayerObject receiver;
	private final int points;
	private final ProsePackage prose;
	
	private CivilWarPointIntent(@NotNull PlayerObject receiver, int points, @Nullable ProsePackage prose) {
		this.receiver = receiver;
		this.points = points;
		this.prose = prose;
	}
	
	public CivilWarPointIntent(PlayerObject receiver, int points) {
		this(receiver, points, null);
	}
	
	@NotNull
	public PlayerObject getReceiver() {
		return receiver;
	}
	
	public int getPoints() {
		return points;
	}
	
	@Nullable
	public ProsePackage getProse() {
		return prose;
	}
	
	/**
	 * Displays basic system message to the player with the amount of points gained.
	 * @param receiver that should receive GCW points
	 * @param points amount that the {@code receiver} should receive
	 */
	public static void broadcast(@NotNull PlayerObject receiver, int points) {
		new CivilWarPointIntent(receiver, points).broadcast();
	}
	
	/**
	 * Displays customized system message to the player.
	 * @param receiver that should receive GCW points
	 * @param points amount that the {@code receiver} should receive
	 * @param prose custom message to display to the player in case the default one doesn't cut it. If {@code null}, we fall back to the default
	 *                 basic system message.
	 */
	public static void broadcast(@NotNull PlayerObject receiver, int points, @Nullable ProsePackage prose) {
		new CivilWarPointIntent(receiver, points, prose).broadcast();
	}
	
}
