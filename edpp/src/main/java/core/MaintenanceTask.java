package core;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import network.Node;

import util.Phase;
import util.PlainNeighbor;
import util.PlainNeighborsTable;
import util.TimedNeighbor;
import util.TimedNeighborsTable;

import comm.MessageBuilder;
import comm.TransferableMessage;
import comm.ProtocolMessage.Message;
import comm.ProtocolMessage.Message.MessageType;

public class MaintenanceTask implements Runnable {
	
	private Map<String, Session> sessions;
	private BlockingQueue<TransferableMessage> outgoingQueue;
	private Node localNode;

	public MaintenanceTask(Map<String, Session> sessions,
			BlockingQueue<TransferableMessage> outgoingQueue,
			Node localNode) {
		this.sessions = sessions;
		this.outgoingQueue = outgoingQueue;
		this.localNode = localNode;
	}

	@Override
	public void run() {
		
		Execution e;
		TimedNeighborsTable inNeighbors;
		
		for (Session s : sessions.values()) {
			//Get all executions of the session
			for (int i = 1; i <= s.getCurrentNumberOfExecutions(); i++) {
				e = s.getExecution(i);
				if (e != null){
					//Check if it is in the INIT phase
					if (e.getPhase() == Phase.INIT) {
						//Check if the INIT phase should end
						if (e.remainingInitTime() <= 0) {
							/*
							 * Enter DATA_EXCHANGE phase
							 * Go to next round and
							 * send message to all out-neighbors
							 */
							e.setPhase(Phase.DATA_EXCHANGE);
							sendOutMessage(MessageType.NEXT, s, e, e.getExecutionNumber(), e.getCurrentRound());
							e.recomputeWeight();
							e.setRound(2);
						}
					} else if (e.getPhase() == Phase.DATA_EXCHANGE) { /* Check for in-neighbors suspected of failure*/
						inNeighbors = e.getInNeighbors();
						boolean endOfRound = true;
						synchronized (inNeighbors) {
							Iterator<TimedNeighbor> iter = inNeighbors.iterator();
							while (iter.hasNext()) {
								TimedNeighbor neighbor = iter.next();
								long remainingTime = neighbor.getTimeToProbe();
								if (remainingTime != TimedNeighbor.INF)
									neighbor.decreaseTime(ProtocolEngine.TIMEOUT);
								if (neighbor.getTimeToProbe() <= 0) { /* Check whether the node is alive */
									if (localNode.isAlive(neighbor)) {
										inNeighbors.renewTimer(neighbor);
										endOfRound = false;
									} else {
										iter.remove();
									}
								} else if (neighbor.getTimeToProbe() != TimedNeighbor.INF) {
									endOfRound = false;
								}
							}
						}
						if (endOfRound) {
							// If it is the end of the round check if we have another round
							if (e.hasAnotherRound()) {
								sendOutMessage(MessageType.NEXT, s, e, e.getExecutionNumber(), e.getCurrentRound());
								e.recomputeWeight();
								e.setRound(e.getCurrentRound() + 1);
								e.getInNeighbors().renewTimers();
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
						}
					} else if (e.getPhase() == Phase.GOSSIP) {
						//TODO handle GOSSIP
						//Almost identical to the DATA_EXCHANGE phase
					}
				}
			}
		}
	}
	
	private void sendGossipMessage(Message m, Execution e) {
		InetAddress address;
		PlainNeighborsTable pnt = e.getOutNeighbors();
		
		synchronized (pnt) {
			for (PlainNeighbor n : pnt) {
				address = n.getAddress();
				outgoingQueue.add(new TransferableMessage(m, address));
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
				outgoingQueue.add(new TransferableMessage(outMessage, address));
			}
		}
	}

}
