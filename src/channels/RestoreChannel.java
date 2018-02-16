package channels;

import server.Peer;
import utils.Constants;

public class RestoreChannel extends Channel {

	public RestoreChannel(Peer peer, String address, String port) {
		super(peer, address, port);
	}
	
	@Override
	public void run() {
		byte[] buf = new byte[Constants.MAX_CHUNK_SIZE];

		while (true) {
			
		}		
	}

}
