package comm;

import comm.ProtocolMessage.Message;
import comm.ProtocolMessage.Message.Builder;
import comm.ProtocolMessage.Message.MessageType;

public class MessageBuilder {

	public static Message buildNewMessage(int numOfExecutions, int numOfRounds) {
		Message m =
				Message.newBuilder()
				.setType(MessageType.NEW)
				.setExecution(numOfExecutions)
				.setRound(numOfRounds)
				.build();
		
		return m;
	}
	
	public static Message buildInitMessage(String nodeId, String sessionId, 
			int numOfExecutions, int numOfRounds, double value) {
		Message m = 
				Message.newBuilder()
				.setNodeId(nodeId)
				.setType(MessageType.INIT)
				.setSession(sessionId)
				.setExecution(numOfExecutions)
				.setRound(numOfRounds)
				.setVal(value)
				.build();
				
		return m;
	}
	
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
	
}
