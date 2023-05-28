/***********************************************************************************
 * Copyright (c) 2023 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.architecture

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods
import com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING
import me.joshlarson.jlcommon.control.Intent
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Manager
import me.joshlarson.jlcommon.control.Service

@AnalyzeClasses(packages = ["com.projectswg.holocore"], importOptions = [DoNotIncludeTests::class])
class ArchitectureTest {

	@ArchTest
	val managerSuffixApplied: ArchRule = classes().that()
		.areAssignableTo(Manager::class.java)
		.should()
		.haveSimpleNameEndingWith("Manager")
		.because("they should be easy to find")

	@ArchTest
	val managerSuffixReserved: ArchRule = classes().that()
		.haveSimpleNameEndingWith("Manager")
		.should()
		.beAssignableTo(Manager::class.java)
		.because("using the suffix for other types of classes would cause confusion")

	@ArchTest
	val serviceSuffixApplied: ArchRule = classes().that()
		.areAssignableTo(Service::class.java)
		.should()
		.haveSimpleNameEndingWith("Service")
		.because("they should be easy to find")

	@ArchTest
	val serviceSuffixReserved: ArchRule = classes().that()
		.haveSimpleNameEndingWith("Service")
		.should()
		.beAssignableTo(Service::class.java)
		.because("using the suffix for other types of classes would cause confusion")

	@ArchTest
	val intentSuffixApplied: ArchRule = classes().that()
		.areAssignableTo(Intent::class.java)
		.should()
		.haveSimpleNameEndingWith("Intent")
		.because("they should be easy to find")

	@ArchTest
	val intentSuffixReserved: ArchRule = classes().that()
		.haveSimpleNameEndingWith("Intent")
		.should()
		.beAssignableTo(Intent::class.java)
		.because("using the suffix for other types of classes would cause confusion")

	@ArchTest
	val privateIntentHandlers: ArchRule = methods().that()
		.areAnnotatedWith(IntentHandler::class.java)
		.should()
		.bePrivate()
		.because("only the framework is supposed to invoke these methods")

	@ArchTest
	val intentHandlersInServices: ArchRule = methods().that()
		.areAnnotatedWith(IntentHandler::class.java)
		.should()
		.beDeclaredInClassesThat()
		.areAssignableTo(Service::class.java)
		.because("the method annotation only works in Service classes")

	@ArchTest
	val noJavaUtilLogging: ArchRule = NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING.because("we use logging from jlcommon")

}