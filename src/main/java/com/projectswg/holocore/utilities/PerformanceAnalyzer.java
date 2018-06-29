package com.projectswg.holocore.utilities;

import me.joshlarson.jlcommon.log.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PerformanceAnalyzer {
	
	private static final Map<String, PerformanceAnalyzer> ANALYZERS = new ConcurrentHashMap<>();
	
	private final long [] previousRuns;
	private final String name;
	private int index;
	
	private PerformanceAnalyzer(String name, int outputFrequency) {
		this.previousRuns = new long[outputFrequency];
		this.name = name;
		this.index = 0;
	}
	
	public synchronized void recordTime(long time) {
		previousRuns[index++] = time;
		if (index >= previousRuns.length) {
			long min = Long.MAX_VALUE, max = Long.MIN_VALUE, avg = 0;
			for (long run : previousRuns) {
				avg += run;
				if (run < min)
					min = run;
				if (run > max)
					max = run;
			}
			avg /= previousRuns.length;
			Log.d("%s: [%.3fms - %.3fms] Avg: %.3fms", this, min/1E6, max/1E6, avg/1E6);
			index = 0;
		}
	}
	
	@Override
	public String toString() {
		return "PerformanceAnalyzer["+name+']';
	}
	
	public static PerformanceAnalyzer getAnalyzer(String name, int outputFrequency) {
		return ANALYZERS.computeIfAbsent(name, n -> new PerformanceAnalyzer(n, outputFrequency));
	}
	
}
