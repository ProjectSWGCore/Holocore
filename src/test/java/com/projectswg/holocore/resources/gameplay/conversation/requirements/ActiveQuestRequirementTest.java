package com.projectswg.holocore.resources.gameplay.conversation.requirements;

import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import com.projectswg.holocore.test.resources.GenericCreatureObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ActiveQuestRequirementTest {
	
	private Player player;
	private PlayerObject playerObject;
	private String questName;
	
	@BeforeEach
	public void setup() {
		GenericCreatureObject genericCreatureObject = new GenericCreatureObject(ObjectCreator.getNextObjectId());
		player = genericCreatureObject.getOwner();
		playerObject = genericCreatureObject.getPlayerObject();
		questName = "testquest";
	}
	
	@Test
	public void questMissingInJournal() {
		Integer task = null;
		boolean active = true;
		ActiveQuestRequirement requirement = new ActiveQuestRequirement(questName, active, task);
		
		boolean result = requirement.test(player);
		
		assertFalse(result);
	}
	
	@Test
	public void questComplete() {
		Integer task = null;
		boolean active = true;
		ActiveQuestRequirement requirement = new ActiveQuestRequirement(questName, active, task);
		playerObject.addQuest(questName);
		playerObject.completeQuest(questName);
		
		boolean result = requirement.test(player);
		
		assertFalse(result);
	}
	
	@Test
	public void taskActive() {
		int task = 1;
		boolean active = true;
		ActiveQuestRequirement requirement = new ActiveQuestRequirement(questName, active, task);
		playerObject.addQuest(questName);
		playerObject.addActiveQuestTask(questName, task);
		
		boolean result = requirement.test(player);
		
		assertTrue(result);
	}
	
	@Test
	public void taskActiveFlipped() {
		int task = 1;
		boolean active = false;
		ActiveQuestRequirement requirement = new ActiveQuestRequirement(questName, active, task);
		playerObject.addQuest(questName);
		playerObject.addActiveQuestTask(questName, task);
		
		boolean result = requirement.test(player);
		
		assertFalse(result);
	}
	
	@Test
	public void skipOptionalTaskCheck() {
		Integer task = null;
		boolean active = true;
		ActiveQuestRequirement requirement = new ActiveQuestRequirement(questName, active, task);
		playerObject.addQuest(questName);
		
		boolean result = requirement.test(player);
		
		assertTrue(result);
	}
}