package filesystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import server.Peer;
import utils.Constants;

public class Chunk {
	private String fileId;
	private int chunkNo;
	private byte[] fileData;
	private int actualRepDegree;
	private int desiredRepDegree;
	
	public Chunk(String fileId, int desiredRepDegree, int chunkNo, byte[] fileData) {
		this.fileId = fileId;
		this.desiredRepDegree = desiredRepDegree;
		this.chunkNo = chunkNo;
		this.fileData = fileData;	
		this.actualRepDegree = 0;
	}
	
	public boolean store(Peer peer) {
		long updatedChunkSpace = peer.getBackedUpFiles().getChunkSpace() + (Constants.MAX_CHUNK_SIZE/1000);
		FileOutputStream chunk = null;
		new File(peer.getPeerId()).mkdirs();
		String newName = peer.getPeerId() + "/" + fileId + ".part" + chunkNo;
		
		File f = new File(newName);
		if( (f.exists() && !f.isDirectory()) || (updatedChunkSpace >= peer.getStorageSpace()) ) {
			System.out.println("\r\n*** BACKUP: The chunk " + chunkNo + " from file " + fileId + " was not stored on peer " + peer.getPeerId() + "... ***");
			return false;
		}
		
		try {
			chunk = new FileOutputStream(new File(newName));
		} catch (FileNotFoundException e) {
			System.err.println("*** BACKUP: Error opening chunk file ***");
			e.printStackTrace();
			return false;
		}
		
        try {
			chunk.write(fileData);
			chunk.flush();
	        chunk.close();
		} catch (IOException e) {
			System.err.println("*** BACKUP: Error writing data into chunk ***");
			e.printStackTrace();
			return false;
		}
        
        return true;
	}
	
	public void incRepDegree() {
		actualRepDegree++;
	}
	
	public void decRepDegree() {
		actualRepDegree--;
	}
	
	public void setRepDegree(int repDegree) {
		this.actualRepDegree = repDegree;
	}
	
	public void setActualRepDegree(int actualRepDegree) {
		this.actualRepDegree = actualRepDegree;
	}

	public String getFileId() {
		return fileId;
	}
	
	public int getDesiredRepDegree() {
		return desiredRepDegree;
	}

	public int getChunkNo() {
		return chunkNo;
	}

	public byte[] getFileData() {
		return fileData;
	}

	public int getActualRepDegree() {
		return actualRepDegree;
	}
}
