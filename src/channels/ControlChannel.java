package channels;

import server.Peer;
import utils.Message;
import utils.TypeMessage;

public class ControlChannel extends Channel {
	public ControlChannel(Peer peer, String address, String port) {
		super(peer, address, port);
	}
	
	@Override
	public void run() {
		while (true) {			
			Message msg = receiveMessage();
			
			//System.out.println("\n\tCONTROL CHANNEL - ServerID " + peer.getPeerId() + ": Message received\n");
			//System.out.print(msg.getHeader());
			
			if (msg.getMsgType().equals(TypeMessage.STORED)){
				peer.getBackupProtocol().handleStored(msg);
			} else if (msg.getMsgType().equals(TypeMessage.GETCHUNK)) {
				peer.getRestoreProtocol().handleGetchunk(msg);
			} else if (msg.getMsgType().equals(TypeMessage.DELETE)) {
				peer.getBackedUpFiles().deleteChunksStorage(msg.getFileId());
			} else if (msg.getMsgType().equals(TypeMessage.REMOVED)) {
				peer.getReclaimProtocol().handleRemoved(msg);
			}

		}
	}
}
