package channels;

import server.Peer;
import utils.Message;
import utils.TypeMessage;

public class RestoreChannel extends Channel {

	public RestoreChannel(Peer peer, String address, String port) {
		super(peer, address, port);
	}
	
	@Override
	public void run() {
		while (true) {
			Message msg = receiveMessage();
			
			//System.out.println("\n\tRESTORE CHANNEL - ServerID " + peer.getPeerId() + ": Message received\n");
			//System.out.print(msg.getHeader());
			
			if(!msg.getSenderId().equals(peer.getPeerId())) {
				if (msg.getMsgType().equals(TypeMessage.CHUNK)){
					if(peer.getRestoreDelay()) {
						peer.setStopChunkMsg(true);
					}
					
					peer.getRestoreProtocol().addToRestoredChunks(msg.getFileId(), msg.getChunkNo(), msg.getBody());
				}
			}
		}		
	}

}
