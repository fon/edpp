package io;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import network.PastryOverlayNode;

import core.ProtocolEngine;
import core.Session;

import rice.environment.Environment;
import rice.pastry.NodeIdFactory;
import rice.pastry.PastryNode;
import rice.pastry.PastryNodeFactory;
import rice.pastry.socket.SocketPastryNodeFactory;
import rice.pastry.standard.RandomNodeIdFactory;

public class TestPastryNode {

	public TestPastryNode(int bindport, InetSocketAddress bootaddress, Environment env) throws Exception {

			    // Generate the NodeIds Randomly
			    NodeIdFactory nidFactory = new RandomNodeIdFactory(env);

			    // construct the PastryNodeFactory, this is how we use rice.pastry.socket
			    PastryNodeFactory factory = new SocketPastryNodeFactory(nidFactory, bindport, env);

			    // construct a node
			    PastryNode node = factory.newNode();

			    // construct a new MyApp
			    PastryOverlayNode pon = new PastryOverlayNode(node, 2);
			    ProtocolEngine pe = new ProtocolEngine(pon);
			    node.boot(bootaddress);

			    // the node may require sending several messages to fully boot into the ring
			    synchronized(node) {
			      while(!node.isReady() && !node.joinFailed()) {
			        // delay so we don't busy-wait
			        node.wait(500);

			        // abort if can't join
			        if (node.joinFailed()) {
			          throw new IOException("Could not join the FreePastry ring.  Reason:"+node.joinFailedReason()); 
			        }
			      }       
			    }
			   
			    System.out.println("Finished creating new node "+node);
			    Session s = pe.requestSessionData();
			   
			    System.out.println("I know about session "+s.getSessionId());
		
		   
			   
			    // wait 10 seconds
			    env.getTimeSource().sleep(10000);
	}
			   
			     
		
	/**
	 * Usage:
	 * java [-cp FreePastry-<version>.jar] rice.tutorial.lesson3.DistTutorial localbindport bootIP bootPort
	 * example java rice.tutorial.DistTutorial 9001 pokey.cs.almamater.edu 9001
	 */
	public static void main(String[] args) throws Exception {
	    // Loads pastry settings
		Environment env = new Environment();
			
		// disable the UPnP setting (in case you are testing this on a NATted LAN)
		env.getParameters().setString("nat_search_policy","never");
			   
		try {
			// the port to use locally
			int bindport = Integer.parseInt(args[0]);
	     
			// build the bootaddress from the command line args
			InetAddress bootaddr = InetAddress.getByName(args[1]);
			int bootport = Integer.parseInt(args[2]);
			InetSocketAddress bootaddress = new InetSocketAddress(bootaddr,bootport);

			// launch our node!
			TestPastryNode dt = new TestPastryNode(bindport, bootaddress, env);
		} catch (Exception e) {
			// remind user how to use
			System.out.println("Usage:"); 
			System.out.println("java [-cp FreePastry-<version>.jar] rice.tutorial.lesson3.DistTutorial localbindport bootIP bootPort");
			System.out.println("example java rice.tutorial.DistTutorial 9001 pokey.cs.almamater.edu 9001");
			throw e; 
		}
	}
}