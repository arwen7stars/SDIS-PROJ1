package protocols;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;
import java.util.Vector;
import java.util.Map.Entry;

import filesystem.Chunk;
import filesystem.FileInstance;
import filesystem.Metadata;
import server.Peer;
import utils.Constants;
import utils.Message;
import utils.TypeMessage;

public class Backup {
	private Peer peer;
	private boolean canBackup;
	
	public Backup(Peer peer) {
		this.peer = peer;
		this.canBackup = false;
	}
	
	public void sendFileChunks(String path, String version, String senderId, int repDegree) {		
		Metadata metadata = new Metadata(path);						// gets metadata (filename, lastModified and owner) using the given file path
		String fileId = metadata.generateFileId();					// generate file id using metadata information
		
		System.out.println("*** BACKUP: Attempting to backup file " + metadata.getFilename() + " ***");
		
		byte[] buffer = new byte[Constants.MAX_CHUNK_SIZE];			// buffer to get each chunk's data
		int chunkNo = 0;
		
		if(!peer.getInitiatorFiles().fileExists(fileId)) {						// checks if backup protocol has already been called for this file
			FileInstance f = new FileInstance(fileId, metadata, repDegree);		// if file doesn't exist on the initiator peer, create a new instance
			peer.getInitiatorFiles().addFile(f);
			
			System.out.println("Added file " + fileId + " to file keeper...");
		} else {
			Vector<Chunk> chunks = new Vector<Chunk>();							// if file already exists on file keeper...
			peer.getInitiatorFiles().getFile(fileId).setChunks(chunks);			// ...reset chunks...
			peer.getInitiatorFiles().getFile(fileId).setMetadata(metadata); 	// ...and metadata...
			peer.getInitiatorFiles().getFile(fileId).setRepDegree(repDegree);	// ...and replication degree
		}
		
		FileInputStream in = null;
		
		try {
			in = new FileInputStream(path);
		} catch (FileNotFoundException e) {
			System.err.println("*** BACKUP: File was not opened correctly! Given path is not correct. ***");
			return;
		}
		
		int bytesRead;
		
		try {
			bytesRead = in.read(buffer);			// read chunks of 64kb until all are read
		
			while(bytesRead != -1)
			{	
				if (!this.putchunk(version, senderId, fileId, chunkNo, repDegree, buffer)){
					System.err.println("*** BACKUP: Error sending file!! ***");
					return;
				}
				
				chunkNo++;
				bytesRead = in.read(buffer);
			}
			
			if(bytesRead == 0){
				if (!this.putchunk(version, senderId, fileId, chunkNo, repDegree, null)) {	// "If the file size is a multiple of the chunk size, the last chunk has size 0"
					System.err.println("*** BACKUP: Error sending file!! ***");
					return;
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public boolean putchunk(String version, String senderId, String fileId, int chunkNo, int repDegree, byte[] body) {
        boolean done = false;
        int tries = 0;
        int delay = 1000;
        
        String header = Message.createHeader(TypeMessage.PUTCHUNK, version, senderId, fileId, chunkNo, repDegree);
        Message msg = null;
		
        if(body != null)
        	msg = new Message(header, body);		// creates PUTCHUNK message to send over the mdb channel
        else msg = new Message(header);
        
    	if (!peer.getInitiatorFiles().getFile(fileId).chunkExists(chunkNo)) {		// checks if chunk instance already exists on this peer
    		Chunk chunk = new Chunk(fileId, repDegree, chunkNo, msg.getBody());
    		peer.getInitiatorFiles().getFile(fileId).addChunk(chunk);				// if chunk instance does not exist on this peer, one is created
		}

        while (!done && tries < Constants.MAX_TRIES) {		// "The initiator will send at most 5 PUTCHUNK messages per chunk"
        	peer.getMdbChannel().sendMessage(msg);			// send message over the MDB channel (backup channel). All opened MDB channels will receive this message.
			
        	try {
				Thread.sleep(delay);				// "The initiator-peer collects the confirmation messages during a time interval of one second"
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        	
        	int actualRepDegree = peer.getInitiatorFiles().getChunkRepDegree(msg.getFileId(), msg.getChunkNo());
        	
        	System.out.println("\n\t\tACTUAL REPLICATION DEGREE OF CHUNK " + msg.getChunkNo() + ": " + actualRepDegree+ "\n");
        	
        	if (repDegree <= actualRepDegree) {				// check whether desired replication degree (repDegree) has been reached
        		System.out.println("*** BACKUP: Backup of chunk " + msg.getChunkNo() + " from file " + msg.getFileId() + " was successful ***");
        		done = true;
        	} else {
        		System.out.println("*** BACKUP: Couldn't store chunk " + msg.getChunkNo() + " with desired replication degree. Trying again... ***");
        		tries++;
        		delay = 2*delay;		// double the time interval for receiving confirmation messages
        		
        		System.out.println("NUMBER OF TRIES " + tries);
        		
        		if(tries > Constants.MAX_TRIES) {
        			System.err.println("*** BACKUP: Error sending PUTCHUNK message. Maximum number of tries achieved ***");
        			return false;
        		}
        	}
        }

		return true;
	}
	
	public boolean putchunkReclaim(int delay, String version, String senderId, String fileId, int chunkNo, int actualRepDegree, int repDegree, byte[] body) {
        String header = Message.createHeader(TypeMessage.PUTCHUNK, version, senderId, fileId, chunkNo, repDegree);
        Message msg = null;
		
        if(body != null)
        	msg = new Message(header, body);		// creates PUTCHUNK message to send over the mdb channel
        else msg = new Message(header);

        if(!peer.getInitiatorFiles().fileExists(fileId)) {
        	Metadata m = new Metadata();
        	FileInstance f = new FileInstance(fileId, m, repDegree);
        	peer.getInitiatorFiles().addFile(f);
        	peer.getBackupProtocol().setCanBackup(true);
        }
    	
    	if (!peer.getInitiatorFiles().getFile(fileId).chunkExists(chunkNo)) {		// checks if chunk instance already exists on this peer
    		Chunk chunk = new Chunk(fileId, repDegree, chunkNo, body);
    		chunk.setActualRepDegree(actualRepDegree);
    		peer.getInitiatorFiles().getFile(fileId).addChunk(chunk);					// if chunk instance does not exist on this peer, one is created
		}
        
        peer.getMdbChannel().sendMessage(msg);		// send message over the MDB channel (backup channel). All opened MDB channels will receive this message.
			
    	try {
			Thread.sleep(delay);					// "The initiator-peer collects the confirmation messages during a time interval of one second"
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    	
    	int arp = peer.getInitiatorFiles().getChunkRepDegree(msg.getFileId(), msg.getChunkNo());
    	
    	System.out.println("\n\t\tACTUAL REPLICATION DEGREE OF CHUNK " + msg.getChunkNo() + ": " + arp + "\n");
        
        //peer.getInitiatorFiles().deleteChunk(fileId, chunkNo);

		return true;
	}
	
	public void stored(String version, String senderId, String fileId, int chunkNo){
	    String header = Message.createHeader(TypeMessage.STORED, version, senderId, fileId, chunkNo);
		Message msg = new Message(header);
		
		Random randomGenerator = new Random();
		Integer randomInt = randomGenerator.nextInt(400);
		
		try {
			Thread.sleep(randomInt);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		peer.getMcChannel().sendMessage(msg);
	}
	
	public void handleStored(Message msg) {
		Chunk bChunk = null;
		
		// increasing replication degree for initiator peer
		if(peer.getInitiatorFiles().fileExists(msg.getFileId())) {
			FileInstance f = peer.getInitiatorFiles().getFile(msg.getFileId());
			
			if (f.chunkExists(msg.getChunkNo())) {
				Chunk c = f.getChunk(msg.getChunkNo());
				c.incRepDegree();
			
				System.out.println("BACKUP: Replication degree of chunk no. " + c.getChunkNo() + " increased for file " + f.getFileId());
			}
		}
		
		if ((bChunk = peer.getBackedUpFiles().getBackedUpChunk(msg.getFileId(), msg.getChunkNo())) != null) {
			System.out.println("BACKUP: Storing info of chunk No. " + msg.getChunkNo());

			bChunk.incRepDegree();
			peer.getBackedUpFiles().updateChunksFile(bChunk.getFileId(), bChunk.getDesiredRepDegree(), bChunk.getChunkNo(), bChunk.getActualRepDegree());
		}
		
		for (Entry<Chunk,Reclaim.Backup> pair : peer.getReclaimProtocol().getReclaimChunks().entrySet()){
			Chunk c = pair.getKey();
			Reclaim.Backup b = pair.getValue();
			
			if(c.getFileId().equals(msg.getFileId()) && (c.getChunkNo() == msg.getChunkNo())) {
				if(!b.done || (b.tries < Constants.MAX_TRIES)) {
					bChunk = peer.getBackedUpFiles().getBackedUpChunk(msg.getFileId(), msg.getChunkNo());
					c.setActualRepDegree(bChunk.getActualRepDegree());
					
					if(bChunk.getDesiredRepDegree() > bChunk.getActualRepDegree()) {
						peer.getReclaimProtocol().replicateChunk(bChunk);
					} else System.out.println("*** BACKUP: Backup of chunk " + c.getChunkNo() + " from file " + c.getFileId() + " was successful ***");

				}
			}
	    }
	}
	
	public void setCanBackup(boolean canBackup) {
		this.canBackup = canBackup;
	}

	public Peer getPeer() {
		return peer;
	}
	
	public boolean getCanBackup() {
		return canBackup;
	}
}