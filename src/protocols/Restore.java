package protocols;

import java.util.HashMap;
import java.util.Random;
import java.util.Vector;

import filesystem.Chunk;
import filesystem.FileInstance;
import server.Peer;
import utils.Message;
import utils.TypeMessage;

public class Restore {
	private Peer peer;
	private HashMap<Integer, byte[]> restoredChunks = new HashMap<Integer, byte[]>();
	
	public Restore(Peer peer) {
		this.peer = peer;
	}
	
	public void getchunk(String version, String senderId, String fileId, int chunkNo) {
		String header = Message.createHeader(TypeMessage.GETCHUNK, version, senderId, fileId, chunkNo);
		Message msg = new Message(header);
		
		peer.getMcChannel().sendMessage(msg);
	}
	
	public void chunk(String version, String fileId, int chunkNo, byte[] body) {
		String header = Message.createHeader(TypeMessage.CHUNK, version, peer.getPeerId(), fileId, chunkNo);
		Message msg = new Message(header, body);
		
		Random randomGenerator = new Random();
		Integer randomInt = randomGenerator.nextInt(400);
		
		peer.setRestoreDelay(true);
		
		try {
			Thread.sleep(randomInt);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
		
		peer.setRestoreDelay(false);
		
		if (peer.getStopChunkMsg()) {
			peer.setStopChunkMsg(false);
			return;
		} else {
			peer.getMdrChannel().sendMessage(msg);
		}
	}
	
	public void restoreFile(String filePath) {
		System.out.println("*** RESTORE: Restoring file with path " + filePath + " ***");
		restoredChunks = new HashMap<Integer, byte[]>();			// reset restored chunks

		String fileId = peer.getInitiatorFiles().getFileId(filePath);
		
		if (fileId.equals(null)) {
			System.err.println("*** RESTORE: The specified file doesn't exist on this server! ***");
		}
		FileInstance f = peer.getInitiatorFiles().getFile(fileId);
		Vector<Chunk> chunks = f.getChunks();
		
		System.out.println("*** RESTORE: Num of chunks to restore: " + chunks.size() + " ***");
		
		for(int i = 0; i < chunks.size(); i++) {
			getchunk(peer.getProtocolVersion(), peer.getPeerId(), fileId, i);
		}
		
	}
	
	public void addToRestoredChunks(int chunkNo, byte[] fileData) {
		System.out.println("\t\t*** RESTORE: Chunk added to restored chunks " + chunkNo);
		if(!restoredChunks.containsKey(chunkNo))
			restoredChunks.put(chunkNo, fileData);
	}
	
	public Peer getPeer() {
		return peer;
	}
	
	public HashMap<Integer, byte[]> getRestoredChunks() {
		return restoredChunks;
	}
}
