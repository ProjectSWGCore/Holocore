plugins {
	application
	idea
	java
	kotlin("jvm") version "1.7.10"
	id("org.beryx.jlink") version "2.25.0"
}

val javaVersion = "18.0.2"
val javaMajorVersion = "18"
val kotlinTargetJdk = "18"
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
	main {
		dependencies {
			implementation(project(":pswgcommon"))
			implementation(kotlin("stdlib"))
			implementation(kotlin("reflect"))
			
			implementation(group="org.xerial", name="sqlite-jdbc", version="3.30.1")
			implementation(group="org.mongodb", name="mongodb-driver-sync", version="3.12.2")
			implementation(group="me.joshlarson", name="fast-json", version="3.0.1")
			implementation(group="me.joshlarson", name="jlcommon-network", version="1.1.0")
			implementation(group="me.joshlarson", name="jlcommon-argparse", version="0.9.6")
			implementation(group="me.joshlarson", name="websocket", version="0.9.4")
		}
	}
	test {
		dependencies {
			val junit5Version = "5.8.1"
			testImplementation(group="org.junit.jupiter", name="junit-jupiter-api", version= junit5Version)
			testRuntimeOnly(group="org.junit.jupiter", name="junit-jupiter-engine", version= junit5Version)
			testImplementation(group="org.junit.jupiter", name="junit-jupiter-params", version= junit5Version)
		}
	}
	create("utility") {
		val utilityImplementation by configurations.getting {
			extendsFrom(configurations.implementation.get())
		}
		
		dependencies {
			utilityImplementation(project(":"))
			utilityImplementation(project(":pswgcommon"))
		}
	}
}

idea {
	targetVersion = javaMajorVersion
    module {
        inheritOutputDirs = true
    }
}

jlink {
//	addOptions("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages")
	addOptions("--ignore-signing-information")
	forceMerge("kotlin-stdlib")
	imageDir.set(file("$buildDir/holocore"))
	imageZip.set(file("$buildDir/holocore.zip"))
	launcher {
		name = "holocore"
		jvmArgs = listOf()
		unixScriptTemplate = file("src/main/resources/jlink-unix-launch-template.txt")
	}
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).configureEach {
	kotlinOptions {
		jvmTarget = kotlinTargetJdk
	}
	destinationDirectory.set(File(destinationDirectory.get().asFile.path.replace("kotlin", "java")))
}

tasks.getByName("run") {
	this as JavaExec
	dependsOn(tasks.getByName("test"))
	
	enableAssertions = true
	classpath = sourceSets.main.get().runtimeClasspath
	mainClass.set("com.projectswg.holocore.ProjectSWG")
	
	if (holocoreLogLevel != null)
		args = listOf("--log-level", holocoreLogLevel!!)
}

tasks.create<JavaExec>("runProduction") {
	this as JavaExec
	
	classpath = sourceSets.main.get().runtimeClasspath
	mainClass.set("com.projectswg.holocore.ProjectSWG")
	
	if (holocoreLogLevel != null)
		args = listOf("--log-level", holocoreLogLevel!!)
}

tasks.create<JavaExec>("runClientdataConversion") {
	enableAssertions = true
	classpath = sourceSets["utility"].runtimeClasspath
	mainClass.set("com.projectswg.utility.ClientdataConvertAll")
}

tasks.withType<Test>().configureEach {
	useJUnitPlatform()
}
