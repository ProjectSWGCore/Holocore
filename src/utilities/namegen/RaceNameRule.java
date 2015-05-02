package utilities.namegen;

import java.util.ArrayList;
import java.util.List;

public class RaceNameRule {
	private List<String> vowels = new ArrayList<>();
	private List<String> startConsonants = new ArrayList<>();
	private List<String> endConsonants = new ArrayList<>();
	private List<String> instructions = new ArrayList<>();
	private int surnameChance = 0;
	
	public RaceNameRule() { }
	
	public void addVowel(String s) {
		vowels.add(s);
	}
	
	public void addStartConsonant(String s) {
		startConsonants.add(s);
	}
	
	public void addEndConsant(String s) {
		endConsonants.add(s);
	}
	
	public void addInstruction(String s) {
		instructions.add(s);
	}
	
	public void setSurnameChance(int chance) {
		this.surnameChance = chance;
	}
	
	public int getSurnameChance() {
		return surnameChance;
	}
	
	public List<String> getVowels() {
		return vowels;
	}
	
	public List<String> getStartConsonants() {
		return startConsonants;
	}

	public List<String> getEndConsonants() {
		return endConsonants;
	}

	public List<String> getInstructions() {
		return instructions;
	}
	
}
