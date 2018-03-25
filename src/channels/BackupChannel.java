package channels;

import filesystem.Chunk;
import filesystem.FileInstance;
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
				if(!peer.getInitiatorFiles().fileExists(msg.getFileId())) {
		        	FileInstance f = new FileInstance(msg.getFileId());
		        	peer.getInitiatorFiles().addFile(f);
				}
				
		    	if (!peer.getInitiatorFiles().getFile(msg.getFileId()).chunkExists(msg.getChunkNo())) {			// checks if chunk instance already exists on this peer
		    		Chunk c = new Chunk(msg.getFileId(), msg.getChunkNo());
		    		peer.getInitiatorFiles().getFile(msg.getFileId()).addChunk(c);							// if chunk instance does not exist on this peer, one is created
				}
				
				Chunk chunk = new Chunk(msg.getFileId(), msg.getRepDegree(), msg.getChunkNo(), msg.getBody());
				
				if(!peer.getInitiatorFiles().getFile(msg.getFileId()).isInitiator()) {
					if(chunk.store(peer)){						
						System.out.println("\n*** BACKUP: Chunk " + msg.getChunkNo() + " of file " + msg.getFileId() + " stored on server " + peer.getPeerId() + " ***\n");
						
						peer.getBackedUpFiles().addBackedUpChunk(chunk);
						peer.getBackupProtocol().stored(msg.getVersion(), peer.getPeerId(), msg.getFileId(), msg.getChunkNo());
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
