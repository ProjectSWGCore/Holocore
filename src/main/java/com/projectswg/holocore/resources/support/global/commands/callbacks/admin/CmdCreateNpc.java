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
package com.projectswg.holocore.resources.support.global.commands.callbacks.admin;

import com.projectswg.holocore.intents.gameplay.world.CreateSpawnIntent;
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcStaticSpawnLoader;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.npc.spawn.SimpleSpawnInfo;
import com.projectswg.holocore.resources.support.npc.spawn.SpawnerType;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureDifficulty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CmdCreateNpc implements ICmdCallback {
    @Override
    public void execute(@NotNull Player player, @Nullable SWGObject target, @NotNull String args) {
        String[] commandArgs = args.split(" ");
        String npcId = determineNpcId(commandArgs);
        CreatureDifficulty difficulty = determineDifficulty(commandArgs);
        int combatLevel = determineCombatLevel(commandArgs);

        spawnNPC(player, npcId, difficulty, combatLevel);
    }

    private String determineNpcId(String[] commandArgs) {
        return commandArgs[0];
    }

    private CreatureDifficulty determineDifficulty(String[] commandArgs) {
        String arg = commandArgs[1];

        switch (arg) {
            case "b": return CreatureDifficulty.BOSS;
            case "e": return CreatureDifficulty.ELITE;
            default:
            case "n": return CreatureDifficulty.NORMAL;
        }
    }

    private int determineCombatLevel(String[] commandArgs) {
        return Integer.parseInt(commandArgs[2]);
    }

    private void spawnNPC(Player player, String npcId, CreatureDifficulty difficulty, int combatLevel) {
        CellObject cell = (CellObject) player.getCreatureObject().getParent();
        SimpleSpawnInfo spawnInfo = SimpleSpawnInfo.builder()
                .withNpcId(npcId)
                .withDifficulty(difficulty)
                .withMinLevel(combatLevel)
                .withMaxLevel(combatLevel)
                .withLocation(player.getCreatureObject().getLocation())
                .withSpawnerFlag(NpcStaticSpawnLoader.SpawnerFlag.ATTACKABLE)
                .withSpawnerType(SpawnerType.EGG)
                .withBuildingId(determineBuildoutTag(cell))
                .withCellId(determineCellId(cell))
                .build();

        new CreateSpawnIntent(spawnInfo).broadcast();
    }

    private String determineBuildoutTag(CellObject cell) {
        if (cell == null) {
            return "";
        }

        SWGObject building = cell.getParent();

        if (building == null) {
            return "";
        }

        return building.getBuildoutTag();
    }

    private int determineCellId(CellObject cell) {
        if (cell == null) {
            return 0;
        }


        return cell.getNumber();
    }
}
