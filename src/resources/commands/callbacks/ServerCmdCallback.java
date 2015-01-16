package resources.commands.callbacks;

import java.util.List;

import resources.commands.ICmdCallback;
import resources.objects.SWGObject;
import resources.player.Player;
import resources.sui.ISuiCallback;
import resources.sui.SuiListBox;
import resources.sui.SuiListBox.ListBoxType;
import resources.sui.SuiWindow.Trigger;
import services.objects.ObjectManager;

public class ServerCmdCallback implements ICmdCallback {

	@Override
	public void execute(ObjectManager objManager, Player player, SWGObject target, String args) {
		SuiListBox listBox = new SuiListBox(player, ListBoxType.OK_CANCEL, "Server Management", "Select the management function you wish to perform from the list.");
		
		listBox.addListItem("Shutdown Server", 0);
		listBox.addListItem("Kick Player", 1);
		listBox.addListItem("Ban Player", 2);
		
		listBox.addCallback(0, "", Trigger.OK, listBox.getSelectedRowReturnList(), new ServerSuiCallback());
		
		listBox.display();
	}

	private class ServerSuiCallback implements ISuiCallback {

		@Override
		public void handleEvent(SWGObject actor, int eventType, List<String> returnParams) {
			
		}
		
	}
}
