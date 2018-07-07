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
package com.projectswg.holocore.resources.support.global.zone.sui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SuiListBox extends SuiWindow {
	private List <SuiListBoxItem> list;
	
	public SuiListBox(SuiButtons buttons, String title, String prompt) {
		super("Script.listBox", buttons, title, prompt);
		clearDataSource("List.dataList");

		list = new ArrayList<>();
	}

	public SuiListBox(String title, String prompt) {
		this(SuiButtons.OK_CANCEL, title, prompt);
	}

	public static int getSelectedRow(Map<String, String> parameters) {
		String selectedIndex = parameters.get("List.lstList.SelectedRow");
		if (selectedIndex != null) return Integer.parseInt(selectedIndex);
		return -1;
	}

	@Override
	protected void setButtons(SuiButtons buttons) {
		switch(buttons) {
			case OK_CANCEL:
				setOkButtonText("@ok");
				setCancelButtonText("@cancel");
				break;
			case YES_NO:
				setOkButtonText("@yes");
				setCancelButtonText("@no");
				break;
			case OK_REFRESH:
				setOkButtonText("@ok");
				setCancelButtonText("@refresh");
				break;
			case OK_CANCEL_REFRESH:
				setOkButtonText("@ok");
				setCancelButtonText("@cancel");
				setShowOtherButton(true, "@refresh");
				break;
			case OK_CANCEL_ALL:
				setOkButtonText("@ok");
				setCancelButtonText("@cancel");
				setShowOtherButton(true, "@all");
				break;
			case REFRESH:
				setOkButtonText("@refresh");
				setShowCancelButton(false);
				break;
			case REFRESH_CANCEL:
				setOkButtonText("@refresh");
				setCancelButtonText("@cancel");
				break;
			case REMOVE_CANCEL:
				setOkButtonText("@remove");
				setCancelButtonText("@cancel");
				break;
			case MOVEUP_MOVEDOWN_DONE:
				setOkButtonText("@moveup");
				setCancelButtonText("@done");
				setShowOtherButton(true, "@movedown");
				break;
			case BET_MAX_BET_ONE_SPIN:
				setOkButtonText("@ok");
				setCancelButtonText("@spin");
				setShowOtherButton(true, "@bet_one");
				break;
			case OK:
			case DEFAULT:
			default:
				setOkButtonText("@ok");
				setShowCancelButton(false);
				break;
		}
	}

	@Override
	protected void onDisplayRequest() {
		addReturnableProperty("List.lstList", "SelectedRow");
		addReturnableProperty("bg.caption.lblTitle", "Text");
	}

	public void addListItem(String name, long id, Object object) {
		SuiListBoxItem item = new SuiListBoxItem(name, id, object);

		int index = list.size();
		String sIndex = String.valueOf(index);
		addDataItem("List.dataList", "Name", sIndex);
		setProperty("List.dataList." + sIndex, "Text", name);

		list.add(item);
	}

	public void addListItem(String name) {
		addListItem(name, -1, null);
	}

	public void addListItem(String name, Object object) {
		addListItem(name, -1, object);
	}
	
	public SuiListBoxItem getListItem(int index) {
		return list.get(index);
	}

	public List<SuiListBoxItem> getList() { return list; }

	public static class SuiListBoxItem {
		private Object object;
		private String name;
		private long id;
		
		public SuiListBoxItem(String name, long id, Object object) {
			this.name = name;
			this.id = id;
			this.object = object;
		}
		
		public String getName() { return this.name; }
		public long getId() { return this.id; }
		public Object getObject() { return this.object; }
	}
}
