// Values are for the default localhost Keycloak instance, run by the docker-compose.yml in the root of this repo.
db.config.insertOne({
	package: "support.data.server_info.oidc",
	authorizationServerBaseURI: "http://localhost:8080/realms/swg",
	wellKnownConfigurationURI: ".well-known/openid-configuration",
	clientId: "holocore-localhost",
	clientSecret: "nrrtW0NqbNWXn62ii6218QHWbEhVGXaf"
});
