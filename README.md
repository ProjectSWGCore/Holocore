![holocore.png](https://bitbucket.org/repo/norXdj/images/3473411954-holocore.png)

# Copyright (c) 2017 /// Project SWG /// www.projectswg.com #

ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on
July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.
Our goal is to create an emulator which will provide a server for players to
continue playing a game similar to the one they used to play. We are basing
it on the final publish of the game prior to end-game events.

--------------------------------------------------------------------------------

Holocore is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

Holocore is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with Holocore.  If not, see <http://www.gnu.org/licenses/>.

# Setting up a development environment #

Ready to help bring back an awesome MMORPG with your programming skills?

The following assumes that you're familiar with:

* Installing applications on your machine
* Command line interfaces
* Git
* Programming in general
* Java, to a lesser degree

Support for any of these topics cannot be expected of the development team.

## Java ##

This project currently requires Java 8. We may use newer versions in the future.

In order to compile the source code, you need a JDK installation on your machine. The `JAVA_HOME` environment variable
should point to the directory of the JDK!

## Gradle ##

This project uses Gradle as it's build tool of choice. You must install Gradle on your machine in order to build the
source code!

Building an uber jar: Run `gradle shadowJar` in the root project folder

Running the uber jar: Run `java -jar build/libs/holocore-all.jar` in the root project folder

## Forwarder ##

Holocore uses TCP for network communications, whereas SWG was programmed for UDP.  This adds numerous efficiencies with
long distance communications, but requires that a little more work is done on the client side.  If you are using the
launcher, you do not have to worry about this.  If you are not using the launcher, follow the guide
[here](https://bitbucket.org/projectswg/forwarder).

## Clientdata ##

Extract the following folders of every sku#_client.toc file to a new clientdata folder in the holocore directory:
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
A tool such as TRE Explorer is capable of opening the files.

## Contributing ##
1. Fork the repository
2. Clone the repository
3. Find something to do on [one of our Trello boards](https://trello.com/projectswg)
4. Create a new branch on your fork of holocore
5. Write code, commit and push it to your branch
6. Once ready, create a pull request with destination branch `quality_assurance` and source branch
`<your_branch_name>`
7. Your changes are reviewed and are merged, unless something is wrong
8. Once merged, your changes will be available on the official server soon
9. If you want to work on something else, go back to step 3

It's not required, but it's definitely a good idea to get in the same chat room as the rest of the
developers if you're serious about contributing. Request an invitation to the development chat by sending a message to
Undercova on the forums! Inactive/malicious members may be removed.