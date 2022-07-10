package com.projectswg.holocore.resources.support.global.commands.callbacks

import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.StaticItemCreator
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject
import com.projectswg.holocore.test.resources.GenericCreatureObject
import com.projectswg.holocore.test.runners.TestRunnerSimulatedWorld
import org.junit.Assert.*
import org.junit.Test

class EquipWeaponTest : TestRunnerSimulatedWorld() {
	
	private val transferItemCallback = TransferItemCallback()
	
	@Test
	fun `can equip weapon when all requirements are fulfilled`() {
		val creatureObject = createCreatureObject()
		val player = creatureObject.owner ?: throw RuntimeException("Unable to access player")
		val oneHandSwordCL1 = createOneHandSwordCL1()
		oneHandSwordCL1.moveToContainer(creatureObject.inventory)
		val args = createArgsForEquippingAnItem(creatureObject)

		transferItemCallback.execute(player, oneHandSwordCL1, args)
		
		assertEquals(creatureObject, oneHandSwordCL1.parent)
	}

	@Test
	fun `can not equip weapon if required combat level is too high`() {
		val creatureObject = createCreatureObject()
		val player = creatureObject.owner ?: throw RuntimeException("Unable to access player")
		val trandoshanHuntingRifle = createTrandoshanHuntingRifle()
		trandoshanHuntingRifle.moveToContainer(creatureObject.inventory)
		val args = createArgsForEquippingAnItem(creatureObject)

		transferItemCallback.execute(player, trandoshanHuntingRifle, args)

		assertEquals(creatureObject.inventory, trandoshanHuntingRifle.parent)
	}

	private fun createArgsForEquippingAnItem(swgObject: SWGObject): String {
		return " ${swgObject.objectId} 4 0.000000 0.000000 0.000000"
	}

	private fun createCreatureObject(): GenericCreatureObject {
		val creatureObject = GenericCreatureObject(ObjectCreator.getNextObjectId())
		broadcastAndWait(ObjectCreatedIntent(creatureObject))
		return creatureObject
	}

	private fun createOneHandSwordCL1(): WeaponObject {
		val oneHandSwordCL1 = StaticItemCreator.createItem("weapon_cl1_1h") as WeaponObject?
		oneHandSwordCL1 ?: throw RuntimeException("Unable to create weapon")
		broadcastAndWait(ObjectCreatedIntent(oneHandSwordCL1))
		return oneHandSwordCL1
	}

	private fun createTrandoshanHuntingRifle(): SWGObject {
		val trandoshanHuntingRifle = StaticItemCreator.createItem("weapon_rifle_trando_hunting") as WeaponObject?
		trandoshanHuntingRifle ?: throw RuntimeException("Unable to create weapon")
		broadcastAndWait(ObjectCreatedIntent(trandoshanHuntingRifle))
		return trandoshanHuntingRifle
	}
}