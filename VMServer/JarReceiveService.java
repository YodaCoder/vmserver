package WebSprocket.VMServer;

import WebSprocket.Shared.*;
import java.io.*;
import java.util.*;
import java.util.jar.*;

/**
*	This is a simple service for the VMServer 
*	which accepts a jar of class files from a user and saves 
*	the data on the server side.  After this service completes,
*	the user can synthesize the code.
*	@see WebSprocket.VMServer.VMServer
**/
public class JarReceiveService implements Service {


	public JarReceiveService()
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
	*		System.out.println("data size= "+size);
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
    public synchronized void handleRequest (ServiceRequest request, 
												ServiceResponse response) {

		System.out.println("receiving file");
		DataInputStream diStream=new DataInputStream(request.getInputStream());
		DataOutputStream doStream=new DataOutputStream(response.getOutputStream());
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
			System.out.println ("JRS: Jar File name " + jarName) ;

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
		}

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
		return "JarReceiveService" ;
	} // method getServiceName ends.


	/**
	*	Sets the runtime properties.
	*	These are uniquely identified by form "JarReceiveService.propertyName".
	*	@param p The properties
	**/
    public void setProperties (Properties p) {
    } // method setProperties ends.
} // class JarReceiveService ends.
