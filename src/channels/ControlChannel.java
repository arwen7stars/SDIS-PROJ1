package channels;

import filesystem.Chunk;
import filesystem.FileInstance;
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
				
				Chunk bChunk = null;
				
				// increasing replication degree for initiator peer
				if(peer.getInitiatorFiles().fileExists(msg.getFileId())) {
					FileInstance f = peer.getInitiatorFiles().getFile(msg.getFileId());
					
					if(!f.chunkExists(msg.getChunkNo())) {
						Chunk chunk = new Chunk(msg.getFileId(), msg.getChunkNo(), msg.getBody());
						f.addChunk(chunk);
					}
					
					Chunk c = f.getChunk(msg.getChunkNo());
					c.incRepDegree();
					
					System.out.println("BACKUP: Replication degree of chunk no. " + c.getChunkNo() + " increased for file " + f.getFileId());
				} else if ((bChunk = peer.getBackedUpFiles().getBackedUpChunk(msg.getFileId(), msg.getChunkNo())) != null) {
					System.out.println("BACKUP: Storing info of chunk No. " + msg.getChunkNo());

					bChunk.incRepDegree();
					peer.getBackedUpFiles().updateChunksFile(bChunk.getFileId(), bChunk.getChunkNo(), bChunk.getActualRepDegree());
				}
			} else if (msg.getMsgType().equals(TypeMessage.GETCHUNK)) {
				Chunk c = null;
				if ((c = peer.getBackedUpFiles().getBackedUpChunk(msg.getFileId(), msg.getChunkNo())) != null) {
					peer.getRestoreProtocol().chunk(msg.getVersion(), msg.getFileId(), msg.getChunkNo(), c.getFileData());
				}
			} else if (msg.getMsgType().equals(TypeMessage.DELETE)) {
				peer.getBackedUpFiles().deleteChunksStorage(msg.getFileId());
			}
		}
	}
}
