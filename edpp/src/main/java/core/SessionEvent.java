package core;

import java.util.Date;

import network.Node;

public class SessionEvent {

	private final Session session;
	private final Date date;
	private final Node localNode;
	
	public SessionEvent(Session session, Node localNode) {
		date = new Date();
		this.session = session;
		this.localNode = localNode;
	}

	public Session getSession() {
		return session;
	}

	public Date getDate() {
		return date;
	}

	public Node getLocalNode() {
		return localNode;
	}

}
