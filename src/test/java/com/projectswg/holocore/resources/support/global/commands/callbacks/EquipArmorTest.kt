package com.projectswg.holocore.resources.support.global.commands.callbacks

import com.projectswg.holocore.intents.gameplay.player.experience.skills.GrantSkillIntent
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.ArmorCategory
import com.projectswg.holocore.resources.support.objects.swg.tangible.Protection
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import com.projectswg.holocore.services.gameplay.player.experience.skills.SkillService
import com.projectswg.holocore.test.resources.GenericCreatureObject
import com.projectswg.holocore.test.runners.TestRunnerSimulatedWorld
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class EquipArmorTest : TestRunnerSimulatedWorld() {
	
	private val transferItemCallback = TransferItemCallback()

	@Before
	fun setUp() {
		registerService(SkillService())
	}

	@Test
	fun `can equip Assault armor when certification is obtained`() {
		val creatureObject = createCreatureObject()
		val player = creatureObject.owner ?: throw RuntimeException("Unable to access player")
		val compositeArmorHelmet = createCompositeArmorHelmet()
		compositeArmorHelmet.moveToContainer(creatureObject.inventory)
		val args = createArgsForEquippingAnItem(creatureObject)
		GrantSkillIntent.broadcast(GrantSkillIntent.IntentType.GRANT, "combat_commando_novice", creatureObject, true)
		waitForIntents()

		transferItemCallback.execute(player, compositeArmorHelmet, args)

		assertEquals(creatureObject, compositeArmorHelmet.parent)
	}

	@Test
	fun `can not equip Assault armor when certification is missing`() {
		val creatureObject = createCreatureObject()
		val player = creatureObject.owner ?: throw RuntimeException("Unable to access player")
		val compositeArmorHelmet = createCompositeArmorHelmet()
		compositeArmorHelmet.moveToContainer(creatureObject.inventory)
		val args = createArgsForEquippingAnItem(creatureObject)
		waitForIntents()

		transferItemCallback.execute(player, compositeArmorHelmet, args)

		assertEquals(creatureObject.inventory, compositeArmorHelmet.parent)
	}

	@Test
	fun `can equip Battle armor when certification is obtained`() {
		val creatureObject = createCreatureObject()
		val player = creatureObject.owner ?: throw RuntimeException("Unable to access player")
		val paddedArmorHelmet = createPaddedArmorHelmet()
		paddedArmorHelmet.moveToContainer(creatureObject.inventory)
		val args = createArgsForEquippingAnItem(creatureObject)
		GrantSkillIntent.broadcast(GrantSkillIntent.IntentType.GRANT, "combat_polearm_novice", creatureObject, true)
		waitForIntents()

		transferItemCallback.execute(player, paddedArmorHelmet, args)

		assertEquals(creatureObject, paddedArmorHelmet.parent)
	}

	@Test
	fun `can not equip Battle armor when certification is missing`() {
		val creatureObject = createCreatureObject()
		val player = creatureObject.owner ?: throw RuntimeException("Unable to access player")
		val paddedArmorHelmet = createPaddedArmorHelmet()
		paddedArmorHelmet.moveToContainer(creatureObject.inventory)
		val args = createArgsForEquippingAnItem(creatureObject)
		waitForIntents()

		transferItemCallback.execute(player, paddedArmorHelmet, args)

		assertEquals(creatureObject.inventory, paddedArmorHelmet.parent)
	}

	@Test
	fun `can equip Reconnaissance armor when certification is obtained`() {
		val creatureObject = createCreatureObject()
		val player = creatureObject.owner ?: throw RuntimeException("Unable to access player")
		val ubeseArmorHelmet = createUbeseArmorHelmet()
		ubeseArmorHelmet.moveToContainer(creatureObject.inventory)
		val args = createArgsForEquippingAnItem(creatureObject)
		GrantSkillIntent.broadcast(GrantSkillIntent.IntentType.GRANT, "combat_rifleman_novice", creatureObject, true)
		waitForIntents()

		transferItemCallback.execute(player, ubeseArmorHelmet, args)

		assertEquals(creatureObject, ubeseArmorHelmet.parent)
	}

	@Test
	fun `can not equip Reconnaissance armor when certification is missing`() {
		val creatureObject = createCreatureObject()
		val player = creatureObject.owner ?: throw RuntimeException("Unable to access player")
		val ubeseArmorHelmet = createUbeseArmorHelmet()
		ubeseArmorHelmet.moveToContainer(creatureObject.inventory)
		val args = createArgsForEquippingAnItem(creatureObject)
		waitForIntents()

		transferItemCallback.execute(player, ubeseArmorHelmet, args)

		assertEquals(creatureObject.inventory, ubeseArmorHelmet.parent)
	}

	private fun createArgsForEquippingAnItem(swgObject: SWGObject): String {
		return " ${swgObject.objectId} 4 0.000000 0.000000 0.000000"
	}

	private fun createCreatureObject(): GenericCreatureObject {
		val creatureObject = GenericCreatureObject(ObjectCreator.getNextObjectId())
		broadcastAndWait(ObjectCreatedIntent(creatureObject))
		return creatureObject
	}

	private fun createCompositeArmorHelmet(): TangibleObject {
		val item = ObjectCreator.createObjectFromTemplate("object/tangible/wearables/armor/composite/shared_armor_composite_helmet.iff") as TangibleObject
		item.protection = Protection(7000, 5000, 6000, 6000, 6000, 6000)
		item.armorCategory = ArmorCategory.assault
		broadcastAndWait(ObjectCreatedIntent(item))

		return item
	}

	private fun createPaddedArmorHelmet(): TangibleObject {
		val item = ObjectCreator.createObjectFromTemplate("object/tangible/wearables/armor/padded/shared_armor_padded_s01_helmet.iff") as TangibleObject
		item.protection = Protection(6000, 6000, 6000, 6000, 6000, 6000)
		item.armorCategory = ArmorCategory.battle
		broadcastAndWait(ObjectCreatedIntent(item))

		return item
	}

	private fun createUbeseArmorHelmet(): TangibleObject {
		val item = ObjectCreator.createObjectFromTemplate("object/tangible/wearables/armor/ubese/shared_armor_ubese_helmet.iff") as TangibleObject
		item.protection = Protection(5000, 7000, 6000, 6000, 6000, 6000)
		item.armorCategory = ArmorCategory.reconnaissance
		broadcastAndWait(ObjectCreatedIntent(item))

		return item
	}

}