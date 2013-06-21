package core;

import java.util.Map;

import comm.ProtocolMessage.Message;

import network.Node;

public class MessageHandler implements Runnable {

	private Message incomingMessage;
	private Node localNode;
	private Map<String, Session> sessions;
	
	public MessageHandler(Message incomingMessage, 
			Map<String, Session> sessions, Node localNode) {
		this.incomingMessage = incomingMessage;
		this.sessions = sessions;
		this.localNode = localNode;	
	}
	
	@Override
	public void run() {
		switch (incomingMessage.getType()) {
		case NEW:
			this.createNewSession();
			break;
		case INIT:
			this.handleInitMessage();
			break;
		case NEXT:
			this.handleNextMessage();
			break;
		case GOSSIP:
			this.handleGossipMessage();
			break;
		default:
			System.err.println("Received malformed message");
			break;
		}
	}
	
	private void createNewSession() {
		int numberOfExecutions = incomingMessage.getExecution();
		int numberOfRounds = incomingMessage.getRound();
		Session s = new Session(localNode, numberOfExecutions, numberOfRounds,true);
		s.createNewExecution(1);
		//TODO must send INIT messages to out neighbors
		sessions.put(s.getSessionId(), s);
	}
	
	private void handleInitMessage() {
		String sessionId = incomingMessage.getSession();
		Session s;
		
		//Check whether session already exists
		s = sessions.get(sessionId);
		if (s == null) {
			//TODO must create this session and send to out neighbors
		} else {
			//TODO must check execution etc
		}
	}
	
	private void handleNextMessage() {
		String sessionId = incomingMessage.getSession();
		Session s;
		
		//Check whether session already exists
		s = sessions.get(sessionId);
		if (s != null) {
			
		} else {
			//TODO must drop message
		}

	}
	
	private void handleGossipMessage() {
		String sessionId = incomingMessage.getSession();
		//TODO

	}

}
