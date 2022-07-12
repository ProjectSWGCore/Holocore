package com.projectswg.holocore.resources.support.objects.swg.weapon;

import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.test.resources.GenericCreatureObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WeaponObjectTest {
	
	@Test
	public void yourModifiedAttackSpeedTakesPresentSpeedModsIntoAccount() {
		WeaponObject weaponObject = (WeaponObject) ObjectCreator.createObjectFromTemplate("object/weapon/melee/unarmed/shared_unarmed_default_player.iff");
		weaponObject.setType(WeaponType.UNARMED);
		weaponObject.setAttackSpeed(2.5f);
		GenericCreatureObject genericCreatureObject = new GenericCreatureObject(1);
		genericCreatureObject.adjustSkillmod("melee_speed", 0, 25);
		genericCreatureObject.adjustSkillmod("unarmed_speed", 0, 25);
		
		assertEquals(1.25, weaponObject.getModdedWeaponAttackSpeedWithCap(genericCreatureObject), 0);
	}
	
	@Test
	public void yourModifiedAttackSpeedTakesMissingSpeedModsIntoAccount() {
		WeaponObject weaponObject = (WeaponObject) ObjectCreator.createObjectFromTemplate("object/weapon/melee/unarmed/shared_unarmed_default_player.iff");
		weaponObject.setType(WeaponType.UNARMED);
		weaponObject.setAttackSpeed(2.5f);
		GenericCreatureObject genericCreatureObject = new GenericCreatureObject(1);
		
		assertEquals(2.5f, weaponObject.getModdedWeaponAttackSpeedWithCap(genericCreatureObject), 0);
	}
}