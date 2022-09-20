package WebSprocket.VMServer ;

import java.io.*;
import java.util.*;

/**
 * <p> Handler class to perform some work requested by the Pool.
 * This starts the service which is any class that implements
 * the Service interface.
 *
 * <p> Typically VMServer accepts requests from a variety of clients
 * of different types. Typically it uses a <code>WorkerThread</code>
 * to handle such request. As a part of request it receives the name
 * of the <code>Service</code> the request was made for. It then
 * instantiates the <code>Service</code> Object and invokes the
 * handleRequest method of the <code>Service</code> Object.
 *
 * <p> Typical WorkerThread's run method is as follows:
 *
 * <pre>
 * <code>
 * public void run () {
 *     ...
 *
 *     // start the service.
 *     startService () ;
 *     ...
 *     // push itself back to the stack.
 *     pushThread (this) ;
 *     ...
 * } // method run ends.
 *
 *
 * public void startService () {
 *     ServiceRequest sReq = null ;
 *     ServiceResponse sRes = null ;
 *     ...
 *     // read the service name.
 *     String serviceName = null ;
 *     serviceName = ... ;
 *     ...
 *     // instantiate the service
 *     Service service = null ;
 *     service = ... ;
 *     ...
 *     // invoke the service's handleRequest method.
 *     service.handleRequest (sReq, sRes) ; 
 *     ...
 * } // method startService ends.
 * </code>
 * </pre>
 *
 * @see WebSprocket.VMServer.Pool
 * @see WebSprocket.VMServer.Service
 */

public class WorkerThread extends Thread {
	private Object data = null ;
	private Stack waitStack ;
	public int maxThreads ;
	Properties sProps = null ;

	/**
	*	Creates a new WorkerThread instance.
	*	@param id Thread ID
	*	@param waiting The Stack to return this thread to when finished.
	*	@param maxThread The maximum number of threads 
	*	accepted on the wait Stack.
	**/
	WorkerThread (String id, Stack waiting, int maxThreads) {	
		super (id) ;
		waitStack = waiting ;
		this.maxThreads = maxThreads ;
	} // constructor WorkerThread ends.


	/**
	*	Wake the thread and do some work
	*	@param data Data to send to the worker
	*	@return	void 
	**/
	synchronized void wake (Object data) {	
		this.data = data ;
		notify () ;
	} // method wake ends.


	/**
	*	Thread.run() implementation.
	*	Do some work, and then wait again.
	**/
	synchronized public void run () {
		boolean stop = false;

		while (!stop) {
			if (data == null) {	
				try {	
					wait () ;
				} catch (InterruptedException ie) {	
					ie.printStackTrace () ;
					continue ;
				}
			}

			if (data != null) {	
				// start the service.
				startService () ;
			}

			data = null;
			stop = !(pushThread (this)) ; //try to store this Thread, or exit
		}
	} // method run ends.

	/**
	*	Retrieves the name of the Service object, and 
	*	creates an instance of it.  Then the Service object 
	*	handleRequest() method is called to perform a task.
	*	If any problem is encountered, this method returns.
	**/
	void startService () {
		System.out.println ("\nWT: startService() on Thread: "+ getName ()) ;
		//System.out.println("data "+data);

		WebSprocket.Shared.NetworkConnection conn = 
			(WebSprocket.Shared.NetworkConnection)data;
		//System.out.println("conn "+conn);

		String serviceName = "";
        Class serviceClass;

		//get the service class info
		try	{	
			InputStream inStream = conn.getInputStream();
			StringBuffer buf = new StringBuffer();
			int in = inStream.read();
			while(in != -1 && in != '\n')
			{	buf.append((char)in);
				in = inStream.read();
			}
			serviceName = buf.toString();
		} catch (IOException ioe) {	
			ioe.printStackTrace();
			return;	//can't go further
		}

		try {	
			System.out.println("loading class: '"+serviceName+"'");
			serviceClass = Class.forName(serviceName);
		} catch (ClassNotFoundException cnfe) {	
			cnfe.printStackTrace () ;
			return ;	//exit method on exception
		}

		Service service = null;
		try {	
			service = (Service) serviceClass.newInstance () ;
		} catch (InstantiationException ie) {	
			ie.printStackTrace () ;
			return ;
		} catch (IllegalAccessException iae) {	
			iae.printStackTrace () ;
			return;
		}

		try {	
			ServiceRequest request = null ;
			request = new ServiceRequest(conn.getInputStream(), arbIndex);
			arbIndex++;
			ServiceResponse response= null ;
			response = new ServiceResponse(conn.getOutputStream());

			service.setProperties (sProps) ;	//specify the settings
			service.handleRequest (request, response) ;
			conn.disconnect () ;	//clean up the connection
		} catch(IOException ioe) {	
			ioe.printStackTrace () ;
		}
	} // method startService ends.

	static int arbIndex = 0;


    /**
    *   Convenience method used by WorkerThread to put Thread back
    *   on the stack.
    *   @param w WorkerThread to push.
    *   @return boolean true if pushed, false otherwise.
    **/
    private boolean pushThread (WorkerThread wThread) {
		System.out.println("pushing "+getName() +" on Stack");
        boolean stayAround = false;
        synchronized (waitStack) {	
			if (waitStack.size () < maxThreads) {	
				stayAround = true ;
                waitStack.push (wThread) ;
            }
        }

        return stayAround;
    } // method pushThread ends.


	/**
	*   Sets the properties object which is passed to the Services.
	*   @param p The runtime properties for this server.
	**/
	public void setProperties (Properties props) {	
		sProps = props ;
	} // method setProperties ends.
} // class WorkerThread ends.
