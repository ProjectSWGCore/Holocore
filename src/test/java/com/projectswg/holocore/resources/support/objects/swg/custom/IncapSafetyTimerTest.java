package com.projectswg.holocore.resources.support.objects.swg.custom;

import org.junit.Test;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;

import static org.junit.Assert.*;

public class IncapSafetyTimerTest {
	
	private final long safetyTimerInSeconds = 30;
	private final long safetyTimerInMillis = safetyTimerInSeconds * 1000;
	private final IncapSafetyTimer incapSafetyTimer = new IncapSafetyTimer(safetyTimerInMillis);
	
	@Test
	public void isExpired_whenTooMuchTimeHasPassed() {
		long currentTime = createTime(50);
		long lastIncapTime = createTime(10);
		boolean safetyTimerExpired = incapSafetyTimer.isExpired(currentTime, lastIncapTime);
		
		assertTrue(safetyTimerExpired);
	}
	
	@Test
	public void isActive_whenNotEnoughTimeHasPassed() {
		long currentTime = createTime(0);
		long lastIncapTime = createTime(28);
		boolean safetyTimerExpired = incapSafetyTimer.isExpired(currentTime, lastIncapTime);
		
		assertFalse(safetyTimerExpired);
	}
	
	@Test
	public void isExpired_whenUnset() {
		long currentTime = 0;
		long lastIncapTime = createTime(10);
		boolean safetyTimerExpired = incapSafetyTimer.isExpired(currentTime, lastIncapTime);
		
		assertFalse(safetyTimerExpired);
	}
	
	private long createTime(int second) {
		LocalDateTime localDateTime = LocalDateTime.of(2022, Month.APRIL, 13, 22, 10, second, 0);
		return convertToEpochMillis(localDateTime);
	}
	
	private long convertToEpochMillis(LocalDateTime localDateTime) {
		return localDateTime.toEpochSecond(ZoneOffset.UTC) * 1000;
	}
}