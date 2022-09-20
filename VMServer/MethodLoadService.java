package WebSprocket.VMServer;

import WebSprocket.Shared.*;
import java.io.*;
import WebSprocket.Router.*;


/**
*	This is a service that provides class loading.
*	Only one classload service runs at a time,
*	so these are queued in a static Vector.
*	When a classload finishes, it notifies the next one
*	in the queue to start.
*
*   This class simple acts as the link between the target and the VMFoundry.
*
*	@see WebSprocket.VMServer.VMServer
**/
public class MethodLoadService implements Service {
    static java.util.Vector idVector = new java.util.Vector();

    public MethodLoadService()
    {	idVector.addElement(this);
    }

    /**
     *	Leave the queue of MethodLoadService objects waiting to run.
     **/
    private void leaveLine()
    {	if(this == (MethodLoadService)idVector.elementAt(0))
	idVector.removeElementAt(0);
    }

    /**
     *   This method is called automatically by the Server Thread
     *   to perform the task at hand.
     *   <P>
     *   <pre>
     *   <code>
     *{
     *	DataInputStream diStream=new DataInputStream(request.getInputStream());
     *	DataOutputStream doStream=new DataOutputStream(response.getOutputStream());
     *	try
     *	{	doMethodLoad(diStream,doStream);
     *	}
     *	catch(IOException ioe)
     *	{	ioe.printStackTrace();
     *	}
     *}
     *   </code>
     *   </pre>
     *   @param request The object containing an InputStream and
     *   parameters describing the request.
     *   @param response The object containing an OutputStream and
     *   parameters describing the request.
     **/
    public synchronized void handleRequest(ServiceRequest request, ServiceResponse response)
    {
	System.out.println("starting service MethodLoad");
	while(this != (MethodLoadService)idVector.elementAt(0))
	    {	try
		{   wait();	//wait for turn
		}
	    catch(InterruptedException ie)
		{   ie.printStackTrace();
		continue;
		}
	    }
	System.out.println(Thread.currentThread().getName()+" awake");
	DataInputStream diStream=new DataInputStream(request.getInputStream());
	DataOutputStream doStream=new DataOutputStream(response.getOutputStream());
	arbID = request.getArbID();

	try
	    {	doMethodLoad(diStream,doStream);
	    }
	catch(IOException ioe)
	    {	ioe.printStackTrace();
	    }
	leaveLine();
	if(idVector.size() > 0)
	    {	MethodLoadService next = (MethodLoadService)idVector.elementAt(0);
	    next.wake();
	    }
    }

    /**
     *   Wake the thread so it starts running.
     **/
    synchronized void wake()
    {	notify();
    }

    /**
     *   Does the actual classloading.
     *	@param fromDIS The DataInputStream to read from.
     *	@param toDOS The DataOutputStream to write to.
     *	@throws IOException
     **/
    private void doMethodLoad(DataInputStream fromDIS,DataOutputStream toDOS)
	throws IOException
    {
	//GKJ: Commented out, as it converted to String.
	//int classID;
	String classID,methodID,methodType;
	int status;
	PipedInputStream pisFRC, fromSS;
	PipedOutputStream posFRC, toSS;

	DataInputStream fromFRC = null;
	DataOutputStream toFRC = null;

	// create the piped input streams.
	posFRC = new PipedOutputStream();
	toSS = new PipedOutputStream();

	// cross connect the input streams to output streams.
	pisFRC = new PipedInputStream(toSS);
	fromSS = new PipedInputStream(posFRC);

	fromFRC = new DataInputStream(pisFRC);
	toFRC = new DataOutputStream(posFRC);

	// get a vmfoundry that can ve used for synthesis
	getMeVMFoundry();


	FRClient frclient = null;
	// we have the serviceID, classID
	// create the foundry router client object.
	frclient = new FRClient (fromSS, toSS,fi);
	frclient.start ();

	// write the service ID to foundry router client.
	toFRC.writeInt (OPCodes.METHODLOAD);

	// start the vm client / server session.
	toDOS.writeInt (OPCodes.SSTART);

	while ((status = fromDIS.readInt ()) != OPCodes.SDONE) {
	    switch (status) {
	    case OPCodes.SCLASSID:
		// read the class ID from target.
		classID = readString (fromDIS) ;
		methodID = readString (fromDIS) ;
		methodType = readString (fromDIS) ;
		System.out.println ("SS: classID: " + classID+" MethodID: "+methodID);

		// write the class ID,methodID, methodType to FRC
		writeString (toFRC, classID) ;
		writeString (toFRC, methodID) ;
		writeString (toFRC, methodType) ;

		// read the synthlet size from FRC
		int synthSize;
		synthSize = fromFRC.readInt();

		//send the size of the synthlet to target.
		toDOS.writeInt(OPCodes.SSYNTHLETSIZE);
		toDOS.writeInt(synthSize);
		System.out.println("SS: synthsize: " + synthSize);

		// write size again
		toDOS.writeInt (synthSize);
		System.out.println ("SS: sy len: " + synthSize);

		// read synthlet data from FRC and write to target.
		byte[] synthCode = new byte [synthSize];
		try {
		    fromFRC.readFully(synthCode);
		    toDOS.write(synthCode);
		} catch(Exception e) {
		    System.err.println("SS: Transfer error: " + e.getMessage());
		    e.printStackTrace();
		}

		// make a call to data base to retrieve the class
		// with the class id, classID.
		System.out.println ("SS: Done sending the Method.");
		break;
	    }
	}
	toDOS.writeInt (OPCodes.SDONE);
    }

    /**
     *	Last method to be called when this service is finished.
     **/
    public void destroy()
    {
    }

    /**
     *	Returns the name of this service: "MethodLoadService".
     *	@return The service name.
     **/
    public String getServiceName()
    {	return "MethodLoadService";
    }

    /**
     *   Set the properties.
     *   @param p The properties that may relate to this service.
     **/
    public void setProperties(java.util.Properties p)
    {
    }


    /* This method writes a string to an OutputStream. */
    private void writeString (OutputStream oStream, String tString) {
	int sLength ;
	int index = 0 ;
	sLength = tString.length () ;

	try {
	    for (index=0; index<sLength; index++) {   
		oStream.write (tString.charAt (index)) ;
	    }

	    oStream.write ('\n') ;
	    oStream.flush () ;
	} catch (Exception e) {
	    System.out.println ("CLS: Exception while reading String.") ;
	}
    } // method writeString ends.

	
    /* This method reads a string from an InputStream. */
    private String readString (InputStream iStream) {
	String tString = null ;

	try {
	    StringBuffer buf = new StringBuffer () ;
	    int byteRead = iStream.read () ;

	    while (byteRead != -1 && byteRead != '\n') {	
		buf.append ((char) byteRead) ;
		byteRead = iStream.read () ;
	    }
	    tString = buf.toString () ;
	} catch (Exception e) {
	    System.out.println ("CLS: Exception while reading String.") ;
	}

	return (tString) ;
    } // method readString ends.


  private void getMeVMFoundry(){

    fi = null;
    // Add to queue to obtain vmfoundry and notify request
    synchronized (Router.q) {
      Router.q.add(this);
      Router.qInfo.add(getServiceName());
      Integer arbID_I = new Integer(arbID);
      Router.qArbID.add(arbID_I);
      Router.q.notify();
    }
    System.out.println("CLS: vmf request added to q, notified (id=" +arbID +")");

    // Wait till a foundry is available
    while (fi == null) {
      try {
        synchronized(monitor) {
          monitor.wait();
        }
      } catch (InterruptedException ie) {
        System.out.println("CLS: InterruptedException");
      }
    }

    // foundry is now available
    // the local fi holds the foundry info
  }

  public FoundryInfo fi = null;
  public int[] monitor = new int[1];
  public int arbID = 0;

} // class MethodLoadService ends.
