package WebSprocket.VMServer ;

import WebSprocket.Shared.*;
import java.net.* ;
import java.io.* ;

import java.util.Properties ;
import WebSprocket.Router.*;

/**
*	This class is a utility for the ClassLoadService class.
**/
public class FRClient extends Thread {

	/**
	*	@parm pis Stream to read from.
	*	@parm pos Stream to write to.
	**/
	public FRClient (PipedInputStream pis, PipedOutputStream pos, FoundryInfo fi) {
		try {
			// initialize the Properties variable.
			sProps = ServerProperties.getProperties () ;

			//DEST_IP = sProps.getProperty ("foundry.address") ;
			//DEST_PORT = Integer.parseInt (sProps.getProperty ("foundry.port")) ;

			this.fi = fi;
			DEST_IP = fi.ipAddress;
			DEST_PORT = fi.port;

			// assign the piped streams.
			fromSS = new DataInputStream (pis) ;
			toSS = new DataOutputStream (pos) ;
			// create the a connection to the Foundry Router server.
			System.out.println 
				("FRC: Connecting to " + DEST_IP + " @ " + DEST_PORT) ;
			socket = new Socket (DEST_IP, DEST_PORT) ;
			// create the streams.
			fromFRS = new DataInputStream (socket.getInputStream ()) ;
			toFRS = new DataOutputStream (socket.getOutputStream ()) ;
		} catch (Exception e) {
			System.out.println ("FRC: Exception caught in application.") ;
			e.printStackTrace () ;
	}
    } // constructor FRClient ends.

    /**
     *	Overrides Thread's run() method.
     **/
    public void run () {

        int serviceID = -1;
		try{
	    	// read the service ID from vm server session.
	   		 serviceID = fromSS.readInt () ;
	   		 // write the serviceID to foundry router server.
	   		 toFRS.writeInt (serviceID) ;
			 System.out.println("ServiceID ");
	   		 System.out.println ("FRC: serviceID: " + serviceID) ;
        }catch(Exception e){
	         System.out.println ("FRC: Exception caught in Application: " + e.getMessage()) ;
		}

		switch(serviceID){
		   case OPCodes.METHODLOAD:
				methodLoadService();
				break;
		   case OPCodes.CLASSLOAD:
				classLoadService();
				break;
        }


	}


	public void methodLoadService(){
	int scStartAddress = 0 ;
	byte[] synthCode = null ;
	int serviceID ;
	//GKJ: Commented out, as it converted to String.
	//int classID ;
	String classID,methodID,methodType ;
	try {
	    int status ;
	    byte [] iValue = new byte [4] ;
	    while ((status = fromFRS.readInt ()) != OPCodes.SDONE) {
		System.out.println ("FRC: OPCode read MethodLoad: " + status) ;
		switch (status) {
		case OPCodes.SSTART:
		    // read the classID methodID methodType from SS
		    classID = readString (fromSS) ;
		    methodID = readString (fromSS) ;
		    methodType = readString (fromSS) ;
		    // write the class ID opcode to FRST
		    toFRS.writeInt (OPCodes.SCLASSID) ;
		    // write the classID methodID methodType to FRST
		    writeString (toFRS, classID) ;
		    writeString (toFRS, methodID) ;
		    writeString (toFRS, methodType) ;
		    System.out.println ("FRC: classID: " + classID +" MethodID "+ methodID+" methodtype "+methodType) ;
		    break;
	
		case OPCodes.SSYNTHLETSIZE:
		    // read the synthlet size from FRS.
		    int synthsize ;
		    // GKJ: synthsize = fromFRS.readInt () ;
		    synthsize = fromFRS.readInt () ;
		    System.out.println ("FRC: synthsize: " + synthsize) ;
		    // write the synthlet size to SS
		    toSS.writeInt (synthsize) ;

			// write the length again
		    toSS.writeInt (synthsize) ;
		    // read & then write the synthlet code from FRS to SS.
		    synthCode = new byte[synthsize];
		    fromFRS.readFully(synthCode);
		    toSS.write(synthCode);
		    toFRS.writeInt (OPCodes.SDONE) ;
		    System.out.println ("FRC: Done sending OPCodes.SDONE.") ;
		    break ;
		}
	    }
	    System.out.println ("FRC: Done reading class from the Server.") ;
	    fromFRS.close () ;
	    toFRS.close () ;
	} catch (Exception e) {
	    System.out.println ("FRC: Exception caught in Application: " + e.getMessage()) ;
	    e.printStackTrace () ;

      }

      // Release VMFoundry
      releaseVMFoundry();

      return;

	}

   private void releaseVMFoundry() {

      if (fi != null) fi.busy = false;   // RELEASE VMFOUNDRY and
      fi = null;                         // trash local fi
      synchronized(Router.q) {
        Router.q.notify();
      }
      System.out.println("CLS/FRC: VMF released, notified. Done.");
    }




    public void classLoadService () {
	int scStartAddress = 0 ;
	byte[] synthCode = null ;
	int serviceID ;
	//GKJ: Commented out, as it converted to String.
	//int classID ;
	String classID ;
	try {
	    int status ;
	    byte [] iValue = new byte [4] ;
	    while ((status = fromFRS.readInt ()) != OPCodes.SDONE) {
		System.out.println ("FRC: OPCode read ClassLoad: " + status) ;
		switch (status) {
		case OPCodes.SSTART:
		    // read the class ID from SS
		    //GKJ: Commented out, as it converted to String.
		    //classID = fromSS.readInt () ;
		    classID = readString (fromSS) ;
		    // write the class ID opcode to FRST
		    toFRS.writeInt (OPCodes.SCLASSID) ;
		    // write the class ID to FRST
		    //GKJ: toFRS.writeInt (classID) ;
		    //GKJ: Commented out, as it converted to String.
		    //toFRS.writeInt (classID) ;
		    writeString (toFRS, classID) ;
		    System.out.println ("FRC: classID: " + classID) ;
		    break;
	
		case OPCodes.SSYNTHLETSIZE:
		    // read the synthlet size from FRS.
		    int synthsize ;
		    // GKJ: synthsize = fromFRS.readInt () ;
		    synthsize = fromFRS.readInt () ;
		    System.out.println ("FRC: synthsize: " + synthsize) ;
		    // write the synthlet size to SS
		    toSS.writeInt (synthsize) ;
		    // create the code array 
		    // read the start address from SS.
		    //synthCode = new int [synthsize/4] ;
		    scStartAddress = fromSS.readInt () ;
		    toFRS.writeInt (OPCodes.SSTARTADDR) ;
		    // GKJ: toFRS.writeInt (5000) ;
		    toFRS.writeInt (scStartAddress) ;
		    System.out.println ("FRC: st addr: " + scStartAddress) ;
		    break;

		case OPCodes.SSYNTHLET:
		    // read the synthlet size from FRS
		    int dLength ;
		    // GKJ: dLength = fromFRS.readInt () ;
		    //dLength = fromFRS.read (data) ;
		    dLength = fromFRS.readInt () ;
		    System.out.println ("FRC: synthlen: " + dLength) ;
		    // write the synthlet size to SS
		    toSS.writeInt (dLength) ;
		    // read & then write the synthlet code from FRS to SS.
		    synthCode = new byte[dLength];
		    fromFRS.readFully(synthCode);
		    toSS.write(synthCode);
		    toFRS.writeInt (OPCodes.SDONE) ;
		    System.out.println ("FRC: Done sending OPCodes.SDONE.") ;
		    break ;
		}
	    }
	    System.out.println ("FRC: Done reading class from the Server.") ;
	    fromFRS.close () ;
	    toFRS.close () ;
	} catch (Exception e) {
	    System.out.println ("FRC: Exception caught in Application: " + e.getMessage()) ;
	    e.printStackTrace () ;
	}

      // Release VMFoundry
      releaseVMFoundry();

      return;
    } // method run ends.


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
	    System.out.println ("FRC: Exception while reading String.") ;
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
			System.out.println ("FRC: Exception while reading String.") ;
		}

		return (tString) ;
	} // method readString ends.


	// Data Members.
	private String DEST_IP = null ;
	// GKJ: value is hardcoded, read it from the properties file.
	// private String DEST_IP = "10.0.0.233" ;

	private int DEST_PORT ;
	// GKJ: value is hardcoded, read it from the properties file.
	//private int DEST_PORT = 22000 ;

	private Socket socket = null ;
	private DataInputStream fromFRS = null ;
	private DataOutputStream toFRS = null ;
	DataInputStream fromSS = null ;
	DataOutputStream toSS = null ;

	Properties sProps = null ;
	FoundryInfo fi = null;
}	// class FRClient ends.
