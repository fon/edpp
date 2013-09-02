package io;

import java.io.Console;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.List;

import app.PushSumEngine;
import core.ProtocolEngine;
import domain.SamplingParameters;
import domain.Session;
import domain.network.PastryOverlayNode;
import evaluation.EvaluationResults;
import evaluation.Evaluator;
import rice.environment.Environment;
import rice.pastry.NodeIdFactory;
import rice.pastry.PastryNode;
import rice.pastry.PastryNodeFactory;
import rice.pastry.socket.SocketPastryNodeFactory;
import rice.pastry.standard.RandomNodeIdFactory;

/**
 * 
 * @author Xenofon Foukas
 * 
 */
public class EvaluationPastryNode {

	private ProtocolEngine pe;
	private PastryNode node;
	private PushSumEngine pse;

	public EvaluationPastryNode(int bindport, InetSocketAddress bootaddress,
			Environment env) throws Exception {

		// Generate the NodeIds Randomly
		NodeIdFactory nidFactory = new RandomNodeIdFactory(env);

		// construct the PastryNodeFactory, this is how we use
		// rice.pastry.socket
		PastryNodeFactory factory = new SocketPastryNodeFactory(nidFactory,
				bindport, env);

		// construct a node
		node = factory.newNode();

		// construct a new MyApp
		PastryOverlayNode pon = new PastryOverlayNode(node);
		pe = new ProtocolEngine(pon);
		node.boot(bootaddress);

		// the node may require sending several messages to fully boot into the
		// ring
		synchronized (node) {
			while (!node.isReady() && !node.joinFailed()) {
				// delay so we don't busy-wait
				node.wait(500);

				// abort if can't join
				if (node.joinFailed()) {
					throw new IOException(
							"Could not join the FreePastry ring.  Reason:"
									+ node.joinFailedReason());
				}
			}
		}
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				terminate();
			}
		});

		pse = new PushSumEngine(pon, bootaddress.getAddress());
		System.out.println("Finished creating new node " + node);
		System.out.println("Wait to properly join pastry overlay");
		// wait 10 seconds
		env.getTimeSource().sleep(5000);
		System.out.println("Application ready");

	}

	public void terminate() {
		pe.terminate();
		node.destroy();
	}

	public ProtocolEngine getProtocolEngine() {
		return this.pe;
	}

	public Session requestSamplingData(int numberOfExecutions,
			int numberOfRounds) {
		SamplingParameters sp = new SamplingParameters(numberOfExecutions,
				numberOfRounds);
		return pe.requestSessionData(sp);
	}

	public double requestSize() {
		return this.pse.estimateSize(true, 3.14);
	}

	/**
	 * Usage: java [-cp FreePastry-<version>.jar]
	 * rice.tutorial.lesson3.DistTutorial localbindport bootIP bootPort example
	 * java rice.tutorial.DistTutorial 9001 pokey.cs.almamater.edu 9001
	 */
	public static void main(String[] args) throws Exception {
		// Loads pastry settings
		Environment env = new Environment();
		Evaluator eval;

		// disable the UPnP setting (in case you are testing this on a NATted
		// LAN)
		env.getParameters().setString("nat_search_policy", "never");

		try {
			// the port to use locally
			int bindport = Integer.parseInt(args[0]);

			// build the bootaddress from the command line args
			InetAddress bootaddr = InetAddress.getByName(args[1]);
			int bootport = Integer.parseInt(args[2]);
			InetSocketAddress bootaddress = new InetSocketAddress(bootaddr,
					bootport);

			// launch our node!
			EvaluationPastryNode dt = new EvaluationPastryNode(bindport,
					bootaddress, env);
			if (bootaddr.isAnyLocalAddress() || bootaddr.isLoopbackAddress()
					|| NetworkInterface.getByInetAddress(bootaddr) != null) {
				eval = new Evaluator(dt.getProtocolEngine(), bootaddr, true);
			} else {
				eval = new Evaluator(dt.getProtocolEngine(), bootaddr, false);
			}

			Console c = System.console();
			if (c == null) {
				System.err.println("No console.");
				System.err.println("Running in support mode");
			} else {
				while (true) {
					String command = c.readLine("Command:");
					List<String> items = Arrays.asList(command.split("\\s+"));
					if (items.get(0).equals("evaluate")) {
						if (items.size() != 4) {
							System.out
									.println("Must give three arguments - (evaluate numOfExecutions numOfRounds error)");
							continue;
						}
						int numberOfExecutions = Integer.parseInt(items.get(1));
						int numberOfRounds = Integer.parseInt(items.get(2));
						double error = Double.parseDouble(items.get(3));
						EvaluationResults results = eval.evaluateEngine(
								numberOfExecutions, numberOfRounds, error);

						System.out.println("Evaluation complete...");
						System.out
								.println("The 50th percentile of the spectral gap percent error is "
										+ results
												.getSpectralGapPercentError(50));
						System.out
								.println("The 50th percentile of the mixing time percent error is "
										+ results.getMixingTimePercentError(50));
						System.out
								.println("The 10th percentile of the spectral gap percent error is "
										+ results
												.getSpectralGapPercentError(10));
						System.out
								.println("The 10th percentile of the mixing time percent error is "
										+ results.getMixingTimePercentError(10));
						System.out
								.println("The 90th percentile of the spectral gap percent error is "
										+ results
												.getSpectralGapPercentError(90));
						System.out
								.println("The 90th percentile of the mixing time percent error is "
										+ results.getMixingTimePercentError(90));
					} else if (items.get(0).equals("exit")) {
						// dt.terminate();
						System.out.println("Exiting...");
						System.exit(0);
					} else if (items.get(0).equals("size")) {
						System.out.println(dt.requestSize());
					} else {
						System.out
								.println("Accepted commands are evaluate and exit...");
					}
				}
			}
		} catch (Exception e) {
			// remind user how to use
			System.out.println("Usage:");
			System.out
					.println("java -jar edpp.jar localbindport bootIP bootPort totalNumOfNodes");
			System.out
					.println("example java -jar edpp.jar 9001 planetlab-1.imperial.ac.uk 9001 100");
			throw e;
		}
	}

}