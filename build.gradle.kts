import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
	application
	idea
	java
	kotlin("jvm") version "1.3.70"
	id("com.github.johnrengelman.shadow") version "5.2.0"
	id("org.javamodularity.moduleplugin") version "1.5.0"
	id("org.beryx.jlink") version "2.17.2"
}

val javaMajorVersion = "13"
val kotlinTargetVersion = "12"

application {
	mainClassName = "holocore/com.projectswg.holocore.ProjectSWG"
}

repositories {
    jcenter()
}

sourceSets {
	main {
		dependencies {
			implementation(project(":pswgcommon"))
			implementation(kotlin("stdlib"))
			
			implementation(group="org.jetbrains.kotlinx", name="kotlinx-coroutines-core", version="1.3.5")
			implementation(group="org.xerial", name="sqlite-jdbc", version="3.30.1")
			implementation(group="org.mariadb.jdbc", name="mariadb-java-client", version="2.5.4")
			implementation(group="org.mongodb", name="mongodb-driver-sync", version="3.12.2")
			implementation(group="me.joshlarson", name="fast-json", version="3.0.0")
			implementation(group="me.joshlarson", name="jlcommon-network", version="1.0.0")
			implementation(group="commons-cli", name="commons-cli", version="1.4")
		}
	}
	test {
		dependencies {
			implementation(group="junit", name="junit", version="4.12")
		}
	}
	create("display") {
		dependencies {
			implementation(project(":pswgcommon"))
		}
	}
	create("utility") {
		dependencies {
			implementation(project(":pswgcommon"))
			implementation(group="org.jetbrains.kotlin", name="kotlin-stdlib", version="1.3.50")
			implementation(group="org.xerial", name="sqlite-jdbc", version="3.23.1")
			implementation(group="org.mongodb", name="mongodb-driver-sync", version="3.12.2")
			implementation(group="me.joshlarson", name="fast-json", version="3.0.0")
		}
	}
	create("integration") {
		dependencies {
			implementation(project(":pswgcommon"))
			implementation(project(":client-holocore"))
			implementation(group="org.xerial", name="sqlite-jdbc", version="3.23.1")
			implementation(group="org.mongodb", name="mongodb-driver-sync", version="3.12.2")
			implementation(group="junit", name="junit", version="4.12")
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
	imageDir.set(file("$buildDir/holocore"))
	imageZip.set(file("$buildDir/holocore.zip"))
	launcher {
		name = "holocore"
		jvmArgs = listOf()
		unixScriptTemplate = file("src/main/resources/jlink-unix-launch-template.txt")
	}
}

tasks.named<ShadowJar>("shadowJar") {
	archiveBaseName.set("Holocore")
	archiveClassifier.set("")
	archiveVersion.set("")
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).configureEach {
	kotlinOptions {
		jvmTarget = kotlinTargetVersion
	}
}

tasks.create<ShadowJar>("CreateConvertLoginJar") {
	archiveBaseName.set("ConvertLogin")
	archiveClassifier.set(null as String?)
	archiveVersion.set(null as String?)
	manifest.attributes["Main-Class"] = "com.projectswg.utility.ConvertLogin"
	from(sourceSets.getByName("utility").output)
	configurations = listOf(project.configurations.getByName("utilityRuntime"))
	exclude("META-INF/INDEX.LIST", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

tasks.create<ShadowJar>("CreatePacketCaptureProcessor") {
	archiveBaseName.set("PacketCaptureProcessor")
	archiveClassifier.set(null as String?)
	archiveVersion.set(null as String?)
	manifest.attributes["Main-Class"] = "com.projectswg.utility.packets.ProcessPacketCapture"
	from(sourceSets.getByName("utility").output)
	configurations = listOf(project.configurations.getByName("utilityRuntime"))
	exclude("META-INF/INDEX.LIST", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}
