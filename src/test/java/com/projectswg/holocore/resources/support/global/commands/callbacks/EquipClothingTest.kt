package com.projectswg.holocore.resources.support.global.commands.callbacks

import com.projectswg.common.data.encodables.tangible.Race
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.StaticItemCreator
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import com.projectswg.holocore.test.resources.GenericCreatureObject
import com.projectswg.holocore.test.runners.TestRunnerSimulatedWorld
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EquipClothingTest : TestRunnerSimulatedWorld() {
	
	private val transferItemCallback = TransferItemCallback()
	
	@Test
	fun `can equip clothing when requirements are fulfilled`() {
		val creatureObject = createCreatureObject()
		val player = creatureObject.owner ?: throw RuntimeException("Unable to access player")
		val specialEditionGoggles = createSpecialEditionGoggles()
		specialEditionGoggles.moveToContainer(creatureObject.inventory)
		val args = createArgsForEquippingAnItem(creatureObject)

		transferItemCallback.execute(player, specialEditionGoggles, args)

		assertEquals(creatureObject, specialEditionGoggles.parent)
	}

	@Test
	fun `can not equip clothing when species requirement is violated`() {
		val wookieeMale = createWookieeMale()
		val player = wookieeMale.owner ?: throw RuntimeException("Unable to access player")
		val muscleShirt = createMuscleShirt()
		muscleShirt.moveToContainer(wookieeMale.inventory)
		val args = createArgsForEquippingAnItem(wookieeMale)

		transferItemCallback.execute(player, muscleShirt, args)

		assertEquals(wookieeMale.inventory, muscleShirt.parent)
	}

	private fun createWookieeMale(): GenericCreatureObject {
		val creatureObject = createCreatureObject()
		creatureObject.race = Race.WOOKIEE_MALE
		creatureObject.template = "object/creature/player/shared_wookiee_male.iff"
		return creatureObject
	}

	private fun createArgsForEquippingAnItem(swgObject: SWGObject): String {
		return " ${swgObject.objectId} 4 0.000000 0.000000 0.000000"
	}

	private fun createCreatureObject(): GenericCreatureObject {
		val creatureObject = GenericCreatureObject(ObjectCreator.getNextObjectId())
		broadcastAndWait(ObjectCreatedIntent(creatureObject))
		return creatureObject
	}

	private fun createSpecialEditionGoggles(): TangibleObject {
		val specialEditionGoggles = StaticItemCreator.createItem("item_clothing_goggles_goggles_01_01") as TangibleObject?
		specialEditionGoggles ?: throw RuntimeException("Unable to create SE Goggles")
		broadcastAndWait(ObjectCreatedIntent(specialEditionGoggles))
		return specialEditionGoggles
	}

	private fun createMuscleShirt(): TangibleObject {
		val muscleShirt = StaticItemCreator.createItem("item_clothing_shirt_01_42") as TangibleObject?
		muscleShirt ?: throw RuntimeException("Unable to create Muscle Shirt")
		broadcastAndWait(ObjectCreatedIntent(muscleShirt))
		return muscleShirt
	}

}