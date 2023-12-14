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

plugins {
	application
	idea
	java
	kotlin("jvm") version "1.9.21"
	id("org.beryx.jlink") version "3.0.1"
}

val javaVersion = "21.0.1"
val javaMajorVersion = "21"
val kotlinTargetJdk = "21"
val holocoreLogLevel: String? by project

subprojects {
	ext {
		set("javaVersion", javaVersion)
		set("javaMajorVersion", javaMajorVersion)
		set("kotlinTargetJdk", kotlinTargetJdk)
	}
}

repositories {
	maven("https://dev.joshlarson.me/maven2")
	mavenCentral()
}

application {
	mainClass.set("com.projectswg.holocore.ProjectSWG")
	mainModule.set("holocore")
}

sourceSets {
	create("utility")
}

val utilityImplementation by configurations.getting {
	extendsFrom(configurations.implementation.get())
}

dependencies {
	implementation(project(":pswgcommon"))
	implementation(kotlin("stdlib"))
	implementation(kotlin("reflect"))
	implementation(group="org.mongodb", name="mongodb-driver-sync", version="3.12.2")
	implementation(group="me.joshlarson", name="fast-json", version="3.0.1")
	implementation(group="me.joshlarson", name="jlcommon-network", version="1.1.0")
	implementation(group="me.joshlarson", name="jlcommon-argparse", version="0.9.6")
	implementation(group="me.joshlarson", name="websocket", version="0.9.4")
	
	utilityImplementation(project(":"))
	utilityImplementation(project(":pswgcommon"))
	
	val junit5Version = "5.9.3"
	testImplementation(group="org.junit.jupiter", name="junit-jupiter-api", version= junit5Version)
	testRuntimeOnly(group="org.junit.jupiter", name="junit-jupiter-engine", version= junit5Version)
	testImplementation(group="org.junit.jupiter", name="junit-jupiter-params", version= junit5Version)
	testImplementation(group="org.testcontainers", name="mongodb", version="1.18.0")
	testRuntimeOnly(group="org.slf4j", name="slf4j-simple", version="1.7.36")

	testImplementation("com.tngtech.archunit:archunit-junit5:1.2.1")
}

idea {
	targetVersion = javaMajorVersion
    module {
        inheritOutputDirs = true
		excludeDirs.add(project.file("log"))
		excludeDirs.add(project.file("mongo_data"))
		excludeDirs.add(project.file("odb"))
    }
}

jlink {
//	addOptions("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages")
	addOptions("--ignore-signing-information")
	forceMerge("kotlin-stdlib")
	imageDir.set(layout.buildDirectory.dir("holocore"))
	imageZip.set(layout.buildDirectory.file("holocore.zip"))
	launcher {
		name = "holocore"
		jvmArgs = listOf()
		unixScriptTemplate = file("src/main/resources/jlink-unix-launch-template.txt")
	}
}

tasks.withType<Jar> {
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).configureEach {
	kotlinOptions {
		jvmTarget = kotlinTargetJdk
	}
	destinationDirectory.set(File(destinationDirectory.get().asFile.path.replace("kotlin", "java")))
}

tasks.create<JavaExec>("runDevelopment") {
	dependsOn(tasks.getByName("test"))

	enableAssertions = true
	classpath = sourceSets.main.get().runtimeClasspath
	mainClass.set("com.projectswg.holocore.ProjectSWG")

	if (holocoreLogLevel != null)
		args = listOf("--log-level", holocoreLogLevel!!)
}

tasks.create<JavaExec>("runProduction") {
	classpath = sourceSets.main.get().runtimeClasspath
	mainClass.set("com.projectswg.holocore.ProjectSWG")
	
	if (holocoreLogLevel != null)
		args = listOf("--log-level", holocoreLogLevel!!)
}

tasks.replace("run", JavaExec::class).apply {
	dependsOn(tasks.getByName("runDevelopment"))
}

tasks.create<JavaExec>("runClientdataConversion") {
	enableAssertions = true
	classpath = sourceSets["utility"].runtimeClasspath
	mainClass.set("com.projectswg.utility.ClientdataConvertAll")
}

tasks.withType<Test>().configureEach {
	useJUnitPlatform()
}
