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
				if(!peer.getFileKeeper().fileExists(msg.getFileId())) {
					FileInstance f = new FileInstance(msg.getFileId(), msg.getRepDegree());
					peer.getFileKeeper().addFile(f);
					System.out.println("Added file " + msg.getFileId() + " to file keeper...");
				}
				
				FileInstance f = peer.getFileKeeper().getFile(msg.getFileId());
				
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
