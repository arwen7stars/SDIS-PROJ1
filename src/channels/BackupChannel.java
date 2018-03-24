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
				if(!peer.getInitiatorFiles().fileExists(msg.getFileId()) || peer.getBackupProtocol().getCanBackup()) {		// a peer can't store its own files!
					Chunk chunk = new Chunk(msg.getFileId(), msg.getRepDegree(), msg.getChunkNo(), msg.getBody());

					if(chunk.store(peer)){						
						System.out.println("*** BACKUP: Chunk " + msg.getChunkNo() + " of file " + msg.getFileId() + " stored on server " + peer.getPeerId() + " ***");
						
						peer.getBackedUpFiles().addBackedUpChunk(chunk);
						peer.getBackupProtocol().stored(msg.getVersion(), peer.getPeerId(), msg.getFileId(), msg.getChunkNo());
					}
					
					if(peer.getBackupProtocol().getCanBackup()) {
						peer.getBackupProtocol().setCanBackup(false);
					}
				}
				
			    for (Chunk c : peer.getReclaimProtocol().getChunksToBackup().keySet()) {
					if (c.getFileId().equals(msg.getFileId()) && (c.getChunkNo() == msg.getChunkNo())) {
						peer.getReclaimProtocol().getChunksToBackup().remove(c);
					}
			    }

			}
			
		}
	}

}
