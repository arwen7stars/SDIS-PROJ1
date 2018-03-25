package filesystem;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.UserPrincipal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Metadata {
	private String filePath;
	private String filename;
	private String lastModified;
	private String owner;
	
	public Metadata() {}
	
	public Metadata(String path) {
		Path file = Paths.get(path);
		BasicFileAttributes attr;
		
		try {
			attr = Files.readAttributes(file, BasicFileAttributes.class);
			UserPrincipal owner = Files.getOwner(file);
			
			this.filePath = path;
			this.filename = file.getFileName().toString();	
			this.lastModified = attr.lastModifiedTime().toString();
			this.owner = owner.getName();
			
		} catch (IOException e) {
			System.out.println("BACKUP: Given path is not correct!");
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * The function getFileId() generates a string which will be used to identify a file
	 * I combined the file metadata (file name, date modified, owner...) to generate a unique bit string
	 * After fileId is generated, SHA256 is applied
	 * 
	 * @return hashed file identifier
	 */
	public String generateFileId(){		
		String str = this.filename + this.lastModified + this.owner; 

		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		try {
			md.update(str.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		byte[] digest = md.digest();
		
		StringBuilder sb = new StringBuilder();
	    for (byte b : digest) {
	        sb.append(String.format("%02X", b));
	    }
	    
	    String fileId = sb.toString();
		
		return fileId;
	}
	
	public static String createRestorePath(String peerId, String filename) {
		new File(peerId).mkdirs();
		String newName = peerId + "//" + filename;
		
		return newName;
	}
	
	public static String getChunkPath(String peerId, String fileId, int chunkNo) {
		String chunkname = ".//" + peerId + "//" + fileId + ".part" + chunkNo;
		return chunkname;
	}
	
	public String getFilePath() {
		return filePath;
	}

	public String getFilename() {
		return filename;
	}

	public String getLastModified() {
		return lastModified;
	}

	public String getOwner() {
		return owner;
	}
}
