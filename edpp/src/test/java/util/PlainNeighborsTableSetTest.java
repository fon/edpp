package util;

import static org.junit.Assert.*;

import java.net.InetAddress;

import org.junit.Before;
import org.junit.Test;

import domain.Id;
import domain.PlainNeighbor;
import domain.structure.PlainNeighborsTable;
import domain.structure.PlainNeighborsTableSet;

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

		PlainNeighbor n1, n2, n3;
		
		n1 = new PlainNeighbor(id1, a1,10);
		n2 = new PlainNeighbor(id2, a2,10);
		n3 = new PlainNeighbor(id3, a3,10);
		
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
		PlainNeighbor n = table.getNeighbor(i);
		PlainNeighbor expected = new PlainNeighbor(i, a1,10);
		assertEquals(expected, n);
	}
	
	@Test
	public void getNeighborByStringId() {
		byte [] id1 = {1,2,3,4,5,6,7,8,9,0};
		Id i = new Id(id1);
		PlainNeighbor n = table.getNeighbor(i.toString());
		PlainNeighbor expected = new PlainNeighbor(i, a1, 10);
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
		PlainNeighbor n = new PlainNeighbor(i, a1, 10);
		assertFalse(table.addNeighbor(n));
	}
	
	@Test
	public void removeNode() {
		byte [] id1 = {1,2,3,4,5,6,7,8,9,0};
		Id i = new Id(id1);
		PlainNeighbor n = new PlainNeighbor(i, a1,10);
		assertTrue(table.removeNeighbor(n));
	}
	
	@Test
	public void removeNonExistentNode() {
		byte [] id1 = {1,2,3,4,5};
		Id i = new Id(id1);
		PlainNeighbor n = new PlainNeighbor(i, a1,10);
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
		PlainNeighbor [] n1 = table.toArray();
		assertEquals(3, n1.length);
	}
	
	@Test
	public void provideSize() {
		assertEquals(3,table.getSize());
	}
	
}
