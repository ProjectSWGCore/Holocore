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
package com.projectswg.holocore.resources.support.data.server_info.loader.npc;

import com.projectswg.common.data.encodables.tangible.PvpFaction;
import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData;
import com.projectswg.holocore.resources.support.data.server_info.loader.combat.FactionLoader.Faction;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

public final class NpcLoader extends DataLoader {
	
	private final Map<String, NpcInfo> npcMap;
	
	public NpcLoader() {
		this.npcMap = new HashMap<>();
	}
	
	public NpcInfo getNpc(String id) {
		return npcMap.get(id);
	}
	
	public void iterate(Consumer<NpcInfo> npc) {
		npcMap.values().forEach(npc);
	}
	
	public Collection<NpcInfo> getNpcs() {
		return Collections.unmodifiableCollection(npcMap.values());
	}
	
	public int getNpcCount() {
		return npcMap.size();
	}
	
	@Override
	public void load() throws IOException {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/npc/npc.msdb"))) {
			npcMap.putAll(set.parallelStream(NpcInfo::new).collect(toMap(NpcInfo::getId, Function.identity())));
		}
	}
	
	public static class NpcInfo {
		
		/*
			* means unimplemented
			npc_id						TEXT
			spawnerFlag					TEXT
			difficulty					TEXT
			combat_level				INTEGER
			npc_name					TEXT
			stf_name					TEXT
			niche						TEXT
			iff_template				TEXT
			planet					*	TEXT
			offers_mission			*	TEXT
			social_group				TEXT
			faction						TEXT
			spec_force					TEXT
			scale_min				*	REAL
			scale_max				*	REAL
			hue						*	INTEGER
			grant_skill				*	TEXT
			ignore_player			*	TEXT
			attack_speed				REAL
			movement_speed				REAL
			default_weapon				TEXT
			default_weapon_specials	*	TEXT
			thrown_weapon			TEXT
			thrown_weapon_specials*	TEXT
			aggressive_radius			INTEGER
			assist_radius				INTEGER
			stalker					*	TEXT
			herd					*	TEXT
			death_blow					BOOLEAN
			skillmods				*	TEXT
			loot_table1_chance			INTEGER
			loot_table1					TEXT
			loot_table2_chance			INTEGER
			loot_table2					TEXT
			loot_table3_chance			INTEGER
			loot_table3					TEXT
			chronicle_loot_chance	*	INTEGER
			chronicle_loot_category	*	TEXT
		 */
		private final String id;
		private final String name;
		private final String stfName;
		private final String niche;
		private final Faction faction;
		private final boolean specForce;
		private final double attackSpeed;
		private final double movementSpeed;
		private final int hue;
		private final double scaleMin;
		private final double scaleMax;
		private final String defaultWeaponSpecials;
		private final String thrownWeaponSpecials;
		private final int aggressiveRadius;
		private final int assistRadius;
		private final boolean deathblow;
		private final String lootTable1;
		private final String lootTable2;
		private final String lootTable3;
		private final int lootTable1Chance;
		private final int lootTable2Chance;
		private final int lootTable3Chance;
		
		private final List<String> iffs;
		private final List<String> defaultWeapon;
		private final List<String> thrownWeapon;
		private final HumanoidNpcInfo humanoidInfo;
		private final DroidNpcInfo droidInfo;
		private final CreatureNpcInfo creatureInfo;
		private final boolean resources;
		private final NpcResourceInfo milkResourceInfo;
		private final NpcResourceInfo meatResourceInfo;
		private final NpcResourceInfo hideResourceInfo;
		private final NpcResourceInfo boneResourceInfo;
		private final String socialGroup;

		private static final Faction DEFAULT_FACTION = ServerData.INSTANCE.getFactions().getFaction(PvpFaction.NEUTRAL.name().toLowerCase(Locale.US));

		public NpcInfo(SdbResultSet set) {
			this.id = set.getText("npc_id");
			this.name = set.getText("npc_name").intern();
			this.scaleMin = set.getReal("scale_min");
			this.scaleMax = set.getReal("scale_max");
			this.hue = (int) set.getInt("hue");
			this.stfName = set.getText("stf_name");
			this.niche = set.getText("niche").intern();
			String factionString = set.getText("faction");
			Faction faction = ServerData.INSTANCE.getFactions().getFaction(factionString);
			if (faction == null) {
				if (!factionString.equals("-"))
					Log.w("Unknown faction: %s", factionString);
				faction = DEFAULT_FACTION;
			}
			this.faction = faction;
			this.iffs = List.of(set.getText("iff_template").split(";")).stream().map(s -> "object/mobile/"+s).map(ClientFactory::formatToSharedFile).collect(Collectors.toUnmodifiableList());
			this.specForce = set.getBoolean("spec_force");
			this.attackSpeed = set.getReal("attack_speed");
			this.movementSpeed = set.getReal("movement_speed");
			this.defaultWeapon = parseWeapons(set.getText("default_weapon"));
			this.defaultWeaponSpecials = set.getText("default_weapon_specials");
			this.thrownWeapon = parseWeapons(set.getText("thrown_weapon"));
			this.thrownWeaponSpecials = set.getText("thrown_weapon_specials");
			this.aggressiveRadius = (int) set.getInt("aggressive_radius");
			this.assistRadius = (int) set.getInt("assist_radius");
			this.deathblow = set.getBoolean("death_blow");
			this.lootTable1 = set.getText("loot_table1");
			this.lootTable2 = set.getText("loot_table2");
			this.lootTable3 = set.getText("loot_table3");
			this.lootTable1Chance = (int) set.getInt("loot_table1_chance");
			this.lootTable2Chance = (int) set.getInt("loot_table2_chance");
			this.lootTable3Chance = (int) set.getInt("loot_table3_chance");
			this.resources = set.getBoolean("resources");
			this.milkResourceInfo = new NpcResourceInfo(set, "milk");
			this.meatResourceInfo = new NpcResourceInfo(set, "meat");
			this.hideResourceInfo = new NpcResourceInfo(set, "hide");
			this.boneResourceInfo = new NpcResourceInfo(set, "bone");
			
			if (scaleMax < scaleMin)
				throw new IllegalArgumentException("scaleMax must be greater than scaleMin");
			
			switch (niche) {
				case "humanoid":
					this.humanoidInfo = new HumanoidNpcInfo(set);
					this.droidInfo = null;
					this.creatureInfo = null;
					break;
				case "droid":
					this.humanoidInfo = null;
					this.droidInfo = new DroidNpcInfo(set);
					this.creatureInfo = null;
					break;
				case "creature":
					this.humanoidInfo = null;
					this.droidInfo = null;
					this.creatureInfo = new CreatureNpcInfo(set);
					break;
				case "vehicle":
					this.humanoidInfo = null;
					this.droidInfo = null;
					this.creatureInfo = null;
					break;
				default:
					throw new IllegalArgumentException("Unknown NPC niche: " + niche);
			}

			String socialGroup = set.getText("social_group");
			if (!"-".equals(socialGroup)) {
				this.socialGroup = socialGroup;
			} else {
				this.socialGroup = null;
			}
		}
		
		public String getId() {
			return id;
		}
		
		public String getName() {
			return name;
		}
		
		public String getStfName() {
			return stfName;
		}
		
		public List<String> getIffs() {
			return Collections.unmodifiableList(iffs);
		}
		
		public Faction getFaction() {
			return faction;
		}
		
		public boolean isSpecForce() {
			return specForce;
		}
		
		public double getAttackSpeed() {
			return attackSpeed;
		}
		
		public double getMovementSpeed() {
			return movementSpeed;
		}
		
		public double getScaleMin() {
			return scaleMin;
		}
		
		public double getScaleMax() {
			return scaleMax;
		}
		
		public int getHue() {
			return hue;
		}
		
		public List<String> getDefaultWeapon() {
			return defaultWeapon;
		}
		
		public String getDefaultWeaponSpecials() {
			return defaultWeaponSpecials;
		}
		
		public List<String> getThrownWeapon() {
			return thrownWeapon;
		}
		
		public String getThrownWeaponSpecials() {
			return thrownWeaponSpecials;
		}
		
		public int getAggressiveRadius() {
			return aggressiveRadius;
		}
		
		public int getAssistRadius() {
			return assistRadius;
		}
		
		public boolean isDeathblow() {
			return deathblow;
		}
		
		public String getLootTable1() {
			return lootTable1;
		}
		
		public String getLootTable2() {
			return lootTable2;
		}
		
		public String getLootTable3() {
			return lootTable3;
		}
		
		public int getLootTable1Chance() {
			return lootTable1Chance;
		}
		
		public int getLootTable2Chance() {
			return lootTable2Chance;
		}
		
		public int getLootTable3Chance() {
			return lootTable3Chance;
		}

		public boolean isResources() {
			return resources;
		}

		public NpcResourceInfo getMilkResourceInfo() {
			return milkResourceInfo;
		}

		public NpcResourceInfo getMeatResourceInfo() {
			return meatResourceInfo;
		}

		public NpcResourceInfo getHideResourceInfo() {
			return hideResourceInfo;
		}

		public NpcResourceInfo getBoneResourceInfo() {
			return boneResourceInfo;
		}

		@Nullable
		public String getSocialGroup() {
			return socialGroup;
		}

		public HumanoidNpcInfo getHumanoidInfo() {
			return humanoidInfo;
		}
		
		public DroidNpcInfo getDroidInfo() {
			return droidInfo;
		}
		
		public CreatureNpcInfo getCreatureInfo() {
			return creatureInfo;
		}
		
		private static List<String> parseWeapons(String str) {
			if (str.isEmpty() || str.equals("-"))
				return List.of();
			return List.of(str.split(";"));
		}
		
	}
	
	public static class HumanoidNpcInfo {
		
		public HumanoidNpcInfo(SdbResultSet set) {
			
		}
		
	}
	
	public static class DroidNpcInfo {
		
		public DroidNpcInfo(SdbResultSet set) {
			
		}
		
	}
	
	public static class CreatureNpcInfo {
		
		public CreatureNpcInfo(SdbResultSet set) {
			
		}
		
	}
	
	public static class NpcResourceInfo {
		private final int amount;
		private final String type;

		public NpcResourceInfo(SdbResultSet set, String prefix) {
			amount = (int) set.getInt(prefix + "_" + "amount");
			type = set.getText(prefix + "_" + "type");
		}

		public int getAmount() {
			return amount;
		}

		public String getType() {
			return type;
		}
	}
	
}
