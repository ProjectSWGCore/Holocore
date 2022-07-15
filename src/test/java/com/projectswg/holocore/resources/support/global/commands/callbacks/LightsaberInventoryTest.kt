package com.projectswg.holocore.resources.support.global.commands.callbacks

import com.projectswg.common.data.combat.DamageType
import com.projectswg.holocore.intents.gameplay.jedi.TuneCrystalNowIntent
import com.projectswg.holocore.intents.gameplay.player.experience.skills.GrantSkillIntent
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent
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
import com.projectswg.holocore.test.resources.GenericCreatureObject
import com.projectswg.holocore.test.runners.TestRunnerSimulatedWorld
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LightsaberInventoryTest : TestRunnerSimulatedWorld() {

	private val transferItemCallback = TransferItemCallback()

	@BeforeEach
	internal fun setUp() {
		registerService(SkillService())
		registerService(LightsaberService())
		registerService(LightsaberCrystalService())
	}

	@Test
	fun `adding color crystal sets elemental type on the lightsaber`() {
		val jedi = createJedi()
		val player = jedi.owner ?: throw RuntimeException("Unable to access player")
		val lightsaber = createFourthGenerationLightsaber()
		lightsaber.moveToContainer(jedi.inventory)
		val colorCrystal = createColorCrystal()
		colorCrystal.moveToContainer(jedi.inventory)
		TuneCrystalNowIntent.broadcast(jedi, colorCrystal)
		waitForIntents()

		transferItemCallback.execute(player, colorCrystal, createArgsForEquippingAnItem(lightsaber.lightsaberInventory ?: return))

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
		TuneCrystalNowIntent.broadcast(jedi, colorCrystal)
		waitForIntents()

		transferItemCallback.execute(player, colorCrystal, createArgsForEquippingAnItem(lightsaber.lightsaberInventory ?: return))

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
		TuneCrystalNowIntent.broadcast(jedi, colorCrystal1)
		waitForIntents()
		transferItemCallback.execute(player, colorCrystal1, createArgsForEquippingAnItem(lightsaber.lightsaberInventory ?: return))

		transferItemCallback.execute(player, colorCrystal1, createArgsForEquippingAnItem(jedi.inventory))

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
		TuneCrystalNowIntent.broadcast(jedi, colorCrystal1)
		waitForIntents()
		transferItemCallback.execute(player, colorCrystal1, createArgsForEquippingAnItem(lightsaber.lightsaberInventory ?: return))

		transferItemCallback.execute(player, colorCrystal1, createArgsForEquippingAnItem(jedi.inventory))

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
		TuneCrystalNowIntent.broadcast(jedi, powerCrystal)
		waitForIntents()
		val expectedMinDmg = lightsaber.minDamage - powerCrystal.lightsaberPowerCrystalMinDmg

		transferItemCallback.execute(player, powerCrystal, createArgsForEquippingAnItem(jedi.inventory))

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
		TuneCrystalNowIntent.broadcast(jedi, powerCrystal)
		waitForIntents()
		val expectedMinDmg = lightsaber.maxDamage - powerCrystal.lightsaberPowerCrystalMaxDmg

		transferItemCallback.execute(player, powerCrystal, createArgsForEquippingAnItem(jedi.inventory))

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
		TuneCrystalNowIntent.broadcast(jedi, powerCrystal)
		waitForIntents()
		val expectedMinDmg = lightsaber.minDamage + powerCrystal.lightsaberPowerCrystalMinDmg

		transferItemCallback.execute(player, powerCrystal, createArgsForEquippingAnItem(lightsaber.lightsaberInventory ?: return))

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
		TuneCrystalNowIntent.broadcast(jedi, powerCrystal)
		waitForIntents()
		val expectedMaxDmg = lightsaber.maxDamage + powerCrystal.lightsaberPowerCrystalMaxDmg

		transferItemCallback.execute(player, powerCrystal, createArgsForEquippingAnItem(lightsaber.lightsaberInventory ?: return))

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
		TuneCrystalNowIntent.broadcast(jedi, colorCrystal)
		waitForIntents()

		transferItemCallback.execute(player, colorCrystal, createArgsForEquippingAnItem(lightsaber.lightsaberInventory ?: return))

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
		TuneCrystalNowIntent.broadcast(differentJedi, colorCrystal)
		waitForIntents()

		transferItemCallback.execute(player, colorCrystal, createArgsForEquippingAnItem(lightsaber.lightsaberInventory ?: return))

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
		TuneCrystalNowIntent.broadcast(differentJedi, powerCrystal)
		waitForIntents()

		transferItemCallback.execute(player, powerCrystal, createArgsForEquippingAnItem(lightsaber.lightsaberInventory ?: return))

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
		TuneCrystalNowIntent.broadcast(jedi, colorCrystal)
		waitForIntents()

		transferItemCallback.execute(player, colorCrystal, createArgsForEquippingAnItem(lightsaber.lightsaberInventory ?: return))

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
		TuneCrystalNowIntent.broadcast(jedi, lavaCrystal)
		waitForIntents()

		transferItemCallback.execute(player, lavaCrystal, createArgsForEquippingAnItem(lightsaber.lightsaberInventory ?: return))

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
		TuneCrystalNowIntent.broadcast(jedi, colorCrystal1)
		val colorCrystal2 = createColorCrystal()
		colorCrystal2.moveToContainer(jedi.inventory)
		TuneCrystalNowIntent.broadcast(jedi, colorCrystal2)
		waitForIntents()
		val args = createArgsForEquippingAnItem(lightsaber.lightsaberInventory ?: return)
		transferItemCallback.execute(player, colorCrystal1, args)

		transferItemCallback.execute(player, colorCrystal2, args)

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
		TuneCrystalNowIntent.broadcast(jedi, powerCrystal)
		waitForIntents()
		val args = createArgsForEquippingAnItem(lightsaber.lightsaberInventory ?: return)

		transferItemCallback.execute(player, powerCrystal, args)

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
		TuneCrystalNowIntent.broadcast(jedi, powerCrystal1)
		val powerCrystal2 = createPowerCrystal()
		powerCrystal2.moveToContainer(jedi.inventory)
		TuneCrystalNowIntent.broadcast(jedi, powerCrystal2)
		waitForIntents()
		val args = createArgsForEquippingAnItem(lightsaber.lightsaberInventory ?: return)
		transferItemCallback.execute(player, powerCrystal1, args)

		transferItemCallback.execute(player, powerCrystal2, args)

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
		val args = createArgsForEquippingAnItem(lightsaber.lightsaberInventory ?: return)

		transferItemCallback.execute(player, trandoshanHuntingRifle, args)

		assertEquals(jedi.inventory, trandoshanHuntingRifle.parent)
	}

	private fun createArgsForEquippingAnItem(swgObject: SWGObject): String {
		return " ${swgObject.objectId} 4 0.000000 0.000000 0.000000"
	}

	private fun createJedi(): GenericCreatureObject {
		val creatureObject = GenericCreatureObject(ObjectCreator.getNextObjectId())
		val defWeapon = DefaultWeaponFactory.createDefaultWeapon()
		defWeapon.moveToContainer(creatureObject)
		broadcastAndWait(ObjectCreatedIntent(creatureObject))
		broadcastAndWait(ObjectCreatedIntent(creatureObject.inventory))
		broadcastAndWait(GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "force_discipline_defender_master", creatureObject, true))
		return creatureObject
	}

	private fun createColorCrystal(): TangibleObject {
		val colorCrystal = StaticItemCreator.createItem("item_color_crystal_02_29") as TangibleObject?
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