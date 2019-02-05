/***********************************************************************************
 * Copyright (c) 2019 /// Project SWG /// www.projectswg.com                       *
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

package com.projectswg.holocore.resources.support.objects.swg;

import com.projectswg.common.data.CRC;
import com.projectswg.common.data.encodables.mongo.MongoData;
import com.projectswg.common.data.encodables.mongo.MongoPersistable;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.encodables.tangible.PvpFlag;
import com.projectswg.holocore.resources.support.data.persistable.SWGObjectFactory;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.intangible.IntangibleObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.OptionFlag;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import com.projectswg.holocore.test.resources.GenericCreatureObject;
import org.bson.Document;
import org.bson.types.Binary;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestSWGPersistence {
	
	@SuppressWarnings("unchecked")
	private void assertContains(Map<String, Object> expected, Map<String, Object> actual) {
		for (Entry<String, Object> e : expected.entrySet()) {
			if (actual.get(e.getKey()) instanceof Map) {
				assertContains((Map<String, Object>) e.getValue(), (Map<String, Object>) actual.get(e.getKey()));
			} else if (actual.get(e.getKey()) instanceof Collection) {
				Collection<Object> expectedCollection = (Collection<Object>) e.getValue();
				Collection<Object> actualCollection = (Collection<Object>) actual.get(e.getKey());
				Assert.assertEquals("Key: '"+e.getKey()+"' Size mismatch.", expectedCollection.size(), actualCollection.size());
				Assert.assertTrue("Key: '"+e.getKey()+"' Expected <"+expectedCollection+"> but was <"+actualCollection+">", actualCollection.containsAll(expectedCollection));
			} else {
				Assert.assertEquals("Key: '" + e.getKey() + "'", e.getValue(), actual.get(e.getKey()));
			}
		}
	}
	
	private Document encode(MongoData data) {
		Document ret = new Document();
		for (Entry<String, Object> e : data.entrySet()) {
			if (e.getValue() instanceof MongoData)
				ret.put(e.getKey(), encode(((MongoData) e.getValue())));
			else
				ret.put(e.getKey(), e.getValue());
		}
		return ret;
	}
	
	private void test(SWGObject obj, Document expected) {
		Document saved = encode(SWGObjectFactory.save(obj, new MongoData()));
		assertContains(expected, saved);
		SWGObject gen = SWGObjectFactory.create(new MongoData(saved));
		Document saved2 = SWGObjectFactory.save(gen, new MongoData()).toDocument();
		assertContains(expected, saved2);
		Assert.assertEquals(saved, saved2);
	}
	
	private void testSWGObject(SWGObject obj) {
		System.out.println(obj);
		obj.getChildObjects().stream()
						.map(SWGObjectFactory::save)
						.map(MongoData::toDocument)
						.forEach(System.out::println);
		Document expected = map(
				"id", obj.getObjectId(),
				"template", obj.getTemplate(),
				"base1", map(
					"cashBalance", obj.getCashBalance(),
					"bankBalance", obj.getBankBalance()
				),
				"base3", map(
						"complexity", obj.getComplexity(),
						"stringId", map("file", obj.getStringId().getFile(), "key", obj.getStringId().getKey()),
						"objectName", obj.getObjectName(),
						"volume", obj.getVolume()
				),
				"base6", map(
					"detailStringId", map("file", obj.getDetailStringId().getFile(), "key", obj.getDetailStringId().getKey())
				),
				"location", map(
						"type", obj.getInstanceLocation().getInstanceType().name(),
						"number", obj.getInstanceLocation().getInstanceNumber(),
						"location", map(
								"terrain", obj.getTerrain().name(),
								"point", map(
										"x", obj.getX(),
										"y", obj.getY(),
										"z", obj.getZ()
								),
								"orientation", map(
										"x", obj.getLocation().getOrientationX(),
										"y", obj.getLocation().getOrientationY(),
										"z", obj.getLocation().getOrientationZ(),
										"w", obj.getLocation().getOrientationW()
								)
						)
				),
				"permissions", map("type", obj.getContainerPermissions().getType().name()),
				"attributes", map(),
				"serverAttributes", map(),
				"children", obj.getChildObjects().stream()
						.map(SWGObjectFactory::save)
						.map(MongoData::toDocument)
						.collect(Collectors.toList())
		);
		test(obj, expected);
	}
	
	private void testTangibleObject(TangibleObject obj) {
		testSWGObject(obj);
		
		Document expected = map(
				"appearance", Map.ofEntries(obj.getCustomization().entrySet().stream().map(e -> Map.entry(e.getKey(), e.getValue().getValue())).toArray(Entry[]::new)),
				"maxHitPoints", obj.getMaxHitPoints(),
				"components", obj.getComponents(),
				"condition", obj.getCondition(),
				"pvpFlags", obj.getPvpFlags().stream().mapToInt(PvpFlag::getBitmask).reduce(0, (a, b) -> a | b),
				"pvpStatus", obj.getPvpStatus().name(),
				"pvpFaction", obj.getPvpFaction().name(),
				"visibleGmOnly", obj.isVisibleGmOnly(),
				"objectEffects", new Binary(obj.getObjectEffects()),
				"optionFlags", obj.getOptionFlags().stream().map(OptionFlag::getFlag).reduce(0, (a, b) -> a | b)
		);
		test(obj, expected);
	}
	
	private void testCreatureObject(CreatureObject obj) {
		testTangibleObject(obj);
		
		Document expected = map(
				"base4", map(
						"accelPercent", obj.getAccelPercent(),
						"accelScale", obj.getAccelScale(),
						"movementPercent", obj.getMovementPercent(),
						"movementScale", obj.getMovementScale(),
						"slopeModPercent", obj.getSlopeModPercent(),
						"slopeModAngle", obj.getSlopeModPercent(),
						"waterModPercent", obj.getWaterModPercent(),
						"runSpeed", obj.getRunSpeed(),
						"walkSpeed", obj.getWalkSpeed(),
						"turnScale", obj.getTurnScale(),
						"totalLevelXp", obj.getTotalLevelXp()
				),
				"base6", map(
						"level", (int) obj.getLevel(),
						"levelHealthGranted", obj.getLevelHealthGranted(),
						"animation", obj.getAnimation(),
						"moodAnimation", obj.getMoodAnimation(),
						"guildId", obj.getGuildId(),
						"lookAtTargetId", obj.getLookAtTargetId(),
						"intendedTargetId", obj.getIntendedTargetId(),
						"moodId", (int) obj.getMoodId(),
						"costume", obj.getCostume(),
						"visible", obj.isVisible(),
						"shownOnRadar", obj.isShownOnRadar(),
						"beast", obj.isBeast(),
						"difficulty", obj.getDifficulty().name(),
						"hologramColor", obj.getHologramColor().name(),
						"equippedWeapon", obj.getEquippedWeapon() == null ? null : obj.getEquippedWeapon().getObjectId(),
						"maxAttributes", List.of(obj.getMaxHealth(), 0, obj.getMaxAction(), 0, obj.getMaxMind(), 0),
						"buffs", Map.ofEntries(obj.getBuffEntries(b -> true).map(b -> Map.entry(CRC.getString(b.getCrc()), MongoData.store(b).toDocument())).toArray(Entry[]::new))
				),
				"posture", obj.getPosture().name(),
				"race", obj.getRace().name(),
				"height", obj.getHeight(),
				"battleFatigue", obj.getBattleFatigue(),
				"ownerId", obj.getOwnerId(),
				"statesBitmask", obj.getStatesBitmask(),
				"factionRank", (int) obj.getFactionRank(),
				"skills", obj.getSkills(),
				"baseAttributes", List.of(obj.getBaseHealth(), 0, obj.getBaseAction(), 0, obj.getBaseMind(), 0)
		);
		test(obj, expected);
	}
	
	private void testIntangibleObject(IntangibleObject obj) {
		testSWGObject(obj);
		
		Document expected = map(
				"count", obj.getCount()
		);
		test(obj, expected);
	}
	
	private void testPlayerObject(PlayerObject obj) {
		testIntangibleObject(obj);
		
		Document expected = map(
				"base3", map(
						
				),
				"base6", map(
						
				),
				"base8", map(
						
				),
				"base9", map(
						"languageId", obj.getLanguageId(),
						"killMeter", obj,
						"petId", obj,
						"friendsList", obj,
						"ignoreList", obj,
						"petAbilities", obj,
						"activePetAbilities", obj
				),
				"biography", obj.getBiography()
		);
		test(obj, expected);
	}
	
	private void test(SWGObject obj) {
		switch (obj.getBaselineType()) {
			case PLAY:
				testPlayerObject((PlayerObject) obj);
				break;
			case ITNO:
				testIntangibleObject((IntangibleObject) obj);
				break;
			case CREO:
				testCreatureObject((CreatureObject) obj);
				break;
			case TANO:
				testTangibleObject((TangibleObject) obj);
				break;
			default:
				testSWGObject(obj);
				break;
		}
	}
	
	@Test
	public void testCreatureObject() {
		GenericCreatureObject creature = new GenericCreatureObject(1);
		creature.setObjectName("TEST");
		creature.setStringId(new StringId("file", "key"));
		creature.setDetailStf(new StringId("file-d", "key-d"));
		creature.setComplexity(2);
		creature.setVolume(3);
		
		test(creature);
	}
	
	private static Document map(Object ... values) {
		assert values.length % 2 == 0;
		Document map = new Document();
		for (int i = 0; i < values.length-1; i+=2) {
			assert values[i] instanceof String;
			if (values[i+1] instanceof Float)
				map.put((String) values[i], ((Float) values[i+1]).doubleValue());
			else
				map.put((String) values[i], values[i+1]);
		}
		return map;
	}
	
}
