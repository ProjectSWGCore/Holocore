# Contributing
Thanks for considering taking the time to contribute to Holocore! :heart:

## Setting up a development environment
This section will guide you through setting up a development environment for Holocore.

### Docker
The project uses [Docker](https://www.docker.com) to make it easy to run a MongoDB instance.

You can download Docker [here](https://www.docker.com/get-started).

### IDE
We recommend using [IntelliJ IDEA](https://www.jetbrains.com/idea/download) as your IDE. It's free and has great support for Gradle projects.

It will also automatically use the project's code style settings and add license headers to relevant files.
This greatly reduces the amount of friction when contributing.

### Java Development Kit
Assuming you are using IntelliJ IDEA, you can easily install the correct JDK version by opening the project in IntelliJ IDEA and clicking on the "Install JDK" link in the top right corner.

If you already have a JDK installed, but perhaps not the correct version, follow [this guide](https://www.jetbrains.com/help/idea/sdk.html#define-sdk) to download a JDK.

### Git submodules
The project uses [git submodules](https://git-scm.com/book/en/v2/Git-Tools-Submodules).

Clone them by running: `git submodule update --init`.

You may have to manually update the submodules in the future, if they become outdated: `git submodule update`.

### MongoDB
A MongoDB instance can be bootstrapped by running `docker-compose up -d`.

This will run and initialize a MongoDB instance with a default game server user.
* Username: user
* Password: pass

This will also run a web UI, which can be accessed in the browser at: http://localhost:8081

You can stop both with: `docker-compose down`.

### Running Holocore
In IntelliJ idea, run the **Start server (development)** run configuration.

**NOTE**: A MongoDB instance must be running for the server to start.

### Running automated tests
In IntelliJ idea, run the **Tests** run configuration.

## Your first contribution
If you're new to contributing to open source projects, we recommend reading [this guide](https://opensource.guide/how-to-contribute/).

### Finding something to work on
If you're looking for something to work on, check out the [issues page](https://github.com/ProjectSWGCore/Holocore/issues).

Some issues require coding, while others are more focused on documentation or other non-coding tasks.

## Reporting bugs
If you've found a bug, please report it as an issue on the [issues page](https://github.com/ProjectSWGCore/Holocore/issues).

Please include as much information as possible, such as:
* Steps to reproduce the bug
* What you expected to happen
* What actually happened
* Screenshots, if applicable
* Server logs, if applicable
