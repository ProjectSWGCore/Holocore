CREATE TABLE IF NOT EXISTS chat_log (
	timestamp		INTEGER NOT NULL,
	sender_id		INTEGER NOT NULL DEFAULT 0,
	sender_name		TEXT NOT NULL DEFAULT '',
	receiver_id		INTEGER NOT NULL DEFAULT 0,
	receiver_name	TEXT NOT NULL DEFAULT '',
	message_type	TEXT NOT NULL DEFAULT '', -- Specifies the type of message: MAIL, TELL, SYSTEM, SPATIAL, etc.
	range			TEXT NOT NULL DEFAULT '', -- The chat range of this message: PERSONAL, LOCAL, PLANET, GALAXY
	room			TEXT NOT NULL DEFAULT '', -- If applicable, the chat room this message was in. For Mails and Broadcasts this is empty
	subject			TEXT NOT NULL DEFAULT '', -- For mails, the subject of the mail
	message			TEXT NOT NULL DEFAULT '' -- For mails, body of the message. For everything else, the actual message
);
