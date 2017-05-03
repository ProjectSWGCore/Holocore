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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.projectswg.common.debug.Log;
import com.projectswg.common.encoding.Encodable;
import com.projectswg.common.network.NetBuffer;

public class SuiBaseWindow implements Encodable {

	private int id;
	private String suiScript;
	private long rangeObjId;
	private float maxDistance = 0;
	private List<SuiComponent> components = new ArrayList<>();
	private Map<String, ISuiCallback> callbacks;
	private Map<String, String> scriptCallbacks;
	private boolean hasSubscriptionComponent = false;

	public SuiBaseWindow() {
	}

	public SuiBaseWindow(String suiScript) {
		this.suiScript = suiScript;
	}

	public final void clearDataSource(String dataSource) {
		SuiComponent component = new SuiComponent(SuiComponent.Type.CLEAR_DATA_SOURCE, dataSource);
		components.add(component);
	}

	public final void addChildWidget(String type, String childWidget, String parentWidget) {
		SuiComponent component = new SuiComponent(SuiComponent.Type.ADD_CHILD_WIDGET, parentWidget);

		component.addNarrowParam(type);
		component.addNarrowParam(childWidget);

		components.add(component);
	}

	public final void setProperty(String widget, String property, String value) {
		SuiComponent component = new SuiComponent(SuiComponent.Type.SET_PROPERTY, widget);

		component.addNarrowParam(property);
		component.addWideParam(value);

		components.add(component);
	}

	public final void addDataItem(String dataSource, String name, String value) {
		SuiComponent component = new SuiComponent(SuiComponent.Type.ADD_DATA_ITEM, dataSource);

		component.addNarrowParam(name);
		component.addWideParam(value);

		components.add(component);
	}

	protected void subscribeToEvent(int event, String widgetSource, String callback) {
		SuiComponent component = getSubscriptionForEvent(event, widgetSource);
		if (component != null) {
			Log.i("Added event callback %d to %s when the event is already subscribed to, replacing callback to %s", event, widgetSource, callback);
			component.getNarrowParams().set(2, callback);
		} else {
			component = new SuiComponent(SuiComponent.Type.SUBSCRIBE_TO_EVENT, widgetSource);
			component.addNarrowParam(getWrappedEventString(event));
			component.addNarrowParam(callback);

			components.add(component);
		}
		if (!hasSubscriptionComponent())
			hasSubscriptionComponent = true;
	}

	protected void subscribeToPropertyEvent(int event, String widgetSource, String propertyWidget, String propertyName) {
		SuiComponent component = getSubscriptionForEvent(event, widgetSource);
		if (component != null) {
			// This component already has the trigger and source param, just need to add the widget and property
			// for client to return the value to the server
			component.addNarrowParam(propertyWidget);
			component.addNarrowParam(propertyName);
		} else {
			component = new SuiComponent(SuiComponent.Type.SUBSCRIBE_TO_EVENT, widgetSource);
			component.addNarrowParam(getWrappedEventString(event));
			component.addNarrowParam("");
			component.addNarrowParam(propertyWidget);
			component.addNarrowParam(propertyName);
			components.add(component);
		}
		if (!hasSubscriptionComponent())
			hasSubscriptionComponent = true;
	}

	public final void addDataSourceContainer(String dataSourceContainer, String name, String value) {
		SuiComponent component = new SuiComponent(SuiComponent.Type.ADD_DATA_SOURCE_CONTAINER, dataSourceContainer);

		component.addNarrowParam(name);
		component.addWideParam(value);

		components.add(component);
	}

	public final void clearDataSourceContainer(String dataSourceContainer) {
		SuiComponent component = new SuiComponent(SuiComponent.Type.CLEAR_DATA_SOURCE_CONTAINER, dataSourceContainer);
		components.add(component);
	}

	public final void addDataSource(String dataSource, String name, String value) {
		SuiComponent component = new SuiComponent(SuiComponent.Type.ADD_DATA_SOURCE, dataSource);

		component.addNarrowParam(name);
		component.addWideParam(value);

		components.add(component);
	}

	public final void addReturnableProperty(SuiEvent event, String source, String widget, String property) {
		subscribeToPropertyEvent(event.getValue(), source, widget, property);
	}

	public final void addReturnableProperty(SuiEvent event, String widget, String property) {
		addReturnableProperty(event, "", widget, property);
	}

	public final void addReturnableProperty(String widget, String property) {
		subscribeToPropertyEvent(SuiEvent.OK_PRESSED.getValue(), "", widget, property);
		subscribeToPropertyEvent(SuiEvent.CANCEL_PRESSED.getValue(), "", widget, property);
	}

	public final void addCallback(SuiEvent event, String source, String name, ISuiCallback callback) {
		subscribeToEvent(event.getValue(), source, name);
		addJavaCallback(name, callback);
	}

	public final void addCallback(SuiEvent event, String name, ISuiCallback callback) {
		addCallback(event, "", name, callback);
	}

	public final void addCallback(SuiEvent event, String source, String script, String function) {
		subscribeToEvent(event.getValue(), source, function);
		addScriptCallback(function, script);
	}

	public final void addCallback(SuiEvent event, String script, String function) {
		addCallback(event, "", script, function);
	}

	public final void addCallback(String source, String script, String function) {
		subscribeToEvent(SuiEvent.OK_PRESSED.getValue(), source, function);
		subscribeToEvent(SuiEvent.CANCEL_PRESSED.getValue(), source, function);
		addScriptCallback(function, script);
	}

	public final void addCallback(String script, String function) {
		addCallback("", script, function);
	}

	public final void addCallback(String source, String name, ISuiCallback callback) {
		subscribeToEvent(SuiEvent.OK_PRESSED.getValue(), source, name);
		subscribeToEvent(SuiEvent.CANCEL_PRESSED.getValue(), source, name);
		addJavaCallback(name, callback);
	}

	public final void addCallback(String name, ISuiCallback callback) {
		addCallback("", name, callback);
	}

	public final SuiComponent getSubscriptionForEvent(int event, String widget) {
		for (SuiComponent component : components) {
			if (component.getType() != SuiComponent.Type.SUBSCRIBE_TO_EVENT)
				continue;

			int eventType = component.getSubscribedToEventType();

			if (eventType == event && component.getTarget().equals(widget))
				return component;
		}
		return null;
	}

	public final SuiComponent getSubscriptionByIndex(int index) {
		int count = 0;
		for (SuiComponent component : components) {
			if (component.getType() == SuiComponent.Type.SUBSCRIBE_TO_EVENT) {
				if (index == count) return component;
				else count++;
			}
		}
		return null;
	}

	public final long getRangeObjId() {
		return rangeObjId;
	}

	public final void setRangeObjId(long rangeObjId) {
		this.rangeObjId = rangeObjId;
	}

	public final int getId() {
		return id;
	}

	public final void setId(int id) {
		this.id = id;
	}

	public final String getSuiScript() {
		return suiScript;
	}

	public final float getMaxDistance() {
		return maxDistance;
	}

	public final void setMaxDistance(float maxDistance) {
		this.maxDistance = maxDistance;
	}

	public final List<SuiComponent> getComponents() {
		return components;
	}

	public final ISuiCallback getJavaCallback(String name) {
		return callbacks != null ? callbacks.get(name) : null;
	}

	public final String getCallbackScript(String function) {
		return scriptCallbacks != null ? scriptCallbacks.get(function) : null;
	}

	public final boolean hasCallbackFunction(String function) {
		return scriptCallbacks != null && scriptCallbacks.containsKey(function);
	}

	public final boolean hasJavaCallback(String name) {
		return callbacks != null && callbacks.containsKey(name);
	}

	public final boolean hasSubscriptionComponent() {
		return hasSubscriptionComponent;
	}

	private void addJavaCallback(String name, ISuiCallback callback) {
		if (callbacks == null) callbacks = new HashMap<>();

		callbacks.put(name, callback);
	}

	private void addScriptCallback(String function, String script) {
		if (scriptCallbacks == null) scriptCallbacks = new HashMap<>();

		scriptCallbacks.put(function, script);
	}

	private String getWrappedEventString(int event) {
		return new String(new byte[]{(byte) event}, StandardCharsets.UTF_8);
	}

	@Override
	public byte[] encode() {
		NetBuffer data = NetBuffer.allocate(getLength());
		data.addInt(id);
		data.addAscii(suiScript);
		data.addList(components);
		data.addLong(rangeObjId);
		data.addFloat(maxDistance);
		data.addLong(0); // Window Location?
		data.addInt(0);
		return data.array();
	}
	
	@Override
	public void decode(NetBuffer data) {
		id = data.getInt();
		suiScript = data.getAscii();
		components = data.getList(SuiComponent.class);
		rangeObjId = data.getLong();
		maxDistance = data.getFloat();
		// unk long
		// unk int
	}
	
	@Override
	public int getLength() {
		int listSize = 0;
		for (SuiComponent component : components) {
			listSize += component.getLength();
		}
		return 34 + suiScript.length() + listSize;
	}
	
}
