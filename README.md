![License](https://img.shields.io/badge/license-GPLv3-blue.svg?longCache=true&style=flat)
![JDK](https://img.shields.io/badge/JDK-9-blue.svg?longCache=true&style=flat)
![Discord](https://img.shields.io/discord/373548910225915905.svg)
![Bitbucket open pull requests](https://img.shields.io/bitbucket/pr/projectswg/holocore.svg)

![Banner](https://imgur.com/V14kDE5.png)

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

The following assumes that you're familiar with:

* Installing applications on your machine
* Command line interfaces
* VCSs, Git in particular
* Programming in general

## Java Development Kit ##

In order to compile the source code, you need a JDK installation on your machine. The `JAVA_HOME` environment variable
should point to the directory of the JDK! It should be version 9 as minimum. You can see your installed Java version
by running `java -version`.

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

## Gradle ##

This project uses Gradle as its build tool of choice. You must install Gradle on your machine in order to build the
source code.

Compile and run Holocores unit tests using Gradle: `gradle test --info` (Might fail if you haven't extracted clientdata yet)
Compile and run Holocores main code using Gradle: `gradle run`

## Forwarder ##

Holocore uses TCP for network communications, whereas SWG was programmed for UDP.  This adds numerous efficiencies with
long distance communications, but requires that a little more work is done on the client side.  If you are using the
launcher, you do not have to worry about this.  If you are not using the launcher, follow the guide
[here](https://bitbucket.org/projectswg/forwarder).

## Credentials ##

Default credentials are currently created. Your username is `holocore` and your password is `password`. This user has the
highest admin level assigned to it.

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
