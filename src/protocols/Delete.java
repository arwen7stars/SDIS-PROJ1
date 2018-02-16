package protocols;

import server.Peer;

public class Delete {
	private Peer peer;
	
	public Delete(Peer peer) {
		this.peer = peer;
	}
	
	public Peer getPeer() {
		return peer;
	}
}
