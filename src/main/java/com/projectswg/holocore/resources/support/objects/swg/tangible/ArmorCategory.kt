package com.projectswg.holocore.resources.support.objects.swg.tangible

data class ArmorCategory(val id: String, val attributeName: String, val requiredCommand: String) {
	
	companion object {
		val assault = ArmorCategory("assault", "@obj_attr_n:armor_assault", "assault_move_mitigate_1")
		val battle = ArmorCategory("battle", "@obj_attr_n:armor_battle", "battle_move_mitigate_1")
		val reconnaissance = ArmorCategory("reconnaissance", "@obj_attr_n:armor_reconnaissance", "recon_move_mitigate_1");
		
		fun getById(id: String?): ArmorCategory? {
			return when (id) {
				assault.id -> assault
				battle.id -> battle
				reconnaissance.id -> reconnaissance
				else -> null
			}
		}
	}
}
