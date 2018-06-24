package com.projectswg.holocore.services.support.global.health;

import com.projectswg.common.data.info.Config;
import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.holocore.resources.support.data.config.ConfigFile;
import com.projectswg.holocore.resources.support.data.server_info.BasicLogStream;
import com.projectswg.holocore.resources.support.data.server_info.DataManager;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.IntentManager;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.TimeUnit;

public class ServerHealthService extends Service {
	
	private static final String [] BINARY_SUFFIXES = new String[]{"B", "kB", "MB", "GB", "TB"};
	
	private final ScheduledThreadPool executor;
	private final BasicLogStream performanceOutput;
	
	public ServerHealthService() {
		this.executor = new ScheduledThreadPool(1, 3, "server-health-service");
		Config debugConfig = DataManager.getConfig(ConfigFile.DEBUG);
		if (debugConfig.getBoolean("PERFORMANCE-LOG", false)) {
			this.performanceOutput = new BasicLogStream(new File("log/performance.txt"));
			executor.start();
			performanceOutput.log("%s\t%s\t%s\t%s", "cpu", "memory-used", "memory-max", "intents");
			executor.executeWithFixedRate(0, 1000, this::updatePerformanceLog);
		} else {
			this.performanceOutput = null;
		}
	}
	
	@Override
	public boolean start() {
		if (!executor.isRunning())
			executor.start();
		executor.executeWithFixedRate(0, TimeUnit.MINUTES.toMillis(10), this::updateServerHealth);
		return true;
	}
	
	@Override
	public boolean terminate() {
		executor.stop();
		return executor.awaitTermination(1000);
	}
	
	private void updatePerformanceLog() {
		OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
		MemoryUsage heapUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
		
		double cpu = (os instanceof com.sun.management.OperatingSystemMXBean) ? ((com.sun.management.OperatingSystemMXBean) os).getProcessCpuLoad()*100 : -1;
		
		long heapUsed = heapUsage.getUsed();
		long heapTotal = heapUsage.getCommitted();
		IntentManager intentManager = IntentManager.getInstance();
		long intents = intentManager == null ? -1 : intentManager.getIntentCount();
		
		performanceOutput.log("%.2f\t%.2f%s\t%.2f%s\t%d", cpu, getBinarySize(heapUsed), getBinarySuffix(heapUsed), getBinarySize(heapTotal), getBinarySuffix(heapTotal), intents);
	}
	
	private void updateServerHealth() {
		Runtime rt = Runtime.getRuntime();
		long total = rt.totalMemory();
		long usedBefore = total - rt.freeMemory();
		ClientFactory.freeMemory();
		DataLoader.freeMemory();
		System.gc();
		long usedAfter = total - rt.freeMemory();
		Log.d("Memory cleanup. Total: %.1fGB  Before: %.2f%%  After: %.2f%%", total/1073741824.0, usedBefore/(double)total*100, usedAfter/(double)total*100);
	}
	
	private static double getBinarySize(long count) {
		double countDecimal = count;
		for (String ignored : BINARY_SUFFIXES) {
			if (countDecimal < 1024)
				return countDecimal;
			countDecimal /= 1024;
		}
		return countDecimal;
	}
	
	private static String getBinarySuffix(long count) {
		double countDecimal = count;
		for (String BINARY_SUFFIX : BINARY_SUFFIXES) {
			if (countDecimal < 1024)
				return BINARY_SUFFIX;
			countDecimal /= 1024;
		}
		return BINARY_SUFFIXES[BINARY_SUFFIXES.length-1];
	}
	
}
