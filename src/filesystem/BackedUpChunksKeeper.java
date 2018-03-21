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
		this.backedUpChunks = getChunksFile();
	}
	
	public Chunk getChunkBackedUp(String fileId, int chunkNo) {
		for(Chunk c : backedUpChunks) {
			if(c.getFileId().equals(fileId) && (c.getChunkNo() == chunkNo)) {
				return c;
			}
		}
		return null;
	}
	
	public void addBackedUpChunk(Chunk c) {
		backedUpChunks.add(c);
		updateChunksFile(c.getFileId(), c.getChunkNo(), c.getActualRepDegree());
	}
	
	public ArrayList<Chunk> getChunksFile() {
        ArrayList<String> lines = new ArrayList<String>();
        String line;
        
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
        	FileReader fr = null;
    		BufferedReader br = null;
    		
        	System.out.println(newName + " already exists.");
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
	        
	        for(int i = 0; i < lines.size(); i++) {
            	String[] parts = lines.get(i).split(",");
            	String fileId = parts[0];
            	int chunkNo = Integer.parseInt(parts[1]);
            	int repDegree = Integer.parseInt(parts[2]);
            	
				String filePath = new File("").getAbsolutePath();
				String relativePath = peer.getInitiatorFiles().getChunkPath(peer.getPeerId(), fileId, chunkNo);
				
				String chunkPath = filePath.concat(relativePath);

				Path path = Paths.get(chunkPath);
				
				byte[] fileData = null;
				try {
					fileData = Files.readAllBytes(path);
				} catch (IOException e1) {
					System.err.println("*** BACKED UP CHUNKS KEEPER: Error reading chunk no. " + chunkNo + " from file " + fileId + " on server " + peer.getPeerId() + " ***");
					e1.printStackTrace();
				}
				
				Chunk c = new Chunk(fileId, chunkNo, fileData);
				c.setRepDegree(repDegree);
				chunks.add(c);
	        }
	        
	        return chunks;
        }
	}
	
	public void updateChunksFile(String fileId, int chunkNo, int repDegree) {
        String line;
        String fileContent = "";
        String newLine = fileId + "," + String.valueOf(chunkNo) + "," + String.valueOf(repDegree) + "\n";
        ArrayList<String> lines = new ArrayList<String>();
        
        new File(peer.getPeerId()).mkdirs();
		String newName = peer.getPeerId() + "/fileChunks.txt";
        File file=new File(newName);
        
        if(!file.exists()){
            try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
            
            System.out.println(newName + " created...");
            fileContent += newLine;
        } else {
    		FileReader fr = null;
    		BufferedReader br = null;
    		
        	System.out.println(newName + " already exists.");
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
	        boolean found = false;
	        
	        for(int i = 0; i < lines.size(); i++) {
            	String[] parts = lines.get(i).split(",");

            	if(parts[0].equals(fileId) && parts[1].equals(String.valueOf(chunkNo))) {
            		System.out.println("Updating " + fileId + "chunk No. " + chunkNo + " on " + newName);
            		fileContent += newLine;
            		found = true;
            	} else {
            		fileContent += lines.get(i);
            		fileContent += "\n";
            	}
	        }
	        
        	if(!found) {
        		System.out.println("Adding " + fileId + "chunk No. " + chunkNo + " on " + newName);
        		fileContent += newLine;
        	}
        }
        
        FileWriter fw = null;
        BufferedWriter bw = null;
        
        try {
            fw = new FileWriter(newName, false);
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


	public ArrayList<Chunk> getBackedUpChunks() {
		return backedUpChunks;
	}


	public void setBackedUpChunks(ArrayList<Chunk> backedUpChunks) {
		this.backedUpChunks = backedUpChunks;
	}
}
