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
package com.projectswg.holocore.services.gameplay.player.experience;

import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.PlayerLevelLoader;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class SdbCombatLevelRepository implements CombatLevelRepository {
	
	@Override
	public Collection<CombatLevel> getCombatLevels() {
		Collection<PlayerLevelLoader.PlayerLevelInfo> sdbModels = getPlayerLevelInfosFromSdb();
		
		return convertSdbModelsToDomainModels(sdbModels);
	}
	
	@NotNull
	private List<CombatLevel> convertSdbModelsToDomainModels(Collection<PlayerLevelLoader.PlayerLevelInfo> playerLevelInfos) {
		return playerLevelInfos.stream()
				.map(this::convertSdbModelToDomainModel)
				.collect(Collectors.toList());
	}
	
	private Collection<PlayerLevelLoader.PlayerLevelInfo> getPlayerLevelInfosFromSdb() {
		PlayerLevelLoader playerLevelLoader = DataLoader.Companion.playerLevels();
		return playerLevelLoader.getLevels();
	}
	
	@NotNull
	private CombatLevel convertSdbModelToDomainModel(PlayerLevelLoader.PlayerLevelInfo playerLevelInfo) {
		return new CombatLevel(playerLevelInfo.getLevel(), playerLevelInfo.getRequiredCombatXp(), playerLevelInfo.getHealthAdded());
	}
}
