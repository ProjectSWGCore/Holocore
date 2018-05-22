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
package com.projectswg.holocore.services.commands.buff;

import com.projectswg.common.data.CRC;
import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.data.swgfile.visitors.DatatableData;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BuffMap {
	
	private final Map<Integer, BuffData> buffMap;
	
	public BuffMap() {
		this.buffMap = new ConcurrentHashMap<>();
	}
	
	public void load() {
		DatatableData buffTable = (DatatableData) ClientFactory.getInfoFromFile("datatables/buff/buff.iff");
		int rows = buffTable.getRowCount();
		
		buffMap.clear();
		for (int row = 0; row < rows; ++row) {
			BuffData buff = new BuffData(buffTable.getString(row, "NAME"), buffTable.getString(row, "GROUP1"), buffTable.getInt(row, "PRIORITY"));
			buff.setMaxStackCount(buffTable.getInt(row, "MAX_STACKS"));
			for (int i = 0; i < 5; i++) {
				buff.setEffectName(i, buffTable.getString(row, "EFFECT"+(i+1)+"_PARAM"));
				buff.setEffectValue(i, buffTable.getFloat(row, "EFFECT"+(i+1)+"_VALUE"));
			}
			buff.setDefaultDuration(buffTable.getFloat(row, "DURATION"));
			buff.setEffectFileName(buffTable.getString(row, "PARTICLE"));
			buff.setParticleHardPoint(buffTable.getString(row, "PARTICLE_HARDPOINT"));
			buff.setStanceParticle(buffTable.getString(row, "STANCE_PARTICLE"));
			buff.setCallback(buffTable.getString(row, "CALLBACK"));
			buff.setPersistent(buffTable.getInt(row, "IS_PERSISTENT") == 1);
			buff.setRemovedOnDeath(buffTable.getInt(row, "REMOVE_ON_DEATH") == 1);
			buff.setDecayOnPvpDeath(buffTable.getInt(row, "DECAY_ON_PVP_DEATH") == 1);
			buffMap.put(buff.getCrc(), buff);
		}
	}
	
	public int size() {
		return buffMap.size();
	}
	
	public BuffData getBuff(int crc) {
		return buffMap.get(crc);
	}
	
	public BuffData getBuff(CRC crc) {
		return getBuff(crc.getCrc());
	}
	
	public BuffData getBuff(String name) {
		return getBuff(getCrc(name));
	}
	
	public boolean containsBuff(int crc) {
		return buffMap.containsKey(crc);
	}
	
	public boolean containsBuff(String name) {
		return containsBuff(getCrc(name));
	}
	
	private int getCrc(String name) {
		return CRC.getCrc(name.toLowerCase(Locale.ENGLISH));
	}
	
}
