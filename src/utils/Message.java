package utils;

import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;

public class Message {
	private TypeMessage msgType;
	private String version;
	private String senderId;
	private String fileId;
	private int chunkNo;
	private int repDegree;
	
	private String header;
	private byte[] body;
	private byte[] msg;
	
	public Message(String header){
		this.header = header;
		this.body = null;
		this.msg = this.header.getBytes();
		
		String tmp = new String(header);
		this.setHeaderVariables(tmp);
	}
	
	public Message(String header, byte[] body) {
		byte[] message = new byte[header.getBytes().length + body.length];
		byte[] header_array = header.getBytes();
		this.header = header;
		this.body = body;
		
		System.arraycopy(header_array, 0, message, 0, header_array.length);
		System.arraycopy(body, 0, message, header_array.length, body.length);
		this.msg = message;
		
		String[] splitMsg = header.split("\r\n\r\n");
		this.setHeaderVariables(splitMsg[0]);
	}
	
	public Message(DatagramPacket packet){
		byte[] content = packet.getData();
		String received = null;
		
		try {
			received = new String(content, "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		String[] splitMsg = received.split("\r\n\r\n", 2);
		
		String header = splitMsg[0];
		
		this.setHeaderVariables(header);
		this.createHeader(this.msgType);

		if( (this.msgType == TypeMessage.PUTCHUNK) || (this.msgType == TypeMessage.CHUNK) ){
			this.body = splitMsg[1].getBytes();
		} else this.body = null;
	
		
		if(this.body == null){
			this.msg = this.header.getBytes();
		} else {
			byte[] message = new byte[this.header.getBytes().length + this.body.length];
			
			System.arraycopy(this.header.getBytes(), 0, message, 0, this.header.getBytes().length);
			System.arraycopy(this.body, 0, message, this.header.getBytes().length, this.body.length);
			this.msg = message;
		}
	}
	
	public void setHeaderVariables(String header) {
		String[] headerParts = header.split(" ");
		
		this.version = headerParts[1];
		this.senderId = headerParts[2];
		this.fileId = headerParts[3];
		
		switch(headerParts[0]) {
		case "PUTCHUNK":
			this.msgType = TypeMessage.PUTCHUNK;
			this.chunkNo = Integer.parseInt(headerParts[4]);
			this.repDegree = Integer.parseInt(headerParts[5]);			
			break;
		case "STORED":
			this.msgType = TypeMessage.STORED;
			this.chunkNo = Integer.parseInt(headerParts[4]);
			break;
		case "GETCHUNK":
			this.msgType = TypeMessage.GETCHUNK;
			this.chunkNo = Integer.parseInt(headerParts[4]);
			break;
		case "CHUNK":
			this.msgType = TypeMessage.CHUNK;
			this.chunkNo = Integer.parseInt(headerParts[4]);
			break;
		case "REMOVED":
			this.msgType = TypeMessage.REMOVED;
			this.chunkNo = Integer.parseInt(headerParts[4]);			
			break;
		case "DELETE":
			this.msgType = TypeMessage.DELETE;
			break;
		default:
			break;		
		}
	}
	
	public void createHeader(TypeMessage type) {
		if (type.equals(TypeMessage.PUTCHUNK)){
			this.header = Message.createHeader(this.msgType, this.version, this.senderId, this.fileId, this.chunkNo, this.repDegree);
		} else if (type.equals(TypeMessage.DELETE)) {
			this.header = Message.createHeader(this.msgType, this.version, this.senderId, this.fileId);
		} else {
			this.header = Message.createHeader(this.msgType, this.version, this.senderId, this.fileId, this.chunkNo);
		}
	}
	
	// Creates header for PUTCHUNK (message to send in the chunk backup subprotocol)
	public static String createHeader(TypeMessage msgType, String version, String senderId, String fileId, int chunkNo, int repDegree) {		
		String chunkNoStr = Integer.toString(chunkNo);
		String repDegreeStr = Integer.toString(repDegree);
		
		String header = msgType + " " + version + " " + senderId + " " + fileId + " " + chunkNoStr + " " + repDegreeStr;
		header += "\r\n\r\n";
		
		return header;
	}
	
	/** Creates header for:
	 * 		-> STORED (message to receive in the chunk backup subprotocol)
	 * 		-> GETCHUNK & CHUNK (messages to send & receive in the chunk restore subprotocol)
	 * 		-> REMOVED (message to send in the space reclaiming subprotocol)
	 */
	public static String createHeader(TypeMessage msgType, String version, String senderId, String fileId, int chunkNo) {
		String chunkNoStr = Integer.toString(chunkNo);		
		
		String header = msgType + " " + version + " " + senderId + " " + fileId + " " + chunkNoStr;
		header += " \r\n\r\n";
		
		return header;
	}
	
	// Creates header for DELETE (message to send in the file deletion subprotocol)
	public static String createHeader(TypeMessage msgType, String version, String senderId, String fileId) {
		String header = msgType.toString() + " " + version + " " + senderId + " " + fileId;
		header += " \r\n\r\n";
		
		return header;
	}

	public TypeMessage getMsgType() {
		return msgType;
	}

	public String getVersion() {
		return version;
	}

	public String getSenderId() {
		return senderId;
	}

	public String getFileId() {
		return fileId;
	}

	public int getChunkNo() {
		return chunkNo;
	}

	public int getRepDegree() {
		return repDegree;
	}

	public String getHeader() {
		return header;
	}

	public byte[] getBody() {
		return body;
	}

	public byte[] getMsg() {
		return msg;
	}
}
