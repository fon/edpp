package core;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import jdbm.PrimaryTreeMap;

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
	private PrimaryTreeMap<Integer, RecordedSession> db;
	
	public MessageHandlerTask(TransferableMessage incomingMessage, 
			Map<String, Session> sessions, Node localNode,
			BlockingQueue<TransferableMessage> outQueue,
			PrimaryTreeMap<Integer, RecordedSession> db) {
		
		this.incomingMessage = incomingMessage;
		this.sessions = sessions;
		this.localNode = localNode;
		this.outQueue = outQueue;
		this.db = db;
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
	
	private Session createNewSession(boolean isInitiator) {
		Execution initExecution;
		Message m = incomingMessage.getMessage();
		
		int numberOfExecutions = m.getExecution();
		int numberOfRounds = m.getRound();
		Session s = new Session(localNode, numberOfExecutions, 
				numberOfRounds, isInitiator);
		initExecution = s.createNewExecution();
		
		if (!isInitiator) {
			//Current round is 1, but we want the value we received
			//to be stored in round 2
			initExecution.addValToRound(m.getVal(), 2);
		}
		
		sendOutMessage(MessageType.INIT, s, initExecution, 
				numberOfExecutions, numberOfRounds);
		
		sessions.put(s.getSessionId(), s);
		return s;
	}

	private void handleInitMessage() {
		Session s;
		Execution e;
		TimedNeighbor tn;
		
		Message m = incomingMessage.getMessage();
		
		String sessionId = m.getSession();
		int executionNumber = m.getExecution();
		
		// Check whether session already exists
		s = sessions.get(sessionId);
		if (s == null) {
			s = createNewSession(false);
			tn = new TimedNeighbor(m.getNodeId(), incomingMessage.getAddress());
			addToInNeighborsTable(tn, s, executionNumber);
		} else {
			// Check whether the execution exists
			e = s.getExecution(executionNumber);
			if (e == null) {
				e = s.createNewExecution(executionNumber);
				e.addValToRound(m.getVal(), 2);
				tn = new TimedNeighbor(m.getNodeId(), incomingMessage.getAddress());
				addToInNeighborsTable(tn, s, executionNumber);
			} else {
				/*
				 * Session and execution both exist.
				 * Check whether we are still in the INIT phase
				 * and add node to in-neighbors and val to current round
				 */
				if (e.getPhase() == Phase.INIT && (!e.hasTerminated())) {
					e.addValToRound(m.getVal(), 2);
					tn = new TimedNeighbor(m.getNodeId(), incomingMessage.getAddress());
					addToInNeighborsTable(tn, s, executionNumber);
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
				//If the data exchange stage is over, just ignore it
				//If we are still in INIT phase, we will use the message later
				if (e.getPhase() == Phase.GOSSIP || e.getPhase() == Phase.TERMINATED)
					return;
				//If the message is for the current or a future round
				if (e.getCurrentRound() >= round) {
					e.addValToRound(m.getVal(), round);
				}
				//If the message was of the current round set the clock to INF
				if (e.getCurrentRound() == round) {
					e.setTimerToInf(m.getNodeId());
				} else {
					//Reset the clock. The node is still alive (just an optimization)
					e.resetTimer(m.getNodeId());
				}
				// Check if all clocks are INF
				if (e.roundIsOver()) {
					e.setCurrentImpulseResponse(e.getCurrentValue());
					//Check if we have terminated or for initiator round
					if (e.hasAnotherRound()) {
						//Send message to out neighbors
						sendOutMessage(MessageType.NEXT, s, e, e.getExecutionNumber(), e.getCurrentRound());
						e.recomputeWeight();								
					} else {
						e.setPhase(Phase.GOSSIP);
						e.computeRealizationMatrix(localNode.getDiameter());
						//compute the eigenvalues of the approximation matrix
						//TODO probably should test this for null
						double [] eigenvals = e.getMatrixAEigenvalues();
						Message msg = MessageBuilder.buildGossipMessage(localNode.getLocalId().toString(),
								s.getSessionId(), e.getExecutionNumber(), eigenvals);
						//send GOSSIP message to out-neighbors
						sendGossipMessage(msg, e);
					}
					e.setRound(e.getCurrentRound()+1);
					e.getInNeighbors().renewTimers();

					//Check whether a new execution should be initiated
					//The initial execution must be currently altered to proceed to create a new execution
					//Otherwise an execution might be initiated multiple times
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
		Session s;
		Execution e;
		Message m = incomingMessage.getMessage();
		String sessionId = m.getSession();
		int executionNumber = m.getExecution();
		double[] eigenvals = new double[m.getEigenvalsCount()];
		for (int i = 0; i < eigenvals.length; i++) {
			eigenvals[i] = m.getEigenvals(i);
		}
		
		s = sessions.get(sessionId);
		
		if (s != null) {
			e = s.getExecution(executionNumber);
			if (e != null) {
				if (e.getPhase() != Phase.GOSSIP) 
					return;
				//Add the collected gossip round values
				e.addGossipEigenvalues(m.getNodeId(), eigenvals);
				e.setTimerToInf(m.getNodeId());
				//If the round is over, the execution finished
				if (e.roundIsOver()) {
					e.setPhase(Phase.TERMINATED);
					s.addCompletedExecution();
					//If the session finished, compute the final eigenvalues
					if (s.hasTerminated()) {
						synchronized (db) {
							db.put(db.size()+1, new RecordedSession(s));
						}
					}
				}
			}
		}
	}
	
	private boolean addToInNeighborsTable(TimedNeighbor tn, Session s, int executionNumber) {
		Execution execution;
		if (s != null) {
			execution = s.getExecution(executionNumber);
			if (execution != null) {
				return execution.addInNeighbor(tn);
			}
		}
		return false;
	}
	
	private void sendGossipMessage(Message m, Execution e) {
		InetAddress address;
		PlainNeighborsTable pnt = e.getOutNeighbors();
		
		synchronized (pnt) {
			for (PlainNeighbor n : pnt) {
				address = n.getAddress();
				outQueue.add(new TransferableMessage(m, address));
			}
		}
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
					//round number + 1, because we send the message for a round
					//using the value of the previous round
					outMessage = MessageBuilder.buildNextMessage(nodeId,
							sessionId, executionNumber, roundNumber+1, valueToSend);
					break;
				}
				outQueue.add(new TransferableMessage(outMessage, address));
			}
		}
	}

}
