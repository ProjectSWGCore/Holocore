package com.projectswg.holocore.services.support.global.health;

import com.projectswg.common.data.swgfile.ClientFactory;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.util.concurrent.TimeUnit;

public class ServerHealthService extends Service {
	
	private final ScheduledThreadPool executor;
	
	public ServerHealthService() {
		this.executor = new ScheduledThreadPool(1, 3, "server-health-service");
	}
	
	@Override
	public boolean start() {
		executor.start();
		executor.executeWithFixedRate(0, TimeUnit.MINUTES.toMillis(10), this::updateServerHealth);
		return true;
	}
	
	@Override
	public boolean stop() {
		executor.stop();
		executor.awaitTermination(1000);
		return true;
	}
	
	private void updateServerHealth() {
		Runtime rt = Runtime.getRuntime();
		long total = rt.totalMemory();
		long usedBefore = total - rt.freeMemory();
		ClientFactory.freeMemory();
		System.gc();
		long usedAfter = total - rt.freeMemory();
		Log.d("Memory cleanup. Total: %.1fGB  Before: %.2f%%  After: %.2f%%", total/1073741824.0, usedBefore/1073741824.0*100, usedAfter/1073741824.0*100);
	}
	
}
