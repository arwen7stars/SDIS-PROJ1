package protocols;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import filesystem.Chunk;
import filesystem.FileInstance;
import filesystem.Metadata;
import server.Peer;
import utils.Message;
import utils.TypeMessage;

public class Reclaim {
	private Peer peer;
	private ConcurrentHashMap<Chunk, Integer> chunksToBackup;
	private ConcurrentHashMap<Chunk, Backup> reclaimChunks;
	
	public Reclaim(Peer peer) {
		this.peer = peer;
		this.chunksToBackup = new ConcurrentHashMap<Chunk, Integer>();
		this.reclaimChunks = new ConcurrentHashMap<Chunk, Backup>();
	}
	
	public static class Backup
	{
	    public int tries;
	    public boolean done;
	    
	    public Backup(int tries, boolean done) {
	    	this.tries = tries;
	    	this.done = done;
	    };
	 };
	
	public void removed(String version, String senderId, String fileId, int chunkNo){
		String header = Message.createHeader(TypeMessage.REMOVED, version, senderId, fileId, chunkNo);
		Message msg = new Message(header);
		
		peer.getMcChannel().sendMessage(msg);
	}
	
	public void handleRemoved(Message msg) {
		Chunk bChunk = null;
		
		if(peer.getInitiatorFiles().fileExists(msg.getFileId())) {
			FileInstance f = peer.getInitiatorFiles().getFile(msg.getFileId());
			
			if(f.chunkExists(msg.getChunkNo())) {
				Chunk c = f.getChunk(msg.getChunkNo());
				c.decRepDegree();
				System.out.println("BACKUP: Replication degree of chunk no. " + c.getChunkNo() + " decreased for file " + f.getFileId());
			}
			
		}
		
		if ((bChunk = peer.getBackedUpFiles().getBackedUpChunk(msg.getFileId(), msg.getChunkNo())) != null) {
			bChunk.decRepDegree();
			peer.getBackedUpFiles().updateChunksFile(bChunk.getFileId(), bChunk.getDesiredRepDegree(), bChunk.getChunkNo(), bChunk.getActualRepDegree());
			
			if (bChunk.getActualRepDegree() < bChunk.getDesiredRepDegree()){
				System.out.println("*** RECLAIM: Initiated backup protocol for chunk " + bChunk.getChunkNo() + " of file " + bChunk.getFileId());
				
				peer.getReclaimProtocol().replicateChunk(bChunk);

			}
		}
		
		if (peer.getPeerId().equals(msg.getSenderId())) {
			peer.getBackedUpFiles().deleteChunk(msg.getFileId(), msg.getChunkNo());
		}
	}
	
	public boolean replicateChunk(Chunk c) {
		Random randomGenerator = new Random();
		Integer randomInt = randomGenerator.nextInt(400);
		
		chunksToBackup.put(c, 1);
		
		try {
			Thread.sleep(randomInt);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if(chunksToBackup.containsKey(c)) {
			Backup backup = new Backup(0, false);
			int delay = 1000;
			String fileId = c.getFileId();
			int chunkNo = c.getChunkNo();
			int desiredRepDegree = c.getDesiredRepDegree();
			byte[] body = c.getFileData();
			
			if(reclaimChunks.containsKey(c)) {
				backup = reclaimChunks.get(c);
			} else reclaimChunks.put(c, backup);
			
			if (backup.tries > 0)
				delay = 2*backup.tries*delay;
				
			this.peer.getBackupProtocol().putchunkReclaim(delay, peer.getProtocolVersion(), peer.getPeerId(), fileId, chunkNo, c.getActualRepDegree(), desiredRepDegree, body);
			// will try to get desired replication degree by sending chunks to the other peers
			
			if(desiredRepDegree <= c.getActualRepDegree()) {
        		System.out.println("*** BACKUP: Backup of chunk " + chunkNo + " from file " + fileId + " was successful ***");
        		backup.done = true;
				reclaimChunks.put(c, backup);
    			chunksToBackup.remove(c);
        		return true;
			} else {
				backup.tries++;
        		System.out.println("NUMBER OF TRIES " + backup.tries);
				reclaimChunks.put(c, backup);
				chunksToBackup.remove(c);
				return false;
			}
		}
		return false;
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
	
	public ConcurrentHashMap<Chunk, Backup> getReclaimChunks() {
		return reclaimChunks;
	}
}
