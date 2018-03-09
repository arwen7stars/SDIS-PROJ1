package channels;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

import server.Peer;
import utils.Constants;
import utils.Message;

public abstract class Channel implements Runnable {
	int num = 0;
	protected Peer peer;				// peer subscribed to this channel
	protected InetAddress address;		// channel's IP address
	protected int port;					// channel's UDP port
	protected MulticastSocket socket;	// multicast datagram socket
	protected boolean open;				// indicates whether the channel is open or not
	
	/**
	 * Creates a multicast group for a given peer's channel (MC channel, MDB channel or MDR channel).
	 * A multicast group is specified by a class D IP address and by a standard UDP port number.
	 * Class D IP addresses are in the range 224.0.0.0 to 239.255.255.255, inclusive.
	 * The address 224.0.0.0 is reserved and should not be used.
	 * @param peer - peer subscribed to this channel
	 * @param address - IP address used to join the multicast group
	 * @param port - UDP port number used to create a multicast socket
	 */
	public Channel(Peer peer, String address, String port) {
		this.peer = peer;					// subscribe peer to this channel
		this.port = Integer.parseInt(port);	// get port integer
		open = true;						// open channel
		
	    try {
	    	this.address = InetAddress.getByName(address);		// determines the IP address of a host, given the host's name
	    } catch (UnknownHostException uhe) {
	    	System.out.println("Problems getting InetAddress");
	    	uhe.printStackTrace();
	    	System.exit(1);
	    }
		
		try {
			this.socket = new MulticastSocket(this.port);		// creates multicast socket on the given channel's port
			this.socket.joinGroup(this.address);				// joins multicast group on the given channel's address
		} catch(IOException ioe){
			System.out.println("Trouble opening multicast port");
			ioe.printStackTrace();
			System.exit(1);
		}	
	}
	
	/**
	 * Sends message across channel
	 * @param msg - message to be sent
	 */
	public void sendMessage(Message msg){
		byte[] buffer = msg.getMsg();		// get byte array with the message content
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);	// create datagram packet with the message content and the channel's IP address and UDP port as arguments

		try {
			socket.send(packet);			// send message across channel
		} catch (IOException e) {
			System.out.println("CHANNEL: Error sending message");
			e.printStackTrace();
		}
	}
	
	public Message receiveMessage() {
		byte[] buf = new byte[Constants.MAX_CHUNK_SIZE+2000];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);		
		try{
			socket.receive(packet);
		} catch(IOException ioe){
			ioe.printStackTrace();
		}
		
		Message msg = new Message(packet);
		return msg;
	}
}
