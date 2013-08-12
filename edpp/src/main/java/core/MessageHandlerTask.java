package core;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

import util.Neighbor;
import util.NeighborsTable;
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
		case REQUEST_VAL:
			this.resendVal();
			break;
		case GOSSIP:
			this.handleGossipMessage();
			break;
		default:
			logger.warning("The message was malformed. Dropping...");
			break;
		}
	}
	
	private void resendVal() {
		Session s;
		Execution e;
		
		Message m = incomingMessage.getMessage();
		
		s = sessions.get(m.getSession());
		if (s != null) {
			e = s.getExecution(m.getExecution());
			if(e != null) {
				int wantedRound = m.getRound();
				int currentRound = e.getCurrentRound();
				if (wantedRound <= currentRound) {
					NeighborsTable<PlainNeighbor> n = e.getOutNeighbors();
					double weight = 1.0/n.getSize();
					double valToSend = e.getImpulseResponse(wantedRound-1)*weight;
					int r = wantedRound;
					logger.info("Sending NEXT message to node with address "+incomingMessage.getAddress()+
							" for round "+wantedRound+" of execution "+m.getExecution()+
							" in session "+s.getSessionId());
					String nodeId = localNode.getLocalId().toString();
					Message outMessage = MessageBuilder.buildNextMessage(nodeId,
							s.getSessionId(), e.getExecutionNumber(),
							r, valToSend);
					outQueue.add(new TransferableMessage(outMessage, incomingMessage.getAddress(), false));
				}
			}
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
				e.addValToNextRound(m.getNodeId(), m.getVal(), round);
//				e.addNeighborToRound(m.getNodeId(), round);
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
				//Add the collected gossip round values
				logger.info("Node with id "+m.getNodeId()+" sent the following eigenvalues: "
										+ eig+" for execution "+executionNumber+
										" of session "+sessionId);
				e.addPendingGossipMessage(m.getNodeId(), eigenvals);
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
				outQueue.add(new TransferableMessage(outMessage, address, true));
			}
		}
	}

}
