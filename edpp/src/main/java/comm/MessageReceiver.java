package comm;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

import comm.ProtocolMessage.Message;

import core.ProtocolController;

public class MessageReceiver implements Runnable {

	private BlockingQueue<TransferableMessage> incomingQueue;
	private ServerSocket ss;
	private Socket incomingSocket;
	
	public MessageReceiver(BlockingQueue<TransferableMessage> incomingQueue) {
		this.incomingQueue = incomingQueue;
	}
	
	@Override
	public void run() {
		InetAddress address;
		try {
			ss = new ServerSocket(ProtocolController.PROTOCOL_PORT);
			while (true) {
				incomingSocket = ss.accept();
				address = incomingSocket.getInetAddress();
				Message pm = Message.parseFrom(
						incomingSocket.getInputStream());
				incomingQueue.add(new TransferableMessage(pm, address));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// TODO Must fix this to locate the exact case of the exception
			e.printStackTrace();
		}

	}

}
