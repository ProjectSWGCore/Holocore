package com.projectswg.holocore.resources.gameplay.conversation.requirements;

import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ActiveQuestRequirementTest {
	
	private Player player;
	private PlayerObject playerObject;
	private String questName;
	
	@Before
	public void setup() {
		player = mock(Player.class);
		playerObject = mock(PlayerObject.class);
		questName = "testquest";
		
		when(player.getPlayerObject()).thenReturn(playerObject);
	}
	
	@Test
	public void questMissingInJournal() {
		Integer task = null;
		boolean active = true;
		ActiveQuestRequirement requirement = new ActiveQuestRequirement(questName, active, task);
		
		when(playerObject.isQuestInJournal(questName)).thenReturn(false);
		
		boolean result = requirement.test(player);
		
		assertFalse(result);
	}
	
	@Test
	public void questComplete() {
		Integer task = null;
		boolean active = true;
		ActiveQuestRequirement requirement = new ActiveQuestRequirement(questName, active, task);
		
		when(playerObject.isQuestInJournal(questName)).thenReturn(true);
		when(playerObject.isQuestComplete(questName)).thenReturn(true);
		
		boolean result = requirement.test(player);
		
		assertFalse(result);
	}
	
	@Test
	public void taskActive() {
		Integer task = 1;
		boolean active = true;
		ActiveQuestRequirement requirement = new ActiveQuestRequirement(questName, active, task);
		
		when(playerObject.isQuestInJournal(questName)).thenReturn(true);
		when(playerObject.isQuestComplete(questName)).thenReturn(false);
		when(playerObject.getQuestActiveTasks(questName)).thenReturn(Collections.singleton(task));
		
		boolean result = requirement.test(player);
		
		assertTrue(result);
	}
	
	@Test
	public void taskActiveFlipped() {
		Integer task = 1;
		boolean active = false;
		ActiveQuestRequirement requirement = new ActiveQuestRequirement(questName, active, task);
		
		when(playerObject.isQuestInJournal(questName)).thenReturn(true);
		when(playerObject.isQuestComplete(questName)).thenReturn(false);
		when(playerObject.getQuestActiveTasks(questName)).thenReturn(Collections.singleton(task));
		
		boolean result = requirement.test(player);
		
		assertFalse(result);
	}
	
	@Test
	public void skipOptionalTaskCheck() {
		Integer task = null;
		boolean active = true;
		ActiveQuestRequirement requirement = new ActiveQuestRequirement(questName, active, task);
		
		when(playerObject.isQuestInJournal(questName)).thenReturn(true);
		when(playerObject.isQuestComplete(questName)).thenReturn(false);
		when(playerObject.getQuestActiveTasks(questName)).thenReturn(Collections.singleton(task));
		
		boolean result = requirement.test(player);
		
		assertTrue(result);
	}
}