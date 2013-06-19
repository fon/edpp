package util;

import static org.junit.Assert.*;

import java.net.InetAddress;

import org.junit.Before;
import org.junit.Test;

public class PlainNeighborsTableSetTest {

	PlainNeighborsTable table;
	InetAddress a1,a2,a3;

	
	@Before
	public void setUp() throws Exception {
		byte [] id1 = {1,2,3,4,5,6,7,8,9,0};
		byte [] id2 = {0,9,8,7,6,5,4,3,2,1};
		byte [] id3 = {11,22,33,44,55,66,77,88,99,10};
		
		a1 = InetAddress.getByName("192.168.0.1");
		a2 = InetAddress.getByName("192.168.0.2");
		a3 = InetAddress.getByName("192.168.0.3");

		Neighbor n1, n2, n3;
		
		n1 = new Neighbor(id1, a1);
		n2 = new Neighbor(id2, a2);
		n3 = new Neighbor(id3, a3);
		
		table = new PlainNeighborsTableSet();
		
		table.addNeighbor(n1);
		table.addNeighbor(n2);
		table.addNeighbor(n3);
		
	}

	@Test
	public void getNeighborById() throws Exception {
		byte [] id1 = {1,2,3,4,5,6,7,8,9,0};
		Id i = new Id(id1);
		InetAddress a1;
		a1 = InetAddress.getByName("192.168.0.1");
		Neighbor n = table.getNeighbor(i);
		Neighbor expected = new Neighbor(i, a1);
		assertEquals(expected, n);
	}
	
	@Test
	public void getNeighborByStringId() {
		byte [] id1 = {1,2,3,4,5,6,7,8,9,0};
		Id i = new Id(id1);
		Neighbor n = table.getNeighbor(i.toString());
		Neighbor expected = new Neighbor(i, a1);
		assertEquals(expected, n);
	}
	
	@Test
	public void neighborDoesNotExistById() {
		byte [] id1 = {11,2,3,4,5};
		Id i = new Id(id1);
		assertNull(table.getNeighbor(i));
	}
	
	@Test
	public void neighborDoesNotExistByStringId() {
		byte [] id1 = {11,2,3,4,5};
		Id i = new Id(id1);
		assertNull(table.getNeighbor(i.toString()));
	}
	
	@Test
	public void addAlreadyExistingNeighbor() {
		byte [] id1 = {1,2,3,4,5,6,7,8,9,0};
		Id i = new Id(id1);
		Neighbor n = new Neighbor(i, a1);
		assertFalse(table.addNeighbor(n));
	}
	
	@Test
	public void removeNode() {
		byte [] id1 = {1,2,3,4,5,6,7,8,9,0};
		Id i = new Id(id1);
		Neighbor n = new Neighbor(i, a1);
		assertTrue(table.removeNeighbor(n));
	}
	
	@Test
	public void removeNonExistentNode() {
		byte [] id1 = {1,2,3,4,5};
		Id i = new Id(id1);
		Neighbor n = new Neighbor(i, a1);
		assertFalse(table.removeNeighbor(n));
	}
	
	@Test
	public void removeNodeUsingId() {
		byte [] id1 = {1,2,3,4,5,6,7,8,9,0};
		Id i = new Id(id1);
		assertTrue(table.removeNeighbor(i));
	}
	
	@Test
	public void removeNonExistentNodeUsingId() {
		byte [] id1 = {1,2,3,4,5};
		Id i = new Id(id1);
		assertFalse(table.removeNeighbor(i));
	}
	
	@Test
	public void convertTableToArray() {
		Neighbor [] n1 = table.toArray();
		assertEquals(3, n1.length);
	}
	
	@Test
	public void provideSize() {
		assertEquals(3,table.getSize());
	}
	
}
