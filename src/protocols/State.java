package protocols;

import java.util.Vector;

import filesystem.Chunk;
import filesystem.FileInstance;
import server.Peer;

public class State {
	private Peer peer;	
	public State(Peer peer) {
		this.peer = peer;
	}
	
	public String state() {
		
		String fileInfo = "STATE OF PEER NO. " + peer.getPeerId() + ":\r\n";
		
		fileInfo += "\r\nFiles information:\r\n";

		if (peer.getInitiatorFiles().getInitiatorFiles().size() != 0) {
			Vector<FileInstance> files = peer.getInitiatorFiles().getInitiatorFiles();
			
			for(FileInstance f : files) {
				fileInfo += "\tFilename: ";
				fileInfo += f.getFileMetadata().getFilename();
				fileInfo += "\r\n";
				
				fileInfo += "\tFile Id: ";
				fileInfo += f.getFileId();
				fileInfo += "\r\n";
				
				fileInfo += "\tDesired replication degree: ";
				fileInfo += f.getRepDegree();
				fileInfo += "\r\n";
				fileInfo += "\tFile chunks:\r\n";
				
				for(Chunk c : f.getChunks()) {
					fileInfo += "\t\tChunk no. ";
					fileInfo += c.getChunkNo();
					fileInfo += " - Actual replication degree: ";
					fileInfo += c.getActualRepDegree();
					fileInfo += "\r\n";
				}
				
				fileInfo += "\r\n";
			}
			
		} else {
			fileInfo += "*** STATE: No files were backed up yet. ***\r\n\r\n";
		}
		
		fileInfo += "Chunks information:\r\n";
		
		if(peer.getBackedUpFiles().getBackedUpChunks().size() != 0) {
			for(Chunk c : peer.getBackedUpFiles().getBackedUpChunks()) {
				fileInfo += "\tChunk of file " + c.getFileId();
				fileInfo += "\r\n";
				fileInfo += "\tChunk no. " + c.getChunkNo();
				fileInfo += "\r\n";
				fileInfo += "\tChunk size " + c.getFileData().length;
				fileInfo += "\r\n";
				fileInfo += "\tActual replication degree " + c.getActualRepDegree();
				fileInfo += "\r\n";
				
				fileInfo += "\r\n";
			}
		}else {
			fileInfo += "*** STATE: No chunks were backed up yet on this peer. ***\r\n\r\n";
		}
		
		fileInfo += "Peer storage capacity: " + peer.getStorageSpace();
		return fileInfo;
	}
}
