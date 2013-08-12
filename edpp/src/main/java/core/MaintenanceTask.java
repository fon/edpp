package core;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

import org.jblas.DoubleMatrix;

import network.Node;
import storage.Database;
import util.Phase;
import util.PlainNeighbor;
import util.PlainNeighborsTable;
import util.TimedNeighbor;
import util.TimedNeighborsTable;
import comm.MessageBuilder;
import comm.MessageSender;
import comm.ProtocolMessage.SessionEvent;
import comm.TransferableMessage;
import comm.ProtocolMessage.Message;
import comm.ProtocolMessage.Message.MessageType;
import comm.ProtocolMessage.SessionEvent.EventType;

public class MaintenanceTask implements Runnable {
	
	private  Logger logger;
	
	private Map<String, Session> sessions;
	private BlockingQueue<TransferableMessage> outgoingQueue;
	private Node localNode;
	private Database db;
	private static List<SessionListener> sessionListeners = new LinkedList<SessionListener>();

	public MaintenanceTask(Map<String, Session> sessions,
			BlockingQueue<TransferableMessage> outgoingQueue,
			Node localNode,
			Database db) {
		logger = Logger.getLogger(MaintenanceTask.class.getName());
		this.sessions = sessions;
		this.outgoingQueue = outgoingQueue;
		this.localNode = localNode;
		this.db = db;
	}

	@Override
	public void run() {
		logger.fine("Running maintenance task");
		Execution e;
		TimedNeighborsTable inNeighbors;
		Collection<Session> sessionSet = sessions.values();
		
		Iterator<Session> sessionIter = sessionSet.iterator();
		Session s;
		while (sessionIter.hasNext()) {
			s = sessionIter.next();
			//Get all executions of the session
			for (int i = 1; i <= s.getCurrentNumberOfExecutions(); i++) {
				e = s.getExecution(i);
				if (e != null){
//					logger.info("Checking execution "+e.getExecutionNumber()+" of session "+s.getSessionId());
					//Check if it is in the INIT phase
					if (e.getPhase() == Phase.INIT) {
						//Check if the INIT phase should end
						if (e.remainingInitTime() <= 0) {
							logger.info("Stage is "+e.getPhase()+" and remaining time is "+e.remainingInitTime());
							logger.info("The INIT phase of Execution "+e.getExecutionNumber()+" in Session "
									+ s.getSessionId()+" is over. Entering the DATA_EXCHANGE phase");
							/*
							 * Enter DATA_EXCHANGE phase
							 * Go to next round and
							 * send message to all out-neighbors
							 */
							e.setPhase(Phase.DATA_EXCHANGE);
//							sendOutNextMessage(MessageType.NEXT, s, e);
//							e.recomputeWeight();
							e.setRound(2);
							checkForNewExecution(s, e);
							logger.info("Recomputed weights and set round to "+e.getCurrentRound()+
									" in execution "+e.getExecutionNumber()+" of session "+s.getSessionId());
							if (e.hasAnotherRound()) {
								sendOutNextMessage(MessageType.NEXT, s, e);
//								e.recomputeWeight();
								e.getInNeighbors().renewTimers();
								int newRound = e.getCurrentRound()+1;
								checkForNewExecution(s, e);
								logger.info("Round "+e.getCurrentRound()+" of execution "+e.getExecutionNumber()
										+" in session "+s.getSessionId()+" is over. Going to round "+newRound);
								e.setRound(newRound);
							} else {
								logger.info("Round "+e.getCurrentRound()+" of execution "+e.getExecutionNumber()
										+" in session "+s.getSessionId()+" is over. This was the final round. "
												+ "Switching to GOSSIP phase");
								e.setPhase(Phase.GOSSIP);
								DoubleMatrix rm = e.computeRealizationMatrix();
								logger.info("The realization matrix of execution "+e.getExecutionNumber()
										+" of session "+s.getSessionId()+" was computed");
//								rm.print();
								e.transferPendingGossipMessages();
//								e.getRealizationMatrix().print(NumberFormat.FRACTION_FIELD, 5);
								//compute the eigenvalues of the approximation matrix
								//TODO probably should test this for null
								double [] eigenvals = e.getMatrixAEigenvalues();
								String eig = "[";
								for (int j = 0; j < eigenvals.length; j++) {
									eig+=eigenvals[j];
									eig+=", ";
								}
								eig+="]";
								logger.info("The computed eigenvalues of execution "+e.getExecutionNumber()
										+" of session "+s.getSessionId()+" are "+eig);
								Message msg = MessageBuilder.buildGossipMessage(localNode.getLocalId().toString(),
										s.getSessionId(), e.getExecutionNumber(), eigenvals);
								//send GOSSIP message to out-neighbors
								sendGossipMessage(msg, e);
							}
						}
					} else if (e.getPhase() == Phase.DATA_EXCHANGE) { /* Check for in-neighbors suspected of failure*/
						e.setProperTimersToInf();
						inNeighbors = e.getInNeighbors();
						boolean endOfRound = true;
//						logger.info("Got in neighbors list and now time to check their times");
						synchronized (inNeighbors) {
							Iterator<TimedNeighbor> iter = inNeighbors.iterator();
//							System.out.println("Making check now");
							while (iter.hasNext()) {
								TimedNeighbor neighbor = iter.next();
//								logger.info("Checking neighbor with id "+neighbor.getId().toString());
								long remainingTime = neighbor.getTimeToProbe();
								if (remainingTime != TimedNeighbor.INF)
									neighbor.decreaseTime(ProtocolController.TIMEOUT);
								if (neighbor.getTimeToProbe() <= 0) { /* Check whether the node is alive */
//									logger.info("Probing node to see if it is alive");
									if (isAlive(neighbor)) {
										requestPreviousVal(s.getSessionId(), e.getExecutionNumber(),
												e.getCurrentRound(), neighbor.getAddress());
										//Send message to request again the same packet
//										logger.info("Node is still alive");
										inNeighbors.renewTimer(neighbor);
										endOfRound = false;
									} else {
										logger.info("Node "+neighbor.getId().toString()+" is no longer alive."
												+ " Removing it from in-neihgbors table of Execution "+
												e.getExecutionNumber()+" in Session "+s.getSessionId());
										iter.remove();
										if (localNode.removeOutNeighborNode(neighbor.getId().toString())) {
											logger.info("Node "+neighbor.getId().toString()+" is no longer alive."
													+ " Removing it from out-neihgbors table");
										}
									}
								} else if (neighbor.getTimeToProbe() != TimedNeighbor.INF) {
									endOfRound = false;
								}
							}
						}
						if (endOfRound) {
							e.getInNeighbors().renewTimers();
							// If it is the end of the round check if we have another round
							if (e.hasAnotherRound()) {
								sendOutNextMessage(MessageType.NEXT, s, e);
//								e.recomputeWeight();
								int newRound = e.getCurrentRound()+1;
								logger.info("Round "+e.getCurrentRound()+" of execution "+e.getExecutionNumber()
										+" in session "+s.getSessionId()+" is over. Going to round "+newRound);
								e.setRound(e.getCurrentRound() + 1);
								checkForNewExecution(s, e);
							} else {
								logger.info("Round "+e.getCurrentRound()+" of execution "+e.getExecutionNumber()
										+" in session "+s.getSessionId()+" is over. This was the final round. "
												+ "Switching to GOSSIP phase");
								e.setRound(e.getCurrentRound()+1);
								e.setPhase(Phase.GOSSIP);
								DoubleMatrix rm = e.computeRealizationMatrix();
								logger.info("The realization matrix of execution "+e.getExecutionNumber()
										+" of session "+s.getSessionId()+" was computed");
//								rm.print();
								e.transferPendingGossipMessages();
//								e.getRealizationMatrix().print(NumberFormat.FRACTION_FIELD, 5);
								//compute the eigenvalues of the approximation matrix
								//TODO probably should test this for null
								double [] eigenvals = e.getMatrixAEigenvalues();
								String eig = "[";
								for (int j = 0; j < eigenvals.length; j++) {
									eig+=eigenvals[j];
									eig+=", ";
								}
								eig+="]";
								logger.info("The computed eigenvalues of execution "+e.getExecutionNumber()
										+" of session "+s.getSessionId()+" are "+eig);
								Message msg = MessageBuilder.buildGossipMessage(localNode.getLocalId().toString(),
										s.getSessionId(), e.getExecutionNumber(), eigenvals);
								//send GOSSIP message to out-neighbors
								sendGossipMessage(msg, e);
							}
						}
					} else if (e.getPhase() == Phase.GOSSIP) {
						e.transferPendingGossipMessages();
						inNeighbors = e.getInNeighbors();
						
						boolean endOfRound = true;
						synchronized (inNeighbors) {
							Iterator<TimedNeighbor> iter = inNeighbors.iterator();
							while (iter.hasNext()) {
								TimedNeighbor neighbor = iter.next();
								long remainingTime = neighbor.getTimeToProbe();
								if (remainingTime != TimedNeighbor.INF)
									neighbor.decreaseTime(ProtocolController.TIMEOUT);
								if (neighbor.getTimeToProbe() <= 0) { /* Check whether the node is alive */
									if (isAlive(neighbor)) {
										inNeighbors.renewTimer(neighbor);
										endOfRound = false;
									} else {
										logger.info("Node "+neighbor.getId().toString()+" is no longer alive."
												+ " Removing it from in-neihgbors table of Execution "+
												e.getExecutionNumber()+" in Session "+s.getSessionId());
										iter.remove();
										if (localNode.removeOutNeighborNode(neighbor.getId().toString())) {
											logger.info("Node "+neighbor.getId().toString()+" is no longer alive."
													+ " Removing it from out-neihgbors table");
										}
									}
								} else if (neighbor.getTimeToProbe() != TimedNeighbor.INF) {
									endOfRound = false;
								}
							}
						}
						if (endOfRound) {
							double [] medianEig = e.computeMedianEigenvalues();
							String eig = "[";
							for (int j = 0; j < medianEig.length; j++) {
								eig+=medianEig[j];
								eig+=", ";
							}
							eig+="]";
							logger.info("Gossip round of execution "+e.getExecutionNumber()+" of session "
									+s.getSessionId()+" is now over. The median of all received eigenvalues is "+
									eig);
							e.setPhase(Phase.TERMINATED);
							s.addCompletedExecution();
							if (s.hasTerminated()) {
								sessionIter.remove();
								RecordedSession recSes = new RecordedSession(s);
								db.addSession(recSes);
								//Notify all listeners
								for (SessionListener sl : sessionListeners) {
									SessionEvent se = MessageBuilder.buildNewSessionEvent(s, localNode, EventType.TERMINAL);
									sl.sessionCompleted(se);
								}
							}
						}
					}
				}
			}
		}
	}
	
	//TODO must add test
	public static void addSessionListener(SessionListener listener) {
		sessionListeners.add(listener);
	}
	
	//TODO must add test
	public static boolean removeSessionListener(SessionListener listener) {
		return sessionListeners.remove(listener);
	}
	
	private boolean isAlive(TimedNeighbor neighbor) {
		Message m = MessageBuilder.buildLivenessMessage();
		return MessageSender.makeLivenessCheck(
				new TransferableMessage(m, neighbor.getAddress(), true));
	}

	private void sendGossipMessage(Message m, Execution e) {
		InetAddress address;
		PlainNeighborsTable pnt = e.getOutNeighbors();
		
		synchronized (pnt) {
			for (PlainNeighbor n : pnt) {
				address = n.getAddress();
				outgoingQueue.add(new TransferableMessage(m, address, true));
			}
		}
	}
	
	private void checkForNewExecution(Session s, Execution e) {
		if (s.isInitiator() && e.equals(s.getInitExecution())
				&& s.newExecutionExpected()) {
			Execution newExecution;
			newExecution = s.createNewExecution();
			
			InetAddress address;
			double valueToSend;
			Message outMessage;
			String sessionId = s.getSessionId();
			String nodeId = localNode.getLocalId().toString();
			PlainNeighborsTable pnt = e.getOutNeighbors();
			
			synchronized (pnt) {
				for (PlainNeighbor n : pnt) {
					address = n.getAddress();
					valueToSend = newExecution.getCurrentValue() * n.getWeight();
					logger.info("Sending INIT message to node "+n.getId()+" with address "+address+
							" for execution "+newExecution.getExecutionNumber()+
							" of session "+sessionId);
					outMessage = MessageBuilder.buildInitMessage(nodeId, sessionId, 
								newExecution.getExecutionNumber(), s.getNumberOfExecutions(),
								s.getNumberOfRounds(), valueToSend);
					outgoingQueue.add(new TransferableMessage(outMessage, address, true));
				}
			}
		}
	}
	
	private void sendOutNextMessage(MessageType type, Session session, Execution execution) {
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
				//round number + 1, because we send the message for a round
				//using the value of the previous round
				int r = execution.getCurrentRound() + 1;
				logger.info("Sending NEXT message to node "+n.getId()+" with address "+address+
						" for round "+r+" of execution "+execution.getExecutionNumber()+
						" in session "+sessionId);
				outMessage = MessageBuilder.buildNextMessage(nodeId,
						sessionId, execution.getExecutionNumber(), 
						r, valueToSend);
				outgoingQueue.add(new TransferableMessage(outMessage, address, false));
			}
		}
	}

	
	private void requestPreviousVal(String sessionId, int execNum, int round, InetAddress address) {
		String nodeId = localNode.getLocalId().toString();
		Message m = MessageBuilder.requestPreviousValMessage(nodeId,sessionId, execNum, round);
		TransferableMessage tm = new TransferableMessage(m, address,false);
		logger.info("Sending REQUEST message to node with address "+address+
				" for round "+round+" of execution "+execNum+
				" in session "+sessionId);
		outgoingQueue.add(tm);
	}
}
