package com.projectswg.holocore.resources.gameplay.player;

import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.player.PlayerFlags;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import com.projectswg.holocore.test.resources.GenericCreatureObject;
import com.projectswg.holocore.test.resources.GenericPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestActivePlayerPredicate {
	
	private final ActivePlayerPredicate predicate;
	
	public TestActivePlayerPredicate() {
		predicate = new ActivePlayerPredicate();
	}
	
	private Player player;
	private CreatureObject creatureObject;
	private PlayerObject playerObject;
	
	@BeforeEach
	public void setup() {
		player = new GenericPlayer();
		creatureObject = new GenericCreatureObject(1);
		playerObject = creatureObject.getPlayerObject();
		player.setCreatureObject(creatureObject);
	}
	
	@Test
	public void testAfk() {
		playerObject.setFlag(PlayerFlags.AFK);
		
		boolean actual = predicate.test(player);
		
		assertFalse(actual);
	}
	
	@Test
	public void testOffline() {
		playerObject.setFlag(PlayerFlags.LD);
		
		boolean actual = predicate.test(player);
		
		assertFalse(actual);
	}
	
	@Test
	public void testIncapacitated() {
		creatureObject.setPosture(Posture.INCAPACITATED);
		
		boolean actual = predicate.test(player);
		
		assertFalse(actual);
	}
	
	@Test
	public void testDead() {
		creatureObject.setPosture(Posture.DEAD);
		
		boolean actual = predicate.test(player);
		
		assertFalse(actual);
	}
	
	@Test
	public void testCloaked() {
		creatureObject.setVisible(false);
		
		boolean actual = predicate.test(player);
		
		assertFalse(actual);
	}
	
	@Test
	public void testInsidePrivateCell() {
		CellObject cell = new CellObject(2);
		cell.setPublic(false);
		creatureObject.systemMove(cell);
		
		boolean actual = predicate.test(player);
		
		assertFalse(actual);
	}
	
	@Test
	public void testActive() {
		boolean actual = predicate.test(player);
		
		assertTrue(actual);
	}
}
