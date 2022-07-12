package com.projectswg.holocore.services.gameplay.combat.command;

import com.projectswg.common.data.encodables.tangible.PvpStatus;
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData;
import com.projectswg.holocore.resources.support.data.server_info.loader.combat.FactionLoader;
import com.projectswg.holocore.resources.support.global.commands.CombatCommand;
import com.projectswg.holocore.resources.support.global.commands.Command;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject;
import com.projectswg.holocore.services.gameplay.combat.buffs.BuffService;
import com.projectswg.holocore.test.resources.GenericCreatureObject;
import com.projectswg.holocore.test.runners.TestRunnerSynchronousIntents;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class TestCombatCommandBuffPlayers extends TestRunnerSynchronousIntents {
	
	private CreatureObject source;
	private CreatureObject target;
	
	public static Collection<Input> data() {
		return Arrays.asList(
				// Neutral cases
				new Input("neutral", PvpStatus.ONLEAVE, "neutral", PvpStatus.ONLEAVE, true),
				new Input("neutral", PvpStatus.ONLEAVE, "rebel", PvpStatus.ONLEAVE, true),
				new Input("neutral", PvpStatus.ONLEAVE, "rebel", PvpStatus.COMBATANT, true),
				new Input("neutral", PvpStatus.ONLEAVE, "rebel", PvpStatus.SPECIALFORCES, true),
				new Input("neutral", PvpStatus.ONLEAVE, "imperial", PvpStatus.COMBATANT, true),
				new Input("neutral", PvpStatus.ONLEAVE, "imperial", PvpStatus.ONLEAVE, true),
				new Input("neutral", PvpStatus.ONLEAVE, "imperial", PvpStatus.SPECIALFORCES, true),
				
				// Rebel cases
				new Input("rebel", PvpStatus.ONLEAVE, "rebel", PvpStatus.ONLEAVE, true),
				new Input("rebel", PvpStatus.ONLEAVE, "rebel", PvpStatus.COMBATANT, true),
				new Input("rebel", PvpStatus.ONLEAVE, "rebel", PvpStatus.SPECIALFORCES, true),
				new Input("rebel", PvpStatus.COMBATANT, "imperial", PvpStatus.ONLEAVE, true),
				new Input("rebel", PvpStatus.COMBATANT, "imperial", PvpStatus.COMBATANT, false),
				new Input("rebel", PvpStatus.COMBATANT, "imperial", PvpStatus.SPECIALFORCES, false),
				new Input("rebel", PvpStatus.SPECIALFORCES, "imperial", PvpStatus.ONLEAVE, false),
				new Input("rebel", PvpStatus.SPECIALFORCES, "imperial", PvpStatus.COMBATANT, false),
				new Input("rebel", PvpStatus.SPECIALFORCES, "imperial", PvpStatus.SPECIALFORCES, false),
		
				// Imperial cases
				new Input("imperial", PvpStatus.ONLEAVE, "imperial", PvpStatus.ONLEAVE, true),
				new Input("imperial", PvpStatus.ONLEAVE, "imperial", PvpStatus.COMBATANT, true),
				new Input("imperial", PvpStatus.ONLEAVE, "imperial", PvpStatus.SPECIALFORCES, true),
				new Input("imperial", PvpStatus.COMBATANT, "rebel", PvpStatus.ONLEAVE, true),
				new Input("imperial", PvpStatus.COMBATANT, "rebel", PvpStatus.COMBATANT, false),
				new Input("imperial", PvpStatus.COMBATANT, "rebel", PvpStatus.SPECIALFORCES, false),
				new Input("imperial", PvpStatus.SPECIALFORCES, "rebel", PvpStatus.ONLEAVE, false),
				new Input("imperial", PvpStatus.SPECIALFORCES, "rebel", PvpStatus.COMBATANT, false),
				new Input("imperial", PvpStatus.SPECIALFORCES, "rebel", PvpStatus.SPECIALFORCES, false)
		);
	}
	
	public void setup(Input input) {
		source = new GenericCreatureObject(1, "Source Creature");
		FactionLoader.Faction sourceFaction = ServerData.INSTANCE.getFactions().getFaction(input.getSourceFactionName());
		source.setFaction(sourceFaction);
		source.setPvpStatus(input.getSourceStatus());
		
		// Create a weapon for the source creature. Is used during buff process and must not be null.
		WeaponObject weapon = new WeaponObject(2);
		weapon.setArrangement(List.of(List.of("hold_r")));
		weapon.systemMove(source);
		source.setEquippedWeapon(weapon);
		
		target = new GenericCreatureObject(3, "Target Creature");
		FactionLoader.Faction targetFaction = ServerData.INSTANCE.getFactions().getFaction(input.getTargetFactionName());
		target.setFaction(targetFaction);
		target.setPvpStatus(input.getTargetStatus());
		
		registerService(new BuffService());
	}
	
	@ParameterizedTest
	@MethodSource("data")
	public void testReceiveBuff(Input input) {
		setup(input);
		String targetBuffName = "hemorrhage";	// Important that the buff actually exists
		Command command = Command.builder()
				.withName(targetBuffName)
				.build();
		
		CombatCommand combatCommand = CombatCommand.builder(command)
				.withBuffNameSelf("")
				.withBuffNameTarget(targetBuffName)
				.withDefaultAnimation(new String[]{""})
				.build();
		
		CombatCommandBuff.INSTANCE.handle(source, target, combatCommand, "");
		
		waitForIntents();	// Let's give the BuffService a chance to process the BuffIntent
		
		if (input.isExpected()) {
			assertTrue(target.hasBuff(targetBuffName), "Source should be allowed to buff target");
		} else {
			assertTrue(source.hasBuff(targetBuffName), "Source should have buffed themselves and not target due to factional restrictions");
		}
	}
	
	private static class Input {
		private final String sourceFactionName;
		private final PvpStatus sourceStatus;
		private final String targetFactionName;
		private final PvpStatus targetStatus;
		private boolean expected;
		
		public Input(String sourceFactionName, PvpStatus sourceStatus, String targetFactionName, PvpStatus targetStatus, boolean expected) {
			this.sourceFactionName = sourceFactionName;
			this.sourceStatus = sourceStatus;
			this.targetFactionName = targetFactionName;
			this.targetStatus = targetStatus;
			this.expected = expected;
		}
		
		public String getSourceFactionName() {
			return sourceFactionName;
		}
		
		public PvpStatus getSourceStatus() {
			return sourceStatus;
		}
		
		public String getTargetFactionName() {
			return targetFactionName;
		}
		
		public PvpStatus getTargetStatus() {
			return targetStatus;
		}
		
		public boolean isExpected() {
			return expected;
		}
		
		@Override
		public String toString() {
			return "sourceFaction=" + sourceFactionName + ", sourceStatus=" + sourceStatus + ", targetFaction=" + targetFactionName + ", targetStatus=" + targetStatus + ", expected=" + expected;
		}
	}
}
