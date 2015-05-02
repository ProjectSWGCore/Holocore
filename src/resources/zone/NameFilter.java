package resources.zone;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NameFilter {
	
	private static final char [] ALLOWED = new char[] {' ', '-', '\''};
	private static final int [] MAX_ALLOWED = new int[] {1 , 1, 1};
	private final List <String> profaneWords;
	private final List <String> reservedWords;
	private final File profaneFile;
	private final File reservedFile;
	
	public NameFilter(String badWordsPath, String reservedPath) {
		this(new File(badWordsPath), new File(reservedPath));
	}
	
	public NameFilter(File badWordsFile, File reservedFile) {
		this.profaneFile = badWordsFile;
		this.reservedFile = reservedFile;
		this.profaneWords = new ArrayList<String>();
		this.reservedWords = new ArrayList<String>();
	}
	
	public boolean load() {
		boolean success = true;
		success = load(profaneWords, profaneFile) && success;
		success = load(reservedWords, reservedFile) && success;
		return success;
	}
	
	private boolean load(List <String> list, File file) {
		if (!file.exists())
			return false;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = "";
			list.clear();
			while ((line = reader.readLine()) != null) {
				if (!line.isEmpty() && !list.contains(line.toLowerCase()))
					list.add(line.toLowerCase());
			}
			reader.close();
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
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
		if (!modified.equals(name)) // If we needed to remove double spaces, trim the ends, etc
			return false;
		return true;
	}
	
	public String cleanName(String name) {
		while (name.contains("  "))
			name = name.replaceAll("  ", " ");
		return name.trim();
	}
	
	public boolean passesFilter(String name) {
		return !isEmpty(name) && !containsBadCharacters(name) && !isProfanity(name);
	}
	
	public boolean isReserved(String name) {
		return contains(reservedWords, name);
	}
	
	public boolean isFictionallyInappropriate(String name) {
		boolean space = true;
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (!Character.isAlphabetic(c))
				space = true;
			else if (Character.isUpperCase(c) && space == false)
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
		name = name.toLowerCase();
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
