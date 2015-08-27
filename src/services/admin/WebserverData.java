package services.admin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import resources.player.Player;

class WebserverData {
	
	private static final Charset ASCII = Charset.forName("ASCII");
	
	private final double [] memoryUsage;
	private final Set<Player> onlinePlayers;
	
	public WebserverData() {
		memoryUsage = new double[60 * 5];
		onlinePlayers = new HashSet<>();
	}
	
	public void addMemoryUsageData(double percent) {
		for (int i = 1; i < memoryUsage.length; i++)
			memoryUsage[i-1] = memoryUsage[i];
		memoryUsage[memoryUsage.length-1] = percent;
	}
	
	public void addOnlinePlayer(Player player) {
		onlinePlayers.add(player);
	}
	
	public void removeOnlinePlayer(Player player) {
		onlinePlayers.remove(player);
	}
	
	public double [] getMemoryUsage() {
		return Arrays.copyOf(memoryUsage, memoryUsage.length);
	}
	
	public Set<Player> getOnlinePlayers() {
		return Collections.unmodifiableSet(onlinePlayers);
	}
	
	public String getLog() throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File("log.txt")), ASCII));
		StringBuilder builder = new StringBuilder();
		while (reader.ready()) {
			builder.append(reader.readLine() + System.lineSeparator());
		}
		reader.close();
		return builder.toString();
	}
	
}
