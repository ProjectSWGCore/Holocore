package com.projectswg.holocore.services.gameplay.training;

class Profession {
	private final String name;
	
	Profession(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public String getNoviceSkillKey() {
		return name + "_novice";
	}

}
