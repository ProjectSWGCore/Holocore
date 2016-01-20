package resources.network;

public enum DisconnectReason {
	TIMEOUT,
	OTHER_SIDE_TERMINATED,
	APPLICATION,
	NEW_CONNECTION_ATTEMPT,
	CONNECTION_REFUSED
}
