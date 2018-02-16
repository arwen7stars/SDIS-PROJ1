package protocols;

import server.Peer;

public class Reclaim {
	private Peer peer;
	
	public Reclaim(Peer peer) {
		this.peer = peer;
	}
	
	public Peer getPeer() {
		return peer;
	}
}
