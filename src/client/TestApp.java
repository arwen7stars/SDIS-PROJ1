package client;

import channels.RMIInterface;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestApp {

	private static String prepareArgs(String[] args) {
		String protocol = "";
		
		if(args.length == 4 && args[1].equals("BACKUP")) {
			protocol = args[1] + " " + args[2] + " " + args[3];
			
		}else if(args.length == 3 && (args[1].equals("RESTORE") || args[1].equals("DELETE") || args[1].equals("RECLAIM"))) {
			protocol = args[1] + " " + args[2];
			
		}else if(args.length == 2 && args[1].equals("STATE")) {
			protocol = args[1];
			
		}else {
			protocol = "java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2> ";
		}
		System.out.println(protocol);
		return protocol;
	}
	
	public static void  main (String[] args) 
	{
		
		String host = null;  //TODO
		
		String peer_ap = (args.length < 1) ? "SERVER" : args[0];
		
		try {
		    Registry registry = LocateRegistry.getRegistry(host);
		    RMIInterface stub = (RMIInterface) registry.lookup(peer_ap);
		    
		    String response = prepareArgs(args);
		    System.out.println("response: " + response);
		    
		    response = stub.RMImessage(prepareArgs(args));
		    
		} catch (Exception e) {
		    System.err.println("Client exception: " + e.toString());
		    e.printStackTrace();
		}
	
	}
}
