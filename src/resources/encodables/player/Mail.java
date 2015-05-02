package resources.encodables.player;

import resources.network.BaselineBuilder.Encodable;

public class Mail implements Encodable {
	private static final long serialVersionUID = 1L;

	private int id;
	private String sender;
	private long receiverId;
	private String subject;
	private String message;
	private byte status;
	private int timestamp;
	// TODO: Waypoint attachments
	
	public static final byte NEW = 0x4E;
	public static final byte READ = 0x52;
	public static final byte UNREAD = 0x55;
	
	public Mail(String sender, String subject, String message, long receiverId) {
		this.sender = sender;
		this.subject = subject;
		this.message = message;
		this.receiverId = receiverId;
		this.status = NEW;
	}
	
	@Override
	public byte [] encode() {
		// TODO Auto-generated method stub
		return new byte[0];
	}

	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}

	public String getSender() {
		return sender;
	}

	public long getReceiverId() {
		return receiverId;
	}

	public String getSubject() {
		return subject;
	}

	public String getMessage() {
		return message;
	}

	public byte getStatus() {
		return status;
	}

	public void setStatus(byte status) {
		this.status = status;
	}

	public int getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}
}
