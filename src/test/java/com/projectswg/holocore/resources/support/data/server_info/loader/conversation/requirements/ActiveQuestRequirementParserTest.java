package com.projectswg.holocore.resources.support.data.server_info.loader.conversation.requirements;

import com.projectswg.holocore.resources.gameplay.conversation.requirements.ActiveQuestRequirement;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class ActiveQuestRequirementParserTest {
	
	private ActiveQuestRequirementParser parser;
	
	@Before
	public void setup() {
		parser = new ActiveQuestRequirementParser();
	}
	
	@Test
	public void testAllParametersSet() {
		String expectedQuestName = "testquest";
		Integer expectedTask = 1234;
		boolean expectedActive = true;
		
		Map<String, Object> args = Map.of(
				"quest", expectedQuestName,
				"task", expectedTask,
				"active", expectedActive
		);
		
		ActiveQuestRequirement requirement = parser.parse(args);
		
		assertEquals(expectedQuestName, requirement.getQuestName());
		assertEquals(expectedTask, requirement.getTask());
		assertEquals(expectedActive, requirement.isActive());
	}
	
	@Test
	public void testTaskOptional() {
		String expectedQuestName = "testquest";
		Integer expectedTask = null;
		boolean expectedActive = true;
		
		Map<String, Object> args = Map.of(
				"quest", expectedQuestName,
				"active", expectedActive
		);
		
		ActiveQuestRequirement requirement = parser.parse(args);
		
		assertEquals(expectedQuestName, requirement.getQuestName());
		assertEquals(expectedTask, requirement.getTask());
		assertEquals(expectedActive, requirement.isActive());
	}
}