package core;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import comm.MessageReceiver;
import comm.MessageSender;
import comm.OutgoingMessage;
import comm.ProtocolMessage.Message;

import network.Node;

public class ProtocolEngine implements Runnable {
	
	public static final int PROTOCOL_PORT = 11990;

	private Node localNode;
	private BlockingQueue<Message> incomingQueue;
	private BlockingQueue<OutgoingMessage> outgoingQueue;
	private MessageReceiver receiver;
	private MessageSender sender;
	
	public ProtocolEngine(Node localNode) {
		this.localNode = localNode;
		
		incomingQueue = new LinkedBlockingQueue<Message>();
		receiver = new MessageReceiver(incomingQueue);
		
		outgoingQueue = new LinkedBlockingQueue<OutgoingMessage>();
		sender = new MessageSender(outgoingQueue);
		
	}

	@Override
	public void run() {
		//Set up and run the basic threads
		Thread receivingThread = new Thread(receiver);
		receivingThread.setDaemon(true);
		Thread sendingThread = new Thread(sender);
		sendingThread.setDaemon(true);
		receivingThread.start();
		sendingThread.start();
		
	}
	
}
