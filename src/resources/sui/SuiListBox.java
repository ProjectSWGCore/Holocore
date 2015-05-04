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

import java.util.ArrayList;
import java.util.List;

import resources.player.Player;

public class SuiListBox extends SuiBaseWindow {
	private List <SuiListBoxItem> list;
	
	public SuiListBox(Player owner, ListBoxType type, String title, String prompt) {
		super("Script.listBox", owner, title, prompt);
		list = new ArrayList<SuiListBoxItem>();
		switch(type) {
		case OK:
			setProperty("btnOk:visible", "True");
			setProperty("btnOk:Text", "@ok");
			setProperty("btnCancel:visible", "False");
			break;
		case OK_CANCEL:
			setProperty("btnOk:visible", "True");
			setProperty("btnCancel:visible", "True");
			setProperty("btnOk:Text", "@ok");
			setProperty("btnCancel:Text", "@cancel");
			break;
		default: break;
		}
		clearDataSource("List.dataList");
	}

	public SuiListBox(Player owner, String title, String prompt) {
		this(owner, ListBoxType.OK_CANCEL, title, prompt);
	}
	
	public void addListItem(String name, long id) {
		SuiListBoxItem item = new SuiListBoxItem(name, id);

		int index = list.size();
		
		addDataItem("List.dataList:Name", String.valueOf(index));
		setProperty("List.dataList." + index + ":Text", name);
		
		list.add(item);
	}

	public void addListItem(String name) {
		addListItem(name, 0);
	}
	
	public void addItemSelectionCallback(int eventId, ISuiCallback callback) {
		List<String> returnList = new ArrayList<String>();
		returnList.add("List.lstList:SelectedRow");
		addCallback(eventId, "", Trigger.OK, returnList, callback);
	}
	
	public long getListItemId(int index) {
		SuiListBoxItem item = list.get(index);
		
		if (item == null) return 0;
		else return item.getId();
	}
	
	public SuiListBoxItem getListItem(int index) {
		SuiListBoxItem item = list.get(index);
		return item;
	}
	
	public static int getSelectedIndex(List<String> returnParams) { return Integer.parseInt(returnParams.get(0)); }
	
	public List<SuiListBoxItem> getList() { return list; }

	public enum ListBoxType {
		OK,
		OK_CANCEL,
		DEFAULT;
	}
	
	public static class SuiListBoxItem {
		private String name;
		private long id;
		
		public SuiListBoxItem(String name, long id) {
			this.name = name;
			this.id = id;
		}
		
		public String getName() { return this.name; }
		public long getId() { return this.id; }
	}
}
