package domain.network;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import cx.ath.troja.chordless.Chord;
import cx.ath.troja.chordless.ServerInfo;
import domain.Id;
import domain.Neighbor;

public class ChordOverlayNode implements Node {

	private Chord localNode;
	
	/**
	 * Class constructor
	 * @param localNode The Chord object provided by the Chordless overlay
	 */
	public ChordOverlayNode(Chord localNode) {
		this.localNode =  localNode;
	}
	
	@Override
	public Set<Neighbor> getOutNeighbors() {
		HashSet<Neighbor> outNeighbors = new HashSet<Neighbor>();
		
		// Get an array of all the successors contained in the Finger Table
		ServerInfo [] si = localNode.getFingerArray();
		//Create a Neighbor object for each successor
		for (ServerInfo server : si) {
			Id nodeId = new Id(server.getIdentifier().toByteArray());
			// Unsafe if the protocol is not IP
			InetSocketAddress isa = (InetSocketAddress) server.getAddress();
			Neighbor n = new Neighbor(nodeId , isa.getAddress());
			// Add the Neighbor to the set of out-neighbors for the node
			outNeighbors.add(n);
		}		
		return outNeighbors;
	}

	@Override
	public Id getLocalId() {
		// Convert the id of the local node to an object of type Id
		Id localId = new Id(localNode.getIdentifier().toByteArray());
		return localId;
	}

	@Override
	public boolean removeOutNeighborNode(String Id) {
		/* This optional method is not implemented for Chord
	 	 * Just ignore the hint of the protocol for removal of 
		 * the node with id Id
		 */
		return false;
	}

}
