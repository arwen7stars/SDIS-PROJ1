package protocols;

import server.Peer;
import utils.Message;
import utils.TypeMessage;

public class Delete {
	private Peer peer;
	
	public Delete(Peer peer) {
		this.peer = peer;
	}
	
	public void deleteFile(String filePath) {
		System.out.println("*** DELETE: Deleting file with path " + filePath + " ***");

		String fileId = peer.getInitiatorFiles().getFileId(filePath);
		
		if (fileId.equals(null)) {
			System.err.println("*** DELETE: The specified file doesn't exist on this server! ***");
		}
		
		peer.getInitiatorFiles().deleteFile(fileId);
		
	    String header = Message.createHeader(TypeMessage.DELETE, peer.getProtocolVersion(), peer.getPeerId(), fileId);
		Message msg = new Message(header);
		
		peer.getMcChannel().sendMessage(msg);
	}
	
	public void deleteChunk(String fileId) {
		
	}
	
	public Peer getPeer() {
		return peer;
	}
}
