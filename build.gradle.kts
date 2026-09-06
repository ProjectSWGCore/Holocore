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

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	application
	idea
	java
	kotlin("jvm") version "2.4.10"
	id("org.beryx.jlink") version "4.1.1"
}

group = "com.projectswg"
version = "1.0.0"
description = "ProjectSWG's SWG Emulator"

val javaVersion = JavaVersion.current()
val kotlinTargetJdk = JvmTarget.JVM_26
val junit5Version = "5.12.2"
val holocoreLogLevel = project.findProperty("holocoreLogLevel") as String?

subprojects {
	ext {
		set("javaVersion", javaVersion)
		set("junit5Version", junit5Version)
		set("kotlinTargetJdk", kotlinTargetJdk)
	}
}

repositories {
	maven("https://dev.joshlarson.me/maven2")
	mavenCentral()
}

java {
	modularity.inferModulePath.set(true)

	toolchain {
		languageVersion.set(JavaLanguageVersion.of(javaVersion.majorVersion))
	}
}

application {
	mainClass.set("com.projectswg.holocore.ProjectSWG")
	mainModule.set("holocore")
}

sourceSets {
	main {
		java {
			output.setResourcesDir(destinationDirectory.get())
		}
		kotlin.destinationDirectory = java.destinationDirectory
	}
	test {
		kotlin.destinationDirectory = java.destinationDirectory
	}
	create("utility") {
		kotlin.destinationDirectory = java.destinationDirectory
	}
}

tasks.named("processResources").configure { dependsOn("compileJava") }

val utilityImplementation = configurations.getByName("utilityImplementation") {
	extendsFrom(configurations.implementation.get())
}

dependencies {
	implementation(project(":pswgcommon"))
	implementation(kotlin("stdlib"))
	implementation(kotlin("reflect"))
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
	implementation("org.mongodb:mongodb-driver-sync:5.5.0")
	implementation("me.joshlarson:fast-json:3.0.1")
	implementation("me.joshlarson:jlcommon-network:1.1.0")
	implementation("me.joshlarson:jlcommon-argparse:0.9.6")
	implementation("me.joshlarson:websocket:0.9.4")
	val slf4jVersion = "1.7.36"
	runtimeOnly("org.slf4j:slf4j-jdk14:$slf4jVersion")

	utilityImplementation(project(":"))
	utilityImplementation(project(":pswgcommon"))

	testImplementation("org.junit.jupiter:junit-jupiter-api:$junit5Version")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit5Version")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.2")
	testImplementation("org.junit.jupiter:junit-jupiter-params:$junit5Version")
	testImplementation("org.testcontainers:mongodb:1.21.4")

	testImplementation("com.tngtech.archunit:archunit-junit5:1.5.0")
}

idea {
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
	compilerOptions {
		jvmTarget.set(kotlinTargetJdk)
	}
	destinationDirectory.set(File(destinationDirectory.get().asFile.path.replace("kotlin", "java")))
}

tasks.withType<JavaCompile>().configureEach {
	options.release.set(javaVersion.majorVersion.toInt())
}

tasks.register<JavaExec>("runDevelopment") {
	dependsOn(tasks.getByName("test"))

	enableAssertions = true
	classpath = sourceSets.main.get().runtimeClasspath
	mainClass.set("com.projectswg.holocore.ProjectSWG")

	if (holocoreLogLevel != null)
		args = listOf("--log-level", holocoreLogLevel)
}

tasks.register<JavaExec>("runProduction") {
	classpath = sourceSets.main.get().runtimeClasspath
	mainClass.set("com.projectswg.holocore.ProjectSWG")
	
	if (holocoreLogLevel != null)
		args = listOf("--log-level", holocoreLogLevel)
}

tasks.replace("run", JavaExec::class).apply {
	dependsOn(tasks.getByName("runDevelopment"))
}

tasks.register<JavaExec>("runClientdataConversion") {
	enableAssertions = true
	classpath = sourceSets["utility"].runtimeClasspath
	mainClass.set("com.projectswg.utility.ClientdataConvertAll")
}

tasks.withType<Test>().configureEach {
	useJUnitPlatform()

	testLogging {
		events = setOf(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
		exceptionFormat = TestExceptionFormat.FULL
	}
}

tasks.named("classes") {
	dependsOn("createRunScript")
}

tasks.register("createRunScript") {
	dependsOn("compileJava", "compileKotlin", "processResources")

	val mainJavaDestinationDirectory = sourceSets["main"].java.destinationDirectory
	val runScriptOutputFile = layout.buildDirectory.file("run")

	doLast {
		// Use the Java executable that Gradle is using
		val javaHome = System.getProperty("java.home")
		val javaExecutable = "$javaHome/bin/java"

		// Collect runtime classpath elements into a single string with path separator
		val runtimeClasspath = configurations["runtimeClasspath"].files.joinToString(File.pathSeparator) {
			it.absolutePath
		}

		// Get the output directory for the main/java source set
		val mainJavaOutputDir = mainJavaDestinationDirectory.get().asFile.absolutePath

		// Assemble the module-path to include both the runtime classpath and the main/java output directory
		val modulePath = "$runtimeClasspath${File.pathSeparator}$mainJavaOutputDir"

		// Assemble the command
		val command = "clear; JAVA_HOME=$javaHome ./gradlew classes && $javaExecutable -Xms1G -Xmx2G -XX:+UseZGC -XX:+ZGenerational -ea -p $modulePath -m holocore/com.projectswg.holocore.ProjectSWG --print-colors"

		// File to write the run command
		val outputFile = runScriptOutputFile.get().asFile
		outputFile.writeText(command)
		outputFile.setExecutable(true)
	}
}
