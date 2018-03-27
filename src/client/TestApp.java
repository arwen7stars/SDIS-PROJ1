package client;

import server.RMIInterface;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestApp {
	private String peerAp;			// peer's access point
	private String protocolMessage;
	private String protocolName;

	public TestApp(String[] args) {
		String protocol = "";
		this.protocolName = args[1];

		this.peerAp = args[0];

		if(args.length == 4 && protocolName.equals("BACKUP")) {
			protocol = protocolName + " " + args[2] + " " + args[3];
		}else if(args.length == 3 && (protocolName.equals("RESTORE") || protocolName.equals("DELETE") || protocolName.equals("RECLAIM"))) {
			protocol = protocolName + " " + args[2];
		}else if(args.length == 2 && args[1].equals("STATE")) {
			protocol = protocolName;
		}else {
			protocol = "java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>";
		}

		System.out.println(protocol);
		this.protocolMessage = protocol;
	}

	public void init() {
		try {
		    Registry registry = LocateRegistry.getRegistry("localhost");
		    RMIInterface stub = (RMIInterface) registry.lookup(this.peerAp);

				System.out.println("Remote invocation of peer " + stub);
		    String response = stub.RMImessage(protocolMessage);

		    if (protocolName.equals("STATE")) {
		    	System.out.println(response);
		    }
		} catch (Exception e) {
		    System.err.println("Client exception: " + e.toString());
		    e.printStackTrace();
		}
	}

	public static void  main (String[] args)
	{
		if (args.length < 2){
			System.out.println("java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>");
			return;
		}

	    TestApp app = new TestApp(args);
	    app.init();
	}
}
