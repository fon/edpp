package core;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import util.Phase;
import util.PlainNeighbor;
import util.PlainNeighborsTable;
import util.TimedNeighbor;

import comm.MessageBuilder;
import comm.TransferableMessage;
import comm.ProtocolMessage.Message;

import network.Node;

public class MessageHandlerTask implements Runnable {

	private TransferableMessage incomingMessage;
	private Node localNode;
	private Map<String, Session> sessions;
	private BlockingQueue<TransferableMessage> outQueue;
	
	public MessageHandlerTask(TransferableMessage incomingMessage, 
			Map<String, Session> sessions, Node localNode,
			BlockingQueue<TransferableMessage> outQueue) {
		
		this.incomingMessage = incomingMessage;
		this.sessions = sessions;
		this.localNode = localNode;
		this.outQueue = outQueue;
	}
	
	@Override
	public void run() {
		Message m = incomingMessage.getMessage();
		switch (m.getType()) {
		case NEW:
			this.createNewSession(true);
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
	
	private void createNewSession(boolean isInitiator) {
		Execution initExecution;
		Message message;
		double valueToSend;
		PlainNeighborsTable pnt;
		InetAddress address;
		Message m = incomingMessage.getMessage();
		
		int numberOfExecutions = m.getExecution();
		int numberOfRounds = m.getRound();
		Session s = new Session(localNode, numberOfExecutions, 
				numberOfRounds, isInitiator);
		initExecution = s.createNewExecution(1);
		
		if (!isInitiator) {
			int currentRound = initExecution.getCurrentRound();
			initExecution.addValToRound(m.getVal(), currentRound);
		}
		
		pnt = initExecution.getOutNeighbors();
		
		synchronized (pnt) {
			for (PlainNeighbor n : pnt) {
				address = n.getAddress();
				valueToSend = initExecution.getCurrentValue() * n.getWeight();
				message = MessageBuilder.buildInitMessage(localNode.getLocalId().toString(),
						s.getSessionId(), numberOfExecutions, numberOfRounds, valueToSend);
				outQueue.add(new TransferableMessage(message, address));
			}
		}
		
		sessions.put(s.getSessionId(), s);
	}

	private void handleInitMessage() {
		Session s;
		Execution e;
		
		Message m = incomingMessage.getMessage();
		
		String sessionId = m.getSession();
		int executionNumber = m.getExecution();
		
		// Check whether session already exists
		s = sessions.get(sessionId);
		if (s == null) {
			createNewSession(false);
			addToInNeighborsTable();
		} else {
			// Check whether the execution exists
			e = s.getExecution(executionNumber);
			if (e == null) {
				createNewSession(false);
				addToInNeighborsTable();
			} else {
				/*
				 * Session and execution both exist.
				 * Check whether we are still in the INIT phase
				 * and add node to in-neighbors and val to current round
				 */
				if (e.getPhase() == Phase.INIT && (!e.hasTerminated())) {
					int currentRound = e.getCurrentRound();
					e.addValToRound(m.getVal(), currentRound);
					addToInNeighborsTable();
				}
			}
		}
	}
	
	private void handleNextMessage() {
		
		Message m = incomingMessage.getMessage();
		String sessionId = m.getSession();
		Session s;
		Execution e;
		
		//Check whether session already exists
		s = sessions.get(sessionId);
		if (s != null) {
			
		} else {
			//TODO must drop message
		}

	}
	
	private void handleGossipMessage() {
		Message m = incomingMessage.getMessage();
		String sessionId = m.getSession();
		//TODO

	}
	
	private boolean addToInNeighborsTable() {
		Message m = incomingMessage.getMessage();
		Execution execution;
		TimedNeighbor tn; 
		Session s = sessions.get(m.getSession());
		if (s != null) {
			execution = s.getExecution(m.getExecution());
			if (execution != null) {
				tn = new TimedNeighbor(m.getNodeId(), incomingMessage.getAddress());
				return execution.addInNeighbor(tn);
			}
		}
		return false;
	}

}
