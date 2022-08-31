package com.projectswg.holocore.services.gameplay.combat.command;

import com.projectswg.common.data.location.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestCombatCommandAttack {
	@Test
	public void testConeRange() {
		Location attackerLocation = new Location.LocationBuilder()
				.setX(0)
				.setY(0)
				.setZ(0)
				.build();
		
		Location targetLocation = new Location.LocationBuilder()
				.setX(20)
				.setY(0)
				.setZ(10)
				.build();
		
		Location collateralInsideCone1 = new Location.LocationBuilder()
				.setX(10)
				.setY(0)
				.setZ(5)
				.build();
		
		Location collateralInsideCone2 = new Location.LocationBuilder()
				.setX(25)
				.setY(0)
				.setZ(10)
				.build();
		
		Location collateralOutsideCone = new Location.LocationBuilder()
				.setX(-20)
				.setY(0)
				.setZ(-15)
				.build();
		
		double dirX = targetLocation.getX() - attackerLocation.getX();
		double dirZ = targetLocation.getZ() - attackerLocation.getZ();
		
		CombatCommandAttack instance = CombatCommandAttack.INSTANCE;
		assertTrue(instance.isInConeAngle(attackerLocation, collateralInsideCone1, 30, dirX, dirZ));
		assertTrue(instance.isInConeAngle(attackerLocation, collateralInsideCone2, 30, dirX, dirZ));
		assertFalse(instance.isInConeAngle(attackerLocation, collateralOutsideCone, 30, dirX, dirZ));
	}
}
