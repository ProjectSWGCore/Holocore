package resources.sui;

import java.util.ArrayList;
import java.util.List;

import resources.player.Player;

public class SuiListBox extends SuiWindow {
	private List<SuiListBoxItem> list;
	
	public SuiListBox(Player owner, ListBoxType type, String title, String prompt) {
		super("Script.listBox", owner);
		list = new ArrayList<SuiListBoxItem>();
		setProperty("bg.caption.lblTitle:Text", title);
		setProperty("Prompt.lblPrompt:Text", prompt);
		
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

	public long getListItemId(int index) {
		SuiListBoxItem item = list.get(index);
		
		if (item == null) return 0;
		else return item.getId();
	}
	
	public List<String> getSelectedRowReturnList() { 
		List<String> returnList = new ArrayList<String>();
		returnList.add("List.lstList:SelectedRow");
		return returnList;
	}
	
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
