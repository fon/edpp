package comm;

import java.util.Date;
import java.util.Set;

import util.Neighbor;

import network.Node;

import comm.ProtocolMessage.Message;
import comm.ProtocolMessage.Message.Builder;
import comm.ProtocolMessage.Message.MessageType;
import comm.ProtocolMessage.SessionEvent;
import comm.ProtocolMessage.SessionEvent.EventType;
import core.Session;

/**
 * Class for producing messages used by the decetralized protocol
 * @author Xenofon Foukas
 *
 */
public class MessageBuilder {

	/**
	 * Builds a new message of type NEW
	 * @param numOfExecutions the total number of executions in the protocol run
	 * @param numOfRounds the number of rounds in each protocol execution
	 * @return the constructed NEW message
	 * @see Message
	 */
	public static Message buildNewMessage(int numOfExecutions, int numOfRounds) {
		Message m =
				Message.newBuilder()
				.setType(MessageType.NEW)
				.setTotalNumberOfExecutions(numOfExecutions)
				.setRound(numOfRounds)
				.build();
		
		return m;
	}
	
	/**
	 * Builds a new message of type INIT
	 * @param nodeId the string representation of the local node id
	 * @param sessionId the string representation of the session id
	 * @param currentExecution the number of the execution for which this INIT message is sent
	 * @param totalNumOfExecutions the total number of execution in the current session
	 * @param numOfRounds the total number of rounds in each execution of the session
	 * @param value the computed value that will be sent along with the message
	 * @return the constructed INIT message
	 * @see Message
	 */
	public static Message buildInitMessage(String nodeId, String sessionId, int currentExecution,
			int totalNumOfExecutions, int numOfRounds, double value) {
		Message m = 
				Message.newBuilder()
				.setNodeId(nodeId)
				.setType(MessageType.INIT)
				.setSession(sessionId)
				.setExecution(currentExecution)
				.setTotalNumberOfExecutions(totalNumOfExecutions)
				.setRound(numOfRounds)
				.setVal(value)
				.build();
				
		return m;
	}
	
	/**
	 * Builds a new message of type NEXT
	 * @param nodeId the string representation of the local node id
	 * @param sessionId the string representation of the session id
	 * @param numOfExecution the number of the execution this message belongs to
	 * @param round the current round
	 * @param val the computed value that will be sent along with the message
	 * @return the constructed NEXT message
	 * @see Message
	 */
	public static Message buildNextMessage(String nodeId, String sessionId, 
			int numOfExecution, int round, double val) {
		Message m =
				Message.newBuilder()
				.setNodeId(nodeId)
				.setType(MessageType.NEXT)
				.setSession(sessionId)
				.setExecution(numOfExecution)
				.setRound(round)
				.setVal(val)
				.build();
		
		return m;
	}
	
	/**
	 * Builds a new message of type GOSSIP
	 * @param nodeId the string representation of the local node id
	 * @param sessionId the string representation of the session id
	 * @param numOfExecution the number of the execution this message belongs to
	 * @param eigenvalues the computed eigenvalues that the local node wants to share with its neighbors
	 * @return the constructed GOSSIP message
	 * @see Message
	 */
	public static Message buildGossipMessage(String nodeId, String sessionId,
			int numOfExecution, double [] eigenvalues) {
		Builder builder =
				Message.newBuilder()
				.setNodeId(nodeId)
				.setType(MessageType.GOSSIP)
				.setSession(sessionId)
				.setExecution(numOfExecution);

		for (int i=0; i < eigenvalues.length; i++) {
			builder = builder.addEigenvals(eigenvalues[i]);
		}
		
		return builder.build();
	}
	
	/**
	 * Builds a new message to check that a remote node is alive
	 * @return the constructed liveness check message
	 * @see Message
	 */
	public static Message buildLivenessMessage() {
		Message m =
				Message.newBuilder()
				.setType(MessageType.LIVENESS_CHECK)
				.build();
		return m;
	}
	
	
	//TODO must add tests
	public static SessionEvent buildNewSessionEvent(Session s, Node localNode, EventType type) {
		double [] eigenvalues; 
		
		//We will have eigenvalues only if this is terminal
		if (type == EventType.TERMINAL) {
			eigenvalues = s.getComputedEigenvalues();
		} else {
			eigenvalues = new double [0]; 
		}
		
		comm.ProtocolMessage.SessionEvent.Builder builder = 
				SessionEvent.newBuilder()
				.setType(type)
				.setDate(new Date().getTime())
				.setSessionId(s.getSessionId())
				.setLocalNodeId(localNode.getLocalId().toString());
		
		Set<Neighbor> outNeighbors = localNode.getOutNeighbors();
		
		for (Neighbor n : outNeighbors) {
			builder = builder.addOutNeighbors(n.getId().toString());
		}
		
		for (int i = 0 ; i < eigenvalues.length; i++) {
			builder.addEigenvalues(eigenvalues[i]);
		}
		
		return builder.build();
	}
	
}
