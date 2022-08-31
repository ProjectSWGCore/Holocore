package com.projectswg.holocore.resources.support.objects.swg.weapon

enum class WeaponClass(val speedSkillMod: String, val defenseSkillMod: String, val accuracySkillMod: String) {
	MELEE("melee_speed", "melee_defense", "melee_accuracy"),
	RANGED("ranged_speed", "ranged_defense", "ranged_accuracy");
}