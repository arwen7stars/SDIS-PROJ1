package protocols;

import filesystem.FileInstance;
import filesystem.Metadata;
import server.Peer;
import utils.Message;
import utils.TypeMessage;

public class Delete {
	private Peer peer;
	
	public Delete(Peer peer) {
		this.peer = peer;
	}
	
	public void deleteFile(String filePath) {
		System.out.println("\n*** DELETE: Deleting file with path " + filePath + " ***\n");

		Metadata metadata = new Metadata(filePath);	// gets metadata using the given file path
		String fileId = metadata.generateFileId();	// generate file id using metadata information
		
		FileInstance f = peer.getInitiatorFiles().getFile(fileId);

		if (f.equals(null)) {
			System.out.println("*** DELETE: The file hasn't been backed up yet on any of the peers. ***");
			return;
		}

		f.setMetadata(metadata);

		peer.getInitiatorFiles().deleteFile(peer.getPeerId(), fileId);
		
	    	String header = Message.createHeader(TypeMessage.DELETE, peer.getProtocolVersion(), peer.getPeerId(), fileId);
		Message msg = new Message(header);
		
		peer.getMcChannel().sendMessage(msg);
	}
	
	public void handleDelete(Message msg) {
		peer.getBackedUpFiles().deleteChunksStorage(msg.getFileId());
		
		if(peer.getInitiatorFiles().fileExists(msg.getFileId())) {
			FileInstance f = peer.getInitiatorFiles().getFile(msg.getFileId());
			if(f.isInitiator()) {
				peer.getInitiatorFiles().deleteFile(peer.getPeerId(), msg.getFileId());
			}
		}
	}
	
	public Peer getPeer() {
		return peer;
	}
}
