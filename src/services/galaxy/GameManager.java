package services.galaxy;

import resources.control.Manager;
import services.chat.ChatService;

public class GameManager extends Manager {
	
	private ChatService chatService;
	
	public GameManager() {
		chatService = new ChatService();
		
		addChildService(chatService);
	}
	
}
