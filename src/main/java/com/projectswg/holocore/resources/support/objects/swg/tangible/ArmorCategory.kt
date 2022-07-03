package com.projectswg.holocore.resources.support.objects.swg.tangible

data class ArmorCategory(val id: String, val attributeName: String, val requiredCommand: String) {
	
	companion object {
		val assault = ArmorCategory("assault", "@obj_attr_n:armor_assault", "assault_move_mitigate_1")
		val battle = ArmorCategory("battle", "@obj_attr_n:armor_battle", "battle_move_mitigate_1")
		val reconnaissance = ArmorCategory("reconnaissance", "@obj_attr_n:armor_reconnaissance", "recon_move_mitigate_1");
		
		fun getById(id: String?): ArmorCategory? {
			if (id == assault.id) {
				return assault
			}
			if (id == battle.id) {
				return battle
			}
			if (id == reconnaissance.id) {
				return reconnaissance
			}
			return null
		}
	}
}
