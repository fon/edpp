package comm;

import java.net.InetAddress;

import comm.ProtocolMessage.Message;

/**
 * A protocol message along with the destination address
 * @author Xenofon Foukas
 *
 */
public class TransferableMessage {
	
	private Message message;
	private InetAddress address;
	private boolean sendReliably;

	/**
	 * Class constructor
	 * @param message The Message that will be sent to the remote node
	 * @param address The InetAddress of the remote node
	 */
	public TransferableMessage(Message message, InetAddress address, boolean sendReliably) {
		this.setMessage(message);
		this.address = address;
		this.setSendReliably(sendReliably);
	}
	
	public TransferableMessage(Message message, InetAddress address) {
		this.setMessage(message);
		this.address = address;
		this.setSendReliably(true);
	}

	/**
	 * 
	 * @return The transfered Message
	 */
	public Message getMessage() {
		return message;
	}

	/**
	 * 
	 * @param message The Message to be transfered
	 */
	public void setMessage(Message message) {
		this.message = message;
	}
	
	/**
	 * 
	 * @return The InetAddress of the remote node
	 */
	public InetAddress getAddress() {
		return address;
	}

	/**
	 * 
	 * @param address The InetAddress of the remote node
	 */
	public void setAddress(InetAddress address) {
		this.address = address;
	}

	public boolean getSendReliably() {
		return sendReliably;
	}

	public void setSendReliably(boolean sendReliably) {
		this.sendReliably = sendReliably;
	}
	
}
