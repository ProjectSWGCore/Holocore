package com.projectswg.holocore.services.gameplay.combat

import com.projectswg.common.data.CRC
import com.projectswg.common.data.encodables.tangible.PvpStatus
import com.projectswg.common.network.packets.swg.zone.object_controller.CommandQueueEnqueue
import com.projectswg.holocore.intents.gameplay.gcw.faction.FactionIntent
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
import com.projectswg.holocore.resources.support.global.commands.State
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.DefaultWeaponFactory
import com.projectswg.holocore.services.gameplay.combat.command.CombatCommandService
import com.projectswg.holocore.services.gameplay.faction.FactionFlagService
import com.projectswg.holocore.services.support.global.commands.CommandExecutionService
import com.projectswg.holocore.services.support.global.commands.CommandQueueService
import com.projectswg.holocore.test.resources.GenericCreatureObject
import com.projectswg.holocore.test.resources.GenericPlayer
import com.projectswg.holocore.test.runners.TestRunnerSimulatedWorld
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FactionPvpTest : TestRunnerSimulatedWorld() {

	@Before
	fun setup() {
		registerService(CommandQueueService())
		registerService(CommandExecutionService())
		registerService(CombatCommandService())
		registerService(FactionFlagService())
		registerService(CombatStatusService())
	}

	@Test
	fun `rebel and imperial characters can fight each other by becoming Special Forces`() {
		val rebelSpecialForcesCharacter = createCharacterInFaction("rebel", PvpStatus.SPECIALFORCES)
		val rebelSpecialForcesPlayer = rebelSpecialForcesCharacter.owner ?: throw RuntimeException("Unable to access player")
		val imperialSpecialForcesCharacter = createCharacterInFaction("imperial", PvpStatus.SPECIALFORCES)

		meleeHit(rebelSpecialForcesPlayer, imperialSpecialForcesCharacter)

		assertTrue(State.COMBAT.isActive(rebelSpecialForcesCharacter))
	}

	@Test
	fun `rebel and imperial characters must be Special Forces to fight each other`() {
		val rebelSpecialForcesCharacter = createCharacterInFaction("rebel", PvpStatus.COMBATANT)
		val rebelSpecialForcesPlayer = rebelSpecialForcesCharacter.owner ?: throw RuntimeException("Unable to access player")
		val imperialSpecialForcesCharacter = createCharacterInFaction("imperial", PvpStatus.SPECIALFORCES)

		meleeHit(rebelSpecialForcesPlayer, imperialSpecialForcesCharacter)

		assertFalse(State.COMBAT.isActive(rebelSpecialForcesCharacter))
	}

	private fun createCharacterInFaction(faction: String, pvpStatus: PvpStatus): GenericCreatureObject {
		val rebelSpecialForcesCreature = createCreatureObject()
		FactionIntent.broadcastUpdateFaction(rebelSpecialForcesCreature, ServerData.factions.getFaction(faction))
		FactionIntent.broadcastUpdateStatus(rebelSpecialForcesCreature, pvpStatus)
		waitForIntents()
		return rebelSpecialForcesCreature
	}

	private fun meleeHit(player: GenericPlayer, target: SWGObject) {
		val crc = CRC.getCrc("meleehit")
		val targetObjectId = target.objectId

		broadcastAndWait(InboundPacketIntent(player, CommandQueueEnqueue(player.creatureObject.objectId, 0, crc, targetObjectId, "")))
	}

	private fun createCreatureObject(): GenericCreatureObject {
		val creatureObject = GenericCreatureObject(ObjectCreator.getNextObjectId())
		ObjectCreatedIntent.broadcast(creatureObject)
		val defaultWeapon = DefaultWeaponFactory.createDefaultWeapon()
		defaultWeapon.moveToContainer(creatureObject)
		creatureObject.equippedWeapon = defaultWeapon
		return creatureObject
	}
}