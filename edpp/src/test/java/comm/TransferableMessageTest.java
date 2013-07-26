package comm;

import static org.junit.Assert.*;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Test;

import comm.ProtocolMessage.Message;

public class TransferableMessageTest {

	@Test
	public void messageAndAddressAreSetProperly() throws UnknownHostException {
		Message m = MessageBuilder.buildLivenessMessage();
		InetAddress address = InetAddress.getLocalHost();
		TransferableMessage tm = new TransferableMessage(m, address);
		
		assertEquals(m, tm.getMessage());
		assertEquals(address, tm.getAddress());
	}

}
