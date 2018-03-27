package protocols;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import filesystem.Chunk;
import filesystem.FileInstance;
import filesystem.Metadata;
import server.Peer;
import utils.Constants;
import utils.Message;
import utils.TypeMessage;

public class Restore {
	private Peer peer;
	private ConcurrentHashMap<String, HashMap<Integer,byte[]>> restoredChunks = new ConcurrentHashMap<String, HashMap<Integer,byte[]>>();		// file mapped to hashmap of chunks
	private Vector<Integer> chunksToResend = new Vector<Integer>();		// array of numbers of chunks to resend
	private boolean restoreOngoing;

	public Restore(Peer peer) {
		this.peer = peer;
		this.restoreOngoing = false;
	}

	public void getchunk(String version, String senderId, String fileId, int chunkNo) {
		String header = Message.createHeader(TypeMessage.GETCHUNK, version, senderId, fileId, chunkNo);
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

	public void handleGetchunk(Message msg) {
		Chunk c = null;
		if ((c = peer.getBackedUpFiles().getBackedUpChunk(msg.getFileId(), msg.getChunkNo())) != null) {
			peer.getRestoreProtocol().chunk(msg.getVersion(), msg.getFileId(), msg.getChunkNo(), c.getFileData());
		}
	}

	public void chunk(String version, String fileId, int chunkNo, byte[] body) {
		String header = Message.createHeader(TypeMessage.CHUNK, version, peer.getPeerId(), fileId, chunkNo);
		Message msg = new Message(header, body);

		Random randomGenerator = new Random();
		Integer randomInt = randomGenerator.nextInt(400);

		peer.setRestoreDelay(true);

		try {
			Thread.sleep(randomInt);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		peer.setRestoreDelay(false);

		if (peer.getStopChunkMsg()) {
			peer.setStopChunkMsg(false);
			return;
		} else {
			peer.getMdrChannel().sendMessage(msg);
		}
	}

	public void restoreFile(String filePath) {
		System.out.println("\n*** RESTORE: Restoring file with path " + filePath + " ***\n");
		this.restoreOngoing = true;

		restoredChunks = new ConcurrentHashMap<String, HashMap<Integer,byte[]>>();	// reset restored chunks
		chunksToResend = new Vector<Integer>();						// reset chunks to resend

		Metadata metadata = new Metadata(filePath);					// gets metadata (filename, lastModified and owner) using the given file path
		String fileId = metadata.generateFileId();					// generate file id using metadata information

		FileInstance f = peer.getInitiatorFiles().getFile(fileId);

		if (f.equals(null)) {
			System.out.println("*** RESTORE: The file hasn't been backed up yet on any of the peers. ***");
			return;
		}

		f.setMetadata(metadata);

		HashMap<Integer, byte[]> tmp = new HashMap<Integer, byte[]>();
		restoredChunks.put(fileId, tmp);

		Vector<Chunk> chunks = f.getChunks();

		System.out.println("*** RESTORE: Num of chunks to restore: " + chunks.size() + " ***");

		for(int i = 0; i < chunks.size(); i++) {
			getchunk(peer.getProtocolVersion(), peer.getPeerId(), fileId, chunks.get(i).getChunkNo());
		}

		try {
			Thread.sleep(chunks.size() * 1000);
		} catch(InterruptedException e){
			e.printStackTrace();
		}

		if(restoredChunks.get(fileId).size() < chunks.size()) {
			System.out.println("*** RESTORE: Some chunks were lost :( Let's try again! ***");
			for(int i = 1; i < Constants.MAX_TRIES; i++){
				this.resendChunks(fileId, chunks.size());

				if(restoredChunks.get(fileId).size() == chunks.size())
					break;
			}
		}

		String filename = peer.getInitiatorFiles().getFilename(fileId);
		System.out.println("*** RESTORE: Filename of the file to be restored: " + filename + " ***\n");

		if(filename != null) {
			putFileTogether(filename, fileId);
		} else {
			System.out.println("*** RESTORE: Filename not found.");
		}
		this.restoreOngoing = false;
	}

	public void putFileTogether(String filename, String fileId) {
		String restorePath = Metadata.createRestorePath(peer.getPeerId(), filename);
		Map<Integer, byte[]> sortedByKey = new TreeMap<Integer, byte[]>(restoredChunks.get(fileId));

		FileOutputStream fos = null;

		try {
			fos = new FileOutputStream(new File(restorePath), true);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		System.out.println("\t\tRESTORED CHUNKS " + restoredChunks.get(fileId).size());

		try {
			for (Map.Entry<Integer, byte[]> entry : sortedByKey.entrySet()) {
				System.out.println("Writing data of chunk " + entry.getKey());
				byte[] fileData = entry.getValue();
				fos.write(fileData);
			}

			fos.flush();
	        fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("RESTORE: File successfully restored!!");
	}

	public void resendChunks(String fileId, int noChunks) {
		Vector<Integer> tmp = new Vector<Integer>();

		for(Map.Entry<Integer, byte[]> entry : restoredChunks.get(fileId).entrySet()){
			tmp.add(entry.getKey());
		}

		chunksToResend = Restore.findMissingNumbers(tmp, noChunks-1);

		for(int i = 0; i < chunksToResend.size(); i++) {
			this.getchunk(peer.getProtocolVersion(), peer.getPeerId(), fileId, chunksToResend.get(i));
		}

	}

	public static Vector<Integer> findMissingNumbers(Vector<Integer> a, int last) {
		Vector<Integer> missingNumbers = new Vector<Integer>();

		// inside the array: at index i, a number is missing if it is between a[i-1]+1 and a[i]-1
		for (int i = 1; i < a.size(); i++) {
		    for (int j = 1 + a.get(i-1); j < a.get(i); j++) {
		    	missingNumbers.add(j);
		    }
		}

		// after the array: numbers between a[a.length-1] and last
		for (int i = 1+ a.get(a.size()-1); i <= last; i++) {
			missingNumbers.add(i);
		}

		return missingNumbers;
	}

	public void addToRestoredChunks(String fileId, int chunkNo, byte[] fileData) {
		if(restoredChunks.containsKey(fileId)) {
			if(!restoredChunks.get(fileId).containsKey(chunkNo)) {
				restoredChunks.get(fileId).put(chunkNo, fileData);
			}
		}
	}

	public Peer getPeer() {
		return peer;
	}

	public ConcurrentHashMap<String, HashMap<Integer,byte[]>> getRestoredChunks() {
		return restoredChunks;
	}

	public boolean isRestoreOngoing() {
		return restoreOngoing;
	}
}
