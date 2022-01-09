![Banner](https://imgur.com/V14kDE5.png)

![License](https://img.shields.io/badge/license-GPLv3-blue.svg?longCache=true&style=flat)
![JDK](https://img.shields.io/badge/JDK-13-blue.svg?longCache=true&style=flat)
![Discord](https://img.shields.io/discord/373548910225915905.svg)

# Introduction #

This is *the* Star Wars Galaxies server emulator for the Java Virtual Machine. The vision for this software is:

* Providing an experience that's reasonably close to the original game servers
* Easily expandable with new functionality
* Good amount of configuration options for in-game features
* *Highly* efficient use of system resources and solid performance

The way we perform code reviews should reflect these points.

You can find detailed information on the [wiki](https://bitbucket.org/projectswg/holocore/wiki/Home).

# Setting up a development environment #

Ready to help bring back an awesome MMORPG with your programming skills?

## Java Development Kit ##

In order to compile the source code, you need a JDK installation on your machine. The `JAVA_HOME` environment variable
should point to the directory of the JDK! It should be version 13 as minimum. You can see your installed Java version
by running `java -version`.

## Submodules ##
The project uses submodules. Get them by running: git submodule update --init

## Clientdata ##

This application reads a lot of information from the original game files. An installation of the game is therefore
required. Create a folder called `clientdata` in the root project directory. Extract the following folders of every
sku#_client.toc file to the `clientdata` folder:

* abstract
* appearance
* creation
* customization
* datatables
* footprint
* interiorlayout
* misc
* object
* quest
* snapshot
* string
* terrain

Note that every TOC file won't necessarily have all of these folders! If they're present, extract them.
A tool such as TRE Explorer is capable of opening the files and extracting their contents.

You should end up with a structure that looks something like this:
```
holocore/
	clientdata/
		abstract/
		appearance/
		creation/
		customization/
		datatables/
		footprint/
		...
	gradle/
	res/
	serverdata/
	src/
	.gitignore
	.gitmodules
	LICENSE.txt
	...
```

## MongoDB ##
User information is read from a MongoDB database that can be run on any machine on your network. Default is the machine that Holocore is running on.

1. Create database: `use cu`
2. Create a user for Holocore: `db.createUser({user: "holocore", pwd: "pass", roles: []})`
3. Create your game user: `db.users.insert({username: "user", password: "pass", accessLevel: "dev", banned: false, characters: []})`

Enabling the Character Builder Terminals:
1. Switch to the relevant database: `use cu`
2. Enable the character builder: `db.config.insertOne({ "package": "support.data.dev", "characterBuilder": true })`

## Running Holocore ##
Compile and run Holocores main code using Gradle: `./gradlew run`

## Running automated tests ##
Compile and run Holocores unit tests using Gradle: `./gradlew test --info`
