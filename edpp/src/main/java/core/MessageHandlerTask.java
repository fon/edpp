package core;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

import org.jblas.DoubleMatrix;

import util.Phase;
import util.PlainNeighbor;
import util.PlainNeighborsTable;
import util.TimedNeighbor;
import comm.MessageBuilder;
import comm.ProtocolMessage.Message.MessageType;
import comm.ProtocolMessage.SessionEvent;
import comm.ProtocolMessage.SessionEvent.EventType;
import comm.TransferableMessage;
import comm.ProtocolMessage.Message;
import network.Node;

public class MessageHandlerTask implements Runnable {

	private Logger logger;
	
	private TransferableMessage incomingMessage;
	private Node localNode;
	private Map<String, Session> sessions;
	private BlockingQueue<TransferableMessage> outQueue;
	
	private static List<SessionListener> sessionListeners = new ArrayList<SessionListener>();
	
	public MessageHandlerTask(TransferableMessage incomingMessage, 
			Map<String, Session> sessions, Node localNode,
			BlockingQueue<TransferableMessage> outQueue) {
		
		this.logger = Logger.getLogger(MessageHandlerTask.class.getName());
		
		this.incomingMessage = incomingMessage;
		this.sessions = sessions;
		this.localNode = localNode;
		this.outQueue = outQueue;
	}
	
	@Override
	public void run() {
		Message m = incomingMessage.getMessage();
		logger.info("Receieved a message of type "+m.getType()+ " from node with IP "+incomingMessage.getAddress());
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
			logger.warning("The message was malformed. Dropping...");
			break;
		}
	}
	
	public static void addSessionListener(SessionListener listener) {
		sessionListeners.add(listener);
	}
	
	public static boolean removeSessionListener(SessionListener listener) {
		return sessionListeners.remove(listener);
	}
	
	private Session createNewSession(boolean isInitiator) {
		Execution initExecution;
		Session s;
		Message m = incomingMessage.getMessage();
		
		int numberOfExecutions = m.getTotalNumberOfExecutions();
		int numberOfRounds = m.getRound();
		if (isInitiator) {
			s = new Session(localNode, numberOfExecutions, 
					numberOfRounds, isInitiator);
		} else {
			s = new Session(localNode,m.getSession(), numberOfExecutions,
					numberOfRounds, isInitiator);
		}
		logger.info("Created a new Session with id "+s.getSessionId());
		initExecution = s.createNewExecution();
		logger.info("Created Execution number "+initExecution.getExecutionNumber()+
				" for session with id "+s.getSessionId());
		
		if (!isInitiator) {
			logger.info("Remote node with id "+m.getNodeId()+" sent the value "
					+m.getVal()+" for round 2 of the initial execution of session"
					+" with id "+s.getSessionId());
			//Current round is 1, but we want the value we received
			//to be stored in round 2
			initExecution.addValToRound(m.getVal(), 2);
		}
		sessions.put(s.getSessionId(), s);
		
		sendOutMessage(MessageType.INIT, s, initExecution);
		
		for (SessionListener sl : sessionListeners) {
			SessionEvent se = MessageBuilder.buildNewSessionEvent(s, localNode, EventType.INITIAL);
			sl.sessionInitiated(se);
		}
		
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
				logger.info("Remote node with id "+m.getNodeId()+" sent the value "
						+m.getVal()+" for round 2 of execution number "+executionNumber+" of session"
						+" with id "+s.getSessionId());
				e.addValToRound(m.getVal(), 2);
				tn = new TimedNeighbor(m.getNodeId(), incomingMessage.getAddress());
				logger.info("Adding node "+m.getNodeId()+" with IP "+incomingMessage.getAddress()
						+" to the in-neighbors list");
				addToInNeighborsTable(tn, s, executionNumber);
				sendOutMessage(MessageType.INIT, s, e);
			} else {
				/*
				 * Session and execution both exist.
				 * Check whether we are still in the INIT phase
				 * and add node to in-neighbors and val to current round
				 */
				logger.info("Execution number "+executionNumber+" of session "+s.getSessionId()+" already exists");
				if (e.getPhase() == Phase.INIT && (!e.hasTerminated())) {
					logger.info("Remote node with id "+m.getNodeId()+" sent the value "
							+m.getVal()+" for round 2 of execution number "+executionNumber+" of session"
							+" with id "+s.getSessionId());
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
				logger.info("Execution "+execution+" of Session with id "+sessionId+" was located");
				//If the data exchange stage is over, just ignore it
				//If we are still in INIT phase, we will use the message later
				if (e.getPhase() == Phase.GOSSIP || e.getPhase() == Phase.TERMINATED) {
					logger.info("Execution "+execution+" of session "+sessionId
							+" is not in the data exchange phase. Will not process incoming message"
							+ " from node with IP "+incomingMessage.getAddress());
					return;
				}
				//If the message is for the current or a future round
				logger.info("Adding "+m.getVal()+" to the values of round "+round
						+" in execution "+execution+" of session "+sessionId);
				e.addValToRound(m.getVal(), round);
				//If the message was of the current round set the clock to INF
				if (e.getCurrentRound() == round) { 
					/*logger.info("The message was for the current round of execution "
							+execution+" of session "+sessionId+". Node "+m.getNodeId()+" is still alive."
							+ " Setting its timer to INF");*/
					e.setTimerToInf(m.getNodeId());
				} else {
					/*logger.info("The message was for round "+round+". Node "+m.getNodeId()+" is still alive");*/
					e.addNeighborToRound(m.getNodeId(), round);
//					e.resetTimer(m.getNodeId());
				}
				// Check if all clocks are INF
				synchronized (e) {
					if (e.roundIsOver()) {
						logger.info("Round "+e.getCurrentRound()+" of execution "+execution
								+" in session "+sessionId+" is over");
						e.setCurrentImpulseResponse(e.getCurrentValue());
						logger.info("The impulse response of round "+e.getCurrentRound()+" is "+ e.getImpulseResponse(e.getCurrentRound())
								+" .Renewing the timers of all the in-neighbors");
						e.getInNeighbors().renewTimers();
						//Check if we have terminated or for initiator round
						if (e.hasAnotherRound()) {
							logger.info("Execution "+execution+" of session "+sessionId+" has another round");
							//Send message to out neighbors
							sendOutMessage(MessageType.NEXT, s, e);
							logger.info("Recomputing the weights of out-neighbors");
							e.recomputeWeight();								
						} else {
							logger.info("This was the final round of the data exchange phase of execution"
									+execution+" of session "+sessionId+" .Entering gossip round");
							e.setPhase(Phase.GOSSIP);
							DoubleMatrix rm = e.computeRealizationMatrix(localNode.getDiameter());
							logger.info("The realization matrix of execution "+execution
									+" of session "+sessionId+" was computed");
							rm.print();
							//compute the eigenvalues of the approximation matrix
							//TODO probably should test this for null
							double [] eigenvals = e.getMatrixAEigenvalues();
							String eig = "[";
							for (int i = 0; i < eigenvals.length; i++) {
								eig+=eigenvals[i];
								eig+=", ";
							}
							eig+="]";
							logger.info("The computed eigenvalues of execution "+execution
									+" of session "+sessionId+" are "+eig);
							Message msg = MessageBuilder.buildGossipMessage(localNode.getLocalId().toString(),
									s.getSessionId(), e.getExecutionNumber(), eigenvals);
							e.transferPendingGossipMessages();
							//send GOSSIP message to out-neighbors
							sendGossipMessage(msg, e);
						
						}
						e.setRound(e.getCurrentRound()+1);
						
						//Check whether a new execution should be initiated
						//The initial execution must be currently altered to proceed to create a new execution
						//Otherwise an execution might be initiated multiple times
						if (s.isInitiator() && e.equals(s.getInitExecution())
								&& s.newExecutionExpected()) {
							Execution newExecution;
							newExecution = s.createNewExecution();
							logger.info("Initiated execution "+newExecution.getExecutionNumber()+
									" of session "+s.getSessionId());
							sendOutMessage(MessageType.INIT, s, newExecution);
						}
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
		String eig = "[";
		for (int i = 0; i < eigenvals.length; i++) {
			eigenvals[i] = m.getEigenvals(i);
			eig+=eigenvals[i];
			eig+=", ";
		}
		eig+="]";
		
		s = sessions.get(sessionId);
		
		if (s != null) {
			logger.info("Session with id "+sessionId+" was found. Checking for execution "+executionNumber);
			e = s.getExecution(executionNumber);
			if (e != null) {
				logger.info("Execution "+executionNumber+" of Session with id "+sessionId+" was located");
				if (e.getPhase() != Phase.GOSSIP) {
					logger.info("Execution "+executionNumber+" of session "
							+sessionId+" was not in the gossip phase.\n"
									+ "Node with id "+m.getNodeId()+" sent the following eigenvalues: "
											+ eig);
					e.addPendingGossipMessage(m.getNodeId(), eigenvals);
					return;
				}
				//Add the collected gossip round values
				logger.info("Node with id "+m.getNodeId()+" sent the following eigenvalues: "
										+ eig+" for execution "+executionNumber+
										" of session "+sessionId);
				e.addGossipEigenvalues(m.getNodeId(), eigenvals);
				//If the round is over, the execution finished
				synchronized (e) {
					e.setTimerToInf(m.getNodeId());
					if (e.roundIsOver()) {
						double [] medianEig = e.computeMedianEigenvalues();
						eig = "[";
						for (int i = 0; i < medianEig.length; i++) {
							eig+=medianEig[i];
							eig+=", ";
						}
						eig+="]";
						logger.info("Gossip round of execution "+executionNumber+" of session "
								+sessionId+" is now over. The median of all received eigenvalues is "+
								eig);
						e.setPhase(Phase.TERMINATED);
						s.addCompletedExecution();
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
				logger.info("Sending GOSSIP message to node "+n.getId()+" with address "+address);
				outQueue.add(new TransferableMessage(m, address));
			}
		}
	}
	
	private void sendOutMessage(MessageType type, Session session, Execution execution) {
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
					logger.info("Sending INIT message to node "+n.getId()+" with address "+address+
							" for execution "+execution.getExecutionNumber()+
							" of session "+sessionId);
					outMessage = MessageBuilder.buildInitMessage(nodeId, sessionId, 
							execution.getExecutionNumber(), session.getNumberOfExecutions(),
							session.getNumberOfRounds(), valueToSend);
					break;
				case NEXT:
				default:
					//round number + 1, because we send the message for a round
					//using the value of the previous round
					int r = execution.getCurrentRound()+1;
					logger.info("Sending NEXT message to node "+n.getId()+" with address "+address+
							" for round "+r+" of execution "+execution.getExecutionNumber()+
							" in session "+sessionId);
					outMessage = MessageBuilder.buildNextMessage(nodeId,
							sessionId, execution.getExecutionNumber(),
							r, valueToSend);
					break;
				}
				outQueue.add(new TransferableMessage(outMessage, address));
			}
		}
	}

}
