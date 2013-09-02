package evaluation;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*import javax.swing.JFrame;

import org.jgraph.JGraph;
import org.jgrapht.ext.JGraphModelAdapter;*/

import java.util.logging.Logger;

import org.jblas.DoubleMatrix;

import algorithms.Algorithms;
import analysis.Analyzer;
import comm.ProtocolMessage.SessionEvent;
import comm.ProtocolMessage.SessionEvent.EventType;
import core.ProtocolEngine;
import domain.RecordedSession;
import domain.SamplingParameters;
import domain.Session;
import event.SessionListener;

public class Evaluator {
	
	public static final int EVALUATION_PORT = 34567;

	private ProtocolEngine pe;
	private boolean initiator;
	private SessionListener listener;
	private List<SessionEvent> initialEvents;
	private List<SessionEvent> terminationEvents;
	private ExecutorService executor;
	private IncomingEvaluationDataListener iedl; 
	
	private Logger logger;
	
	class IncomingEvaluationDataListener implements Runnable {
		
		Socket s;
		ServerSocket ss;
		
		@Override
		public void run() {
			try {
				ss = new ServerSocket(EVALUATION_PORT);
				
				while ((s = ss.accept()) != null) {
					SessionEvent se = SessionEvent.parseFrom(s.getInputStream());
					if (se.getType() == EventType.INITIAL) {
						logger.info("Received an initiation event from node with address "+s.getInetAddress());
						synchronized (initialEvents) {
							initialEvents.add(se);
						}
					} else {
						logger.info("Received a termination event from node with address "+s.getInetAddress());
						synchronized (terminationEvents) {
							terminationEvents.add(se);
						}
					}
					s.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	
	public Evaluator(ProtocolEngine pe, final InetAddress evaluatorAddress, boolean init) {
		
		this.pe = pe;
		this.initiator = init;
		
		logger = Logger.getLogger(Evaluator.class.getName());
		
		initialEvents = new ArrayList<SessionEvent>();
		terminationEvents = new ArrayList<SessionEvent>();
		
		if (initiator) {
			executor = Executors.newSingleThreadExecutor();
			iedl = new IncomingEvaluationDataListener();
			executor.execute(iedl);
		}

		listener = new SessionListener() {
			
			@Override
			public void sessionInitiated(SessionEvent e) {
				if (initiator) {
					logger.info("Received an initiation SessionEvent by node "+e.getLocalNodeId()
							+" for session with id "+e.getSessionId());
					synchronized (initialEvents) {
						initialEvents.add(e);
					}
				} else {
					try {
						Socket s = new Socket(evaluatorAddress, EVALUATION_PORT);
						e.writeTo(s.getOutputStream());
						s.close();
						logger.info("Sent an initiation SessionEvent to node with address "+evaluatorAddress
								+" for session with id "+e.getSessionId());
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
			
			@Override
			public void sessionCompleted(SessionEvent e) {
				if (initiator) {
					logger.info("Received a termination SessionEvent by node "+e.getLocalNodeId()
							+" for session with id "+e.getSessionId());
					synchronized (terminationEvents) {
						terminationEvents.add(e);
					}
				} else {
					try {
						System.out.println("At least got here");
						Socket s = new Socket(evaluatorAddress, EVALUATION_PORT);
						e.writeTo(s.getOutputStream());
						s.close();
						System.out.println("Sent a termination SessionEvent to node with address "+evaluatorAddress
								+" for session with id "+e.getSessionId());
						logger.info("Sent a termination SessionEvent to node with address "+evaluatorAddress
								+" for session with id "+e.getSessionId());
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}

			@Override
			public void sessionStored(RecordedSession rs) {
				return;
			}
		};
		pe.addSessionListener(listener);
	}
	
	public EvaluationResults evaluateEngine(int numberOfExecutions, int numberOfRounds, double error) {
		
		initialEvents = new ArrayList<SessionEvent>();
		terminationEvents = new ArrayList<SessionEvent>();
		
		
		if (initiator) {
			SamplingParameters sp = new SamplingParameters(numberOfExecutions, numberOfRounds);
			logger.info("Making a request for sampling data");
			Session evalSession = pe.requestSessionData(sp);
			EvaluationResults er = new EvaluationResults(evalSession.getSessionId());
			logger.info("Sampling completed...");
			try {
				Thread.sleep(60000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			NetworkGraph initialGraph = new NetworkGraph();
			NetworkGraph terminalGraph = new NetworkGraph();
			
			/*JGraph graph1 = new JGraph( new JGraphModelAdapter( initialGraph.getGraph() ) );
			JGraph graph2 = new JGraph( new JGraphModelAdapter( terminalGraph.getGraph() ) );

			JFrame frame = new JFrame();
			frame.getContentPane().add(graph1);
			frame.setSize(300, 200);
		    frame.setVisible(true);*/
		      
			logger.info("Constructing the initial graph of the network topology");
			synchronized (initialEvents) {
				for (SessionEvent se : initialEvents) {
					//Construct initial graph
					initialGraph.addNode(se.getLocalNodeId());
					List<String> outNeighbors = se.getOutNeighborsList();
					double weight = (double) 1 / outNeighbors.size();
					for (String outNeighbor : outNeighbors) {
						initialGraph.addNode(outNeighbor);
						initialGraph.addLinkWithWeight(se.getLocalNodeId(), outNeighbor, weight);
					}
				}
			}
			logger.info("Constructing the final graph of the network topology");
			synchronized (terminationEvents) {
				for (SessionEvent se : terminationEvents) {
					//Construct final graph
					terminalGraph.addNode(se.getLocalNodeId());
					List<String> outNeighbors = se.getOutNeighborsList();
					double weight = (double) 1 / outNeighbors.size();
					for (String outNeighbor : outNeighbors) {
						terminalGraph.addNode(outNeighbor);
						terminalGraph.addLinkWithWeight(se.getLocalNodeId(), outNeighbor, weight);
					}
					
					List<Double> eigenvalues = se.getEigenvaluesList();
					double [] computedEigenvals = toDoubleArray(eigenvalues);
					double computedGap = Analyzer.computeSpectralGap(computedEigenvals);
					er.addComputedSpectralGap(computedGap);
					double computedMixingTime = Analyzer.computeMixingTime(computedEigenvals, error);
					er.addComputedMixningTime(computedMixingTime);		
					
				}
			}
			logger.info("Construction of graphs completed");
			System.out.println("Initial graph:");
			System.out.println(initialGraph.getGraph().toString()+"\n");
			System.out.println("Final graph:");
			System.out.println(terminalGraph.getGraph().toString()+"\n");
			
			double [][] matrixOfWeights = terminalGraph.getMatrixOfWeights();
			terminalGraph.exportAdjacencyMatrix();
			DoubleMatrix dm = new DoubleMatrix(matrixOfWeights);
			double [] expectedEigenvals = Algorithms.computeEigenvaluesModulus(dm);
			double expectedGap = Analyzer.computeSpectralGap(expectedEigenvals);
			er.setExpectedSpectralGap(expectedGap);
			double expectedMixingTime = Analyzer.computeMixingTime(expectedEigenvals, error);
			er.setExpectedMixingTime(expectedMixingTime);
			System.out.println("The expected mixing time is: "+expectedMixingTime);
			System.out.println("The computed mixing time is: "+Analyzer.computeMixingTime(evalSession.getComputedEigenvalues(), error));
			System.out.println("The expected spectral gap is: "+expectedGap);
			System.out.println("The computed spectral gap is: "+Analyzer.computeSpectralGap(evalSession.getComputedEigenvalues()));
			System.out.println("The computed spectral gap v2 is: "+Analyzer.computeSpectralGap2(evalSession.getComputedEigenvalues()));
			return er;
		}
		return null;
	}
	
	public void detachEvaluator() {
		pe.removeSessionListener(listener);
		executor.shutdown();
		try {
			iedl.s.close();
			iedl.ss.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private double[] toDoubleArray(List<Double> list){
		double[] ret = new double[list.size()];
		for(int i = 0;i < ret.length;i++)
			ret[i] = list.get(i);
		return ret;
	}
	
}
