package util;

import static org.junit.Assert.*;

import org.junit.Test;

public class IdTest {

	@Test
	public void testByteToStringIdConversion() {
		byte [] id = {1,2,3,4,5,6,7,8,9,0,10};
		assertEquals("AQIDBAUGBwgJAAo=",Id.getStringId(id));
	}
	
	@Test
	public void testStringToByteIdConversion() {
		String stringId = "AQIDBAUGBwgJAAo=";
		byte [] byteId = {1,2,3,4,5,6,7,8,9,0,10};
		assertArrayEquals(byteId, Id.getByteIdFromString(stringId));
	}
	
	@Test
	public void testIdCreatedFromByteAndStringEquality(){
		String stringId = "AQIDBAUGBwgJAAo=";
		byte [] byteId = {1,2,3,4,5,6,7,8,9,0,10};
		Id id1 = new Id(stringId);
		Id id2 = new Id(byteId);
		assertEquals(id1,id2);
	}

}
