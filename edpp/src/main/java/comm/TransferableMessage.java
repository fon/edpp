package comm;

import java.net.InetAddress;

import comm.ProtocolMessage.Message;

public class TransferableMessage {
	
	private Message message;
	private InetAddress address;

	public TransferableMessage(Message message, InetAddress address) {
		this.setMessage(message);
		this.address = address;
	}

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}
	
	public InetAddress getAddress() {
		return address;
	}

	public void setAddress(InetAddress address) {
		this.address = address;
	}
	
}
