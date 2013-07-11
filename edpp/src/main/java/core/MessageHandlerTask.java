package core;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

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

	private Logger logger;
	
	private TransferableMessage incomingMessage;
	private Node localNode;
	private Map<String, Session> sessions;
	private BlockingQueue<TransferableMessage> outQueue;
	private PrimaryTreeMap<Integer, RecordedSession> db;
	
	public MessageHandlerTask(TransferableMessage incomingMessage, 
			Map<String, Session> sessions, Node localNode,
			BlockingQueue<TransferableMessage> outQueue,
			PrimaryTreeMap<Integer, RecordedSession> db) {
		
		this.logger = Logger.getLogger(MessageHandlerTask.class.getName());
		
		this.incomingMessage = incomingMessage;
		this.sessions = sessions;
		this.localNode = localNode;
		this.outQueue = outQueue;
		this.db = db;
	}
	
	@Override
	public void run() {
		Message m = incomingMessage.getMessage();
		logger.info("Receieved a message of type "+m.getType());
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
			logger.warning("Received malformed message");
			break;
		}
	}
	
	private Session createNewSession(boolean isInitiator) {
		Execution initExecution;
		Session s;
		Message m = incomingMessage.getMessage();
		
		int numberOfExecutions = m.getExecution();
		int numberOfRounds = m.getRound();
		logger.info("Created new Session");
		if (isInitiator) {
			s = new Session(localNode, numberOfExecutions, 
					numberOfRounds, isInitiator);
		} else {
			s = new Session(localNode,m.getSession(), numberOfExecutions,
					numberOfRounds, isInitiator);
		}
		logger.info("Created new execution");
		initExecution = s.createNewExecution();
		
		if (!isInitiator) {
			//Current round is 1, but we want the value we received
			//to be stored in round 2
			initExecution.addValToRound(m.getVal(), 2);
		}
		sessions.put(s.getSessionId(), s);
		
		sendOutMessage(MessageType.INIT, s, initExecution, 
				initExecution.getExecutionNumber(), numberOfRounds);
		
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
			logger.info("Session with id "+sessionId+" does not exist");
			s = createNewSession(false);
			tn = new TimedNeighbor(m.getNodeId(), incomingMessage.getAddress());
			logger.info("Adding node "+m.getNodeId()+" with IP "+incomingMessage.getAddress()
					+" to the in-neighbors list");
			addToInNeighborsTable(tn, s, executionNumber);
		} else {
			// Check whether the execution exists
			logger.info("Session with id "+sessionId+" already exists");
			e = s.getExecution(executionNumber);
			if (e == null) {
				logger.info("Execution number "+executionNumber+" does not exist. Will create it");
				e = s.createNewExecution(executionNumber);
				e.addValToRound(m.getVal(), 2);
				tn = new TimedNeighbor(m.getNodeId(), incomingMessage.getAddress());
				logger.info("Adding node "+m.getNodeId()+" with IP "+incomingMessage.getAddress()
						+" to the in-neighbors list");
				addToInNeighborsTable(tn, s, executionNumber);
			} else {
				/*
				 * Session and execution both exist.
				 * Check whether we are still in the INIT phase
				 * and add node to in-neighbors and val to current round
				 */
				logger.info("Execution number "+executionNumber+" already exists");
				if (e.getPhase() == Phase.INIT && (!e.hasTerminated())) {
					e.addValToRound(m.getVal(), 2);
					tn = new TimedNeighbor(m.getNodeId(), incomingMessage.getAddress());
					logger.info("Adding node "+m.getNodeId()+" with IP "+incomingMessage.getAddress()
							+" to the in-neighbors list");
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
			logger.info("Session with id "+sessionId+" was found. Checking for execution "+execution);
			e = s.getExecution(execution);
			if (e != null) {
				logger.info("Execution "+execution+" was located");
				//If the data exchange stage is over, just ignore it
				//If we are still in INIT phase, we will use the message later
				if (e.getPhase() == Phase.GOSSIP || e.getPhase() == Phase.TERMINATED) {
					logger.info("The execution is not in the data exchange phase. Will not process incoming message");
					return;
				}
				//If the message is for the current or a future round
				if (e.getCurrentRound() >= round) {
					logger.info("Adding "+m.getVal()+" to the values of round "+round);
					e.addValToRound(m.getVal(), round);
				}
				//If the message was of the current round set the clock to INF
				if (e.getCurrentRound() == round) {
					logger.info("The message was for the current round. Node "+m.getNodeId()+" is still alive");
					e.setTimerToInf(m.getNodeId());
				} else {
					//Reset the clock. The node is still alive (just an optimization)
					logger.info("The message was for round "+round+". Node "+m.getNodeId()+" is still alive");
					e.addNeighborToRound(m.getNodeId(), round);
//					e.resetTimer(m.getNodeId());
				}
				// Check if all clocks are INF
				if (e.roundIsOver()) {
					logger.info("Round "+e.getCurrentRound()+" is over");
					e.setCurrentImpulseResponse(e.getCurrentValue());
					logger.info("The impulse response of round "+e.getCurrentRound()+" is "+ e.getImpulseResponse(e.getCurrentRound()));
					//Check if we have terminated or for initiator round
					if (e.hasAnotherRound()) {
						logger.info("The session has another round");
						//Send message to out neighbors
						sendOutMessage(MessageType.NEXT, s, e, e.getExecutionNumber(), e.getCurrentRound()+1);
						logger.info("Recomputing the weights of out-neighbors");
						e.recomputeWeight();								
					} else {
						logger.info("This was the final round of the data exchange phase. Entering gossip round");
						e.setPhase(Phase.GOSSIP);
						logger.info("Computing realization matrix");
						e.computeRealizationMatrix(localNode.getDiameter());
						//compute the eigenvalues of the approximation matrix
						//TODO probably should test this for null
						double [] eigenvals = e.getMatrixAEigenvalues();
						Message msg = MessageBuilder.buildGossipMessage(localNode.getLocalId().toString(),
								s.getSessionId(), e.getExecutionNumber(), eigenvals);
						//send GOSSIP message to out-neighbors
						sendGossipMessage(msg, e);
					}
					logger.info("Renewing the timers of all the in-neighbors");
					e.getInNeighbors().renewTimers();
					e.setRound(e.getCurrentRound()+1);

					//Check whether a new execution should be initiated
					//The initial execution must be currently altered to proceed to create a new execution
					//Otherwise an execution might be initiated multiple times
					if (s.isInitiator() && e.equals(s.getInitExecution())
							&& s.newExecutionExpected()) {
						logger.info("Initiating a new execution");
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
			logger.info("Session with id "+sessionId+" was found. Checking for execution "+executionNumber);
			e = s.getExecution(executionNumber);
			if (e != null) {
				logger.info("Execution "+executionNumber+" was located");
				if (e.getPhase() != Phase.GOSSIP) {
					logger.warning("The execution was not in the gossip phase");
					e.addPendingGossipMessage(m.getNodeId(), eigenvals);
					return;
				}
				//Add the collected gossip round values
				logger.info("Adding the eigenvalues of node with id "+m.getNodeId()+" and IP"+ incomingMessage.getAddress()
						+" to the list of gossip eigenvalues");
				e.addGossipEigenvalues(m.getNodeId(), eigenvals);
				e.setTimerToInf(m.getNodeId());
				//If the round is over, the execution finished
				if (e.roundIsOver()) {
					logger.info("Gossip round is now over");
					e.computeMedianEigenvalues();
					e.setPhase(Phase.TERMINATED);
					s.addCompletedExecution();
					//If the session finished, compute the final eigenvalues
					if (s.hasTerminated()) {
						logger.info("Session "+s.getSessionId()+" terminated.");
						sessions.remove(s.getSessionId());
						synchronized (db) {
							int size = db.size();
							RecordedSession recSes = new RecordedSession(s);
							System.out.println(recSes.getRecordedSession().getSessionId());
							db.put(size, recSes);
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
				logger.info("Sending GOSSIP message to "+n.getId()+" with address "+address);
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
					logger.info("Sending INIT message to "+n.getId()+" with address "+address);
					logger.info("The current node is "+nodeId);
					outMessage = MessageBuilder.buildInitMessage(nodeId, sessionId, 
							executionNumber, roundNumber, valueToSend);
					break;
				case NEXT:
				default:
					//round number + 1, because we send the message for a round
					//using the value of the previous round
					logger.info("Sending NEXT message to "+n.getId()+" with address "+address);
					outMessage = MessageBuilder.buildNextMessage(nodeId,
							sessionId, executionNumber, roundNumber, valueToSend);
					break;
				}
				outQueue.add(new TransferableMessage(outMessage, address));
			}
		}
	}

}
