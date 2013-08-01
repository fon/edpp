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

import algorithms.Algorithms;
import analysis.Analyzer;

import comm.ProtocolMessage.SessionEvent;
import comm.ProtocolMessage.SessionEvent.EventType;

import util.SamplingParameters;
import core.ProtocolEngine;
import core.RecordedSession;
import core.Session;
import core.SessionListener;

public class Evaluator {
	
	public static final int EVALUATION_PORT = 34567;

	private ProtocolEngine pe;
	private boolean initiator;
	private SessionListener listener;
	private List<SessionEvent> initialEvents;
	private List<SessionEvent> terminationEvents;
	private ExecutorService executor;
	private IncomingEvaluationDataListener iedl; 
	
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
						System.out.println("Received an initiation event from node with IP "+s.getInetAddress());
						initialEvents.add(se);
					} else {
						System.out.println("Received a termination event");
						terminationEvents.add(se);
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
					initialEvents.add(e);
				} else {
					try {
						Socket s = new Socket(evaluatorAddress, EVALUATION_PORT);
						e.writeTo(s.getOutputStream());
						s.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
			
			@Override
			public void sessionCompleted(SessionEvent e) {
				if (initiator) {
					terminationEvents.add(e);
				} else {
					try {
						Socket s = new Socket(evaluatorAddress, EVALUATION_PORT);
						e.writeTo(s.getOutputStream());
						s.close();
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
			Session evalSession = pe.requestSessionData(sp);
			EvaluationResults er = new EvaluationResults(evalSession.getSessionId());
			try {
				Thread.sleep(5000);
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
			
			double [] expectedEigenvals = Algorithms.computeEigenvaluesModulus(terminalGraph.getMatrixOfWeights());
			double expectedGap = Analyzer.computeSpectralGap(expectedEigenvals);
			er.setExpectedSpectralGap(expectedGap);
			double expectedMixingTime = Analyzer.computeMixingTime(expectedEigenvals, error);
			er.setExpectedMixingTime(expectedMixingTime);
			System.out.println("The expected mixing time is: "+expectedMixingTime);
			System.out.println("The computed mixing time is: "+Analyzer.computeMixingTime(evalSession.getComputedEigenvalues(), error));
			System.out.println("The expected spectral gap is: "+expectedGap);
			System.out.println("The computed spectral gap is: "+Analyzer.computeSpectralGap(evalSession.getComputedEigenvalues()));
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
