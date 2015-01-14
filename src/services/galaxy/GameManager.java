package services.galaxy;

import resources.control.Manager;
import services.chat.ChatService;
import services.commands.CommandService;

public class GameManager extends Manager {
	
	private ChatService chatService;
	private CommandService commandService;
	private ConnectionService connectionService;
	
	public GameManager() {
		chatService = new ChatService();
		commandService = new CommandService();
		connectionService = new ConnectionService();
		
		addChildService(chatService);
		addChildService(commandService);
		addChildService(connectionService);
	}
}
