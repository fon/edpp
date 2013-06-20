package comm;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

import comm.ProtocolMessage.Message;

import core.ProtocolEngine;

public class MessageReceiver implements Runnable {

	private BlockingQueue<Message> incomingQueue;
	private ServerSocket ss;
	private Socket incomingSocket;
	
	public MessageReceiver(BlockingQueue<Message> incomingQueue) {
		this.incomingQueue = incomingQueue;
	}
	
	@Override
	public void run() {
		try {
			ss = new ServerSocket(ProtocolEngine.PROTOCOL_PORT);
			while (true) {
				incomingSocket = ss.accept();
				Message pm = Message.parseFrom(
						incomingSocket.getInputStream());
				incomingQueue.add(pm);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// TODO Must fix this to locate the exact case of the exception
			e.printStackTrace();
		}

	}

}
