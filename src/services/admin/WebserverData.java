package services.admin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import resources.player.Player;

class WebserverData {
	
	private static final Charset ASCII = Charset.forName("ASCII");
	
	private final double [] memoryUsage;
	private final double [] cpuUsage;
	private final double [] systemMemoryUsage;
	private final double [] systemCpuUsage;
	private final Set<Player> onlinePlayers;
	
	public WebserverData() {
		memoryUsage = new double[60 * 5];
		cpuUsage = new double[memoryUsage.length];
		systemMemoryUsage = new double[memoryUsage.length];
		systemCpuUsage = new double[memoryUsage.length];
		onlinePlayers = new HashSet<>();
	}
	
	@SuppressWarnings("restriction")
	public void updateResourceUsage() {
		OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
		if (os instanceof com.sun.management.OperatingSystemMXBean) {
			com.sun.management.OperatingSystemMXBean castedOS = (com.sun.management.OperatingSystemMXBean) os;
			updateMemory(castedOS);
			updateCPU(castedOS);
		}
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
	
	public double [] getCpuUsage() {
		return Arrays.copyOf(cpuUsage, cpuUsage.length);
	}

	public double [] getSystemMemoryUsage() {
		return Arrays.copyOf(systemMemoryUsage, systemMemoryUsage.length);
	}
	
	public double [] getSystemCpuUsage() {
		return Arrays.copyOf(systemCpuUsage, systemCpuUsage.length);
	}
	
	public Set<Player> getOnlinePlayers() {
		return Collections.unmodifiableSet(onlinePlayers);
	}
	
	public String getLog() throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File("log.txt")), ASCII))) {
			StringBuilder builder = new StringBuilder();
			while (reader.ready()) {
				builder.append(reader.readLine() + System.lineSeparator());
			}
			reader.close();
			return builder.toString();
		}
	}
	
	@SuppressWarnings("restriction")
	private void updateMemory(com.sun.management.OperatingSystemMXBean os) {
		Runtime r = Runtime.getRuntime();
		appendData(memoryUsage, (double) r.totalMemory() / os.getTotalPhysicalMemorySize());
		appendData(systemMemoryUsage, 1 - ((double) os.getFreePhysicalMemorySize() / os.getTotalPhysicalMemorySize()));
	}
	
	@SuppressWarnings("restriction")
	private void updateCPU(com.sun.management.OperatingSystemMXBean os) {
		appendData(cpuUsage, os.getProcessCpuLoad());
		appendData(systemCpuUsage, os.getSystemCpuLoad());
	}
	
	private void appendData(double [] data, double value) {
		for (int i = 1; i < data.length; i++)
			data[i-1] = data[i];
		data[data.length-1] = value;
	}
	
}
