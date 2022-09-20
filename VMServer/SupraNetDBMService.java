package WebSprocket.VMServer ;

import java.io.ByteArrayOutputStream ;
import java.io.DataInputStream ;
import java.io.File ;
import java.io.InputStream ;
import java.io.ObjectOutputStream ;

import java.util.Properties ;
import java.util.Hashtable ;
import java.util.Vector ;

public class SupraNetDBMService implements Service {

	public SupraNetDBMService () {
	} // constructor SupraNetDBMService ends.


	public void handleRequest (ServiceRequest request, 
		ServiceResponse response) {

		System.out.println ("SNDS: Begin handleRequest.") ;

		// check to see the data source type.
		String dbType = null ;	
		dbType = sProps.getProperty ("dataSource") ;

		if (dbType.equals ("fileSystem")) {
			fsdAccess = new FileSystemDataAccess () ;
		} else {
			odAccess = new OracleDataAccess (sProps) ;
		}

		System.out.println ("SNDS: dbType " + dbType) ;

		DataInputStream fromDIS = null ;
		ObjectOutputStream toOOS = null ;

		try {
			fromDIS = new DataInputStream (request.getInputStream ()) ;
			toOOS = new ObjectOutputStream (response.getOutputStream ()) ;

			Vector tempV = null ;
			Hashtable tempH = null ;
			String prjName = null ;
			String fileType = null ;
			String fileName = null ;
			byte [] fileData = null ;
			int action ;

			action = fromDIS.readInt () ;
			System.out.println ("SNDS: action " + action) ;

			switch (action) {
				case CREATE_FILE:
					// create a file in the project.
					String createFlag ;
					prjName = readString (fromDIS) ;
					fileType = readString (fromDIS) ;
					fileName = readString (fromDIS) ;

					// read the length of the file.
					int fLength ;
					fLength = fromDIS.readInt () ;

					ByteArrayOutputStream toBAOS = null ;
		
					int bytesRead ;
					byte [] packet = new byte [2*1024] ;
			
					toBAOS = new ByteArrayOutputStream () ;
			
					// read the file data.
					while ((bytesRead = fromDIS.read (packet)) < fLength) {
						toBAOS.write (packet, 0, bytesRead) ;
					}
			
					fileData =  toBAOS.toByteArray () ;
					toBAOS.close () ;
						
					createFlag = fsdAccess.createFile 
									(prjName, fileType, fileName, fileData) ;

					toOOS.writeObject (createFlag) ;
					System.out.println 
						("SNDS: sent create status " + createFlag) ;
					break ;

				case DELETE_FILE:
					// delete a file from the project.
					String deleteFlag ;
					prjName = readString (fromDIS) ;
					fileType = readString (fromDIS) ;
					fileName = readString (fromDIS) ;
					deleteFlag = fsdAccess.deleteFile 
									(prjName, fileType, fileName) ;

					toOOS.writeObject (deleteFlag) ;
					System.out.println 
						("SNDS: sent delete status " + deleteFlag) ;
					break ;

				case PROJECT_LIST:
					// send the project list.
					tempV = fsdAccess.getProjectList () ;

					toOOS.writeObject (tempV) ;
					System.out.println ("SNDS: sending the project list.") ;
					break ;

				case REGULARDATA_FILE_LIST:
					// send the regular data file list.
					prjName = readString (fromDIS) ;
					tempH = fsdAccess.getFileList 
								(prjName, "REGULARDATA_FILE") ;

					toOOS.writeObject (tempH) ;
					System.out.println 
						("SNDS: sending the regular data file list.") ;
					break ;

				case REGULARCLASS_FILE_LIST:
					// send the regular class file list.
					prjName = readString (fromDIS) ;
					tempH = fsdAccess.getFileList 
								(prjName, "REGULARCLASS_FILE") ;

					toOOS.writeObject (tempH) ;
					System.out.println 
						("SNDS: sending the regular class file list.") ;
					break ;

				case SUPRANETCLASS_FILE_LIST:
					// send the supranet data file list.
					prjName = readString (fromDIS) ;
					tempH = fsdAccess.getFileList 
								(prjName, "SUPRANETCLASS_FILE") ;

					toOOS.writeObject (tempH) ;
					System.out.println 
						("SNDS: sending the supranet data file list.") ;
					break ;
			}
		} catch (Exception e) {
			System.out.println ("SNDS: Exception caught in handleRequest.") ;
			e.printStackTrace () ;
		}

		System.out.println ("SNDS: End handleRequest.") ;
	}  // method handleRequest ends.

	
	public void destroy () {
		// clean up code.
	} // method destroy ends.


	public String getServiceName () {
		String serviceName = "WebSprocket.VMServer.SupraNetDBMService" ;

		return (serviceName) ;
	} // method getServiceName ends.


	public void setProperties (Properties props) {
		// assign it to class variable.
		sProps = props ;
	} // method setProperties ends.


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
	// server properties.
	private Properties sProps = null ;

	// Data base variable.
	FileSystemDataAccess fsdAccess = null ;
	OracleDataAccess odAccess = null ;

	private static final int PROJECT_LIST = 2001 ;
	private static final int REGULARDATA_FILE_LIST = 2002 ;
	private static final int REGULARCLASS_FILE_LIST = 2003 ;
	private static final int SUPRANETCLASS_FILE_LIST = 2004 ;
	private static final int DELETE_FILE = 2005 ;
	private static final int CREATE_FILE = 2006 ;
} // class SupraNetDBMService ends.
