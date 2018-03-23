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
					
					if (f.chunkExists(msg.getChunkNo())) {
						Chunk c = f.getChunk(msg.getChunkNo());
						c.incRepDegree();
					
						System.out.println("BACKUP: Replication degree of chunk no. " + c.getChunkNo() + " increased for file " + f.getFileId());
					}
				} else if ((bChunk = peer.getBackedUpFiles().getBackedUpChunk(msg.getFileId(), msg.getChunkNo())) != null) {
					System.out.println("BACKUP: Storing info of chunk No. " + msg.getChunkNo());

					bChunk.incRepDegree();
					peer.getBackedUpFiles().updateChunksFile(bChunk.getFileId(), bChunk.getDesiredRepDegree(), bChunk.getChunkNo(), bChunk.getActualRepDegree());
				}
			} else if (msg.getMsgType().equals(TypeMessage.GETCHUNK)) {
				Chunk c = null;
				if ((c = peer.getBackedUpFiles().getBackedUpChunk(msg.getFileId(), msg.getChunkNo())) != null) {
					peer.getRestoreProtocol().chunk(msg.getVersion(), msg.getFileId(), msg.getChunkNo(), c.getFileData());
				}
			} else if (msg.getMsgType().equals(TypeMessage.DELETE)) {
				peer.getBackedUpFiles().deleteChunksStorage(msg.getFileId());
			} else if (msg.getMsgType().equals(TypeMessage.REMOVED)) {
				Chunk bChunk = null;
				
				if(peer.getInitiatorFiles().fileExists(msg.getFileId())) {
					FileInstance f = peer.getInitiatorFiles().getFile(msg.getFileId());
					
					if(f.chunkExists(msg.getChunkNo())) {
						Chunk c = f.getChunk(msg.getChunkNo());
						c.decRepDegree();
						System.out.println("BACKUP: Replication degree of chunk no. " + c.getChunkNo() + " decreased for file " + f.getFileId());
					}
					
				} else if ((bChunk = peer.getBackedUpFiles().getBackedUpChunk(msg.getFileId(), msg.getChunkNo())) != null) {
					bChunk.decRepDegree();
					peer.getBackedUpFiles().updateChunksFile(bChunk.getFileId(), bChunk.getDesiredRepDegree(), bChunk.getChunkNo(), bChunk.getActualRepDegree());
				} else if (peer.getPeerId().equals(msg.getSenderId())) {					
					peer.getBackedUpFiles().deleteChunk(msg.getFileId(), msg.getChunkNo());
				}
			}
		}
	}
}
