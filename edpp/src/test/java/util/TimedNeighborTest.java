package util;

import static org.junit.Assert.*;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Test;

import domain.TimedNeighbor;

public class TimedNeighborTest {

	@Test
	public void increaseTimedNeighborRemainingTime() {
		byte [] id = {1,2,3,4,5,6,7,8,9,0};
		InetAddress address = null;
		long remainingTime = 1000;
		try {
			address = InetAddress.getByName("192.168.0.1");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		TimedNeighbor tn = new TimedNeighbor(id, address, remainingTime);
		tn.increaseTime(100);
		assertEquals(1100, tn.getTimeToProbe());
	}
	
	@Test
	public void decreaseTimedNeighborRemainingTime() {
		byte [] id = {1,2,3,4,5,6,7,8,9,0};
		InetAddress address = null;
		long remainingTime = 1000;
		try {
			address = InetAddress.getByName("192.168.0.1");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		TimedNeighbor tn = new TimedNeighbor(id, address, remainingTime);
		tn.decreaseTime(100);
		assertEquals(900, tn.getTimeToProbe());
	}

}
