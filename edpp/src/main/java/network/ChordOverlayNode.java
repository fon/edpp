package network;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import cx.ath.troja.chordless.Chord;
import cx.ath.troja.chordless.ServerInfo;
import util.Id;
import util.Neighbor;

public class ChordOverlayNode implements Node {

	private Chord localNode;
	
	
	public ChordOverlayNode(Chord node) {
		this.localNode =  node;
	}
	
	@Override
	public Set<Neighbor> getOutNeighbors() {
		HashSet<Neighbor> outNeighbors = new HashSet<Neighbor>();
		
		ServerInfo [] si = localNode.getFingerArray();
		for (ServerInfo server : si) {
			Id nodeId = new Id(server.getIdentifier().toByteArray());
			InetSocketAddress isa = (InetSocketAddress) server.getAddress();
			Neighbor n = new Neighbor(nodeId , isa.getAddress());
			outNeighbors.add(n);
		}		
		return outNeighbors;
	}

	@Override
	public Id getLocalId() {
		Id localId = new Id(localNode.getIdentifier().toByteArray());
		return localId;
	}

	@Override
	public boolean removeOutNeighborNode(String Id) {
		// TODO Auto-generated method stub
		return false;
	}

}
