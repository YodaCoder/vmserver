package WebSprocket.VMServer;

import java.io.* ;

import java.net.InetAddress ;
import java.net.UnknownHostException ;

import java.util.* ;

import WebSprocket.Shared.* ;

/**
 * <p> VMServer is a reference implementation of a Server using 
 * VMServer API's. This Object with the help of the other API's 
 * accepts requests (clas load / synthesis) from the targets and 
 * processes them.
 *
 * <p> A typical server could be implemented as follows:
 *
 * <pre>
 * <code>
 * public ExampleServer extends Thread () {
 *     public ExampleServer () {
 *     } // constructor ExampleServer ends.
 *
 *     // run method.
 *     public void run () {
 *         ...
 *         // create a pool of threads.
 *         Pool pool = new Pool () ;
 *         ...
 *         // create a ServerConnection object that accepts requests.
 *         ServerConnection sCon = new ServerConnection () ;
 *         ...
 *         // while there is a request, accept and process it.
 *         while (true) {
 *             NetworkConnection nCon = null ;
 *             // accept the request.
 *             nCon = sCon.acceptConnection () ;
 *				
 *             // process the request by using one of the thread in the pool.
 *             pool.performWork (nCon) ;
 *         }
 *     } // method run ends.
 * } // class ExampleServer ends.
 * </code>
 * </pre>
 *
 * @see WebSprocket.VMServer.ServerConnection
 * @see WebSprocket.VMServer.Pool
 * @see WebSprocket.VMServer.WorkerThread
 * 
 */

public class VMServer extends Thread {
	/**
	 * Holds the Server Properties.
	 */
    Properties sProps ;

	/**
	 * Holds the Server Properties file name.
	 */
    String propertiesFileName = "server.properties" ;

	//Accept socket
	private static ServerConnection connection;

	//Handler threads
	private Pool threadPool;

	/**
	*   Thread pool size.  Default is 10.
	**/
	int poolSize = 10 ;

	/**
	*   Server socket port to listen for connections.
	**/
	int port ;


	/**
	*   The application starts here.
	*	There are no command line arguments passed to args[].
	*	All runtime parameters are loaded via the VMServerProperties file.
	*	If this file does not exist at runtime, it will be created
	*	in the local directory.
	**/
	public static void main (String args []) {

		  String classPath = System.getProperty ("java.class.path",".") ;
		  System.out.println ("CLASSPATH="+classPath) ;
		  VMServer ss = new VMServer () ;
		  ss.start();

	} // method main ends.



	/**
	*	Constructor using the default thread pool size.
	*	@param port Port to listen on
	*/
	public VMServer (int port) {
		super ("VMServer") ;
		this.port = port ;
		init () ;
	} // constructor VMServer ends.


	/**
	*	Create a new HttpServer instance
	*	@param port Port to listen on
	*	@param poolSize Number of handler threads
	*	throw IOException Thrown if the accept socket cannot be opened
	**/
	public VMServer (int port, int poolSize) {
		super ("VMServer") ;
		this.poolSize = poolSize ;
		this.port = port ;
		init () ;
	} // constructor VMServer ends.


	/**
	*	Constructor using the default thread pool size.
	*	@param port Port to listen on
	*/
	public VMServer () {	
		super ("VMServer") ;
		init () ;
	} // constructor VMServer ends.


	/**
	*	Initialize the network connections and thread pool.
	*/
	private void init () {
		sProps = ServerProperties.getProperties () ;

		try {	
			String serverIP =  InetAddress.getLocalHost().getHostAddress();
			System.out.println("Starting server on: " +serverIP);
		} catch (UnknownHostException uhe) {	
			uhe.printStackTrace();
		}

		try {
			port = Integer.parseInt (sProps.getProperty ("vmserver.port")) ;
		} catch (NumberFormatException nfe) {	
			System.err.println("Error in file: "+propertiesFileName);
			System.err.println("Cannot parse the property: vmserver.port");
			System.err.println(nfe);
			System.exit(1);
		}

		String homePath = sProps.getProperty("vmserver.homePath");
		String homeDirName = sProps.getProperty("vmserver.homeName");
		File homeDir = new File(homePath, homeDirName);

		if (! homeDir.exists ()) {
			boolean worked = homeDir.mkdirs();

			if (!worked) {
				System.err.println("Could not create directory: "+homeDir);
				System.exit(1);
			}
		}

		connection = new ServerConnection (port) ;
		threadPool = new Pool (poolSize, sProps) ;
		//saveProperties();	//create properties file if it doesn't exist

		// Start Router
		String routingTablefile = sProps.getProperty("routingtable.filename");
		System.out.println("Router started");
		WebSprocket.Router.Router router = new WebSprocket.Router.Router(routingTablefile);
		router.start();
	} // method init ends.


	/**
	*	This method is an infinite loop which listens for and 
	*	accepts connections.
	**/
	public void run () {
		while (true) {	
			WebSprocket.Shared.NetworkConnection netConn=
				connection.acceptConnection();
			threadPool.performWork(netConn);
		}
	} // method run ends.


	/**
     *   Save the current runtime properties for this server.
    void saveProperties()
    {
        try
        {   FileOutputStream out = new FileOutputStream(propertiesFileName);
            sProps.store(out, "VMServer Properties");
            out.close();
        }
        catch(IOException ioe)
        {   ioe.printStackTrace();
        }
    }
    **/
} // class VMServer ends.
