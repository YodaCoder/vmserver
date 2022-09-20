package WebSprocket.VMServer;

import WebSprocket.Shared.*;
import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.net.*;
import WebSprocket.Router.*;
import WebSprocket.Shared.OPCodes;

        /* The function of this thread is to bridge the
         * WDKLite and VMServer/VMFoundry products in such
         * a way that the client does not need to know
         * where the VMFoundry is running. WDKLite talks only
         * to VMServer while the later routes the request to
         * an available Foundry  WDKLite talks only
         * to VMServer while the later routes the request to
         * an available VMFoundry. If there is no free Foundry,
         * the request from WDKlite is rejected.
         */
/**
*	This is a simple service for the VMServer 
*	which accepts a jar of class files from a user and saves 
*	the data on the server side.  After this service completes,
*	the user can synthesize the code.
*	@see WebSprocket.VMServer.VMServer
**/
public class FirstLoadService implements Service {


  	public int[] monitor = new int[1];
	public FoundryInfo fi = null;

	public FirstLoadService()
	{
	}

	/**
	*   This method is called automatically by the Server Thread
	*   to perform the task at hand.
	*   <P>
	*	Example implementation:
	*   <pre>
	*   <code>
	*{
	*	DataInputStream diStream=new DataInputStream(request.getInputStream());
	*	DataOutputStream doStream=new DataOutputStream(response.getOutputStream());
	*	try
	*	{
	*		int size = diStream.readInt();
	*		System.out.println("FirstLoadService: data size= "+size);
	*		byte [] result = new byte[size];
	*		diStream.readFully(result, 0, result.length);
	*		File oldDir = new File("classes");
	*		if(oldDir.exists())
	*			deleteDirectory(oldDir.getName());
	*		ByteArrayInputStream bais = new ByteArrayInputStream(result);
	*		unjarStream(bais);
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
    public synchronized void handleRequest (ServiceRequest request, ServiceResponse response) {

		System.out.println("receiving file");
		DataInputStream diStream=new DataInputStream(request.getInputStream());
		DataOutputStream doStream=new DataOutputStream(response.getOutputStream());
		arbID = request.getArbID();
		try {
			// GKJ: added code to read the jar file name.
			String jarName = null ;

			StringBuffer buf = new StringBuffer () ;
			int in = diStream.read () ;
			while (in != -1 && in != '\n') {	
				buf.append ((char) in) ;
				in = diStream.read () ;
			}
			jarName = buf.toString () ;
			System.out.println ("FLS: Jar File name " + jarName) ;

			int size = diStream.readInt();
			System.out.println("data size= "+size);
			byte [] result = new byte[size];
			diStream.readFully(result, 0, result.length);
			/*
			FileOutputStream foStream = new FileOutputStream("testFile.jar");
			foStream.write(result, 0, result.length);
			foStream.flush();
			foStream.close();
			*/
			File oldDir = new File("classes"+File.separator+"SA285");
			if(oldDir.exists())
				deleteDirectory(oldDir.getName());

			ByteArrayInputStream bais = new ByteArrayInputStream (result) ;

			// check to see the type of data base to be used.
			//unjarStream(bais);

			Properties sProps = ServerProperties.getProperties () ;
			// represents the type of data base used.
			String dsType = null ;
			dsType = sProps.getProperty ("dataSource") ;

			if (dsType.equals ("fileSystem")) {
				// data base used is file system.
				FileSystemDataAccess fsdAccess = null ;
				fsdAccess = new FileSystemDataAccess () ;
				fsdAccess.processJar (jarName, bais) ;
			} else { 
				// data base used is RDBMS.
				OracleDataAccess odAccess = null ;
				odAccess = new OracleDataAccess (sProps) ;
				odAccess.processJar (jarName, bais) ;
			}

			// close the ByteArrayInputStream.
			bais.close () ;
		} catch (IOException ioe) {	
			ioe.printStackTrace () ;
			// close sockets, release vmfoundry and return
			return;
		}

		handleRequest2(request, response);  // first load related

	} // method handleRequest ends.


    String root = "classes";

	/**
	*	Read jar entry from input stream and write the the file system.
	*	@param in The InputStream to read the jar data from.
	**/
    void unjarStream(InputStream in) throws IOException
    {
        JarInputStream jarInStream = new JarInputStream(in);
        JarEntry entry = null;
        int count=0;
        File dir;
        //File dir = new File(root);	
        //dir.mkdir();
        byte [] buf = new byte[1024];
        FileOutputStream foStream;
        String name;
        while((entry = jarInStream.getNextJarEntry()) != null)
        {
            name = entry.getName();
            System.out.println(name);
			/*
            //if(name.endsWith("/"))  
            if(name.endsWith(File.separator))  	//directory
            {   
				//dir = new File(root+File.separator+name);
				dir = new File(name);
                dir.mkdir();
            }
            else
            {
			*/
                //foStream = new FileOutputStream(root+File.separator+name);

				int lastIndex;
				//if((lastIndex = name.lastIndexOf(File.separatorChar)) > 0)
				if((lastIndex = name.lastIndexOf('/')) > 0)
				{
					dir = new File(name.substring(0, lastIndex));
					//System.out.println("DIR "+ dir.toString());
					if(!dir.exists())
					{
						//System.out.println("CREATING ");
						dir.mkdirs();
					}
				}

                foStream = new FileOutputStream(name);
                while((count = jarInStream.read(buf, 0, buf.length)) != -1)
                {   foStream.write(buf, 0, count);
                }
                foStream.flush();
                foStream.close();
            //}
        }
    }

	/**
	*	Delete a directory by recursively deleting all files.
	*	@param dirName The name of the directory.
	**/
	void deleteDirectory(String dirName)
	{
		File dir = new File(dirName);
		if(!dir.exists())
			return;
		ArrayList dirListing = new ArrayList();
		getFileList(dirListing, dir);
		Collections.sort(dirListing,  java.text.Collator.getInstance());
		Collections.reverse(dirListing);
		File delFile;
		for(int i=0;i<dirListing.size(); i++)
		{
			delFile = new File((String)dirListing.get(i));
			delFile.delete();
		}
		dir.delete();
	}

	/**
	*	Get a list of all files in a directory.
	*	@param aList	The ArrayList to which file names are added.
	*	@param dir	The directory of interest.
	**/
    void getFileList(ArrayList aList, File dir)
    {
        //System.out.println(dir);
        if(dir.isFile())
            return; //should never happen
        String [] files = dir.list();
        if(files == null || files.length == 0)
            return; //empty directory
        for(int i= 0; i<files.length; i++)
        {
            //if(files[i].endsWith(".class"))
            File file = new File(dir + File.separator + files[i]);
            if(file.isDirectory())
            {   aList.add(dir+ File.separator + files[i]+File.separator);
                getFileList(aList, file);
            }
            else
                aList.add(dir+ File.separator + files[i]);
        }
    }

	/**
	*	Last method to be called when this service is finished.
	**/
	public void destroy () {
	} // method destroy ends.


	/**
	*	Returns the name of this service.
	*	@return The service name.
	**/
	public String getServiceName () {	
		return "FirstLoadService" ;
	} // method getServiceName ends.


	/**
	*	Sets the runtime properties.
	*	These are uniquely identified by form "FirstLoadService.propertyName".
	*	@param p The properties
	**/
    public void setProperties (Properties p) {
    } // method setProperties ends.


  private DataInputStream fromDIS = null;
  private DataOutputStream toDOS = null;
  private DataInputStream vmfDIS = null;
  private DataOutputStream vmfDOS = null;

  private String vmfIp = null;
  private int vmfPort = 0;


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
    System.out.println("FLS: vmf request added to q, notified (id=" +arbID +")");

    // Wait till a foundry is available
    while (fi == null) {
      try {
        synchronized(monitor) {
          monitor.wait();
        }
      } catch (InterruptedException ie) {
        System.out.println("FLS: InterruptedException");
      }
    }
  
    // foundry is now available
    // the local fi holds the foundry info
  }


  private void releaseVMFoundry() {

    // Release VMFoundry and notify
    if (fi != null) fi.busy = false;    // RELEASE VMFOUNDRY and 
    fi = null;                          // trash local fi
    synchronized(Router.q) {
      Router.q.notify();
    }
    System.out.println("FLS: VMF released, notified. Done (id=" +arbID +")");
  }


  private void handleRequest2 (ServiceRequest request, ServiceResponse response) {

    int CLOSECONNECTION = 0;
    int FOUNDRYOBTAINED = 1;

    fromDIS = new DataInputStream(request.getInputStream());
    toDOS = new DataOutputStream(response.getOutputStream());

    int qLen;
    synchronized(Router.q) {
      qLen = Router.q.size();
    }

    try {
      // send arbID to wdklite; this enables wdklite to kill the waiting synthesis request
      toDOS.writeInt(arbID);   
      // send the number of foundry requests waiting ahead of the presnet request
      toDOS.writeInt(qLen);   

      getMeVMFoundry();

      if (fi == Router.NULLFI) {
	toDOS.writeInt(CLOSECONNECTION);
        fromDIS.close () ;
        toDOS.close () ;
        System.out.println("id=" +arbID +" Removed from synthesis queue");
        return;
      }

      vmfIp= fi.ipAddress;
      vmfPort = fi.port;

      // Open socket to VMFoundry and faithfully bridge data received from
      // WDKlite client
      Socket socket = null;;
      System.out.println ("id=" +arbID +" Connecting to VMFoundry at:"+vmfIp+"   port:"+vmfPort+"\n");
      socket = new Socket(vmfIp, vmfPort);
      socket.setSoTimeout(5000000);
      vmfDIS = new DataInputStream (socket.getInputStream());
      vmfDOS = new DataOutputStream (socket.getOutputStream());

      //inform that a foundry has been obtained
      toDOS.writeInt(FOUNDRYOBTAINED);

      // Receive serviceID from wdklite and send to vmfoundry
      int serviceID = fromDIS.readInt () ;
      vmfDOS.writeInt(serviceID);
      System.out.println("FirstLoadService: serviceID= " +serviceID);
      // Receive response from vmfoundry and send to wdklite
      int resp = vmfDIS.readInt();
      toDOS.writeInt (resp) ;
      System.out.println("FirstLoadService: response= " +resp);

      // Lets do the checking just like VMFoundry does; to make sure the
      // bridge protocol works
      if (serviceID == OPCodes.SFIRSTIMAGE) firstLoad(fromDIS, toDOS, vmfDIS, vmfDOS);
      else System.out.println("FLS: Service not supported");

    } catch (Exception ioe) {
      System.out.println ("FLS: Exception Caught in Application");
      ioe.printStackTrace();
      // Close sockets and release foundry
      releaseVMFoundry();
    }

    // Close sockets
    try {
      fromDIS.close () ;
      toDOS.close () ;
      vmfDIS.close();
      vmfDOS.close();
    } catch (IOException ioe) {
    }

    // Release VMFoundry
    releaseVMFoundry();

    return;
  }


  private void firstLoad(DataInputStream fromDIS, DataOutputStream toDOS,
                         DataInputStream vmfDIS, DataOutputStream vmfDOS) throws Exception {
  
     try {
       // Receive project name from wdklite and send to vmfoundry
       // get the jar name.
       BufferedReader fromBR = null ;
       fromBR = new BufferedReader (new InputStreamReader (fromDIS)) ;
       String projectName = fromBR.readLine () ;   //get the code version
       System.out.println("FirstLoadService: projectName= " +projectName);

       // write the project name.
       int index = 0 ;
       for (index=0; index<projectName.length(); index++) {
         vmfDOS.write (projectName.charAt (index)) ;
       }
       vmfDOS.write ('\n') ;
       vmfDOS.flush () ;

       //Receive size from vmfoundry and send to wdklite
       int size = vmfDIS.readInt();
       toDOS.writeInt (size);
       System.out.println("FirstLoadService: size= " +size);

       //Receive jar data from vmfoundry and send to wdklite
       byte [] jarBytes = new byte[size];
       vmfDIS.readFully(jarBytes, 0, jarBytes.length);
       toDOS.write (jarBytes, 0, jarBytes.length);

       //Receive SDONE from vmfoundry and send to wdklite
       int resp = vmfDIS.readInt();
       toDOS.writeInt (resp);
       System.out.println("FirstLoadService: SDONE= " +resp);

       return;
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("FirstLoadService: Exception caught in firstLoad");
      throw new Exception();
    }
  }

  public int arbID = 0;

} // class FirstLoadService ends.

