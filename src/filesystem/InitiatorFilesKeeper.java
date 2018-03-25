package filesystem;

import java.io.File;
import java.util.Vector;

public class InitiatorFilesKeeper {
	private Vector<FileInstance> files = new Vector<FileInstance>();
	
	public InitiatorFilesKeeper() {	}
	
	public boolean fileExists(String fileId) {
		if(files != null){
			for(int i = 0; i < files.size(); i++) {
				if(files.get(i).getFileId().equals(fileId)){
					return true;
				}
			}
		}
		return false;
	}
	
	public void addFile(FileInstance f) {
		files.addElement(f);
	}
	
	public boolean deleteFile(String peerId, String fileId) {
		FileInstance f = this.getFile(fileId);
		
		if(f != null) {
			File file = new File(f.getFileMetadata().getFilePath());
    		
			if (file.delete()) {
    			System.out.println(file.getName() + " was deleted!");
    		} else {
    			System.out.println("Delete operation has failed.");
    		}
			
			String restorePath = Metadata.createRestorePath(peerId, f.getFileMetadata().getFilename());
			file = new File(restorePath);
			
			if (file.delete()) {
    			System.out.println(file.getName() + " was deleted!");
    		} else {
    			System.out.println("There's no restored files on this peer.");
    		}
			
			files.remove(f);
			return true;
		} else {
			System.out.println("*** DELETE: File has already been deleted from this peer.***");
			return false;
		}
	}
	
	public void deleteChunk(String fileId, int chunkNo) {
		for(int i = 0; i < files.size(); i++) {
			FileInstance f = files.get(i);
			if(f.getFileId().equals(fileId)) {
				f.deleteChunk(chunkNo);
			}
		}
	}
	
	public FileInstance getFile(String fileId) {
		for(int i = 0; i < files.size(); i++) {
			if(files.get(i).getFileId().equals(fileId)){
				return files.get(i);
			}
		}
		return null;
	}
	
	public String getFileId(String filePath) {
		for(int i = 0; i < files.size(); i++) {
			if(files.get(i).getFileMetadata().getFilePath().equals(filePath)) {
				return files.get(i).getFileId();
			}
		}
		return null;
	}
	
	public String getFilename(String fileId) {
		for(int i = 0; i < files.size(); i++) {
			if(files.get(i).getFileId().equals(fileId)) {
				return files.get(i).getFileMetadata().getFilename();
			}
		}
		return null;
	}
	
	public Chunk getChunk(String fileId, int chunkNo) {
		FileInstance f = this.getFile(fileId);
		Chunk c = f.getChunk(chunkNo);
		
		return c;
	}
	
	public int getChunkRepDegree(String fileId, int chunkNo) {
		FileInstance f = this.getFile(fileId);
		Chunk c = f.getChunk(chunkNo);
		
		return c.getActualRepDegree();
	}

	public Vector<FileInstance> getInitiatorFiles() {
		Vector<FileInstance> initiatorFiles = new Vector<FileInstance>();
		
		for(FileInstance f : files) {
			if (f.isInitiator()) {
				initiatorFiles.add(f);
			}
		}
		return initiatorFiles;
	}
}
