package network;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;


import util.Neighbor;

public class FakeNode implements Node {

	private boolean isAlive;
	private Set<Neighbor> outNeighbors;
	
	public FakeNode() throws Exception {
		outNeighbors = new HashSet<Neighbor>();
		Neighbor n1, n2, n3;
		InetAddress a1, a2, a3;
		
		byte id1 [] = {1,2,3,4,5,6,7,8,9,0};
		byte id2 [] = {0,9,8,7,6,5,4,3,2,1};
		byte id3 [] = {11,22,33,44,55,66,77,88,99,00};
		
		a1 = InetAddress.getByName("192.168.0.1");
		a2 = InetAddress.getByName("192.168.0.2");
		a3 = InetAddress.getByName("192.168.0.3");
		
		n1 = new Neighbor(id1, a1);
		n2 = new Neighbor(id2, a2);
		n3 = new Neighbor(id3, a3);
		
		outNeighbors.add(n1);
		outNeighbors.add(n2);
		outNeighbors.add(n3);
	}
	
	@Override
	public Set<Neighbor> getOutNeighbors() {
		return outNeighbors;
	}

	@Override
	public boolean isAlive(Neighbor n) {
		return isAlive;
	}

	public void setIsAlive(boolean val) {
		this.isAlive = val;
	}
	
	public static byte [] getNode1() {
		byte id1 [] = {1,2,3,4,5,6,7,8,9,0};
		return id1;
	}

}
