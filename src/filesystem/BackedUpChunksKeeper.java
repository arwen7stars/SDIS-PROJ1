package filesystem;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import server.Peer;

public class BackedUpChunksKeeper {
	private Peer peer;
	private ArrayList<Chunk> backedUpChunks;
	
	public BackedUpChunksKeeper(Peer peer) {
		this.peer = peer;
		this.backedUpChunks = setChunksFile();
	}
	
	public long getChunkSpace() {
		long sum = 0;
		
        for (int i = 0; i < backedUpChunks.size(); i++){
        	sum += backedUpChunks.get(i).getFileData().length;
        }
        
		sum /= 1000;		// bytes to KB
		
		return sum;
	}
	
	public boolean deleteChunksStorage(String fileId) {
		ArrayList<Chunk> chunksToDelete = new ArrayList<Chunk>();
		
		for(int i = 0; i < backedUpChunks.size(); i++) {
			if(backedUpChunks.get(i).getFileId().equals(fileId)) {
				int chunkNo = backedUpChunks.get(i).getChunkNo();
				String path = Metadata.getChunkPath(peer.getPeerId(), fileId, chunkNo);
				File chunk = new File(path);
				
				System.out.println("*** DELETE: CHUNK PATH " + path + " ***");
				if(chunk.exists()) {
					if (chunk.delete())
						System.out.println("*** DELETE: Chunk no. " + chunkNo + " deleted with success from file " + fileId + " ***");
					else System.out.println("*** DELETE: An error occurred deleting chunk no. " + chunkNo + " from file " + fileId + " ***");
				}
				
				chunksToDelete.add(backedUpChunks.get(i));
			}
		}
		deleteChunksFile(fileId);
		return backedUpChunks.removeAll(chunksToDelete);
	}
	
	public ArrayList<String> readChunksFile(File file) {
        ArrayList<String> lines = new ArrayList<String>();
        String line;
		FileReader fr = null;
		BufferedReader br = null;
		
        try{
            fr = new FileReader(file);
            br = new BufferedReader(fr);

            while((line = br.readLine()) != null){
            	lines.add(line);
            }
        } catch (IOException e) {
			e.printStackTrace();
		} finally {
            try {
				if (br != null)
					br.close();

				if (fr != null)
					fr.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
        
        return lines;
	}
	
	public void writeChunksFile(String path, String fileContent) {
        FileWriter fw = null;
        BufferedWriter bw = null;
        
        try {
            fw = new FileWriter(path, false);
            bw = new BufferedWriter(fw);
            
            String[] parts = fileContent.split("\\r?\\n");
            for(int i = 0; i < parts.length; i++) {
                bw.write(parts[i]);
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
			try {
				if (bw != null)
					bw.close();

				if (fw != null)
					fw.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
        }
	}
	
	public void deleteChunksFile(String fileId) {
		String fileContent = "";
		
        new File(peer.getPeerId()).mkdirs();
		String newName = peer.getPeerId() + "/fileChunks.txt";
        File file=new File(newName);
        
        ArrayList<String> lines = readChunksFile(file);
        
        for(int i = 0; i < lines.size(); i++) {
        	String[] parts = lines.get(i).split(",");

        	if(!(parts[0].equals(fileId))) {
        		fileContent += lines.get(i);
        		fileContent += "\n";
        	}
        }
        
    	writeChunksFile(newName, fileContent);
	}

	public void deleteChunk(String fileId, int chunkNo) {
		String fileContent = "";
		
        new File(peer.getPeerId()).mkdirs();
		String newName = peer.getPeerId() + "/fileChunks.txt";
        File file=new File(newName);
        
        ArrayList<String> lines = readChunksFile(file);
        
        for(int i = 0; i < lines.size(); i++) {
        	String[] parts = lines.get(i).split(",");
        	String fid = parts[0];
        	int cno = Integer.parseInt(parts[2]);

        	if(!(fid.equals(fileId) && (cno == chunkNo))) {
        		fileContent += lines.get(i);
        		fileContent += "\n";
        	}
        }
        
    	writeChunksFile(newName, fileContent);
	}
	
	public void updateChunksFile(String fileId, int desiredRepDegree, int chunkNo, int repDegree) {
        String fileContent = "";
        String newLine = fileId + "," + String.valueOf(desiredRepDegree) + "," + String.valueOf(chunkNo) + "," + String.valueOf(repDegree) + "\n";
        
        new File(peer.getPeerId()).mkdirs();
		String newName = peer.getPeerId() + "/fileChunks.txt";
        File file=new File(newName);
        
        ArrayList<String> lines = readChunksFile(file);
        boolean found = false;
        
        for(int i = 0; i < lines.size(); i++) {
        	String[] parts = lines.get(i).split(",");
        	String fid = parts[0];
        	int cno = Integer.parseInt(parts[2]);

        	if(fid.equals(fileId) && (cno == chunkNo)) {
        		//System.out.println("Updating " + fileId + "chunk No. " + chunkNo + " on " + newName);
        		fileContent += newLine;
        		found = true;
        	} else {
        		fileContent += lines.get(i);
        		fileContent += "\n";
        	}
        }
        
    	if(!found) {
    		//System.out.println("Adding " + fileId + "chunk No. " + chunkNo + " on " + newName);
    		fileContent += newLine;
    	}
        
    	writeChunksFile(newName, fileContent);
	}
	
	public ArrayList<Chunk> setChunksFile() {
        ArrayList<String> lines = new ArrayList<String>();
        
        new File(peer.getPeerId()).mkdirs();
		String newName = peer.getPeerId() + "/fileChunks.txt";
        File file=new File(newName);
        ArrayList<Chunk> chunks = new ArrayList<Chunk>();
        
        if(!file.exists()){
            try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
            
            return chunks;
        } else {
        	lines = readChunksFile(file);
	        
	        for(int i = 0; i < lines.size(); i++) {
            	String[] parts = lines.get(i).split(",");
            	String fileId = parts[0];
            	int desiredRepDegree = Integer.parseInt(parts[1]);
            	int chunkNo = Integer.parseInt(parts[2]);
            	int repDegree = Integer.parseInt(parts[3]);
            	
				String filePath = new File("").getAbsolutePath();
				String relativePath = Metadata.getChunkPath(peer.getPeerId(), fileId, chunkNo);
				
				String chunkPath = filePath.concat(relativePath);

				Path path = Paths.get(chunkPath);
				
				byte[] fileData = null;
				try {
					fileData = Files.readAllBytes(path);
				} catch (IOException e1) {
					System.err.println("*** BACKED UP CHUNKS KEEPER: Error reading chunk no. " + chunkNo + " from file " + fileId + " on server " + peer.getPeerId() + " ***");
					e1.printStackTrace();
				}
				
				Chunk c = new Chunk(fileId, desiredRepDegree, chunkNo, fileData);
				c.setRepDegree(repDegree);
				chunks.add(c);
	        }
	        
	        return chunks;
        }
	}
	
	public void addBackedUpChunk(Chunk c) {
		backedUpChunks.add(c);
		updateChunksFile(c.getFileId(), c.getDesiredRepDegree(), c.getChunkNo(), c.getActualRepDegree());
	}

	public Chunk getBackedUpChunk(String fileId, int chunkNo) {
		for(Chunk c : backedUpChunks) {
			if(c.getFileId().equals(fileId) && (c.getChunkNo() == chunkNo)) {
				return c;
			}
		}
		return null;
	}
	
	public ArrayList<Chunk> getBackedUpChunks() {
		return backedUpChunks;
	}


	public void setBackedUpChunks(ArrayList<Chunk> backedUpChunks) {
		this.backedUpChunks = backedUpChunks;
	}
}
