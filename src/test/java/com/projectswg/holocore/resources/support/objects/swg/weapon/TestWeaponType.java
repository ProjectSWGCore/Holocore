package com.projectswg.holocore.resources.support.objects.swg.weapon;

import com.projectswg.holocore.test.runners.TestRunnerNoIntents;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestWeaponType extends TestRunnerNoIntents {
	@Test
	public void testIsLightsaber() {
		assertTrue(WeaponType.ONE_HANDED_SABER.isLightsaber());
		assertTrue(WeaponType.TWO_HANDED_SABER.isLightsaber());
		assertTrue(WeaponType.POLEARM_SABER.isLightsaber());
		assertFalse(WeaponType.ONE_HANDED_MELEE.isLightsaber());
	}
	
	@Test
	public void testIsMelee() {
		assertTrue(WeaponType.ONE_HANDED_SABER.isMelee());
		assertTrue(WeaponType.TWO_HANDED_SABER.isMelee());
		assertTrue(WeaponType.POLEARM_SABER.isMelee());
		assertTrue(WeaponType.ONE_HANDED_MELEE.isMelee());
		assertTrue(WeaponType.TWO_HANDED_MELEE.isMelee());
		assertTrue(WeaponType.POLEARM_MELEE.isMelee());
		assertTrue(WeaponType.UNARMED.isMelee());
		assertFalse(WeaponType.RIFLE.isMelee());
	}
}
