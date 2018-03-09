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
			
			System.out.println("\n\tCONTROL CHANNEL - ServerID " + peer.getPeerId() + ": Message received\n");
			System.out.println(msg.getHeader());
			
			if (msg.getMsgType().equals(TypeMessage.STORED)){					
				
				if(peer.getInitiatorFiles().fileExists(msg.getFileId())) {
					FileInstance f = peer.getInitiatorFiles().getFile(msg.getFileId());
					
					if(!f.chunkExists(msg.getChunkNo())) {
						Chunk chunk = new Chunk(msg.getFileId(), msg.getChunkNo(), msg.getBody());
						f.addChunk(chunk);
					}
					
					Chunk c = f.getChunk(msg.getChunkNo());
	
					System.out.println("BACKUP: Replication degree of chunk no. " + c.getChunkNo() + " increased for file " + f.getFileId());
					c.incRepDegree();
				}
			}
		}
	}
}
