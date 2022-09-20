package WebSprocket.VMServer;

import java.io.* ;
import java.net.* ;
import WebSprocket.Shared.* ;

/**
 * ServerConnection is a TCP/IP socket based implementation of a Server 
 * object in WebSprocket paradigm. A ServerConnection in a WebSprocket 
 * paradigm is a generic process that accepts requests while abstracting 
 * the underlying network / transport mechanism used for communication
 * / transfer of data between server / client. The network / transport 
 * mechanism could Ethernet (TCP/IP) or Wireless or Fiber Optics.
 *
 * <p> It achieves this capability by implementing the NetworkConnection
 * interface of the VMServer API. This class is a reference 
 * implementation of a Server side NetworkConnection based on TCP/IP 
 * sockets. For example, a wireless based communication ServerConnection 
 * could be implemented as:
 *
 * <p><code>
 * <pre>
 * public class WServerConnection implements NetworkConnection {
 *	public WServerConnection () {
 *	} // constructor WServerConnection ends.
 *
 *	public boolean acceptConnection () {
 *		....
 *		// Implement the Wireless interface to accept a connection
 *		// from a client.
 *		....
 *	} // method acceptConnection ends.
 *
 *	// Other methods.
 * } // class WServerConnection ends.
 * </pre>
 * </code>
 *
 * @see WebSprocket.Shared.NetworkConnection
 * @see WebSprocket.Shared.SocketConnection
 * @see WebSprocket.VMServer.VMServer
 */

public class ServerConnection implements NetworkConnection {
    /**
     * ServerSocket on which it will listen to all connections.
     */
    ServerSocket ssocket = null;
    /**
     * Port used for communication.
     */
    int port;// = 21000;

    /**
	 *	@param port The port on which to listen.
     */
    public ServerConnection(int port)
	{
		this.port = port;
		System.out.println("Listening on port: "+port);
		try 
		{	ssocket = new ServerSocket(port);
		} 
		catch(Exception e) 
		{	System.out.println("*** Exception caught in ServerConnection().");
			e.printStackTrace();
		}
    }

	/**
	*	Listens for a connection to be made to this ServerConnection.
	*	The method blocks until a connection is made. 
	*	A new NetworkConnection is created.
	*	@return A new NetworkConnection object.
	*/
    public WebSprocket.Shared.NetworkConnection acceptConnection () {
		Socket tSocket = null ;
		try {	
			tSocket = ssocket.accept () ;
			return new SocketConnection (tSocket) ;
		} catch (java.io.IOException ioe) {
			System.out.println ("SC: Exception caught in acceptConnection.");
			ioe.printStackTrace();
		}
		return null;
    }

    /**
     *	Disconnect this connection, and free it's resources.
     */
	public void disconnect()
	{
		try
		{	ssocket.close();
		}
		catch(IOException ioe)
		{	ioe.printStackTrace();
		}
	}

    /**
     *	Get the timeout in milliseconds for this connection.
	 *	For a server connection, the timeout is usually forever and
	 *	this method returns the value 0.
	 *	@return The timeout for this server connection.
     */
	public int getTimeout()
	{	
		int timeout=0;
		try
		{	timeout = ssocket.getSoTimeout();
		}
		catch(IOException ioe)
		{	ioe.printStackTrace();
		}
		return timeout;
	}

	/**
	*	This method enables or disables NetworkConnection timeout 
	*	with the specified timeout, in milliseconds. 
	*	With this option set to a non-zero timeout, 
	*	a call to acceptConnection() for this ServerSocket will block 
	*	for only this amount of time. 
	*	If the timeout expires, a java.io.InterruptedIOException is raised, 
	*	though the ServerSocket is still valid. 
	*	The option must be enabled prior to entering the blocking 
	*	operation to have effect. 
	*	The timeout must be > 0. 
	*	A timeout of zero is interpreted as an infinite timeout.
	*	@param timeout The timeout value in milliseconds.
	**/
	public void setTimeout (int timeout) {
		try {	
			ssocket.setSoTimeout(timeout);
		} catch (IOException ioe) {	
			ioe.printStackTrace () ;
		}
	} // method setTimeOut ends.


    /**
     *	Returns the local address of this server.
	 *	@return The local address, which is often a String ("www3.sun.com").
     */
	public Object getLocalAddress () {
		try
		{	return InetAddress.getLocalHost().getHostAddress();
		}
		catch(UnknownHostException uhe)
		{	uhe.printStackTrace();
		}
		return null;
	} // method getLocalAddress ends.


	/**
	*   Get the address of the remote machine.
	*	The return object varies according to implementation, 
	*	but this method	often returns a String.
	**/
	public Object getRemoteAddress () {
		return (null) ;
	} // method getRemoteAddress ends.


	/**
	*	Returns true if currently connected.
	**/
	public boolean isConnected () {
		return (true) ;
	} // method isConnected ends.


    /**
    *   Get the OutputStream for this connection.
    **/
    public InputStream getInputStream () {
		return (null) ;
	} // method getInputStream ends.


    /**
    *   Get the OutputStream for this connection.
    **/
    public OutputStream getOutputStream () {
		return (null) ;
	} // method getOutputStream ends.
} // class ServerConnection ends.
