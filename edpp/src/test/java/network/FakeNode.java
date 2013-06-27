package network;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;


import util.Id;
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

	@Override
	public Id getLocalId() {
		byte localId [] = {1,1,1,1,1,1,1,1,1,1,1,1};
		return new Id(localId);
	}
	
	public void renewOutNeighbors() throws UnknownHostException {
		outNeighbors = new HashSet<Neighbor>();
		Neighbor n1, n2, n3, n4;
		InetAddress a1, a2, a3, a4;
		
		byte id1 [] = {1,2,3,4,5,6,7,8,9,0};
		byte id2 [] = {0,9,8,7,6,5,4,3,2,1};
		byte id3 [] = {11,22,33,44,55,66,77,88,99,00};
		byte id4 [] = {111,22,3,55,66,77,88,99,00};

		
		a1 = InetAddress.getByName("192.168.0.1");
		a2 = InetAddress.getByName("192.168.0.2");
		a3 = InetAddress.getByName("192.168.0.3");
		a4 = InetAddress.getByName("192.168.0.4");

		
		n1 = new Neighbor(id1, a1);
		n2 = new Neighbor(id2, a2);
		n3 = new Neighbor(id3, a3);
		n4 = new Neighbor(id4, a4);

		
		outNeighbors.add(n1);
		outNeighbors.add(n2);
		outNeighbors.add(n3);
		outNeighbors.add(n4);

	}

	@Override
	public int getDiameter() {
		return 1;
	}

}
