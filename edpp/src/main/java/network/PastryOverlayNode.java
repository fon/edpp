package network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.SerializationUtils;
import org.mpisws.p2p.transport.multiaddress.MultiInetSocketAddress;

import rice.pastry.PastryNode;
import rice.pastry.leafset.LeafSet;
import rice.pastry.routing.RoutingTable;
import rice.pastry.socket.TransportLayerNodeHandle;

import util.Id;
import util.Neighbor;

public class PastryOverlayNode implements Node {

	private PastryNode localNode;
	private int networkSize;
	
	public PastryOverlayNode(PastryNode localNode ,int networkSize) {
		this.localNode = localNode;
		this.networkSize = networkSize;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Set<Neighbor> getOutNeighbors()  {
		HashSet<Neighbor> outNeighbors = new HashSet<Neighbor>();
		
		RoutingTable rt = localNode.getRoutingTable();
		List<rice.pastry.NodeHandle> routingTableNodes = rt.asList();
		
		for (rice.pastry.NodeHandle remoteNode : routingTableNodes) {
			if (remoteNode instanceof TransportLayerNodeHandle<?>) {
				TransportLayerNodeHandle<MultiInetSocketAddress> nh =
						(TransportLayerNodeHandle<MultiInetSocketAddress>) remoteNode;
				
				byte [] nodeId = SerializationUtils.serialize(nh);
				Neighbor n = new Neighbor(nodeId, 
						nh.getAddress().getInnermostAddress().getAddress());
				outNeighbors.add(n);
			}
		}
		
		LeafSet ls = localNode.getLeafSet();
		
		List<rice.pastry.NodeHandle> leafSetNodes = ls.asList();
		
		for (rice.pastry.NodeHandle remoteNode : leafSetNodes) {
			if (remoteNode instanceof TransportLayerNodeHandle<?>) {
				TransportLayerNodeHandle<MultiInetSocketAddress> nh =
						(TransportLayerNodeHandle<MultiInetSocketAddress>) remoteNode;
				byte[] nodeId;
				try {
					nodeId = PastryOverlayNode.serialize(remoteNode);
					Neighbor n = new Neighbor(nodeId, 
							nh.getAddress().getInnermostAddress().getAddress());
					outNeighbors.add(n);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return outNeighbors;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean isAlive(Neighbor n) {
		Id edppId = n.getId();
		Object o;
		try {
			o = deserialize(edppId.getByteRepresentation());
			TransportLayerNodeHandle<MultiInetSocketAddress> nh = 
					(TransportLayerNodeHandle<MultiInetSocketAddress>) o;
			System.out.println("Probing now....");
			return localNode.isAlive(nh);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public Id getLocalId() {
		Id localId = new Id(localNode.getId().toByteArray());
		return localId;
	}

	@Override
	public int getDiameter() {
		return (int) Math.ceil(Math.log(networkSize));
	}

	private static byte[] serialize(Object obj) throws IOException {
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    ObjectOutputStream os = new ObjectOutputStream(out);
	    os.writeObject(obj);
	    return out.toByteArray();
	}
	
	private static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
	    ByteArrayInputStream in = new ByteArrayInputStream(data);
	    ObjectInputStream is = new ObjectInputStream(in);
	    return is.readObject();
	}
	
}
