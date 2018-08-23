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
package com.projectswg.holocore.resources.support.global.network;

import com.projectswg.common.encoding.StringType;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.global.player.Player;
import me.joshlarson.jlcommon.log.Log;

import java.lang.ref.SoftReference;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.List;

public class BaselineObject {
	
	private transient final List<SoftReference<Baseline>> baselineData;
	
	private final BaselineType type;
	
	public BaselineObject(BaselineType type) {
		this.type = type;
		baselineData = new ArrayList<>(9);
		for (int i = 0; i < 9; i++) {
			baselineData.add(null);
		}
	}
	
	public BaselineType getBaselineType() {
		return type;
	}
	
	public void parseBaseline(Baseline baseline) {
		NetBuffer buffer = NetBuffer.wrap(baseline.getBaselineData());
		buffer.getShort();
		try {
			switch (baseline.getNum()) {
				case 1:
					parseBaseline1(buffer);
					break;
				case 3:
					parseBaseline3(buffer);
					break;
				case 4:
					parseBaseline4(buffer);
					break;
				case 6:
					parseBaseline6(buffer);
					break;
				case 7:
					parseBaseline7(buffer);
					break;
				case 8:
					parseBaseline8(buffer);
					break;
				case 9:
					parseBaseline9(buffer);
					break;
			}
		} catch (BufferUnderflowException | BufferOverflowException e) {
			Log.e("Failed to parse baseline %s %d with object: %s", baseline.getType(), baseline.getNum(), this);
			Log.e(e);
		}
	}
	
	public Baseline createBaseline1(Player target) {
		return createBaseline(target, 1, this::createBaseline1);
	}
	
	public Baseline createBaseline3(Player target) {
		return createBaseline(target, 3, this::createBaseline3);
	}
	
	public Baseline createBaseline4(Player target) {
		return createBaseline(target, 4, this::createBaseline4);
	}
	
	public Baseline createBaseline6(Player target) {
		return createBaseline(target, 6, this::createBaseline6);
	}
	
	public Baseline createBaseline7(Player target) {
		return createBaseline(target, 7, this::createBaseline7);
	}
	
	public Baseline createBaseline8(Player target) {
		return createBaseline(target, 8, this::createBaseline8);
	}
	
	public Baseline createBaseline9(Player target) {
		return createBaseline(target, 9, this::createBaseline9);
	}
	
	protected void createBaseline1(Player target, BaselineBuilder data) {
		
	}
	
	protected void createBaseline3(Player target, BaselineBuilder data) {
		
	}
	
	protected void createBaseline4(Player target, BaselineBuilder data) {
		
	}
	
	protected void createBaseline6(Player target, BaselineBuilder data) {
		
	}
	
	protected void createBaseline7(Player target, BaselineBuilder data) {
		
	}
	
	protected void createBaseline8(Player target, BaselineBuilder data) {
		
	}
	
	protected void createBaseline9(Player target, BaselineBuilder data) {
		
	}
	
	protected void parseBaseline1(NetBuffer buffer) {
		
	}
	
	protected void parseBaseline3(NetBuffer buffer) {
		
	}
	
	protected void parseBaseline4(NetBuffer buffer) {
		
	}
	
	protected void parseBaseline6(NetBuffer buffer) {
		
	}
	
	protected void parseBaseline7(NetBuffer buffer) {
		
	}
	
	protected void parseBaseline8(NetBuffer buffer) {
		
	}
	
	protected void parseBaseline9(NetBuffer buffer) {
		
	}
	
	public final void sendDelta(int type, int update, Object value) {
		verifySwgObject();
		synchronized (baselineData) {
			baselineData.set(type-1, null);
		}
		DeltaBuilder.send((SWGObject) this, this.type, type, update, value);
	}
	
	public final void sendDelta(int type, int update, Object value, StringType strType) {
		verifySwgObject();
		synchronized (baselineData) {
			baselineData.set(type-1, null);
		}
		DeltaBuilder.send((SWGObject) this, this.type, type, update, value, strType);
	}
	
	private Baseline createBaseline(Player target, int num, BaselineCreator bc) {
		verifySwgObject();
		synchronized (baselineData) {
			Baseline data = getBaseline(num);
			if (data == null) {
				BaselineBuilder bb = new BaselineBuilder((SWGObject) this, type, num);
				bc.createBaseline(target, bb);
				data = bb.buildAsBaselinePacket();
				setBaseline(num, data);
			}
			return data;
		}
	}
	
	private void verifySwgObject() {
		if (!(this instanceof SWGObject))
			throw new IllegalStateException("This object is not an SWGObject!");
	}
	
	private Baseline getBaseline(int num) {
		SoftReference<Baseline> ref = baselineData.get(num-1);
		return ref == null ? null : ref.get();
	}
	
	private void setBaseline(int num, Baseline baseline) {
		baselineData.set(num-1, new SoftReference<>(baseline));
	}
	
	private interface BaselineCreator {
		void createBaseline(Player target, BaselineBuilder bb);
	}
	
}
