package core;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

import comm.MessageBuilder;
import comm.ProtocolMessage.Message.MessageType;
import comm.ProtocolMessage.SessionEvent;
import comm.ProtocolMessage.SessionEvent.EventType;
import comm.TransferableMessage;
import comm.ProtocolMessage.Message;
import domain.Execution;
import domain.Phase;
import domain.PlainNeighbor;
import domain.Session;
import domain.TimedNeighbor;
import domain.network.Node;
import domain.structure.NeighborsTable;
import domain.structure.PlainNeighborsTable;
import event.SessionListener;

/**
 * This is the message handling tasks which is initiated by the
 * ProtocolController once a new message arrives for the protocol. It will check
 * in which class it belongs and it will handle it accordingly
 * 
 * @author Xenofon Foukas
 * 
 */
public class MessageHandlerTask implements Runnable {

	private Logger logger;

	private TransferableMessage incomingMessage;
	private Node localNode;
	private Map<String, Session> sessions;
	private BlockingQueue<TransferableMessage> outQueue;

	private static List<SessionListener> sessionListeners = new ArrayList<SessionListener>();

	/**
	 * Class constructor
	 * 
	 * @param incomingMessage
	 *            the message that needs to be handled by this task
	 * @param sessions
	 *            a Map containing all the currently active Sessions in the
	 *            protocol, using their ids as keys
	 * @param localNode
	 *            the abstract Node representation of the local node
	 * @param outQueue
	 *            the queue where the outgoing messages should be placed
	 */
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
		logger.info("Receieved a message of type " + m.getType()
				+ " from node with IP " + incomingMessage.getAddress());
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
		// if the Session referred by the message exists check for the execution
		// mentioned
		if (s != null) {
			e = s.getExecution(m.getExecution());
			// if the execution also exists check which value is required to be
			// resent and send it
			if (e != null) {
				int wantedRound = m.getRound();
				if (wantedRound >= 0) {
					int currentRound = e.getCurrentRound();
					if (wantedRound <= currentRound) {
						NeighborsTable<PlainNeighbor> n = e.getOutNeighbors();
						double weight = 1.0 / n.getSize();
						double valToSend = e
								.getImpulseResponse(wantedRound - 1) * weight;
						int r = wantedRound;
						logger.info("Sending NEXT message to node with address "
								+ incomingMessage.getAddress()
								+ " for round "
								+ wantedRound
								+ " of execution "
								+ m.getExecution()
								+ " in session "
								+ s.getSessionId());
						String nodeId = localNode.getLocalId().toString();
						Message outMessage = MessageBuilder.buildNextMessage(
								nodeId, s.getSessionId(),
								e.getExecutionNumber(), r, valToSend);
						outQueue.add(new TransferableMessage(outMessage,
								incomingMessage.getAddress(), false));
					}
				} else { // it wantedRound < 0, the request is for the estimated
							// eigenvalues by this Execution
					double[] eigenvals;
					if ((eigenvals = e.getMatrixAEigenvalues()) != null) {
						double[] valsToSend;
						if (eigenvals.length > 3) {
							valsToSend = new double[3];
							for (int j = 0; j < 3; j++) {
								valsToSend[j] = eigenvals[j];
							}
						} else {
							valsToSend = eigenvals;
						}
						Message msg = MessageBuilder.buildGossipMessage(
								localNode.getLocalId().toString(),
								s.getSessionId(), e.getExecutionNumber(),
								valsToSend);
						// send GOSSIP message to out-neighbors
						outQueue.add(new TransferableMessage(msg,
								incomingMessage.getAddress(), false));
					}
				}
			}
		}

	}

	/**
	 * Adds a new SessionListener to check for new Session events
	 * 
	 * @param listener
	 *            the new SessionListener
	 */
	public static void addSessionListener(SessionListener listener) {
		sessionListeners.add(listener);
	}

	/**
	 * Removes a SessionListener from the list of listeners of this task
	 * 
	 * @param listener
	 *            the SessionListener to be removed
	 * @return true if the listener was removed, otherwise false
	 */
	public static boolean removeSessionListener(SessionListener listener) {
		return sessionListeners.remove(listener);
	}

	private Session createNewSession(boolean isInitiator) {
		Execution initExecution;
		Session s;
		Message m = incomingMessage.getMessage();

		int numberOfExecutions = m.getTotalNumberOfExecutions();
		int numberOfRounds = m.getRound();
		// if the node is the initiator of the sampling request set the
		// initiator flag of the Session to true
		if (isInitiator) {
			s = new Session(localNode, numberOfExecutions, numberOfRounds,
					isInitiator);
		} else {
			s = new Session(localNode, m.getSession(), numberOfExecutions,
					numberOfRounds, isInitiator);
		}
		logger.info("Created a new Session with id " + s.getSessionId());
		initExecution = s.createNewExecution();
		logger.info("Created Execution number "
				+ initExecution.getExecutionNumber() + " for session with id "
				+ s.getSessionId());

		if (!isInitiator) {
			logger.info("Remote node with id " + m.getNodeId()
					+ " sent the value " + m.getVal()
					+ " for round 2 of the initial execution of session"
					+ " with id " + s.getSessionId());
			// Current round is 1, but we want the value we received
			// to be stored in round 2
			initExecution.addValToRound(m.getVal(), 2);
		}
		sessions.put(s.getSessionId(), s);

		sendOutMessage(MessageType.INIT, s, initExecution);

		// notify all listeners that a new Session was initiated
		for (SessionListener sl : sessionListeners) {
			SessionEvent se = MessageBuilder.buildNewSessionEvent(s, localNode,
					EventType.INITIAL);
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
		// if it does not exist we need to create it
		if (s == null) {
			logger.info("Session with id " + sessionId + " does not exist");
			s = createNewSession(false);
			tn = new TimedNeighbor(m.getNodeId(), incomingMessage.getAddress());
			logger.info("Adding node " + m.getNodeId() + " with IP "
					+ incomingMessage.getAddress()
					+ " to the in-neighbors list");
			addToInNeighborsTable(tn, s, executionNumber);
		} else { // if the Session exists check whether the Execution exists
			logger.info("Session with id " + sessionId + " already exists");
			e = s.getExecution(executionNumber);
			// if the Execution does not exist create a new execution for this
			// Session
			if (e == null) {
				logger.info("Execution number " + executionNumber
						+ " does not exist. Will create it");
				e = s.createNewExecution(executionNumber);
				logger.info("Remote node with id " + m.getNodeId()
						+ " sent the value " + m.getVal()
						+ " for round 2 of execution number " + executionNumber
						+ " of session" + " with id " + s.getSessionId());
				// add the value contained in the INIT message to round 2
				e.addValToRound(m.getVal(), 2);
				// the node who sent the INIT message should be added in the
				// in-neighbors list
				tn = new TimedNeighbor(m.getNodeId(),
						incomingMessage.getAddress());
				logger.info("Adding node " + m.getNodeId() + " with IP "
						+ incomingMessage.getAddress()
						+ " to the in-neighbors list");
				addToInNeighborsTable(tn, s, executionNumber);
				sendOutMessage(MessageType.INIT, s, e);
			} else {
				/*
				 * Session and execution both exist. Check whether we are still
				 * in the INIT phase and add node to in-neighbors and val to
				 * current round
				 */
				logger.info("Execution number " + executionNumber
						+ " of session " + s.getSessionId() + " already exists");
				if (e.getPhase() == Phase.INIT && (!e.hasTerminated())) {
					logger.info("Remote node with id " + m.getNodeId()
							+ " sent the value " + m.getVal()
							+ " for round 2 of execution number "
							+ executionNumber + " of session" + " with id "
							+ s.getSessionId());
					e.addValToRound(m.getVal(), 2);
					tn = new TimedNeighbor(m.getNodeId(),
							incomingMessage.getAddress());
					logger.info("Adding node " + m.getNodeId() + " with IP "
							+ incomingMessage.getAddress()
							+ " to the in-neighbors list");
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

		// Check whether session already exists
		s = sessions.get(sessionId);
		if (s != null) {
			logger.info("Session with id " + sessionId
					+ " was found. Checking for execution " + execution);
			e = s.getExecution(execution);
			if (e != null) {
				logger.info("Execution " + execution + " of Session with id "
						+ sessionId + " was located");
				// If the data exchange stage is over, just ignore it
				// If we are still in INIT phase, we will use the message later
				if (e.getPhase() == Phase.GOSSIP
						|| e.getPhase() == Phase.TERMINATED) {
					logger.info("Execution "
							+ execution
							+ " of session "
							+ sessionId
							+ " is not in the data exchange phase. Will not process incoming message"
							+ " from node with IP "
							+ incomingMessage.getAddress());
					return;
				}
				// If the message is for the current or a future round
				logger.info("Adding " + m.getVal() + " to the values of round "
						+ round + " in execution " + execution + " of session "
						+ sessionId);
				e.addValToNextRound(m.getNodeId(), m.getVal(), round);
				// e.addNeighborToRound(m.getNodeId(), round);
			} else {
				// TODO What if execution does not exist
				// Do we hold the message?
				// Drop them for now
			}
		} else {
			// TODO what id session does not exist
			// Do we hold the message
			// Drop them for now
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
			eig += eigenvals[i];
			eig += ", ";
		}
		eig += "]";

		s = sessions.get(sessionId);

		// check whether the Session and Execution mentioned in the GOSSIP
		// message exist
		if (s != null) {
			logger.info("Session with id " + sessionId
					+ " was found. Checking for execution " + executionNumber);
			e = s.getExecution(executionNumber);
			if (e != null) {
				logger.info("Execution " + executionNumber
						+ " of Session with id " + sessionId + " was located");
				// Add the collected gossip round values
				logger.info("Node with id " + m.getNodeId()
						+ " sent the following eigenvalues: " + eig
						+ " for execution " + executionNumber + " of session "
						+ sessionId);
				e.addPendingGossipMessage(m.getNodeId(), eigenvals);
			}
		}
	}

	private boolean addToInNeighborsTable(TimedNeighbor tn, Session s,
			int executionNumber) {
		Execution execution;
		if (s != null) {
			execution = s.getExecution(executionNumber);
			if (execution != null) {
				return execution.addInNeighbor(tn);
			}
		}
		return false;
	}

	private void sendOutMessage(MessageType type, Session session,
			Execution execution) {
		InetAddress address;
		double valueToSend;
		Message outMessage;
		String sessionId = session.getSessionId();
		String nodeId = localNode.getLocalId().toString();
		PlainNeighborsTable pnt = execution.getOutNeighbors();

		// send the message to all the nodes in the out-neighbors list
		synchronized (pnt) {
			for (PlainNeighbor n : pnt) {
				address = n.getAddress();
				valueToSend = execution.getCurrentValue() * n.getWeight();
				switch (type) {
				case INIT:
					logger.info("Sending INIT message to node " + n.getId()
							+ " with address " + address + " for execution "
							+ execution.getExecutionNumber() + " of session "
							+ sessionId);
					outMessage = MessageBuilder.buildInitMessage(nodeId,
							sessionId, execution.getExecutionNumber(),
							session.getNumberOfExecutions(),
							session.getNumberOfRounds(), valueToSend);
					break;
				case NEXT:
				default:
					// round number + 1, because we send the message for a round
					// using the value of the previous round
					int r = execution.getCurrentRound() + 1;
					logger.info("Sending NEXT message to node " + n.getId()
							+ " with address " + address + " for round " + r
							+ " of execution " + execution.getExecutionNumber()
							+ " in session " + sessionId);
					outMessage = MessageBuilder.buildNextMessage(nodeId,
							sessionId, execution.getExecutionNumber(), r,
							valueToSend);
					break;
				}
				outQueue.add(new TransferableMessage(outMessage, address, true));
			}
		}
	}

}
