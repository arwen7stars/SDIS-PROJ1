package channels;

import server.Peer;
import utils.Message;

public class RestoreChannel extends Channel {

	public RestoreChannel(Peer peer, String address, String port) {
		super(peer, address, port);
	}
	
	@Override
	public void run() {
		while (true) {
			Message msg = receiveMessage();
			
			System.out.println("\n\tRESTORE CHANNEL - ServerID " + peer.getPeerId() + ": Message received\n");
			System.out.println(msg.getHeader());
		}		
	}

}
