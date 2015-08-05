package services.galaxy.terminals;

import resources.control.Manager;

public class TerminalManager extends Manager {
	
	private final BankingService bankingService;
	
	public TerminalManager() {
		bankingService = new BankingService();
		
		addChildService(bankingService);
	}
	
}
