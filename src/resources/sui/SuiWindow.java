/***********************************************************************************
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
package resources.sui;

import intents.sui.SuiWindowIntent;
import intents.sui.SuiWindowIntent.SuiWindowEvent;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import network.packets.Packet;
import resources.encodables.Encodable;
import resources.player.Player;

public class SuiWindow implements Encodable {

	private int id;
	private String script;
	private Player owner;
	private long rangeObjId;
	private float maxDistance = 0;
	private List<SuiComponent> components = new ArrayList<>();
	private Map<Integer, String> scriptCallbacks;
	private Map<Integer, ISuiCallback> javaCallbacks;
	
	public SuiWindow(String script, Player owner) {
		this.script = script;
		this.owner = owner;
	}

	public final void clearDataSource(String dataSource) {
		SuiComponent component = createComponent((byte) 1);
		
		component.getNarrowParams().add(dataSource);
		components.add(component);
	}
	
	public final void addChildWidget(String property, String value) {
		SuiComponent component = createComponent((byte) 2);
		
		addNarrowParams(component, property);
		
		component.getWideParams().add(value);
		components.add(component);
	}
	
	public final void setProperty(String property, String value) {
		SuiComponent component = createComponent((byte) 3);
		
		addNarrowParams(component, property);
		
		component.getWideParams().add(value);
		components.add(component);
	}
	
	public final void addDataItem(String name, String value) {
		SuiComponent component = createComponent((byte) 4);
		
		addNarrowParams(component, name);
		
		component.getWideParams().add(value);
		components.add(component);
	}
	
	private void addCallbackComponent(String source, Trigger trigger, List<String> returnParams) {
		SuiComponent component = createComponent((byte) 5);
		
		component.getNarrowParams().add(source);
		component.getNarrowParams().add(new String(new byte[] {trigger.getByte()}, Charset.forName("UTF-8")));
		component.getNarrowParams().add("handleSUI");
		
		for (String returnParam : returnParams) {
			addNarrowParams(component, returnParam);
		}
		
		components.add(component);
	}
	
	public final void addDataSource(String name, String value) {
		SuiComponent component = createComponent((byte) 6);
		
		addNarrowParams(component, name);
		
		component.getWideParams().add(value);
		components.add(component);
	}
	
	public final void clearDataSourceContainer(String dataSource) {
		SuiComponent component = createComponent((byte) 7);
		
		addNarrowParams(component, dataSource);
		
		components.add(component);
	}
	
	public final void addTableDataSource(String dataSource, String value) {
		SuiComponent component = createComponent((byte) 8);
		
		addNarrowParams(component, dataSource);
		
		component.getWideParams().add(value);
		components.add(component);
	}
	
	public final void addCallback(int eventId, String source, Trigger trigger, List<String> returnParams, ISuiCallback callback) {
		addCallbackComponent(source, trigger, returnParams);
		
		if (javaCallbacks == null)
			javaCallbacks = new HashMap<>();
		javaCallbacks.put(eventId, callback);
	}
	
	public final void addCallback(int eventId, String source, Trigger trigger, List<String> returnParams, String callbackScript) {
		addCallbackComponent(source, trigger, returnParams);
		
		if (scriptCallbacks == null)
			scriptCallbacks = new HashMap<>();
		scriptCallbacks.put(eventId, callbackScript);
	}

	private SuiComponent createComponent(byte type) {
		SuiComponent component = new SuiComponent();
		component.setType(SuiComponent.Type.valueOf(type));
		
		return component;
	}
	
	private void addNarrowParams(SuiComponent component, String property) {
		for (String s : property.split(":")) {
			component.getNarrowParams().add(s);
		}
	}
	
	public final void display() { new SuiWindowIntent(owner, this, SuiWindowEvent.NEW).broadcast(); }
	public final void display(Player player) { new SuiWindowIntent(player, this, SuiWindowEvent.NEW).broadcast();}
	
	public final long getRangeObjId() { return rangeObjId; }
	public final void setRangeObjId(long rangeObjId) { this.rangeObjId = rangeObjId; }
	public final int getId() { return id; }
	public final void setId(int id) { this.id = id; }
	public final String getScript() { return script; }
	public final Player getOwner() { return owner; }
	public final float getMaxDistance() { return maxDistance; }
	public final void setMaxDistance(float maxDistance) { this.maxDistance = maxDistance; }
	public final List<SuiComponent> getComponents() { return components; }
	public final ISuiCallback getJavaCallback(int eventType) { return ((javaCallbacks == null) ? null : javaCallbacks.get(eventType)); }
	public final String getScriptCallback(int eventType) { return ((scriptCallbacks == null) ? null : scriptCallbacks.get(eventType)); }

	@Override
	public byte[] encode() {

		int listSize = 0;
		List<byte[]> componentData = new ArrayList<>();
		for (SuiComponent component : components) {
			byte[] data = component.encode();
			componentData.add(data);
			listSize += data.length;
		}

		ByteBuffer data = ByteBuffer.allocate(34 + script.length() + listSize);
		Packet.addInt(data, id);
		Packet.addAscii(data, script);
		Packet.addList(data, componentData);
		Packet.addLong(data, rangeObjId);
		Packet.addFloat(data, maxDistance);
		Packet.addLong(data, 0); // Window Location?
		Packet.addInt(data, 0);

		return data.array();
	}

	@Override
	public void decode(ByteBuffer data) {
		id			= Packet.getInt(data);
		script		= Packet.getAscii(data);
		components	= Packet.getList(data, SuiComponent.class);
		rangeObjId	= Packet.getLong(data);
		maxDistance	= Packet.getFloat(data);
		// unk long
		// unk int
	}

	public enum Trigger {
		UPDATE	((byte) 4),
		OK		((byte) 9),
		CANCEL	((byte) 10);
		
		private byte b;
		
		Trigger(byte b) {
			this.b = b;
		}
		
		public byte getByte() {
			return b;
		}
	}
}
