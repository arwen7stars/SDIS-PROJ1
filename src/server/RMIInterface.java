package server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIInterface extends Remote {
	String RMImessage(String message)throws RemoteException;
}

