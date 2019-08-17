FROM debian:stretch-slim

# Adds necessary Holocore files
RUN mkdir /holocore
ADD build/holocore/ /holocore
ADD serverdata/ /holocore/serverdata

# Sets up networking
EXPOSE 44463/tcp

# Sets up execution
WORKDIR /holocore
ENTRYPOINT ["/holocore/bin/java", "-m", "holocore/com.projectswg.holocore.ProjectSWG", "--database", "mongodb://mongo", "--dbName", "nge"]
