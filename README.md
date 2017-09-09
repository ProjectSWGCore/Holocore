# Introduction #

This is a Star Wars Galaxies server emulator for the Java Virtual Machine. The vision for this software is:

* Providing an experience that's reasonably close to the original game servers
* Easily expandable with new functionalities
* Good amount of configuration options for in-game features
* *Highly* efficient use of system resources and solid performance

The way we perform code reviews should reflect these points.

# Setting up a development environment #

Ready to help bring back an awesome MMORPG with your programming skills?

The following assumes that you're familiar with:

* Installing applications on your machine
* Command line interfaces
* VCSs, Git in particular
* Programming in general

Support for any of these topics cannot be expected of the development team.

## Java ##

In order to compile the source code, you need a JDK installation on your machine. The `JAVA_HOME` environment variable
should point to the directory of the JDK! It should be version 1.8 as minimum. You can see your installed Java version
by running `java -version`.

## Gradle ##

This project uses Gradle as its build tool of choice. You must install Gradle on your machine in order to build the
source code.

Building an executable: Run `gradle shadowJar` in the root project folder

Running the executable: Run `java -jar build/libs/holocore-all.jar` in the root project folder

## Forwarder ##

Holocore uses TCP for network communications, whereas SWG was programmed for UDP.  This adds numerous efficiencies with
long distance communications, but requires that a little more work is done on the client side.  If you are using the
launcher, you do not have to worry about this.  If you are not using the launcher, follow the guide
[here](https://bitbucket.org/projectswg/forwarder).

## Clientdata ##

This application reads a lot of information from the original game files. An installation of the game is therefore
required. Create a folder called clientdata in the root project directory. Extract the following folders of every
sku#_client.toc file to the clientdata folder:

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

## Contributing ##

1. Fork this repository
2. Clone the fork you just created, using `git clone`
3. Get the submodules using `git submodule update --init`
4. Find something to do on [one of our Trello boards](https://trello.com/projectswg)
5. Create a new branch on your fork of holocore
6. Write code, commit and push it to your branch
7. Once ready, create a pull request with destination branch `quality_assurance` and source branch
`<your_branch_name>`
8. Your changes are reviewed and are merged, unless something is wrong
9. Once merged, your changes will be available in future builds
10. If you want to work on something else, go back to step 4

# Contact #

We use HipChat for communication. Join [the public room](https://www.hipchat.com/g4xSy62ko) to get in touch!
