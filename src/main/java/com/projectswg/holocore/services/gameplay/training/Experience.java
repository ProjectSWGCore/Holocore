package com.projectswg.holocore.services.gameplay.training;

class Experience {
	private final int amount;
	private final String type;
	
	Experience(int amount, String type) {
		this.amount = amount;
		this.type = type;
	}
	
	public int getPoints() {
		return amount;
	}
	
	public String getType() {
		return type;
	}
	
	@Override
	public String toString() {
		return "Experience{" + "amount=" + amount + ", type='" + type + '\'' + '}';
	}
}
