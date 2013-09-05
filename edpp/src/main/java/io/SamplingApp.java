package io;

import java.io.Console;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import rice.environment.Environment;
import rice.pastry.NodeIdFactory;
import rice.pastry.PastryNode;
import rice.pastry.PastryNodeFactory;
import rice.pastry.socket.SocketPastryNodeFactory;
import rice.pastry.standard.RandomNodeIdFactory;
import analysis.Analyzer;
import app.PushSumEngine;
import core.ProtocolEngine;
import domain.SamplingParameters;
import domain.Session;
import domain.network.PastryOverlayNode;

/**
 * Demonstration application used for estimating the average number of files per
 * node in the network by using the mixing time estimation provided by the
 * decentralized impulse response protocol. The protocol runs continuously on
 * the background and provides new estimations
 * 
 * @author Xenofon Foukas
 * 
 */
public class SamplingApp {

	private ProtocolEngine pe;
	private PastryNode node;
	private PushSumEngine pse;

	public SamplingApp(int bindport, InetSocketAddress bootaddress,
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

		pe = new ProtocolEngine(pon);
		pse = new PushSumEngine(pon, bootaddress.getAddress());
		System.out.println("Finished creating new node " + node);
		System.out.println("Wait to properly join pastry overlay");
		// wait 10 seconds
		env.getTimeSource().sleep(5000);
		System.out.println("Application ready");
	}

	public double requestSize(double mixingTime) {
		return this.pse.estimateSize(true, mixingTime);
	}

	public ProtocolEngine getProtocolEngine() {
		return this.pe;
	}

	public void terminate() {
		pe.terminate();
		node.destroy();
	}

	/**
	 * Usage: java edpp.jar localbindport bootIP bootPort example: java -jar
	 * edpp.jar 9001 planetlab-1.imperial.ac.uk 9001
	 */
	public static void main(String[] args) throws Exception {

		BlockingQueue<Double> currentMixingTimeEstimate = new LinkedBlockingQueue<Double>(
				1);

		// Set logging to a file instead of the console
		Logger rootLogger = Logger.getLogger("");
		FileHandler logHandler = new FileHandler("logger.txt",
				200 * 1024 * 1024, 5, false);
		logHandler.setFormatter(new SimpleFormatter());
		logHandler.setLevel(Level.INFO);
		rootLogger.removeHandler(rootLogger.getHandlers()[0]);
		rootLogger.setLevel(Level.INFO);
		rootLogger.addHandler(logHandler);

		// class to perform the samplings on the background in order to provide estimates of the mixing time
		class Sampler implements Runnable {

			private ProtocolEngine p;
			private BlockingQueue<Double> estimate;
			private InetAddress bootaddr;

			public Sampler(ProtocolEngine protoEngine,
					BlockingQueue<Double> estimate, InetAddress bootaddr) {
				this.p = protoEngine;
				this.estimate = estimate;
				this.bootaddr = bootaddr;
			}

			@Override
			public void run() {
				// make a sampling request for a single execution of 20 rounds
				SamplingParameters sp = new SamplingParameters(1, 20);
				Session s = null;
				boolean isDemoNode = false;
				// check whether this is the node where the demonstration will
				// be performed
				try {
					isDemoNode = (bootaddr.isAnyLocalAddress()
							|| bootaddr.isLoopbackAddress() || NetworkInterface
							.getByInetAddress(bootaddr) != null);
				} catch (SocketException e) {
					e.printStackTrace();
				}
				while (true) {
					if (isDemoNode) {
						// if this is the demo node make a request for the
						// mixing time
						s = p.requestSessionData(sp);
						double mixingTime = Analyzer.computeMixingTime(
								s.getComputedEigenvalues(), 0.001);
						estimate.clear();
						estimate.offer(mixingTime);
						try {
							Thread.sleep(30000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}

		}

		// Loads pastry settings
		Environment env = new Environment();

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

			// launch our node
			SamplingApp sa = new SamplingApp(bindport, bootaddress, env);
			ExecutorService executorService = Executors.newFixedThreadPool(1);
			executorService.execute(new Sampler(sa.getProtocolEngine(),
					currentMixingTimeEstimate, bootaddr));

			Console c = System.console();
			if (c == null) {
				System.err.println("No console.");
				System.err.println("Running in support mode");
			} else {
				while (true) {
					String command = c.readLine("Command:");
					List<String> items = Arrays.asList(command.split("\\s+"));
					if (items.get(0).equals("size")) {
						// Get mixing time
						double mixingTime = currentMixingTimeEstimate.take();
						currentMixingTimeEstimate.offer(mixingTime);
						System.out.println(sa.requestSize(mixingTime));
					} else if (items.get(0).equals("exit")) {
						System.out.println("Exiting...");
						System.exit(0);
					} else {
						System.out
								.println("Accepted commands are size and exit...");
					}
				}
			}
		} catch (Exception e) {
			// remind user how to use
			System.out.println("Usage:");
			System.out
					.println("java -jar edpp.jar localbindport bootIP bootPort");
			System.out
					.println("example java -jar edpp.jar 9001 planetlab-1.imperial.ac.uk 9001");
			throw e;
		}
	}

}
