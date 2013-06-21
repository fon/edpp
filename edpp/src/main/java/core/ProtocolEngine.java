package core;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import comm.MessageReceiver;
import comm.MessageSender;
import comm.OutgoingMessage;
import comm.ProtocolMessage.Message;

import network.Node;

public class ProtocolEngine implements Runnable {
	
	public static final int PROTOCOL_PORT = 11990;
	public static final long TIMEOUT = 1000;
	
	private static final int NTHREADS = 10;
	

	private Node localNode;
	private BlockingQueue<Message> incomingQueue;
	private BlockingQueue<OutgoingMessage> outgoingQueue;
	private MessageReceiver receiver;
	private MessageSender sender;
	private ExecutorService executor;
	private Map<String, Session> sessions;
	
	public ProtocolEngine(Node localNode) {
		this.localNode = localNode;
		
		incomingQueue = new LinkedBlockingQueue<Message>();
		receiver = new MessageReceiver(incomingQueue);
		
		outgoingQueue = new LinkedBlockingQueue<OutgoingMessage>();
		sender = new MessageSender(outgoingQueue);
		
		executor = Executors.newFixedThreadPool(NTHREADS);
		
		sessions = new ConcurrentHashMap<String, Session>();
	}

	@Override
	public void run() {
		Message incomingMessage;
		
		//Set up and run the basic threads
		Thread receivingThread = new Thread(receiver);
		receivingThread.setDaemon(true);
		Thread sendingThread = new Thread(sender);
		sendingThread.setDaemon(true);
		receivingThread.start();
		sendingThread.start();
		
		while (true) {
			try {
				incomingMessage = 
						incomingQueue.poll(TIMEOUT, TimeUnit.MILLISECONDS);
				if (incomingMessage == null) {				
					// TODO perform maintenance tasks
				} else {
					executor.execute(new MessageHandler(incomingMessage, sessions, localNode));
					//TODO handle message and perform maintenance tasks
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
}
