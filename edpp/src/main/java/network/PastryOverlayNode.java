package network;

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
				
				byte [] nodeId = SerializationUtils.serialize(nh);
				Neighbor n = new Neighbor(nodeId, 
						nh.getAddress().getInnermostAddress().getAddress());
				outNeighbors.add(n);
			}
		}
		
		return outNeighbors;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean isAlive(Neighbor n) {
		Id edppId = n.getId();
		TransportLayerNodeHandle<MultiInetSocketAddress> nh = 
				(TransportLayerNodeHandle<MultiInetSocketAddress>) SerializationUtils.deserialize(edppId.getByteRepresentation());
		return localNode.isAlive(nh);
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

}
