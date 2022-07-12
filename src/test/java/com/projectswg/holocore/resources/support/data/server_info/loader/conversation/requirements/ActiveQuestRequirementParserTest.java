package com.projectswg.holocore.resources.support.data.server_info.loader.conversation.requirements;

import com.projectswg.holocore.resources.gameplay.conversation.requirements.ActiveQuestRequirement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ActiveQuestRequirementParserTest {
	
	private ActiveQuestRequirementParser parser;
	
	@BeforeEach
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
				"task", Long.valueOf(expectedTask),
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