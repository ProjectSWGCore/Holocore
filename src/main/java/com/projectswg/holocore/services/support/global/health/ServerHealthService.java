package com.projectswg.holocore.services.support.global.health;

import com.projectswg.holocore.resources.support.data.server_info.BasicLogStream;
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.IntentManager;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.io.File;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ServerHealthService extends Service {
	
	private static final String [] BINARY_SUFFIXES = new String[]{"B", "kB", "MB", "GB", "TB"};
	
	private final ScheduledThreadPool executor;
	private final BasicLogStream performanceOutput;
	private final AtomicLong previousGcCollection;
	private final AtomicLong previousGcTime;
	private final AtomicBoolean completedInitialIntents;
	
	public ServerHealthService() {
		this.executor = new ScheduledThreadPool(1, 3, "server-health-service");
		this.previousGcCollection = new AtomicLong(0);
		this.previousGcTime = new AtomicLong(0);
		this.completedInitialIntents = new AtomicBoolean(true);
		
		if (PswgDatabase.INSTANCE.getConfig().getBoolean(this, "performanceLog", false)) {
			this.performanceOutput = new BasicLogStream(new File("log/performance.txt"));
			executor.start();
			performanceOutput.log("%s\t%s\t%s\t%s\t%s\t%s", "cpu", "memory-used", "memory-max", "gc-collectionRate", "gc-time", "intents");
			executor.executeWithFixedRate(0, 1000, this::updatePerformanceLog);
		} else {
			this.performanceOutput = null;
		}
	}
	
	@Override
	public boolean start() {
		completedInitialIntents.set(false);
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
		List<GarbageCollectorMXBean> gc = ManagementFactory.getGarbageCollectorMXBeans();
		
		double cpu = (os instanceof com.sun.management.OperatingSystemMXBean) ? ((com.sun.management.OperatingSystemMXBean) os).getProcessCpuLoad()*100 : -1;
		
		long heapUsed = heapUsage.getUsed();
		long heapTotal = heapUsage.getCommitted();
		long gcCollectionRate, gcTime;
		{
			long collected = gc.stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();
			long collectionTime = gc.stream().mapToLong(GarbageCollectorMXBean::getCollectionTime).sum();
			long previousCollected = previousGcCollection.getAndSet(collected);
			long previousTime = previousGcTime.getAndSet(collectionTime);
			gcCollectionRate = collected - previousCollected;
			gcTime = collectionTime - previousTime;
		}
		
		IntentManager intentManager = IntentManager.getInstance();
		long intents = intentManager == null ? -1 : intentManager.getIntentCount();
		if (intents == 0 && !completedInitialIntents.getAndSet(true)) {
			Log.i("Completed initial intents");
		}
		
		performanceOutput.log("%.2f\t%.2f%s\t%.2f%s\t%d\t%d\t%d", cpu, getBinarySize(heapUsed), getBinarySuffix(heapUsed), getBinarySize(heapTotal), getBinarySuffix(heapTotal), gcCollectionRate, gcTime, intents);
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
