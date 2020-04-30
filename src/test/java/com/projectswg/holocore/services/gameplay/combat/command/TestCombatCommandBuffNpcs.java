package com.projectswg.holocore.services.gameplay.combat.command;

import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData;
import com.projectswg.holocore.resources.support.global.commands.CombatCommand;
import com.projectswg.holocore.resources.support.global.commands.Command;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.OptionFlag;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject;
import com.projectswg.holocore.services.gameplay.combat.buffs.BuffService;
import com.projectswg.holocore.test.resources.GenericCreatureObject;
import com.projectswg.holocore.test.runners.TestRunnerSynchronousIntents;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class TestCombatCommandBuffNpcs extends TestRunnerSynchronousIntents {
	
	private CreatureObject source;
	private CreatureObject target;
	
	@Parameterized.Parameter
	public Input input;
	
	@Parameterized.Parameters(name = "{0}")
	public static Collection<Input> input() {
		long group1 = 1234;
		long group2 = 4321;
		
		return Arrays.asList(
				new Input("Ungrouped player should not be able to buff ungrouped NPC", 0, 0, false,false),
				new Input("Grouped player should not be able to buff ungrouped NPC", group1, 0, false,false),
				new Input("Ungrouped player should not be able to buff grouped NPC", 0, group2, false,false),
				new Input("Grouped player should not be able to buff NPC in a different group", group1, group2, false,false),
				new Input("Grouped player should be able to buff NPC in the same group", group1, group1, false,true),
				new Input("Player should not be able to buff invulnerable NPC", 0, 0, true, false)
		);
	}
	
	@Before
	public void setup() {
		source = new GenericCreatureObject(1, "Player", true);
		source.setFaction(ServerData.INSTANCE.getFactions().getFaction("neutral"));
		source.setGroupId(input.getPlayerGroupId());
		
		// Create a weapon for the source creature. Is used during buff process and must not be null.
		WeaponObject weapon = new WeaponObject(2);
		weapon.setArrangement(List.of(List.of("hold_r")));
		weapon.systemMove(source);
		source.setEquippedWeapon(weapon);
		
		target = new GenericCreatureObject(3, "NPC", false);
		target.setFaction(ServerData.INSTANCE.getFactions().getFaction("neutral"));
		target.setGroupId(input.getNpcGroupId());
		
		if (input.isNpcInvulnerable()) {
			target.setOptionFlags(OptionFlag.INVULNERABLE);
		}
		
		registerService(new BuffService());
	}
	
	@Test
	public void testReceiveBuff() {
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
		
		boolean expected = input.isExpected();
		String caseName = input.getCaseName();
		
		assertEquals(caseName, expected, target.hasBuff(targetBuffName));
	}
	
	private static class Input {
		private final String caseName;
		private final long playerGroupId;	// Group ID of the player
		private final long npcGroupId;	// Group ID of the NPC
		private final boolean npcInvulnerable;
		private final boolean expected;
		
		public Input(String caseName, long playerGroupId, long npcGroupId, boolean npcInvulnerable, boolean expected) {
			this.caseName = caseName;
			this.playerGroupId = playerGroupId;
			this.npcGroupId = npcGroupId;
			this.npcInvulnerable = npcInvulnerable;
			this.expected = expected;
		}
		
		public String getCaseName() {
			return caseName;
		}
		
		public long getPlayerGroupId() {
			return playerGroupId;
		}
		
		public long getNpcGroupId() {
			return npcGroupId;
		}
		
		public boolean isNpcInvulnerable() {
			return npcInvulnerable;
		}
		
		public boolean isExpected() {
			return expected;
		}
		
		@Override
		public String toString() {
			return caseName;
		}
	}
}
