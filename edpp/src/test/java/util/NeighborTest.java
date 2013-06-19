package util;

import static org.junit.Assert.*;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Test;

public class NeighborTest {

	@Test
	public void timedNeighborIsNotNeighbor() {
		byte [] id = {1,2,3,4,5,6,7,8,9,0};
		Id nodeId = new Id(id);
		InetAddress address = null;
		long remainingTime = 1000;
		try {
			address = InetAddress.getByName("192.168.0.1");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Neighbor n = new Neighbor(nodeId, address);
		TimedNeighbor n2 = new TimedNeighbor(nodeId, address, remainingTime);
		assertNotEquals(n, n2);
	}

}
