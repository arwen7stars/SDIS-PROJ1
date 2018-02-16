package channels;

import java.io.IOException;
import java.net.DatagramPacket;

import filesystem.Chunk;
import server.Peer;
import utils.Constants;
import utils.Message;
import utils.TypeMessage;

public class BackupChannel extends Channel {

	public BackupChannel(Peer peer, String address, String port) {
		super(peer, address, port);
	}
	
	@Override
	public void run() {
		byte[] buf = new byte[Constants.MAX_CHUNK_SIZE];

		while (true) {
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			
			try{
				socket.receive(packet);
			} catch(IOException ioe){
				ioe.printStackTrace();
			}

			Message msg = new Message(packet);
			
			System.out.println("\r\nBACKUP CHANNEL - ServerID " + peer.getPeerId() + ": Message received\r\n");
			System.out.println(msg.getHeader());
			
			if (msg.getMsgType().equals(TypeMessage.PUTCHUNK)){
				if(!peer.getPeerId().equals(msg.getSenderId())) {		// a peer can't store its own files!
					Chunk chunk = new Chunk(msg.getFileId(), msg.getChunkNo(), msg.getBody());

					if(chunk.store(peer.getPeerId())){						
						System.out.println("*** BACKUP: Chunk " + msg.getChunkNo() + " stored on server " + peer.getPeerId() + " ***");

						peer.getBackupProtocol().stored(msg.getVersion(), peer.getPeerId(), msg.getFileId(), msg.getChunkNo());
					}

				}
			}
			
		}
	}

}
