# Running your own server
Ready to run our own Holocore server? Great! This document will guide you through the process.

## Warning
Holocore is currently still in early development, and not yet ready for public use. Breaking changes may occur at any time.

If you're interested in running your own Holocore server anyway, read on.

## Docker
The only supported way to deploy Holcore is using Docker.
We regularly push new Docker images to [GitHub Packages](https://github.com/ProjectSWGCore/Holocore/pkgs/container/Holocore%2Fholocore).

## User database
User databases are used to authenticate players and generate session tokens. It's a required component for the game server to function.

There are currently two user database implementations available:
### 1. An OpenID Connect implementation
The recommended option for **production use**.
Activate the integration in MongoDB like so, replacing values as needed:
```js
db.config.insertOne({
	package: "support.data.server_info.oidc",
	authorizationServerBaseURI: "http://localhost:8080/realms/swg",
	wellKnownConfigurationURI: ".well-known/openid-configuration",
	clientId: "holocore-localhost",
	clientSecret: "nrrtW0NqbNWXn62ii6218QHWbEhVGXaf"
});
```

Optional information can be added to the `userprofile` of a user for an optimal experience. These are:
1. `banned` (boolean)
2. `roles` (array of strings, representing which roles the user has on the game server)

   These can be the following, ordered from least to most privileges:
   1. PLAYER
   2. WARDEN
   3. QA		
   4. CSR		
   5. LEAD_QA
   6. LEAD_CSR
   7. DEV
### 2. A MongoDB implementation
The recommended option for **testing purposes**.
Simply create a user for yourself in MongoDB:
```js
db.users.insert({
	username: "user",
	password: "pass",
	accessLevel: "dev",
	banned: false, 
	characters: []
});
```

## Game database
Game databases are used to store all game data, such as items, characters, etc. It's a required component for the game server to function.
The only supported game database implementation is MongoDB.
