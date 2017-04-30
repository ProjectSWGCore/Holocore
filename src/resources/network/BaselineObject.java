/************************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
 *                                                                                  *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
 * Our goal is to create an emulator which will provide a server for players to     *
 * continue playing a game similar to the one they used to play. We are basing      *
 * it on the final publish of the game prior to end-game events.                    *
 *                                                                                  *
 * This file is part of Holocore.                                                   *
 *                                                                                  *
 * -------------------------------------------------------------------------------- *
 *                                                                                  *
 * Holocore is free software: you can redistribute it and/or modify                 *
 * it under the terms of the GNU Affero General Public License as                   *
 * published by the Free Software Foundation, either version 3 of the               *
 * License, or (at your option) any later version.                                  *
 *                                                                                  *
 * Holocore is distributed in the hope that it will be useful,                      *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
 * GNU Affero General Public License for more details.                              *
 *                                                                                  *
 * You should have received a copy of the GNU Affero General Public License         *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
 *                                                                                  *
 ***********************************************************************************/
package resources.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

import com.projectswg.common.debug.Log;
import com.projectswg.common.encoding.StringType;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.common.utilities.ByteUtilities;

import network.packets.swg.zone.baselines.Baseline;
import network.packets.swg.zone.baselines.Baseline.BaselineType;
import resources.config.ConfigFile;
import resources.objects.SWGObject;
import resources.player.Player;
import resources.server_info.DataManager;

public class BaselineObject {
	
	private transient List<SoftReference<Baseline>> baselineData;
	
	private final BaselineType type;
	
	public BaselineObject(BaselineType type) {
		this.type = type;
		initBaselineData();
	}
	
	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		ois.defaultReadObject();
		initBaselineData();
	}
	
	private void initBaselineData() {
		baselineData = new ArrayList<>(9);
		for (int i = 0; i < 9; i++) {
			baselineData.add(null);
		}
	}
	
	public void parseBaseline(Baseline baseline) {
		NetBuffer buffer = NetBuffer.wrap(baseline.getBaselineData());
		buffer.getShort();
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
	}
	
	public Baseline createBaseline1(Player target) {
		return createBaseline(target, 1, (t, bb)->createBaseline1(t, bb));
	}
	
	public Baseline createBaseline3(Player target) {
		return createBaseline(target, 3, (t, bb)->createBaseline3(t, bb));
	}
	
	public Baseline createBaseline4(Player target) {
		return createBaseline(target, 4, (t, bb)->createBaseline4(t, bb));
	}
	
	public Baseline createBaseline6(Player target) {
		return createBaseline(target, 6, (t, bb)->createBaseline6(t, bb));
	}
	
	public Baseline createBaseline7(Player target) {
		return createBaseline(target, 7, (t, bb)->createBaseline7(t, bb));
	}
	
	public Baseline createBaseline8(Player target) {
		return createBaseline(target, 8, (t, bb)->createBaseline8(t, bb));
	}
	
	public Baseline createBaseline9(Player target) {
		return createBaseline(target, 9, (t, bb)->createBaseline9(t, bb));
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
		DeltaBuilder builder = new DeltaBuilder((SWGObject) this, this.type, type, update, value);
		boolean sent = builder.send();
		if (sent && isDeltaLogging())
			Log.v("Delta [type=%d, update=%d, value=%s] = %s", type, update, value, ByteUtilities.getHexString(builder.getEncodedData()));
	}
	
	public final void sendDelta(int type, int update, Object value, StringType strType) {
		verifySwgObject();
		synchronized (baselineData) {
			baselineData.set(type-1, null);
		}
		DeltaBuilder builder = new DeltaBuilder((SWGObject) this, this.type, type, update, value, strType);
		boolean sent = builder.send();
		if (sent && isDeltaLogging())
			Log.v("Delta %s: [type=%d, update=%d, value=%s, strType=%s] = %s", this, type, update, value, strType, ByteUtilities.getHexString(builder.getEncodedData()));
	}
	
	private boolean isDeltaLogging() {
		return DataManager.getConfig(ConfigFile.DEBUG).getBoolean("DEBUG-LOG-DELTA", false);
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
		baselineData.set(num-1, new SoftReference<Baseline>(baseline));
	}
	
	private interface BaselineCreator {
		void createBaseline(Player target, BaselineBuilder bb);
	}
	
}
