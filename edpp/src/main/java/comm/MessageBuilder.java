package comm;

import java.util.Date;
import java.util.Set;

import comm.ProtocolMessage.Message;
import comm.ProtocolMessage.Message.Builder;
import comm.ProtocolMessage.Message.MessageType;
import comm.ProtocolMessage.SessionEvent;
import comm.ProtocolMessage.SessionEvent.EventType;
import domain.Neighbor;
import domain.Session;
import domain.network.Node;

/**
 * Class for producing messages used by the decetralized protocol
 * 
 * @author Xenofon Foukas
 * 
 */
public class MessageBuilder {

	/**
	 * Builds a new message of type NEW
	 * 
	 * @param numOfExecutions
	 *            the total number of executions in the protocol run
	 * @param numOfRounds
	 *            the number of rounds in each protocol execution
	 * @return the constructed NEW message
	 * @see Message
	 */
	public static Message buildNewMessage(int numOfExecutions, int numOfRounds) {
		Message m = Message.newBuilder().setType(MessageType.NEW)
				.setTotalNumberOfExecutions(numOfExecutions)
				.setRound(numOfRounds).build();

		return m;
	}

	/**
	 * Builds a new message of type INIT
	 * 
	 * @param nodeId
	 *            the string representation of the local node id
	 * @param sessionId
	 *            the string representation of the session id
	 * @param currentExecution
	 *            the number of the execution for which this INIT message is
	 *            sent
	 * @param totalNumOfExecutions
	 *            the total number of execution in the current session
	 * @param numOfRounds
	 *            the total number of rounds in each execution of the session
	 * @param value
	 *            the computed value that will be sent along with the message
	 * @return the constructed INIT message
	 * @see Message
	 */
	public static Message buildInitMessage(String nodeId, String sessionId,
			int currentExecution, int totalNumOfExecutions, int numOfRounds,
			double value) {
		Message m = Message.newBuilder().setNodeId(nodeId)
				.setType(MessageType.INIT).setSession(sessionId)
				.setExecution(currentExecution)
				.setTotalNumberOfExecutions(totalNumOfExecutions)
				.setRound(numOfRounds).setVal(value).build();

		return m;
	}

	/**
	 * Builds a new message of type NEXT
	 * 
	 * @param nodeId
	 *            the string representation of the local node id
	 * @param sessionId
	 *            the string representation of the session id
	 * @param numOfExecution
	 *            the number of the execution this message belongs to
	 * @param round
	 *            the current round
	 * @param val
	 *            the computed value that will be sent along with the message
	 * @return the constructed NEXT message
	 * @see Message
	 */
	public static Message buildNextMessage(String nodeId, String sessionId,
			int numOfExecution, int round, double val) {
		Message m = Message.newBuilder().setNodeId(nodeId)
				.setType(MessageType.NEXT).setSession(sessionId)
				.setExecution(numOfExecution).setRound(round).setVal(val)
				.build();

		return m;
	}

	/**
	 * Builds a new message of type GOSSIP
	 * 
	 * @param nodeId
	 *            the string representation of the local node id
	 * @param sessionId
	 *            the string representation of the session id
	 * @param numOfExecution
	 *            the number of the execution this message belongs to
	 * @param eigenvalues
	 *            the computed eigenvalues that the local node wants to share
	 *            with its neighbors
	 * @return the constructed GOSSIP message
	 * @see Message
	 */
	public static Message buildGossipMessage(String nodeId, String sessionId,
			int numOfExecution, double[] eigenvalues) {
		Builder builder = Message.newBuilder().setNodeId(nodeId)
				.setType(MessageType.GOSSIP).setSession(sessionId)
				.setExecution(numOfExecution);

		for (int i = 0; i < eigenvalues.length; i++) {
			builder = builder.addEigenvals(eigenvalues[i]);
		}

		return builder.build();
	}

	/**
	 * Builds a new message to check that a remote node is alive
	 * 
	 * @return the constructed liveness check message
	 * @see Message
	 */
	public static Message buildLivenessMessage() {
		Message m = Message.newBuilder().setType(MessageType.LIVENESS_CHECK)
				.build();
		return m;
	}

	/**
	 * Builds a new message to request for a previous value
	 * 
	 * @param nodeId
	 *            the string representation of the local node id
	 * @param sessionId
	 *            the string representation of the session id
	 * @param execNum
	 *            the number of the execution for which the request is being
	 *            made
	 * @param round
	 *            the round for which the value is requested
	 * @return the constructed Message for requesting a previous value
	 */
	public static Message requestPreviousValMessage(String nodeId,
			String sessionId, int execNum, int round) {
		Message m = Message.newBuilder().setType(MessageType.REQUEST_VAL)
				.setNodeId(nodeId).setSession(sessionId).setExecution(execNum)
				.setRound(round).build();
		return m;
	}

	/**
	 * Build a message of type SessionEvent which will be used by the
	 * SessionListener to notify listers that a change has occurred in some
	 * Session
	 * 
	 * @param s
	 *            the Session in which the event occurred
	 * @param localNode
	 *            the abstract Node representation of the local node
	 * @param type
	 *            the type of event occurred in the Session
	 * @return
	 */
	public static SessionEvent buildNewSessionEvent(Session s, Node localNode,
			EventType type) {
		double[] eigenvalues;

		// We will have eigenvalues only if this is an event notifying about the
		// termination of the Session
		if (type == EventType.TERMINAL) {
			eigenvalues = s.getComputedEigenvalues();
		} else {
			eigenvalues = new double[0];
		}

		comm.ProtocolMessage.SessionEvent.Builder builder = SessionEvent
				.newBuilder().setType(type).setDate(new Date().getTime())
				.setSessionId(s.getSessionId())
				.setLocalNodeId(localNode.getLocalId().toString());

		Set<Neighbor> outNeighbors = localNode.getOutNeighbors();

		// Add to the event the out neighbors of the local node
		for (Neighbor n : outNeighbors) {
			builder = builder.addOutNeighbors(n.getId().toString());
		}

		// Add to the event the estimations of the eigenvalues provided by the
		// Session
		for (int i = 0; i < eigenvalues.length; i++) {
			builder.addEigenvalues(eigenvalues[i]);
		}

		return builder.build();
	}

}
