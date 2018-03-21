package filesystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Chunk {
	private String fileId;
	private int chunkNo;
	private byte[] fileData;
	private int actualRepDegree;
	
	public Chunk(String fileId, int chunkNo, byte[] fileData) {
		this.fileId = fileId;
		this.chunkNo = chunkNo;
		this.fileData = fileData;	
		this.actualRepDegree = 0;
	}
	
	public boolean store(String peerId) {
		FileOutputStream chunk = null;
		new File(peerId).mkdirs();
		String newName = peerId + "/" + fileId + ".part" + chunkNo;
		
		File f = new File(newName);
		if(f.exists() && !f.isDirectory()) {
			System.out.println("\r\n*** BACKUP: The chunk " + chunkNo + " from file " + fileId + " has already been stored on this peer! Store failed... ***");
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

	public String getFileId() {
		return fileId;
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
