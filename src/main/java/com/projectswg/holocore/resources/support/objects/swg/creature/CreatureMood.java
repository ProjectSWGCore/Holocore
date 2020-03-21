/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.support.objects.swg.creature;

public enum CreatureMood {
	NONE			((byte) 0),
	ABSENTMINDED	((byte) 1),
	ADVENTUROUS		((byte) 136),
	ALERT			((byte) 144),
	AMAZED			((byte) 2),
	AMBIVALENT		((byte) 164),
	AMUSED			((byte) 3),
	ANGRY			((byte) 4),
	ANNOYED			((byte) 137),
	APPROVING		((byte) 5),
	BELLIGERENT		((byte) 170),
	BITTER			((byte) 6),
	BLOODTHIRSTY	((byte) 7),
	BRAVE			((byte) 8),
	BUBBLY			((byte) 151),
	CALLOUS			((byte) 9),
	CALMDOWN		((byte) 140),
	CAREFUL			((byte) 10),
	CARELESS		((byte) 11),
	CASUAL			((byte) 12),
	CHEERFUL		((byte) 161),
	CLINICAL		((byte) 13),
	COCKY			((byte) 14),
	COLD			((byte) 15),
	COMPASSIONATE	((byte) 16),
	CONDENSCENDING	((byte) 17),
	CONFIDENT		((byte) 18),
	CONFUSED		((byte) 19),
	CONTEMPOUS		((byte) 181),
	CONTENT			((byte) 20),
	COURTLY			((byte) 21),
	COY				((byte) 22),
	CROTCHETY		((byte) 147),
	CRUDE			((byte) 23),
	CRUEL			((byte) 24),
	CURIOUS			((byte) 25),
	CYNICAL			((byte) 26),
	DAINTY			((byte) 174),
	DEFENSIVE		((byte) 27),
	DEPRESSED		((byte) 28),
	DEVIOUS			((byte) 29),
	DIGNIFIED		((byte) 175),
	DIMWITTED		((byte) 30),
	DIPLOMATIC		((byte) 182),
	DISAPPOINTED	((byte) 31),
	DISCREET		((byte) 32),
	DISDAINFUL		((byte) 180),
	DISGRUNTLED		((byte) 33),
	DISGUSTED		((byte) 34),
	DISMAYED		((byte) 35),
	DISORIENTATED	((byte) 36),
	DISTRACTED		((byte) 37),
	DOUBTFUL		((byte) 38),
	DRAMATIC		((byte) 39),
	DREAMY			((byte) 40),
	DRUNK			((byte) 41),
	EARNEST			((byte) 42),
	ECSTATIC		((byte) 43),
	EMBARRASSED		((byte) 44),
	EMOTIONAL		((byte) 157),
	EMOTIONLESS		((byte) 162),
	EMPHATIC		((byte) 45),
	ENCOURAGING		((byte) 46),
	ENRAGED			((byte) 168),
	ENTHUSIASTIC	((byte) 47),
	ENVIOUS			((byte) 165),
	EVIL			((byte) 48),
	EXASPERATED		((byte) 49),
	EXHAUSTED		((byte) 128),
	EXHUBERANT		((byte) 50),
	FANATICAL		((byte) 51),
	FASTIDIOUS		((byte) 172),
	FEARFUL			((byte) 167),
	FIRM			((byte) 187),
	FORGIVE			((byte) 52),
	FRIENDLY		((byte) 129),
	FRUSTRATED		((byte) 53),
	GLOOMY			((byte) 163),
	GOOFY			((byte) 178),
	GRUMPY			((byte) 155),
	GUILTY			((byte) 54),
	HAPPY			((byte) 55),
	HAUGHTY			((byte) 176),
	HEROIC			((byte) 152),
	HONEST			((byte) 56),
	HOPEFUL			((byte) 57),
	HOPELESS		((byte) 58),
	HUMBLE			((byte) 59),
	HUNGRY			((byte) 142),
	HURRIED			((byte) 185),
	HYSTERICAL		((byte) 60),
	IMPLORING		((byte) 61),
	INDIFFERENT		((byte) 62),
	INDIGNANT		((byte) 63),
	INNOCENT		((byte) 133),
	INTERESTED		((byte) 64),
	JEALOUS			((byte) 65),
	JOYFUL			((byte) 66),
	LAZY			((byte) 131),
	LOFTY			((byte) 67),
	LOGICAL			((byte) 156),
	LOUD			((byte) 68),
	LOVING			((byte) 69),
	LUSTFUL			((byte) 70),
	MALEVOLENT		((byte) 184),
	MEAN			((byte) 71),
	MISCHEVIOUS		((byte) 72),
	NERVOUS			((byte) 73),
	NEUTRAL			((byte) 74),
	NICE			((byte) 160),
	OBNOXIOUS		((byte) 171),
	OBSCURE			((byte) 177),
	OFFENDED		((byte) 75),
	OPTIMISTIC		((byte) 76),
	PAINFUL			((byte) 149),
	PANICKED		((byte) 159),
	PATIENT			((byte) 186),
	PEDANTIC		((byte) 77),
	PERTURBED		((byte) 138),
	PESSIMISTIC		((byte) 78),
	PETULANT		((byte) 79),
	PHILOSOPHICAL	((byte) 80),
	PITYING			((byte) 81),
	PLAYFUL			((byte) 82),
	POLITE			((byte) 83),
	POMPOUS			((byte) 84),
	PROUD			((byte) 85),
	PROVOCATIVE		((byte) 86),
	PUZZLED			((byte) 87),
	QUIET			((byte) 153),
	REGRETFUL		((byte) 88),
	RELAXED			((byte) 146),
	RELIEVED		((byte) 89),
	RELUCTANT		((byte) 90),
	REMORSEFUL		((byte) 154),
	RESIGNED		((byte) 91),
	RESPECTFUL		((byte) 92),
	ROMANTIC		((byte) 93),
	RUDE			((byte) 94),
	SAD				((byte) 95),
	SARCASTIC		((byte) 96),
	SCARED			((byte) 97),
	SCOLDING		((byte) 98),
	SCORNFUL		((byte) 99),
	SEDATE			((byte) 139),
	SERIOUS			((byte) 100),
	SHAMELESS		((byte) 101),
	SHEEPISH		((byte) 169),
	SHIFTY			((byte) 145),
	SHOCKED			((byte) 102),
	SHY				((byte) 103),
	SILLY			((byte) 179),
	SINCERE			((byte) 104),
	SLEEPY			((byte) 105),
	SLY				((byte) 106),
	SMUG			((byte) 107),
	SNOBBY			((byte) 108),
	SORRY			((byte) 109),
	SPITEFUL		((byte) 110),
	SQUEAMISH		((byte) 173),
	STUBBORN		((byte) 111),
	SUFFERING		((byte) 141),
	SULLEN			((byte) 112),
	SURLY			((byte) 148),
	SURPRISED		((byte) 138),
	SUSPICIOUS		((byte) 113),
	TAUNTING		((byte) 114),
	TERRIFIED		((byte) 115),
	THANKFUL		((byte) 116),
	THIRSTY			((byte) 143),
	THOUGHTFUL		((byte) 117),
	TIMID			((byte) 130),
	TIRED			((byte) 127),
	TOLERANT		((byte) 118),
	TROUBLED		((byte) 158),
	UNCERTAIN		((byte) 119),
	UNDEAD			((byte) 188),
	UNHAPPY			((byte) 120),
	UNWILLING		((byte) 121),
	VENGEFUL		((byte) 166),
	WARM			((byte) 122),
	WARY			((byte) 183),
	WINY			((byte) 123),
	WICKED			((byte) 124),
	WISE			((byte) 134),
	WISTFUL			((byte) 125),
	WORRIED			((byte) 126),
	WOUNDED			((byte) 150),
	YOUTHFUL		((byte) 135);
	
	
	private byte mood;
	
	CreatureMood(byte mood) {
		this.mood = mood;
	}
	
	public byte getMood() {
		return mood;
	}
	
	public static CreatureMood getForMood(byte mood) {
		for (CreatureMood d : values())
			if (d.getMood() == mood)
				return d;
		return NONE;
	}
}
