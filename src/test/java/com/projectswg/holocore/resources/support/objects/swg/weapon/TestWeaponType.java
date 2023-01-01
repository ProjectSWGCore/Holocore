package com.projectswg.holocore.resources.support.objects.swg.weapon;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class TestWeaponType {
	
	public static Collection<Input> parameters() {
		return Arrays.asList(
				// Melee, not lightsabers
				new Input(WeaponType.ONE_HANDED_MELEE, true, false, false),
				new Input(WeaponType.TWO_HANDED_MELEE, true, false, false),
				new Input(WeaponType.POLEARM_MELEE, true, false, false),
				new Input(WeaponType.UNARMED, true, false, false),
				
				// Lightsabers
				new Input(WeaponType.ONE_HANDED_SABER, true, true, false),
				new Input(WeaponType.TWO_HANDED_SABER, true, true, false),
				new Input(WeaponType.POLEARM_SABER, true, true, false),
				
				// Ranged
				new Input(WeaponType.RIFLE, false, false, true),
				new Input(WeaponType.CARBINE, false, false, true),
				new Input(WeaponType.PISTOL, false, false, true),
				new Input(WeaponType.HEAVY, false, false, true)
		);
	}
	
	@ParameterizedTest
	@MethodSource("parameters")
	public void testMelee(Input input) {
		boolean expectMelee = input.isExpectMelee();
		WeaponType type = input.getType();
		
		assertEquals(expectMelee, type.isMelee());
	}
	
	@ParameterizedTest
	@MethodSource("parameters")
	public void testLightsaber(Input input) {
		boolean expectLightsaber = input.isExpectLightSaber();
		WeaponType type = input.getType();
		
		assertEquals(expectLightsaber, type.isLightsaber());
	}
	
	@ParameterizedTest
	@MethodSource("parameters")
	public void testRanged(Input input) {
		boolean expectRanged = input.isExpectRanged();
		WeaponType type = input.getType();
		
		assertEquals(expectRanged, type.isRanged());
	}
	
	private static class Input {
		private final WeaponType type;
		private final boolean expectMelee;
		private final boolean expectLightSaber;
		private final boolean expectRanged;
		
		public Input(WeaponType type, boolean expectMelee, boolean expectLightSaber, boolean expectRanged) {
			this.type = type;
			this.expectMelee = expectMelee;
			this.expectLightSaber = expectLightSaber;
			this.expectRanged = expectRanged;
		}
		
		public WeaponType getType() {
			return type;
		}
		
		public boolean isExpectMelee() {
			return expectMelee;
		}
		
		public boolean isExpectLightSaber() {
			return expectLightSaber;
		}
		
		public boolean isExpectRanged() {
			return expectRanged;
		}
		
		@Override
		public String toString() {
			return "type=" + type + ", expectMelee=" + expectMelee + ", expectLightSaber=" + expectLightSaber + ", expectRanged=" + expectRanged;
		}
	}
}
