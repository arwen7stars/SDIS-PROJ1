package filesystem;

import java.util.Vector;

public class FileInstance {
	private String fileId;
	private int repDegree;
	private Vector<Chunk> chunks;
	
	public FileInstance(String fileId, int repDegree) {
		this.fileId = fileId;
		this.repDegree = repDegree;
		this.chunks = new Vector<Chunk>();		
	}
	
	public boolean chunkExists(int chunkNo) {
		if(chunks != null){
			for(int i = 0; i < chunks.size(); i++) {
				if(chunks.get(i).getChunkNo() == chunkNo){
					return true;
				}
			}
		}
		return false;
	}

	public void addChunk(Chunk c) {
		this.chunks.add(c);
	}
	
	public void setRepDegree(int repDegree) {
		this.repDegree = repDegree;
	}
	
	public void setChunks(Vector<Chunk> chunks) {
		this.chunks = chunks;
	}
	
	public Chunk getChunk(int chunkNo) {
		for(int i = 0; i < chunks.size(); i++) {
			if(chunks.get(i).getChunkNo() == chunkNo)
				return chunks.get(i);
		}
		return null;
	}

	public String getFileId() {
		return fileId;
	}

	public int getRepDegree() {
		return repDegree;
	}

	public Vector<Chunk> getChunks() {
		return chunks;
	}
}
