/***********************************************************************************
 * Copyright (c) 2019 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/

package com.projectswg.holocore.resources.support.data.namegen;

import com.projectswg.common.data.encodables.tangible.Race;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class SWGNameGenerator {
	
	private final Map<String, Reference<RaceNameRule>> rules;
	private final NameFilter nameFilter;
	
	/**
	 * Creates a new instance of {@link SWGNameGenerator} with all racial naming rules loaded.
	 */
	public SWGNameGenerator() {
		this.rules = new HashMap<>();
		this.nameFilter = new NameFilter();
	}
	
	@NotNull
	public String generateName(Race race) {
		if (!nameFilter.isLoaded())
			nameFilter.load();
		String species = race.getSpecies();
		species = species.substring(0, species.indexOf('_'));
		return generateName("race_" + species);
	}
	
	@NotNull
	public String generateName(String file) {
		if (!nameFilter.isLoaded())
			nameFilter.load();
		Reference<RaceNameRule> ruleRef = rules.get(file);
		RaceNameRule rule = (ruleRef == null) ? null : ruleRef.get();
		if (rule == null)
			rule = loadNamingRule(file);
		rules.replace(file, ruleRef, new SoftReference<>(rule));
		
		return generateFilteredName(rule, true);
	}
	
	/**
	 * Generates a random name for the defined rule.
	 *
	 * @param rule    The rule to generate from
	 * @param surname Determines if a surname should be generated or not.
	 * @return Generated name in the form of a {@link String}, as well as the surname dependent on chance if true
	 */
	@NotNull
	private String generateFilteredName(@NotNull RaceNameRule rule, boolean surname) {
		StringBuilder name = new StringBuilder();
		do {
			name.setLength(0);
			do {
				name.append(generateName(rule));
			} while (name.length() <= 0); // Some reason a name can be empty, I think it has to do with the removeExcessDuplications check.
			
			if (surname && shouldGenerateSurname(rule))
				name.append(' ').append(generateFilteredName(rule, false));
			
			name.setCharAt(0, Character.toUpperCase(name.charAt(0)));
		} while (!nameFilter.isValid(name.toString()));
		
		return name.toString();
	}
	
	private String generateName(RaceNameRule rule) {
		StringBuilder buffer = new StringBuilder();
		String instructions = getRandomInstruction(rule);
		int l = instructions.length();
		
		for (int i = 0; i < l; i++) {
			char x = instructions.charAt(0);
			String append;
			
			switch (x) {
				case 'v':
					append = removeExcessDuplications(rule.getVowels(), buffer.toString(), getRandomElementFrom(rule.getVowels()));
					break;
				case 'c':
					append = removeExcessDuplications(rule.getStartConsonants(), buffer.toString(), getRandomElementFrom(rule.getStartConsonants()));
					break;
				case 'd':
					append = removeExcessDuplications(rule.getEndConsonants(), buffer.toString(), getRandomElementFrom(rule.getEndConsonants()));
					break;
				case '/':
					append = "\'";
					break;
				default:
					append = "";
					break;
			}
			if (buffer.length() + append.length() >= rule.getMaxLength())
				break;
			buffer.append(append);
			
			instructions = instructions.substring(1);
		}
		if (buffer.length() == 0)
			return generateName(rule);
		return buffer.toString();
	}
	
	private String getRandomInstruction(RaceNameRule rule) {
		return getRandomElementFrom(rule.getInstructions());
	}
	
	private RaceNameRule loadNamingRule(String file) {
		try (InputStream stream = getClass().getResourceAsStream("/namegen/" + file + ".txt")) {
			if (stream == null)
				throw new FileNotFoundException("/namegen/" + file + ".txt");
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
			RaceNameRule rule = new RaceNameRule();
			
			populateRule(rule, reader);
			
			return rule;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private boolean populateRule(RaceNameRule rule, BufferedReader reader) throws IOException {
		String populating = "NONE";
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.isEmpty())
				continue;
			
			if (line.startsWith("[")) {
				populating = getPopulateType(line);
				
				if (populating.equals("End"))
					return true;
				
			} else if (!line.startsWith("#")) {
				switch (populating) {
					case "Settings":
						populateSettings(rule, line);
						break;
					case "Vowels":
						rule.addVowel(line);
						break;
					case "StartConsonants":
						rule.addStartConsonant(line);
						break;
					case "EndConsonants":
						rule.addEndConsant(line);
						break;
					case "Instructions":
						rule.addInstruction(line);
						break;
					default:
						break;
				}
			}
		}
		reader.close();
		
		return false;
	}
	
	private String getPopulateType(String line) {
		switch (line) {
			case "[Settings]":
				return "Settings";
			case "[Vowels]":
				return "Vowels";
			case "[StartConsonants]":
				return "StartConsonants";
			case "[EndConsonants]":
				return "EndConsonants";
			case "[Instructions]":
				return "Instructions";
			case "[END]":
				return "End";
			default:
				return "End";
		}
	}
	
	private void populateSettings(RaceNameRule rule, String line) {
		String key = line.split("=")[0];
		String value = line.split("=")[1];
		
		if ("SurnameChance".equals(key)) {
			rule.setSurnameChance(Integer.parseInt(value));
		}
		if ("MaxLength".equals(key)) {
			rule.setMaxLength(Integer.parseInt(value));
		}
	}
	
	private static String removeExcessDuplications(List<String> list, String orig, String n) {
		// Only checks the first and last for repeating
		if (orig.length() <= 1)
			return n;
		
		if (orig.charAt(orig.length() - 1) == n.charAt(0)) {
			if (!list.contains(Character.toString(orig.charAt(orig.length() - 1)) + n.charAt(0))) {
				return removeExcessDuplications(list, orig, getRandomElementFrom(list));
			}
		}
		return n;
	}
	
	private static String getRandomElementFrom(List<String> list) {
		return list.get(ThreadLocalRandom.current().nextInt(0, list.size()-1));
	}
	
	private static boolean shouldGenerateSurname(RaceNameRule rule) {
		return rule.getSurnameChance() != 0 && (ThreadLocalRandom.current().nextInt(0, 100) <= rule.getSurnameChance());
	}
	
}
