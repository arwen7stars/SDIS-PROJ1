package protocols;

import server.Peer;

public class Restore {
	private Peer peer;
	
	public Restore(Peer peer) {
		this.peer = peer;
	}
	
	public Peer getPeer() {
		return peer;
	}
}
