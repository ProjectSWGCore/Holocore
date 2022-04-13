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
		return playerLevelLoader.getPlayerLevelInfos();
	}
	
	@NotNull
	private CombatLevel convertSdbModelToDomainModel(PlayerLevelLoader.PlayerLevelInfo playerLevelInfo) {
		return new CombatLevel(playerLevelInfo.getLevel(), playerLevelInfo.getRequiredCombatXp(), playerLevelInfo.getHealthAdded());
	}
}
