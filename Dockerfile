FROM eclipse-temurin:21.0.1_12-jdk-alpine AS builder
WORKDIR /holocore
ADD . /holocore

RUN ./gradlew --no-daemon jlink

FROM alpine:3.18.3 AS runner

# Adds necessary Holocore files
RUN mkdir /holocore
COPY --from=builder /holocore/build/holocore/ /holocore
ADD serverdata/ /holocore/serverdata

# Sets up networking
EXPOSE 44463/tcp

# Sets up timezone - default timezone can be overridden by setting TZ environment variable.
RUN apk add --no-cache tzdata
ENV TZ=UTC

# Sets up execution
WORKDIR /holocore
ENTRYPOINT ["/holocore/bin/java", "-m", "holocore/com.projectswg.holocore.ProjectSWG", "--database", "mongodb://pswg_game_database", "--dbName", "cu"]
