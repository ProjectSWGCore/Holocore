package com.projectswg.holocore.services.gameplay.player.experience;

public class Experience {
	private final String xpType;
	private final int amount;
	
	public Experience(String xpType, int amount) {
		this.xpType = xpType;
		this.amount = amount;
	}
	
	public String getXpType() {
		return xpType;
	}
	
	public int getAmount() {
		return amount;
	}
}
