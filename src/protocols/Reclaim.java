package protocols;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import filesystem.Chunk;
import filesystem.Metadata;
import server.Peer;
import utils.Message;
import utils.TypeMessage;

public class Reclaim {
	private Peer peer;
	private ConcurrentHashMap<Chunk, Integer> chunksToBackup;
	
	public Reclaim(Peer peer) {
		this.peer = peer;
		this.chunksToBackup = new ConcurrentHashMap<Chunk, Integer>();
	}
	
	public void removed(String version, String senderId, String fileId, int chunkNo){
		String header = Message.createHeader(TypeMessage.REMOVED, version, senderId, fileId, chunkNo);
		Message msg = new Message(header);
		
		peer.getMcChannel().sendMessage(msg);
	}
	
	public void replicateChunk(Chunk c) {
		Random randomGenerator = new Random();
		Integer randomInt = randomGenerator.nextInt(400);
		
		chunksToBackup.put(c, 1);
		
		try {
			Thread.sleep(randomInt);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if(chunksToBackup.containsKey(c)) {
			this.peer.getBackupProtocol().putchunkReclaim(peer.getProtocolVersion(), peer.getPeerId(), c.getFileId(), c.getChunkNo(), c.getActualRepDegree(), c.getDesiredRepDegree(), c.getFileData());
			// will try to get desired replication degree by sending chunks to the other peers
			
			chunksToBackup.remove(c);
		}
	}
	
	public void manageDiskSpace(int diskSpace) {
		peer.setStorageSpace(diskSpace);
		System.out.println("Peer storage changed to " + diskSpace + " bytes");
		System.out.println("ACTUAL STORAGE " + peer.getBackedUpFiles().getChunkSpace());

		if(peer.getBackedUpFiles().getChunkSpace() >= peer.getStorageSpace()) {
			ArrayList<Chunk> chunks = peer.getBackedUpFiles().getBackedUpChunks();
			
			for(Iterator<Chunk> it = chunks.iterator(); it.hasNext();) {
				Chunk currentChunk = it.next();
				String fileId = currentChunk.getFileId();
				int chunkNo = currentChunk.getChunkNo();
				int actualRepDegree = currentChunk.getActualRepDegree();
				int desiredRepDegree = currentChunk.getDesiredRepDegree();
				
				System.out.println("*** RECLAIM: fileId " + fileId + " chunkNo. " + chunkNo + " ***");
				System.out.println("Replication degrees - Actual: " + actualRepDegree + " Desired: " + desiredRepDegree);
			
				String chunkPath = Metadata.getChunkPath(peer.getPeerId(), fileId, chunkNo);
				File chunk = new File(chunkPath);
				
				if(!chunk.exists()) {
					System.out.println("*** RECLAIM: The peer " + peer.getPeerId() + " doesn't have stored any chunks of file " + fileId + " ***");
					continue;
				}
				
				if(peer.getBackedUpFiles().getChunkSpace() >= peer.getStorageSpace()) {
					if (chunk.delete())		// delete chunk from physical storage system
						System.out.println("*** RECLAIM: Chunk no. " + chunkNo + " deleted with success from file " + fileId + " ***");
					else System.out.println("*** RECLAIM: An error occurred deleting chunk no. " + chunkNo + " from file " + fileId + " ***");
					
					it.remove();			// remove chunk from backedUpChunks
					this.removed(peer.getProtocolVersion(), peer.getPeerId(), fileId, chunkNo);		// send message to other peers
					
					currentChunk.decRepDegree();
					if (currentChunk.getActualRepDegree() < desiredRepDegree){
						replicateChunk(currentChunk);
					}
					
				} else break;
			}
			
			System.out.println("\t\tStorage space - Max: " + this.peer.getStorageSpace() + " Occupied: " + this.peer.getBackedUpFiles().getChunkSpace());
		}
	}
	
	public Peer getPeer() {
		return peer;
	}
	
	public ConcurrentHashMap<Chunk, Integer> getChunksToBackup() {
		return chunksToBackup;
	}
}
