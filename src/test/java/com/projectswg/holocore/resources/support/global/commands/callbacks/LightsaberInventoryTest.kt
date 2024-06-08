/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package com.projectswg.holocore.resources.support.global.commands.callbacks

import com.projectswg.common.data.CRC
import com.projectswg.common.data.combat.DamageType
import com.projectswg.common.network.packets.swg.zone.object_controller.CommandQueueEnqueue
import com.projectswg.holocore.intents.gameplay.jedi.TuneCrystalNowIntent
import com.projectswg.holocore.intents.gameplay.player.experience.skills.GrantSkillIntent
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.StaticItemCreator
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.DefaultWeaponFactory
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponType
import com.projectswg.holocore.services.gameplay.jedi.LightsaberCrystalService
import com.projectswg.holocore.services.gameplay.jedi.LightsaberService
import com.projectswg.holocore.services.gameplay.player.experience.skills.SkillService
import com.projectswg.holocore.services.support.global.commands.CommandExecutionService
import com.projectswg.holocore.services.support.global.commands.CommandQueueService
import com.projectswg.holocore.test.resources.GenericCreatureObject
import com.projectswg.holocore.test.runners.TestRunnerSimulatedWorld
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LightsaberInventoryTest : TestRunnerSimulatedWorld() {

	@BeforeEach
	internal fun setUp() {
		registerService(CommandQueueService(5))
		registerService(CommandExecutionService())
		registerService(SkillService())
		registerService(LightsaberService())
		registerService(LightsaberCrystalService())
	}

	private fun transferItem(player: Player, item: SWGObject, container: SWGObject?) {
		val args = createArgsForEquippingAnItem(container ?: return)
		val transferItemArmorPacket = CommandQueueEnqueue(player.creatureObject.objectId, 0, CRC.getCrc("transferitemmisc"), item.objectId, args)

		InboundPacketIntent.broadcast(player, transferItemArmorPacket)
		waitForIntents()
	}

	@Test
	fun `adding color crystal sets elemental type on the lightsaber`() {
		val jedi = createJedi()
		val player = jedi.owner ?: throw RuntimeException("Unable to access player")
		val lightsaber = createFourthGenerationLightsaber()
		lightsaber.moveToContainer(jedi.inventory)
		val colorCrystal = createColorCrystal()
		colorCrystal.moveToContainer(jedi.inventory)
		TuneCrystalNowIntent(jedi, colorCrystal).broadcast()
		waitForIntents()

		transferItem(player, colorCrystal, lightsaber.lightsaberInventory)

		assertEquals(colorCrystal.lightsaberColorCrystalElementalType, lightsaber.elementalType)
	}

	@Test
	fun `adding color crystal sets elemental value on the lightsaber`() {
		val jedi = createJedi()
		val player = jedi.owner ?: throw RuntimeException("Unable to access player")
		val lightsaber = createFourthGenerationLightsaber()
		lightsaber.moveToContainer(jedi.inventory)
		val colorCrystal = createColorCrystal()
		colorCrystal.moveToContainer(jedi.inventory)
		TuneCrystalNowIntent(jedi, colorCrystal).broadcast()
		waitForIntents()

		transferItem(player, colorCrystal, lightsaber.lightsaberInventory)

		assertEquals(14, lightsaber.elementalValue)	// The color crystal takes 6% of lightsaber max damage and turns that into the elemental value, rounded down
	}

	@Test
	fun `removing color crystal removes elemental type on the lightsaber`() {
		val jedi = createJedi()
		val player = jedi.owner ?: throw RuntimeException("Unable to access player")
		val lightsaber = createFourthGenerationLightsaber()
		lightsaber.moveToContainer(jedi.inventory)
		val colorCrystal1 = createColorCrystal()
		colorCrystal1.moveToContainer(jedi.inventory)
		TuneCrystalNowIntent(jedi, colorCrystal1).broadcast()
		waitForIntents()
		transferItem(player, colorCrystal1, lightsaber.lightsaberInventory)

		transferItem(player, colorCrystal1, jedi.inventory)

		assertNull(lightsaber.elementalType)
	}

	@Test
	internal fun `removing color crystal removes elemental value on the lightsaber`() {
		val jedi = createJedi()
		val player = jedi.owner ?: throw RuntimeException("Unable to access player")
		val lightsaber = createFourthGenerationLightsaber()
		lightsaber.moveToContainer(jedi.inventory)
		val colorCrystal1 = createColorCrystal()
		colorCrystal1.moveToContainer(jedi.inventory)
		TuneCrystalNowIntent(jedi, colorCrystal1).broadcast()
		waitForIntents()
		transferItem(player, colorCrystal1, lightsaber.lightsaberInventory)

		transferItem(player, colorCrystal1, jedi.inventory)

		assertEquals(0, lightsaber.elementalValue)
	}

	@Test
	fun `removing power crystal decreases minimum damage of the lightsaber`() {
		val jedi = createJedi()
		val player = jedi.owner ?: throw RuntimeException("Unable to access player")
		val lightsaber = createFourthGenerationLightsaber()
		lightsaber.moveToContainer(jedi.inventory)
		val powerCrystal = createPowerCrystal()
		powerCrystal.moveToContainer(lightsaber.lightsaberInventory)
		TuneCrystalNowIntent(jedi, powerCrystal).broadcast()
		waitForIntents()
		val expectedMinDmg = lightsaber.minDamage - powerCrystal.lightsaberPowerCrystalMinDmg

		transferItem(player, powerCrystal, jedi.inventory)

		assertEquals(expectedMinDmg, lightsaber.minDamage)
	}

	@Test
	fun `removing power crystal decreases maximum damage of the lightsaber`() {
		val jedi = createJedi()
		val player = jedi.owner ?: throw RuntimeException("Unable to access player")
		val lightsaber = createFourthGenerationLightsaber()
		lightsaber.moveToContainer(jedi.inventory)
		val powerCrystal = createPowerCrystal()
		powerCrystal.moveToContainer(lightsaber.lightsaberInventory)
		TuneCrystalNowIntent(jedi, powerCrystal).broadcast()
		waitForIntents()
		val expectedMinDmg = lightsaber.maxDamage - powerCrystal.lightsaberPowerCrystalMaxDmg

		transferItem(player, powerCrystal, jedi.inventory)

		assertEquals(expectedMinDmg, lightsaber.maxDamage)
	}

	@Test
	fun `inserting power crystal increases minimum damage of the lightsaber`() {
		val jedi = createJedi()
		val player = jedi.owner ?: throw RuntimeException("Unable to access player")
		val lightsaber = createFourthGenerationLightsaber()
		lightsaber.moveToContainer(jedi.inventory)
		val powerCrystal = createPowerCrystal()
		powerCrystal.moveToContainer(jedi.inventory)
		TuneCrystalNowIntent(jedi, powerCrystal).broadcast()
		waitForIntents()
		val expectedMinDmg = lightsaber.minDamage + powerCrystal.lightsaberPowerCrystalMinDmg

		transferItem(player, powerCrystal, lightsaber.lightsaberInventory)

		assertEquals(expectedMinDmg, lightsaber.minDamage)
	}

	@Test
	fun `inserting power crystal increases maximum damage of the lightsaber`() {
		val jedi = createJedi()
		val player = jedi.owner ?: throw RuntimeException("Unable to access player")
		val lightsaber = createFourthGenerationLightsaber()
		lightsaber.moveToContainer(jedi.inventory)
		val powerCrystal = createPowerCrystal()
		powerCrystal.moveToContainer(jedi.inventory)
		TuneCrystalNowIntent(jedi, powerCrystal).broadcast()
		waitForIntents()
		val expectedMaxDmg = lightsaber.maxDamage + powerCrystal.lightsaberPowerCrystalMaxDmg

		transferItem(player, powerCrystal, lightsaber.lightsaberInventory)

		assertEquals(expectedMaxDmg, lightsaber.maxDamage)
	}

	@Test
	fun `lightsaber gets blade color from the color crystal`() {
		val jedi = createJedi()
		val player = jedi.owner ?: throw RuntimeException("Unable to access player")
		val lightsaber = createFourthGenerationLightsaber()
		lightsaber.moveToContainer(jedi.inventory)
		val colorCrystal = createColorCrystal()
		colorCrystal.moveToContainer(jedi.inventory)
		TuneCrystalNowIntent(jedi, colorCrystal).broadcast()
		waitForIntents()

		transferItem(player, colorCrystal, lightsaber.lightsaberInventory)

		assertEquals(colorCrystal.getCustomization("/private/index_color_1"), lightsaber.getCustomization("/private/index_color_blade"))
	}

	@Test
	fun `color crystal must be tuned by us`() {
		val jedi = createJedi()
		val differentJedi = createJedi()
		val player = jedi.owner ?: throw RuntimeException("Unable to access player")
		val lightsaber = createFourthGenerationLightsaber()
		lightsaber.moveToContainer(jedi.inventory)
		val colorCrystal = createColorCrystal()
		colorCrystal.moveToContainer(jedi.inventory)
		TuneCrystalNowIntent(differentJedi, colorCrystal).broadcast()
		waitForIntents()

		transferItem(player, colorCrystal, lightsaber.lightsaberInventory)

		assertEquals(jedi.inventory, colorCrystal.parent)
	}

	@Test
	fun `power crystal must be tuned by us`() {
		val jedi = createJedi()
		val differentJedi = createJedi()
		val player = jedi.owner ?: throw RuntimeException("Unable to access player")
		val lightsaber = createFourthGenerationLightsaber()
		lightsaber.moveToContainer(jedi.inventory)
		val powerCrystal = createPowerCrystal()
		powerCrystal.moveToContainer(jedi.inventory)
		TuneCrystalNowIntent(differentJedi, powerCrystal).broadcast()
		waitForIntents()

		transferItem(player, powerCrystal, lightsaber.lightsaberInventory)

		assertEquals(jedi.inventory, powerCrystal.parent)
	}

	@Test
	fun `color crystal can be added to a lightsaber`() {
		val jedi = createJedi()
		val player = jedi.owner ?: throw RuntimeException("Unable to access player")
		val lightsaber = createFourthGenerationLightsaber()
		lightsaber.moveToContainer(jedi.inventory)
		val colorCrystal = createColorCrystal()
		colorCrystal.moveToContainer(jedi.inventory)
		TuneCrystalNowIntent(jedi, colorCrystal).broadcast()
		waitForIntents()

		transferItem(player, colorCrystal, lightsaber.lightsaberInventory)

		assertEquals(lightsaber.lightsaberInventory, colorCrystal.parent)
	}

	/**
	 * The lava crystal is a bit special, because it doesn't have a color in wp_lightsaber.pal.
	 */
	@Test
	fun `Lava Crystal can be added to a lightsaber`() {
		val jedi = createJedi()
		val player = jedi.owner ?: throw RuntimeException("Unable to access player")
		val lightsaber = createFourthGenerationLightsaber()
		lightsaber.moveToContainer(jedi.inventory)
		val lavaCrystal = createLavaCrystal()
		lavaCrystal.moveToContainer(jedi.inventory)
		TuneCrystalNowIntent(jedi, lavaCrystal).broadcast()
		waitForIntents()

		transferItem(player, lavaCrystal, lightsaber.lightsaberInventory)

		assertEquals(0, lightsaber.getCustomization("/private/index_color_blade"))
		assertEquals(1, lightsaber.getCustomization("private/alternate_shader_blade"))
	}

	@Test
	fun `only one color crystal can be added to a lightsaber`() {
		val jedi = createJedi()
		val player = jedi.owner ?: throw RuntimeException("Unable to access player")
		val lightsaber = createFourthGenerationLightsaber()
		lightsaber.moveToContainer(jedi.inventory)
		val colorCrystal1 = createColorCrystal()
		colorCrystal1.moveToContainer(jedi.inventory)
		TuneCrystalNowIntent(jedi, colorCrystal1).broadcast()
		val colorCrystal2 = createColorCrystal()
		colorCrystal2.moveToContainer(jedi.inventory)
		TuneCrystalNowIntent(jedi, colorCrystal2).broadcast()
		waitForIntents()
		transferItem(player, colorCrystal1, lightsaber.lightsaberInventory)

		transferItem(player, colorCrystal2, lightsaber.lightsaberInventory)

		assertEquals(jedi.inventory, colorCrystal2.parent)
	}

	@Test
	fun `power crystal can be added to a lightsaber`() {
		val jedi = createJedi()
		val player = jedi.owner ?: throw RuntimeException("Unable to access player")
		val lightsaber = createFourthGenerationLightsaber()
		lightsaber.moveToContainer(jedi.inventory)
		val powerCrystal = createPowerCrystal()
		powerCrystal.moveToContainer(jedi.inventory)
		TuneCrystalNowIntent(jedi, powerCrystal).broadcast()
		waitForIntents()

		transferItem(player, powerCrystal, lightsaber.lightsaberInventory)

		assertEquals(lightsaber.lightsaberInventory, powerCrystal.parent)
	}

	@Test
	fun `multiple power crystals can be added to a lightsaber`() {
		val jedi = createJedi()
		val player = jedi.owner ?: throw RuntimeException("Unable to access player")
		val lightsaber = createFourthGenerationLightsaber()
		lightsaber.moveToContainer(jedi.inventory)
		val powerCrystal1 = createPowerCrystal()
		powerCrystal1.moveToContainer(jedi.inventory)
		TuneCrystalNowIntent(jedi, powerCrystal1).broadcast()
		val powerCrystal2 = createPowerCrystal()
		powerCrystal2.moveToContainer(jedi.inventory)
		TuneCrystalNowIntent(jedi, powerCrystal2).broadcast()
		waitForIntents()
		transferItem(player, powerCrystal1, lightsaber.lightsaberInventory)

		transferItem(player, powerCrystal2, lightsaber.lightsaberInventory)

		assertEquals(lightsaber.lightsaberInventory, powerCrystal2.parent)
	}

	@Test
	fun `only valid items can be added to a lightsaber`() {
		val jedi = createJedi()
		val player = jedi.owner ?: throw RuntimeException("Unable to access player")
		val lightsaber = createFourthGenerationLightsaber()
		lightsaber.moveToContainer(jedi.inventory)
		val trandoshanHuntingRifle = createTrandoshanHuntingRifle()
		trandoshanHuntingRifle.moveToContainer(jedi.inventory)
		waitForIntents()

		transferItem(player, trandoshanHuntingRifle, lightsaber.lightsaberInventory)

		assertEquals(jedi.inventory, trandoshanHuntingRifle.parent)
	}

	private fun createArgsForEquippingAnItem(swgObject: SWGObject): String {
		return " ${swgObject.objectId} 4 0.000000 0.000000 0.000000"
	}

	private fun createJedi(): GenericCreatureObject {
		val creatureObject = GenericCreatureObject(ObjectCreator.getNextObjectId())
		val defWeapon = DefaultWeaponFactory.createDefaultWeapon()
		defWeapon.moveToContainer(creatureObject)
		creatureObject.equippedWeapon = defWeapon
		broadcastAndWait(ObjectCreatedIntent(creatureObject))
		broadcastAndWait(ObjectCreatedIntent(creatureObject.inventory))
		broadcastAndWait(GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "force_discipline_defender_master", creatureObject, true))
		return creatureObject
	}

	private fun createColorCrystal(): TangibleObject {
		val colorCrystal = StaticItemCreator.createItem("item_color_crystal_02_20") as TangibleObject?
		colorCrystal ?: throw RuntimeException("Unable to create color crystal")
		broadcastAndWait(ObjectCreatedIntent(colorCrystal))

		return colorCrystal
	}

	private fun createLavaCrystal(): TangibleObject {
		val colorCrystal = StaticItemCreator.createItem("item_tow_lava_crystal_06_01") as TangibleObject?
		colorCrystal ?: throw RuntimeException("Unable to create color crystal")
		broadcastAndWait(ObjectCreatedIntent(colorCrystal))

		return colorCrystal
	}

	private fun createPowerCrystal(): TangibleObject {
		val powerCrystal = StaticItemCreator.createItem("item_power_crystal_04_14") as TangibleObject?
		powerCrystal ?: throw RuntimeException("Unable to create power crystal")
		broadcastAndWait(ObjectCreatedIntent(powerCrystal))

		return powerCrystal
	}

	private fun createTrandoshanHuntingRifle(): SWGObject {
		val trandoshanHuntingRifle = StaticItemCreator.createItem("weapon_rifle_trando_hunting") as WeaponObject?
		trandoshanHuntingRifle ?: throw RuntimeException("Unable to create weapon")
		broadcastAndWait(ObjectCreatedIntent(trandoshanHuntingRifle))
		return trandoshanHuntingRifle
	}

	private fun createFourthGenerationLightsaber(): WeaponObject {
		val weaponType = WeaponType.TWO_HANDED_SABER
		val template = "object/weapon/melee/2h_sword/crafted_saber/shared_sword_lightsaber_two_handed_s1_gen4.iff"
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
		waitForIntents()
		return fourthGenerationSaber
	}

}