package core;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import util.Phase;
import util.PlainNeighbor;
import util.PlainNeighborsTable;
import util.TimedNeighbor;

import comm.MessageBuilder;
import comm.ProtocolMessage.Message.MessageType;
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
		
		//TODO introduce new type of message INIT_SESSION and INIT_EXECUTION
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
		Message m = incomingMessage.getMessage();
		
		int numberOfExecutions = m.getExecution();
		int numberOfRounds = m.getRound();
		Session s = new Session(localNode, numberOfExecutions, 
				numberOfRounds, isInitiator);
		initExecution = s.createNewExecution();
		
		if (!isInitiator) {
			int currentRound = initExecution.getCurrentRound();
			initExecution.addValToRound(m.getVal(), currentRound);
		}
		
		sendOutMessage(MessageType.INIT, s, initExecution, 
				numberOfExecutions, numberOfRounds);
		
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
				e = s.createNewExecution(executionNumber);
				e.addValToRound(m.getVal(), e.getCurrentRound());
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
		int execution = m.getExecution();
		int round = m.getRound();
		Session s;
		Execution e;
		
		//Check whether session already exists
		s = sessions.get(sessionId);
		if (s != null) {
			e = s.getExecution(execution);
			if (e != null) {
				if (e.getPhase() == Phase.GOSSIP)
					return;
				//If the message is for the current or a future round
				if (e.getCurrentRound() >= round) {
					e.addValToRound(m.getVal(), round);
				}
				//If the message was of the current round set the clock to INF
				if (e.getCurrentRound() == round) {
					e.setTimerToInf(m.getNodeId());
				} else {
					//Reset the clock. The node is still alive
					e.resetTimer(m.getNodeId());
				}
				// Check if all clocks are INF
				if (e.roundIsOver()) {
					e.setCurrentImpulseResponse(e.getCurrentValue());
					e.setRound(e.getCurrentRound()+1);
					//Check if we have terminated or for initiator round
					if (!e.hasTerminated()) {
						//Send message to out neighbors
						sendOutMessage(MessageType.NEXT, s, e, e.getExecutionNumber(), e.getCurrentRound());
						e.setCurrentValue(0);
						e.recomputeWeight();								
					} else {
						e.setPhase(Phase.GOSSIP);
					}

					//Check whether a new execution should be initiated
					if (s.isInitiator() && e.equals(s.getInitExecution())
							&& s.newExecutionExpected()) {
						Execution newExecution;
						newExecution = s.createNewExecution();
						sendOutMessage(MessageType.INIT, s, newExecution, 
								newExecution.getExecutionNumber(), s.getNumberOfRounds());
					}
				}
			} else {
				// TODO What if execution does not exist
				// Do we hold the message?
				//Drop them for now
			}
		} else {
			//TODO what id session does not exist
			//Do we hold the message
			//Drop them for now
		}
	}
	
	private void handleGossipMessage() {
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
	
	private void sendOutMessage(MessageType type, Session session, Execution execution,
			int executionNumber, int roundNumber) {
		InetAddress address;
		double valueToSend;
		Message outMessage;
		String sessionId = session.getSessionId();
		String nodeId = localNode.getLocalId().toString();
		PlainNeighborsTable pnt = execution.getOutNeighbors();
		
		synchronized (pnt) {
			for (PlainNeighbor n : pnt) {
				address = n.getAddress();
				valueToSend = execution.getCurrentValue() * n.getWeight();
				switch (type) {
				case INIT:
					outMessage = MessageBuilder.buildNextMessage(nodeId, sessionId, 
							executionNumber, roundNumber, valueToSend);
					break;
				case NEXT:
				default:
					outMessage = MessageBuilder.buildNextMessage(nodeId,
							sessionId, executionNumber, roundNumber, valueToSend);
					break;
				}
				outQueue.add(new TransferableMessage(outMessage, address));
			}
		}
	}

}
