FROM debian:stretch-slim

# Adds necessary Holocore files
RUN mkdir /holocore
ADD build/holocore/ /holocore
ADD serverdata/ /holocore/serverdata

# Sets up networking
EXPOSE 44463/tcp

# Sets up timezone - default timezone can be overridden by setting TZ environment variable.
RUN apt-get install -y tzdata
ENV TZ=UTC

# Sets up execution
WORKDIR /holocore
ENTRYPOINT ["/holocore/bin/java", "-m", "holocore/com.projectswg.holocore.ProjectSWG", "--database", "mongodb://pswg_game_database", "--dbName", "cu"]
