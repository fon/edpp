package network;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.mpisws.p2p.transport.multiaddress.MultiInetSocketAddress;

import rice.environment.Environment;
import rice.pastry.NodeIdFactory;
import rice.pastry.PastryNode;
import rice.pastry.PastryNodeFactory;
import rice.pastry.socket.SocketPastryNodeFactory;
import rice.pastry.socket.TransportLayerNodeHandle;
import rice.pastry.standard.RandomNodeIdFactory;

public class PastryOverlayNodeTest {


	@SuppressWarnings("unchecked")
	@Test
	public void serializeAndDeserializeProperly() {
		PastryNode node;
		Environment env = new Environment();
		NodeIdFactory nidFactory = new RandomNodeIdFactory(env);

	    // construct the PastryNodeFactory, this is how we use rice.pastry.socket
	    PastryNodeFactory factory;
		try {
			factory = new SocketPastryNodeFactory(nidFactory, 11111, env);
			node = factory.newNode();
			TransportLayerNodeHandle<MultiInetSocketAddress> nh =
					(TransportLayerNodeHandle<MultiInetSocketAddress>) node.getLocalHandle();
			byte [] lh = PastryOverlayNode.serialize(nh);
			TransportLayerNodeHandle<MultiInetSocketAddress>  nh2 =  
					(TransportLayerNodeHandle<MultiInetSocketAddress>) PastryOverlayNode.deserialize(lh);
			assertEquals(nh, nh2);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	    
	}

}
