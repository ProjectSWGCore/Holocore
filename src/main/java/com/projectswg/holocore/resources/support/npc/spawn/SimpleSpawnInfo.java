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
package com.projectswg.holocore.resources.support.npc.spawn;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcStaticSpawnLoader;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureDifficulty;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIBehavior;

import java.util.concurrent.TimeUnit;

public class SimpleSpawnInfo implements SpawnInfo {

    private String id;
    private Terrain terrain;
    private double x;
    private double y;
    private double z;
    private int heading;
    private int cellId;
    private String spawnerType;
    private String npcId;
    private NpcStaticSpawnLoader.SpawnerFlag spawnerFlag;
    private CreatureDifficulty difficulty;
    private int minLevel;
    private int maxLevel;
    private String buildingId;
    private String mood;
    private AIBehavior behavior;
    private String patrolId;
    private NpcStaticSpawnLoader.PatrolFormation patrolFormation;
    private int loiterRadius;
    private int minSpawnTime;
    private int maxSpawnTime;
    private int amount;

    private SimpleSpawnInfo() {
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Terrain getTerrain() {
        return terrain;
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public double getZ() {
        return z;
    }

    @Override
    public int getHeading() {
        return heading;
    }

    @Override
    public int getCellId() {
        return cellId;
    }

    @Override
    public String getSpawnerType() {
        return spawnerType;
    }

    @Override
    public String getNpcId() {
        return npcId;
    }

    @Override
    public NpcStaticSpawnLoader.SpawnerFlag getSpawnerFlag() {
        return spawnerFlag;
    }

    @Override
    public CreatureDifficulty getDifficulty() {
        return difficulty;
    }

    @Override
    public int getMinLevel() {
        return minLevel;
    }

    @Override
    public int getMaxLevel() {
        return maxLevel;
    }

    @Override
    public String getBuildingId() {
        return buildingId;
    }

    @Override
    public String getMood() {
        return mood;
    }

    @Override
    public AIBehavior getBehavior() {
        return behavior;
    }

    @Override
    public String getPatrolId() {
        return patrolId;
    }

    @Override
    public NpcStaticSpawnLoader.PatrolFormation getPatrolFormation() {
        return patrolFormation;
    }

    @Override
    public int getLoiterRadius() {
        return loiterRadius;
    }

    @Override
    public int getMinSpawnTime() {
        return minSpawnTime;
    }

    @Override
    public int getMaxSpawnTime() {
        return maxSpawnTime;
    }

    @Override
    public int getAmount() {
        return amount;
    }
	
	@Override
	public String getConversationId() {
		return null;
	}
	
	public static class Builder {

        private final SimpleSpawnInfo info;

        private Builder() {
            info = new SimpleSpawnInfo();
            info.id = "simple";
            info.behavior = AIBehavior.IDLE;
            info.mood = "";
            info.spawnerFlag = NpcStaticSpawnLoader.SpawnerFlag.INVULNERABLE;
            info.buildingId = "";
            info.amount = 1;
            info.minSpawnTime = (int) TimeUnit.SECONDS.convert(8, TimeUnit.MINUTES);
            info.maxSpawnTime = (int) TimeUnit.SECONDS.convert(12, TimeUnit.MINUTES);
            info.loiterRadius = 15;
        }

        public Builder withNpcId(String npcId) {
            info.npcId = npcId;
            info.patrolId = "";

            return this;
        }

        public Builder withLocation(Location location) {
            info.x = location.getX();
            info.y = location.getY();
            info.z = location.getZ();
            info.terrain = location.getTerrain();
            info.heading = (int) location.getYaw();

            return this;
        }

        public Builder withBuildingId(String buildingId) {
            info.buildingId = buildingId;

            return this;
        }

        public Builder withCellId(int cellId) {
            info.cellId = cellId;

            return this;
        }

        public Builder withDifficulty(CreatureDifficulty difficulty) {
            info.difficulty = difficulty;

            return this;
        }

        public Builder withMinLevel(int minLevel) {
            info.minLevel = minLevel;

            return this;
        }

        public Builder withMaxLevel(int maxLevel) {
            info.maxLevel = maxLevel;

            return this;
        }

        public Builder withSpawnerFlag(NpcStaticSpawnLoader.SpawnerFlag spawnerFlag) {
            info.spawnerFlag = spawnerFlag;

            return this;
        }

        public Builder withSpawnerType(SpawnerType spawnerType) {
            info.spawnerType = spawnerType.name();

            return this;
        }

        public Builder withAmount(int amount) {
            info.amount = amount;

            return this;
        }

        public Builder withBehavior(AIBehavior behavior) {
            info.behavior = behavior;

            return this;
        }

        public SimpleSpawnInfo build() {
            return info;
        }
    }
}
