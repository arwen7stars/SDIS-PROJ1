package server;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import channels.BackupChannel;
import channels.ControlChannel;
import server.RMIInterface;
import channels.RestoreChannel;
import filesystem.BackedUpChunksKeeper;
import filesystem.InitiatorFilesKeeper;
import protocols.Backup;
import protocols.Delete;
import protocols.Reclaim;
import protocols.Restore;
import protocols.State;
import utils.Constants;

public class Peer implements RMIInterface {
	private String protocolVersion;				// version of the protocol
	private String peerId;						// peer identifier (must be unique!)
	private String serviceAp;					// name of the remote object providing the testing service (will be used on the client side to test the application)

	private ControlChannel mcChannel;			// control channel (channel used to monitor the activity in this peer)
	private BackupChannel mdbChannel;			// backup channel (channel used to backup chunks of files)
	private RestoreChannel mdrChannel;			// restore channel (channel used to restore chunks of files)
	
	private Backup backupProtocol;				// protocol used to backup a file chunk
	private Restore restoreProtocol;			// protocol used to restore a file chunk
	private Delete deleteProtocol;				// protocol used to delete a file
	private Reclaim reclaimProtocol;			// protocol used to reclaim space on a peer
	private State state;
	
	private long storageSpace;						// maximum storage space in KB on this peer (will be used on reclaim protocol)
	private InitiatorFilesKeeper initiatorFiles;	// used to keep track of all files which backup started on this server
	private BackedUpChunksKeeper backedUpChunks;	// used to keep track of all chunks that were backed up on this peer
	private boolean restoreDelay;					// used to check whether the peer is sleeping (used on restore protocol)
	private boolean stopChunkMsg;					// used to check whether the peer should send CHUNK message (used on restore protocol)

	public String RMImessage(String message) throws RemoteException {
		System.out.println(message);
		String[] request = message.split(" ");
		String filePath;
		String protocol = request[0];
		String response = "";
		
		switch(protocol) {
		case "BACKUP":
			filePath = request[1];
			int repDegree = Integer.parseInt(request[2]);
			backup(filePath, repDegree);
			break;
		case "RESTORE":
			filePath = request[1];
			restore(filePath);
			break;
		case "DELETE":
			filePath = request[1];
			delete(filePath);
			break;
		case "RECLAIM":
			long diskSpace = Long.parseLong(request[1]);
			reclaim(diskSpace);
			break;
		case "STATE":
			response = state();
			break;
		default:
			System.out.println("ERRO!");//TODO
			break;
		}
		
    	return response;
    }
	
	 
	public static void main(String[] args) {
		try {
			Peer peer = new Peer(args);
		    RMIInterface stub = (RMIInterface) UnicastRemoteObject.exportObject(peer, 0);
	
		    // Bind the remote object's stub in the registry
		    Registry registry = LocateRegistry.getRegistry();
		    registry.bind(peer.getServiceAp(), stub);
		    System.err.println("Server ready");
		    
		} catch (Exception e) {
		    System.err.println("Server exception: " + e.toString());
		    e.printStackTrace();
		}
		
	}
	
	public Peer(String[] args) {
		if(args.length < 9) {
			System.out.println("java Peer <protocol_version> <peer_id> <peer_ap> <mc_address> <mc_port> <mdb_address> <mdb_port> <mdr_address> <mdr_port>");
			return;
		}
		
		this.protocolVersion = args[0];
		this.peerId = args[1];
		this.serviceAp = args[2];
		
		String mc_address = args[3];
		String mc_port = args[4];
		String mdb_address = args[5];
		String mdb_port = args[6];
		String mdr_address = args[7];
		String mdr_port = args[8];
		
        this.mcChannel = new ControlChannel(this, mc_address, mc_port);			// initialize control channel on this peer (NOTE: channel constructor is called here)
        this.mdbChannel = new BackupChannel(this, mdb_address, mdb_port);		// initialize backup channel on this peer (NOTE: channel constructor is called here)
        this.mdrChannel = new RestoreChannel(this, mdr_address, mdr_port);		// initialize restore channel on this peer (NOTE: channel constructor is called here)
        
        this.storageSpace = Constants.INITIAL_STORAGE_SPACE;
        this.backedUpChunks = new BackedUpChunksKeeper(this);
        this.restoreDelay = false;
        this.stopChunkMsg = false;
        
        this.backupProtocol = new Backup(this);		// initialize backup protocol on this peer
        this.restoreProtocol = new Restore(this);	// initialize restore protocol on this peer
        this.deleteProtocol = new Delete(this);		// initialize delete protocol on this peer
        this.reclaimProtocol = new Reclaim(this);	// initialize reclaim protocol on this peer
        this.state = new State(this);				// initialize reclaim protocol on this peer
        
        this.initiatorFiles = new InitiatorFilesKeeper();
        this.backedUpChunks = new BackedUpChunksKeeper(this);
        
        
		new Thread(mcChannel).start();				// start control channel
		new Thread(mdbChannel).start();				// start backup channel
		new Thread(mdrChannel).start();				// start restore channel
		
		System.out.println("Peer with id " + this.peerId + " ready!");
		
		/*if(this.peerId.equals("3")) {
			backup("C:\\Users\\Cláudia Marinho\\Documents\\NEON\\SDIS\\SDIS.pdf", 2);
			state();
		}*/
	}
	
	public void backup(String filePath, int replicationDegree) {
		backupProtocol.sendFileChunks(filePath, protocolVersion, peerId, replicationDegree);
	}
	
	public void restore(String filePath) {
		restoreProtocol.restoreFile(filePath);
	}
	
	public void delete(String filePath) {
		deleteProtocol.deleteFile(filePath);
	}
	
	public void reclaim(long diskSpace) {
		reclaimProtocol.manageDiskSpace(diskSpace);
	}
	
	public String state() {
		return state.state();
	}
	
	public void setStorageSpace(long storageSpace) {
		this.storageSpace = storageSpace;
	}
	
	public void setRestoreDelay(boolean restoreDelay) {
		this.restoreDelay = restoreDelay;
	}
	
	public void setStopChunkMsg(boolean stopChunkMsg) {
		this.stopChunkMsg = stopChunkMsg;
	}

	public String getProtocolVersion() {
		return protocolVersion;
	}

	public String getPeerId() {
		return peerId;
	}

	public String getServiceAp() {
		return serviceAp;
	}

	public ControlChannel getMcChannel() {
		return mcChannel;
	}

	public BackupChannel getMdbChannel() {
		return mdbChannel;
	}

	public RestoreChannel getMdrChannel() {
		return mdrChannel;
	}

	public long getStorageSpace() {
		return storageSpace;
	}

	public InitiatorFilesKeeper getInitiatorFiles() {
		return initiatorFiles;
	}
	
	public BackedUpChunksKeeper getBackedUpFiles() {
		return backedUpChunks;
	}
	
	public boolean getRestoreDelay() {
		return restoreDelay;
	}
	
	public boolean getStopChunkMsg() {
		return stopChunkMsg;
	}

	public Backup getBackupProtocol() {
		return backupProtocol;
	}

	public Restore getRestoreProtocol() {
		return restoreProtocol;
	}

	public Delete getDeleteProtocol() {
		return deleteProtocol;
	}

	public Reclaim getReclaimProtocol() {
		return reclaimProtocol;
	}
}
