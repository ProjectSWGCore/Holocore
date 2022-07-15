package com.projectswg.holocore.resources.support.global.commands.callbacks

import com.projectswg.common.data.combat.DamageType
import com.projectswg.holocore.intents.gameplay.jedi.TuneCrystalNowIntent
import com.projectswg.holocore.intents.gameplay.player.experience.skills.GrantSkillIntent
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.StaticItemCreator
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponType
import com.projectswg.holocore.services.gameplay.jedi.LightsaberCrystalService
import com.projectswg.holocore.services.gameplay.jedi.LightsaberService
import com.projectswg.holocore.services.gameplay.player.experience.skills.SkillService
import com.projectswg.holocore.test.resources.GenericCreatureObject
import com.projectswg.holocore.test.runners.TestRunnerSimulatedWorld
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EquipLightsaberTest : TestRunnerSimulatedWorld() {

	private val transferItemCallback = TransferItemCallback()

	@BeforeEach
	internal fun setUp() {
		registerService(SkillService())
		registerService(LightsaberService())
		registerService(LightsaberCrystalService())
	}

	@Test
	fun `can equip lightsaber when there is a color crystal inside`() {
		val jedi = createJedi()
		val player = jedi.owner ?: throw RuntimeException("Unable to access player")
		val lightsaber = createLightsaber()
		lightsaber.moveToContainer(jedi.inventory)
		val colorCrystal = createColorCrystal()
		colorCrystal.moveToContainer(jedi.inventory)
		TuneCrystalNowIntent.broadcast(jedi, colorCrystal)
		waitForIntents()
		colorCrystal.moveToContainer(lightsaber.lightsaberInventory)
		val args = createArgsForEquippingAnItem(jedi)

		transferItemCallback.execute(player, lightsaber, args)
		
		assertEquals(jedi, lightsaber.parent)
	}

	@Test
	fun `can not equip lightsaber when there is no color crystal inside`() {
		val jedi = createJedi()
		val player = jedi.owner ?: throw RuntimeException("Unable to access player")
		val lightsaber = createLightsaber()
		lightsaber.moveToContainer(jedi.inventory)
		val args = createArgsForEquippingAnItem(jedi)

		transferItemCallback.execute(player, lightsaber, args)

		assertEquals(jedi.inventory, lightsaber.parent)
	}

	private fun createArgsForEquippingAnItem(swgObject: SWGObject): String {
		return " ${swgObject.objectId} 4 0.000000 0.000000 0.000000"
	}

	private fun createJedi(): GenericCreatureObject {
		val creatureObject = GenericCreatureObject(ObjectCreator.getNextObjectId())
		broadcastAndWait(ObjectCreatedIntent(creatureObject))
		broadcastAndWait(GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "force_discipline_defender_master", creatureObject, true))
		return creatureObject
	}

	private fun createLightsaber(): WeaponObject {
		val lightsaber = createFourthGenerationLightsaber(WeaponType.TWO_HANDED_SABER, "object/weapon/melee/2h_sword/crafted_saber/shared_sword_lightsaber_two_handed_s1_gen4.iff")
		waitForIntents()
		return lightsaber
	}

	private fun createColorCrystal(): TangibleObject {
		val colorCrystal = StaticItemCreator.createItem("item_color_crystal_02_29") as TangibleObject?
		colorCrystal ?: throw RuntimeException("Unable to create color crystal")
		broadcastAndWait(ObjectCreatedIntent(colorCrystal))

		return colorCrystal
	}

	private fun createFourthGenerationLightsaber(weaponType: WeaponType, template: String): WeaponObject {
		val jediInitiateSkill = "force_title_jedi_rank_01"
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