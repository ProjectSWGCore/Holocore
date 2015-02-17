package resources.commands.callbacks;

import intents.ServerManagementIntent;
import intents.ServerManagementIntent.ServerManagementEvent;

import java.util.List;

import resources.commands.ICmdCallback;
import resources.objects.SWGObject;
import resources.player.Player;
import resources.sui.ISuiCallback;
import resources.sui.SuiInputBox;
import resources.sui.SuiInputBox.InputBoxType;
import resources.sui.SuiListBox;
import resources.sui.SuiListBox.ListBoxType;
import resources.sui.SuiMessageBox;
import resources.sui.SuiMessageBox.MessageBoxType;
import services.objects.ObjectManager;

public class ServerCmdCallback implements ICmdCallback {

	@Override
	public void execute(ObjectManager objManager, Player player, SWGObject target, String args) {
		SuiListBox listBox = new SuiListBox(player, ListBoxType.OK_CANCEL, "Server Management", "Select the management function you wish to perform from the list.");
		
		listBox.addListItem("Kick Player", 0);
		listBox.addListItem("Ban Player", 1);
		listBox.addListItem("Unban Player", 2);
		listBox.addListItem("Shutdown Server - 15 Minutes", 3);
		listBox.addListItem("Shutdown Server - Custom Time");
		
		listBox.addItemSelectionCallback(0, new ServerSuiCallback());
		listBox.display();
	}
	
	private static class ServerSuiCallback implements ISuiCallback {
		
		public void handleEvent(Player player, SWGObject actor, int eventType, List<String> returnParams) {
			int selection = SuiListBox.getSelectedIndex(returnParams);
			
			switch(selection) {
				case 0: handleKickPlayer(player); break;
				case 1: handleBanPlayer(player); break;
				case 2: handleUnbanPlayer(player); break;
				case 3: handleShutdownServer(player); break;
				case 4: handleCustomShutdownServer(player); break;
				default: break;
			}
		}
		
		private void handleKickPlayer(Player actor) {
			SuiInputBox window = new SuiInputBox(actor, InputBoxType.OK_CANCEL, "Kick Player", "Enter the name of the player that you wish to KICK from the server.");
			window.addInputTextCallback(0, new ISuiCallback() {
				public void handleEvent(Player player, SWGObject actor, int eventType, List<String> returnParams) {
					String name = returnParams.get(0); // input
					new ServerManagementIntent(player, name, ServerManagementEvent.KICK).broadcast();
				}
			});
			
			window.display();
		}
		
		private void handleBanPlayer(Player actor) {
			SuiInputBox window = new SuiInputBox(actor, InputBoxType.OK_CANCEL, "Ban Player", "Enter the name of the player that you wish to BAN from the server.");
			window.addInputTextCallback(0, new ISuiCallback() {
				public void handleEvent(Player player, SWGObject actor, int eventType, List<String> returnParams) {
					String name = returnParams.get(0); // input
					new ServerManagementIntent(player, name, ServerManagementEvent.BAN).broadcast();
				}
			});
			
			window.display();
		}
		
		private void handleUnbanPlayer(Player actor) {
			SuiInputBox window = new SuiInputBox(actor, InputBoxType.OK_CANCEL, "Unban Player", "Enter the name of the player that you wish to UNBAN from the server.");
			window.addInputTextCallback(0, new ISuiCallback() {
				public void handleEvent(Player player, SWGObject actor, int eventType, List<String> returnParams) {
					String name = returnParams.get(0);
					new ServerManagementIntent(player, name, ServerManagementEvent.UNBAN).broadcast();
				}
			});
			
			window.display();
		}
		
		private void handleShutdownServer(Player actor) {
			SuiMessageBox window = new SuiMessageBox(actor, MessageBoxType.YES_NO, "Shutdown Server", "Are you sure you wish to begin the shutdown sequence?");
			window.addOkButtonCallback(0, new ISuiCallback() {
				public void handleEvent(Player player, SWGObject actor, int eventType, List<String> returnParams) {
					new ServerManagementIntent(15, ServerManagementEvent.SHUTDOWN).broadcast();
				}
			});
			
			window.display();
		}
		
		private void handleCustomShutdownServer(Player actor) {
			SuiInputBox window = new SuiInputBox(actor, InputBoxType.OK_CANCEL, "Shutdown Server", "Enter the time until the server shuts down. The shutdown sequence "
					+ "will begin upon hitting OK.");
			window.allowStringCharacters(false);

			window.addInputTextCallback(0, new ISuiCallback() {
				public void handleEvent(Player player, SWGObject actor, int eventType, List<String> returnParams) {
					long countdown = Long.parseLong(returnParams.get(0));
					new ServerManagementIntent(countdown, ServerManagementEvent.SHUTDOWN).broadcast();
				}
				
			});
			
			window.display();
		}
	}
}
