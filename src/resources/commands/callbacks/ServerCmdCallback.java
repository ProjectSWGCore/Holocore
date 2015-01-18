package resources.commands.callbacks;

import java.util.List;

import resources.commands.ICmdCallback;
import resources.objects.SWGObject;
import resources.player.Player;
import resources.sui.ISuiCallback;
import resources.sui.SuiInputBox;
import resources.sui.SuiInputBox.InputBoxType;
import resources.sui.SuiListBox;
import resources.sui.SuiListBox.ListBoxType;
import services.objects.ObjectManager;

public class ServerCmdCallback implements ICmdCallback {

	@Override
	public void execute(ObjectManager objManager, Player player, SWGObject target, String args) {
		SuiListBox listBox = new SuiListBox(player, ListBoxType.OK_CANCEL, "Server Management", "Select the management function you wish to perform from the list.");
		
		listBox.addListItem("Shutdown Server", 0);
		listBox.addListItem("Kick Player", 1);
		listBox.addListItem("Ban Player", 2);
		
		listBox.addItemSelectionCallback(0, new ServerSuiCallback());
		
		listBox.display();
	}

	private class ServerSuiCallback implements ISuiCallback {

		@Override
		public void handleEvent(Player player, SWGObject actor, int eventType, List<String> returnParams) {
			int selection = SuiListBox.getSelectedIndex(returnParams);
			
			switch(selection) {
			case 0: handleShutdownServer(); break;
			case 1: handleKickPlayer(player); break;
			case 2: handleBanPlayer(player); break;
			default: break;
			}
		}
		
		private void handleShutdownServer() {
			// TODO: initiate server shutdown
		}
		
		private void handleKickPlayer(Player actor) {
			SuiInputBox window = new SuiInputBox(actor, InputBoxType.OK_CANCEL, "Kick Player", "Enter the name of the player that you wish to KICK from the server.");
			window.addInputTextCallback(0, new ISuiCallback() {
				@Override
				public void handleEvent(Player player, SWGObject actor, int eventType, List<String> returnParams) {
					//String name = returnParams.get(0); // input
					// TODO: Kick the player that has name entered
				}
				
			});
			window.display();
		}
		
		private void handleBanPlayer(Player actor) {
			SuiInputBox window = new SuiInputBox(actor, InputBoxType.OK_CANCEL, "Ban Player", "Enter the name of the player that you wish to BAN from the server.");
			window.addInputTextCallback(0, new ISuiCallback() {
				@Override
				public void handleEvent(Player player, SWGObject actor, int eventType, List<String> returnParams) {
					//String name = returnParams.get(0); // input
					// TODO: Ban the player that has name entered
				}
			});
			window.display();
		}
	}
}
