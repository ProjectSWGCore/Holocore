package com.projectswg.holocore.resources.support.objects.swg.weapon;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class TestWeaponType {
	
	@Parameterized.Parameters(name = "{0}")
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
				new Input(WeaponType.HEAVY, false, false, true),
				new Input(WeaponType.HEAVY_WEAPON, false, false, true),
				new Input(WeaponType.DIRECTIONAL_TARGET_WEAPON, false, false, true),
				new Input(WeaponType.LIGHT_RIFLE, false, false, true)
		);
	}
	
	@Parameterized.Parameter
	public Input input;
	
	@Test
	public void testMelee() {
		boolean expectMelee = input.isExpectMelee();
		WeaponType type = input.getType();
		
		assertEquals("Type should be melee", expectMelee, type.isMelee());
	}
	
	@Test
	public void testLightsaber() {
		boolean expectLightsaber = input.isExpectLightSaber();
		WeaponType type = input.getType();
		
		assertEquals("Type should be lightsaber", expectLightsaber, type.isLightsaber());
	}
	
	@Test
	public void testRanged() {
		boolean expectRanged = input.isExpectRanged();
		WeaponType type = input.getType();
		
		assertEquals("Type should be ranged", expectRanged, type.isRanged());
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
