package channels;

import filesystem.Chunk;
import server.Peer;
import utils.Message;
import utils.TypeMessage;

public class BackupChannel extends Channel {
	public BackupChannel(Peer peer, String address, String port) {
		super(peer, address, port);
	}
	
	@Override
	public void run() {
		while (true) {
			Message msg = receiveMessage();
			
			//System.out.println("\n\tBACKUP CHANNEL - ServerID " + peer.getPeerId() + ": Message received\n");
			//System.out.print(msg.getHeader());
			
			if (msg.getMsgType().equals(TypeMessage.PUTCHUNK)){
				if(!peer.getPeerId().equals(msg.getSenderId())) {		// a peer can't store its own files!
					Chunk chunk = new Chunk(msg.getFileId(), msg.getChunkNo(), msg.getBody());

					if(chunk.store(peer.getPeerId())){						
						System.out.println("*** BACKUP: Chunk " + msg.getChunkNo() + " stored on server " + peer.getPeerId() + " ***");
						
						peer.getBackedUpFiles().addBackedUpChunk(chunk);
						peer.getBackupProtocol().stored(msg.getVersion(), peer.getPeerId(), msg.getFileId(), msg.getChunkNo());
					}

				}
			}
			
		}
	}

}
