package resources.sui;

import java.util.ArrayList;
import java.util.List;

import resources.player.Player;

public class SuiListBox extends SuiBaseWindow {
	private List<SuiListBoxItem> list;
	
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
	
	public static int getSelectedIndex(List<String> returnParams) { return Integer.valueOf(returnParams.get(0)); }
	
	public List<SuiListBoxItem> getList() { return list; }

	public enum ListBoxType {
		OK,
		OK_CANCEL,
		DEFAULT;
	}
	
	public class SuiListBoxItem {
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
