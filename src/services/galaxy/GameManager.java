package services.galaxy;

import resources.control.Manager;
import services.chat.ChatService;
import services.commands.CommandService;
import services.sui.SuiService;

public class GameManager extends Manager {
	
	private ChatService chatService;
	private CommandService commandService;
	private ConnectionService connectionService;
	private SuiService suiService;
	
	public GameManager() {
		chatService = new ChatService();
		commandService = new CommandService();
		connectionService = new ConnectionService();
		suiService = new SuiService();
		
		addChildService(chatService);
		addChildService(commandService);
		addChildService(connectionService);
		addChildService(suiService);
	}
}
