package domain.network;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mpisws.p2p.transport.multiaddress.MultiInetSocketAddress;

import domain.Id;
import domain.Neighbor;
import rice.pastry.PastryNode;
import rice.pastry.leafset.LeafSet;
import rice.pastry.routing.RoutingTable;
import rice.pastry.socket.TransportLayerNodeHandle;

public class PastryOverlayNode implements Node {

	private PastryNode localNode;
	
	/**
	 * Class constructor
	 * @param localNode The PastryNode object provided by the FreePastry overlay
	 */
	public PastryOverlayNode(PastryNode localNode) {
		this.localNode = localNode;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Set<Neighbor> getOutNeighbors()  {
		HashSet<Neighbor> outNeighbors = new HashSet<Neighbor>();
		
		//Get the routing table of the local node
		RoutingTable rt = localNode.getRoutingTable();
		List<rice.pastry.NodeHandle> routingTableNodes = rt.asList();
		
		// Convert each record of the routing table in a Neighbor object
		// and add it to the set of neighbors
		for (rice.pastry.NodeHandle remoteNode : routingTableNodes) {
			// The remote node must be using the IP protocol for communication
			if (remoteNode instanceof TransportLayerNodeHandle<?>) {
				TransportLayerNodeHandle<MultiInetSocketAddress> nh =
						(TransportLayerNodeHandle<MultiInetSocketAddress>) remoteNode;
				
				Neighbor n = new Neighbor(nh.getId().toByteArray(), 
						nh.getAddress().getInnermostAddress().getAddress());
				outNeighbors.add(n);
			}
		}
		
		//Get the leaf set of the local node
		LeafSet ls = localNode.getLeafSet();
		List<rice.pastry.NodeHandle> leafSetNodes = ls.asList();
		
		// Convert each record of the leaf set in a Neighbor object
		// and add it to the set of neighbors
		for (rice.pastry.NodeHandle remoteNode : leafSetNodes) {
			// Like in the routing table, the remote node 
			// must be using the IP protocol for communication
			TransportLayerNodeHandle<MultiInetSocketAddress> nh =
					(TransportLayerNodeHandle<MultiInetSocketAddress>) remoteNode;
			byte[] nodeId = nh.getId().toByteArray();
			Neighbor n = new Neighbor(nodeId, 
					nh.getAddress().getInnermostAddress().getAddress());
			outNeighbors.add(n);
			
		}
		return outNeighbors;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Id getLocalId() {
		Id localId = null;
		// Convert the handle of the local node to a handle operating over IP
		// Unsafe operation if the network is not IP
		TransportLayerNodeHandle<MultiInetSocketAddress> nh = 
				(TransportLayerNodeHandle<MultiInetSocketAddress>) localNode.getLocalNodeHandle();
		localId = new Id(nh.getId().toByteArray());
		return localId;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean removeOutNeighborNode(String Id) {
		boolean removed = false;
		
		//Get the routing table of the local node
		RoutingTable rt = localNode.getRoutingTable();
		List<rice.pastry.NodeHandle> routingTableNodes = rt.asList();
		
		// Check every node in the routing table to see whether
		// the node of interest with id Id is available
		for (rice.pastry.NodeHandle remoteNode : routingTableNodes) {
			if (remoteNode instanceof TransportLayerNodeHandle<?>) {
				TransportLayerNodeHandle<MultiInetSocketAddress> nh =
						(TransportLayerNodeHandle<MultiInetSocketAddress>) remoteNode;
				
				Id i = new Id(nh.getId().toByteArray());
				// If it is remove it
				if(i.toString().equals(Id)) {
					rt.remove(nh);
					removed = true;
				}
			}
		}
		
		// Do the same for the leaf set
		LeafSet ls = localNode.getLeafSet();
		
		List<rice.pastry.NodeHandle> leafSetNodes = ls.asList();
		
		for (rice.pastry.NodeHandle remoteNode : leafSetNodes) {
			TransportLayerNodeHandle<MultiInetSocketAddress> nh =
					(TransportLayerNodeHandle<MultiInetSocketAddress>) remoteNode;
			Id i = new Id(nh.getId().toByteArray());
			if(i.toString().equals(Id)) {
				ls.remove(nh);
				removed = true;
			}
			
		}
		return removed;
	}
}
