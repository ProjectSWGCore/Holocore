package services.galaxy;

import resources.control.Manager;
import services.chat.ChatService;
import services.commands.CommandService;

public class GameManager extends Manager {
	
	private ChatService chatService;
	private CommandService commandService;
	
	public GameManager() {
		chatService = new ChatService();
		commandService = new CommandService();
		
		addChildService(chatService);
		addChildService(commandService);
	}
}
