package com.projectswg.holocore.resources.support.objects.swg.custom;

import java.time.Instant;

public class IncapSafetyTimer {
	
	private final long safetyTime;
	
	public IncapSafetyTimer(long safetyTime) {
		this.safetyTime = safetyTime;
	}
	
	public boolean isExpired(long currentTime, long lastIncapTime) {
		Instant nowInstant = Instant.ofEpochMilli(currentTime);
		Instant expirationInstant = Instant.ofEpochMilli(lastIncapTime + safetyTime);
		
		return nowInstant.isAfter(expirationInstant);
	}
}
