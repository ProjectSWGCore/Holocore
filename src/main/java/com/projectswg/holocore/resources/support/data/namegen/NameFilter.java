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

import me.joshlarson.jlcommon.log.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class NameFilter {
	
	private static final Pattern WHITESPACE = Pattern.compile(" +");
	private static final char [] ALLOWED = new char[] {' ', '-', '\''};
	private static final int [] MAX_ALLOWED = new int[] {1 , 1, 1};
	private final List <String> profaneWords;
	private final List <String> reservedWords;
	private final List <String> fictionNames;
	private final InputStream profaneStream;
	private final InputStream reservedStream;
	private final InputStream fictionStream;
	private final AtomicBoolean loaded;
	
	public NameFilter() {
		this.profaneStream = Objects.requireNonNull(getClass().getResourceAsStream("/namegen/filter/bad_word_list.txt"), "profaneStream");
		this.reservedStream = Objects.requireNonNull(getClass().getResourceAsStream("/namegen/filter/reserved_words.txt"), "reservedStream");
		this.fictionStream = Objects.requireNonNull(getClass().getResourceAsStream("/namegen/filter/fiction_reserved.txt"), "fictionStream");
		this.profaneWords = new ArrayList<>();
		this.reservedWords = new ArrayList<>();
		this.fictionNames = new ArrayList<>();
		this.loaded = new AtomicBoolean(false);
	}
	
	public boolean isLoaded() {
		return loaded.get();
	}
	
	public boolean load() {
		if (loaded.getAndSet(true))
			return true;
		boolean success = load(profaneWords, profaneStream);
		success = load(reservedWords, reservedStream) && success;
		success = load(fictionNames, fictionStream) && success;
		return success;
	}
	
	private boolean load(List <String> list, InputStream input) {
		try {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
				String line;
				list.clear();
				while ((line = reader.readLine()) != null) {
					line = line.toLowerCase(Locale.US);
					if (!line.isEmpty() && !list.contains(line))
						list.add(line);
				}
			}
			return true;
		} catch (IOException e) {
			Log.e(e);
			return false;
		}
	}
	
	public boolean isValid(String name) {
		String modified = cleanName(name);
		if (isEmpty(modified)) // Empty name
			return false;
		if (containsBadCharacters(modified)) // Has non-alphabetic characters
			return false;
		if (isProfanity(modified)) // Contains profanity
			return false;
		if (isFictionallyInappropriate(modified))
			return false;
		if (isReserved(modified))
			return false;
		
		return modified.equals(name); // If we needed to remove double spaces, trim the ends, etc
	}
	
	public String cleanName(String name) {
		return WHITESPACE.matcher(name).replaceAll(" ").trim();
	}
	
	public boolean passesFilter(String name) {
		return !isEmpty(name) && !containsBadCharacters(name) && !isProfanity(name);
	}
	
	public boolean isReserved(String name) {
		return contains(reservedWords, name);
	}
	public boolean isFictionallyReserved(String name) {
		return contains(fictionNames, name);
	}
	public boolean isFictionallyInappropriate(String name) {
		boolean space = true;
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (!Character.isAlphabetic(c))
				space = true;
			else if (Character.isUpperCase(c) && !space)
				return true;
			else
				space = false;
		}
		return false;
	}
	
	public boolean isEmpty(String name) {
		return name.length() < 3;
	}
	
	public boolean isProfanity(String name) {
		return contains(profaneWords, name);
	}
	
	public boolean containsBadCharacters(String word) {
		int [] max = new int[ALLOWED.length];
		boolean matched = false;
		for (int i = 0; i < word.length(); i++) {
			char c = word.charAt(i);
			if (!Character.isAlphabetic(c)) {
				for (int a = 0; a < ALLOWED.length; a++) {
					if (ALLOWED[a] == c) {
						if (++max[a] > MAX_ALLOWED[a]) // Increments and checks
							return true; // More than what can be had
						if (matched && c != ' ')
							return true; // Two unallowed characters in a row
						matched = true;
						break;
					}
				}
				if (!matched) // Some other un-allowed character
					return true;
			} else
				matched = false;
		}
		return false;
	}
	
	private boolean contains(List <String> list, String name) {
		name = name.toLowerCase(Locale.US);
		for (String str : list)
			if (name.length() >= str.length() && (name.startsWith(str) || name.endsWith(str)))
				return true;
		String filtered = name.replaceAll("[^a-zA-Z]", "");
		if (!filtered.equals(name))
			for (String w : name.split("[^a-zA-Z]"))
				if (!w.isEmpty() && contains(list, w))
					return true;
			for (String str : list)
				if (filtered.length() >= str.length() && (filtered.startsWith(str) || filtered.endsWith(str)))
					return true;
		return false;
	}
	
}
