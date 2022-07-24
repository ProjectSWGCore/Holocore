![Banner](https://i.imgur.com/vIaNzCm.png)

![License](https://img.shields.io/badge/license-GPLv3-blue.svg?longCache=true&style=flat)
![Discord](https://img.shields.io/discord/373548910225915905.svg)

# Introduction #
This is a Star Wars Galaxies server emulator, targeted at the Combat Upgrade (CU)
era of the game.

# Vision
* Providing an experience that's reasonably close to the original, out of the box
* Easily expandable with new functionality
* Good amount of configuration options for in-game features
* Efficient use of system resources and solid performance

# Setting up a development environment #
Ready to help bring back an awesome MMORPG with your programming skills?

## Java Development Kit ##
In order to compile the source code, you need a JDK installation on your machine. The `JAVA_HOME` environment variable
should point to the directory of the JDK! It should be version **18** as minimum. You can see your installed Java version
by running `java -version`.

## Submodules ##
The project uses submodules. Get them by running: git submodule update --init

## MongoDB ##
User information is read from a MongoDB database that can be run on any machine on your network. Default is the machine that Holocore is running on.

1. Create database: `use cu`
2. Create your game user: `db.users.insert({username: "user", password: "pass", accessLevel: "dev", banned: false, characters: []})`

Enabling the Character Builder Terminals:
1. Switch to the relevant database: `use cu`
2. Enable the character builder: `db.config.insertOne({ "package": "support.data.dev", "characterBuilder": true })`

## Running Holocore ##
Compile and run Holocores main code using Gradle: `./gradlew run`

## Running automated tests ##
Compile and run Holocores automated tests using Gradle: `./gradlew test --info`

# Running your own server #
If you're interesting in running your own server, you should use the provided
Docker image.
