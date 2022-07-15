package com.projectswg.holocore.services.gameplay.jedi

import com.projectswg.common.data.combat.DamageType
import com.projectswg.common.network.packets.swg.zone.ClientOpenContainerMessage
import com.projectswg.holocore.intents.gameplay.jedi.CreateTestLightsaberIntent
import com.projectswg.holocore.intents.gameplay.jedi.OpenLightsaberIntent
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponType
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service

class LightsaberService : Service() {

	private val jediInitiateSkill = "force_title_jedi_rank_01"

	@IntentHandler
	private fun handleCreateTestLightsaberIntent(intent: CreateTestLightsaberIntent) {
		val player = intent.player

		val fourthGenerationOneHandedSaber = createFourthGenerationLightsaber(WeaponType.ONE_HANDED_SABER, "object/weapon/melee/sword/crafted_saber/shared_sword_lightsaber_one_handed_s1_gen4.iff")
		val fourthGenerationTwoHandedSaber = createFourthGenerationLightsaber(WeaponType.TWO_HANDED_SABER, "object/weapon/melee/2h_sword/crafted_saber/shared_sword_lightsaber_two_handed_s1_gen4.iff")
		val fourthGenerationPolearmSaber = createFourthGenerationLightsaber(WeaponType.POLEARM_SABER, "object/weapon/melee/polearm/crafted_saber/shared_sword_lightsaber_polearm_s1_gen4.iff")

		fourthGenerationOneHandedSaber.moveToContainer(player.creatureObject.inventory)
		fourthGenerationTwoHandedSaber.moveToContainer(player.creatureObject.inventory)
		fourthGenerationPolearmSaber.moveToContainer(player.creatureObject.inventory)
	}

	@IntentHandler
	private fun handleOpenLightsaberIntent(intent: OpenLightsaberIntent) {
		val weaponObject = intent.weaponObject
		val player = intent.player
		val currentlyEquipped = player.creatureObject.equippedWeapon == weaponObject

		if (currentlyEquipped) {
			SystemMessageIntent.broadcastPersonal(player, "@jedi_spam:saber_not_while_equpped")
			return
		}

		val lightsaberInventory = weaponObject.lightsaberInventory
		if (lightsaberInventory != null) {
			player.sendPacket(ClientOpenContainerMessage(lightsaberInventory.objectId, ""))
		}
	}

	private fun createFourthGenerationLightsaber(weaponType: WeaponType, template: String): WeaponObject {
		val fourthGenerationSaber = ObjectCreator.createObjectFromTemplate(template) as WeaponObject
		fourthGenerationSaber.type = weaponType

		fourthGenerationSaber.forcePowerCost = 52
		fourthGenerationSaber.requiredCombatLevel = 54
		fourthGenerationSaber.requiredSkill = jediInitiateSkill

		fourthGenerationSaber.damageType = DamageType.ENERGY
		fourthGenerationSaber.attackSpeed = 3.42f
		fourthGenerationSaber.minDamage = 145
		fourthGenerationSaber.maxDamage = 241
		fourthGenerationSaber.woundChance = 30.46f

		fourthGenerationSaber.maxRange = 5f
		fourthGenerationSaber.specialAttackCost = 126

		val fourthGenerationSaberInventory = ObjectCreator.createObjectFromTemplate("object/tangible/inventory/shared_lightsaber_inventory_4.iff") as TangibleObject
		fourthGenerationSaberInventory.moveToContainer(fourthGenerationSaber)

		ObjectCreatedIntent.broadcast(fourthGenerationSaber)
		ObjectCreatedIntent.broadcast(fourthGenerationSaberInventory)
		return fourthGenerationSaber
	}
}