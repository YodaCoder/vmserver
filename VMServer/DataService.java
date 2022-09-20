package WebSprocket.VMServer;

import java.io.*;

import java.util.Hashtable ;

import WebSprocket.Shared.*;
import WebSprocket.FoundryShared.*;

/**
 * This is a service that provides access to the Database.
 * It runs on a separate thread, allowing multiple sources to 
 * access the database simultaneously.
 *
 * <P> The general idea is that this data service will handle all
 * types of interactive services with the database.
 */

public class DataService implements Service {

	/**
	*	This method is called automatically by the Server Thread
	*	to perform the task at hand.
	*	<P>
	*	<pre>
	*	<code>
	*	{
	*		fromDIS = new DataInputStream(request.getInputStream());
	*		toDOS = new DataOutputStream(response.getOutputStream());
	*		doServices();
	*	}
	*	</code>
	*	</pre>
	*   @param request The object containing an InputStream and 
	*   parameters describing the request.
	*   @param response The object containing an OutputStream and
	*   parameters describing the request.
	**/
	public void handleRequest (ServiceRequest request, 
									ServiceResponse response) {

		fromDIS = new DataInputStream (request.getInputStream ()) ;
		toDOS = new DataOutputStream (response.getOutputStream ()) ;

		doServices () ;
	} // method handleRequest ends.


	/**
	*	Perform the data services requested.
	**/
	public void doServices () {
		System.out.println("database service started");
		System.out.println("write START "+OPCodes.SSTART);
		int databaseCommand = 0;

		try {
			toDOS.writeInt(OPCodes.SSTART);
			databaseCommand = fromDIS.readInt () ;  //get command
		} catch(IOException ioe) {	
			ioe.printStackTrace();
		}

		System.out.println ("database command: " + databaseCommand) ;

		switch (databaseCommand) {
			case OPCodes.GETCODEVERSION:
				getCodeVersion () ;
				break ;

			case OPCodes.ADDRUNTIMECODE:
				addRuntime () ;
				break ;

			case OPCodes.FETCH_CLASSES:
				fetchClasses () ;
				break ;

			case OPCodes.FETCH_CLASS:
				fetchClass () ;
				break ;

			case OPCodes.INSERT_CLASS:
				insertClass () ;
				break ;

			default:
				System.out.println ("DS: Unknown Data Base command.") ;
				break ;
		}

		/*
		if (databaseCommand == OPCodes.GETCODEVERSION) {	
			getCodeVersion();
		} else if (databaseCommand == OPCodes.ADDRUNTIMECODE) {	
			addRuntime();
		}
		*/

		System.out.println ("DS: DataService finished.") ;
	} // method doServices ends.


	/**
	*	Handles the request for retrieving code of specified version.
	**/
	public void getCodeVersion () {
		System.out.println ("DS: DataService.getCodeVersion ()") ;

		String version = null ;

		// read the project name.
		version = readString (fromDIS) ;
		System.out.println ("DS: version is " + version) ;

		String dataSource =  null;
		if (sProps != null)
			dataSource = sProps.getProperty ("dataSource") ;
		else
			System.out.println("null properties");

		System.out.println ("DS: dataSource " + dataSource) ;

		if (version != null) {
			if(dataSource!=null && dataSource.toLowerCase().equals("database"))
				getDBClassModels(version);
			else
				getFileClassModels(version);
		}
	} // method getCodeVersion ends.


	/**
	*	Handles the request for 
	*	uploading the java classes to the database.
	**/
	public void addRuntime () {
		System.out.println ("DS: DataService.addRuntime ()") ;

		String version = null ;

		// read project name.
		version = readString (fromDIS) ;
		System.out.println ("DS: version " + version) ;

		if (version == null) {
			System.out.println ("Bad version") ;
			return;
		}

		if (sProps != null) {
			String dataSource = sProps.getProperty ("dataSource") ;

			if ((dataSource != null) 
					&& 
				(dataSource.toLowerCase ().equals ("database"))) {	

				saveToDatabase (version) ;
				return ;
			}
		}

		saveToFileSystem (version) ;
	} // method addRuntime ends.


	/**
	*	Save the data to the database.  For the current implementation
	*	this is Oracle.
	*	@version The version of this data. (ie. "1.1")
	**/
	void saveToDatabase (String version) {
        OracleDataAccess odAccess = new OracleDataAccess (sProps) ;
		odAccess.removeVersion (version) ; //remove the old runtime version
		int transferState = 0;

		while(transferState != OPCodes.DONE) {	
			try {
				ObjectInputStream oiStream =new ObjectInputStream(fromDIS);
				Object obj = oiStream.readObject();
				ClassModel cModel = (ClassModel)obj;
				odAccess.storeClass(cModel);
				transferState = fromDIS.read () ;
			} catch(IOException ioe) {   
				ioe.printStackTrace();
				return;
			} catch(java.lang.ClassNotFoundException cnfe) {   
				cnfe.printStackTrace();
				return;
			}
		}
	} // method saveToDatabase ends.


	/**
	*	Save the data to the local file system.
	*	This is a convenience method for running without a database.
	*	@param version The version to save the data.
	**/
	void saveToFileSystem (String version) {
		//File dataFile = new File ("dataFile_" + version) ;

		String prjDir = null ;
		String ofDir = null; 
		String vmdHomePath = sProps.getProperty ("vmserver.homePath") ;
		String vmdHomeName = sProps.getProperty ("vmserver.homeName") ;

		// if vmserver.homePath is "." then vmdHomePath is uDir
		// otherwise it is the absolute path of the property itself.
		if (vmdHomePath.equals ("."))
			vmdHomePath = uDir ;

		prjDir = vmdHomePath + fSep + vmdHomeName + fSep + version  ;
		ofDir = prjDir + fSep + "OUTPUT" ;

		File dataFile = new File (ofDir + fSep + "dataFile") ;
		int transferState = 0;
		java.util.ArrayList arrayList = new java.util.ArrayList();

		System.out.println("STORING classNames");

		while(transferState != OPCodes.DONE)
		{	try
			{
				ObjectInputStream oiStream =new ObjectInputStream(fromDIS);
				Object obj = oiStream.readObject();
				ClassModel cModel = (ClassModel)obj;
				if (DEBUG) System.out.println("STORING: "+cModel.className);
				//odAccess.storeClass(cModel);
				arrayList.add(cModel);
				transferState = fromDIS.read () ;
			}
			catch(IOException ioe)
			{   ioe.printStackTrace();
				return;
			}
			catch(java.lang.ClassNotFoundException cnfe)
			{   cnfe.printStackTrace();
				return;
			}
		}

		System.out.println("Received "+arrayList.size() +" classes to store");
		ClassModel [] models = new ClassModel[arrayList.size()];
		for(int i=0; i<models.length; i++)
			models[i] = (ClassModel)arrayList.get(i);
		try
		{	
			FileOutputStream foStream = new FileOutputStream (dataFile) ;
			ObjectOutputStream objStream=new ObjectOutputStream(foStream);
			objStream.writeObject(models);
			objStream.flush();
			objStream.close();
		}
		catch(IOException ioe)
		{	ioe.printStackTrace();
		}
	} // method saveToFileSystem ends.


	/**
	*	Retrieve the ClassModel objects from the database
	*	and sends thems to the requestor.
	*	@param version The version of the ClassModels to get.
	**/
	void getDBClassModels (String version) {
		System.out.println ("DS: DataService.getDBClassModels ()") ;

		OracleDataAccess odAccess = new OracleDataAccess (sProps) ;
		String condition = ("version like '"+ version + "'") ;
		ClassModel [] models = odAccess.selectClassConditions (condition) ;

		//write the data
		try {	
			ObjectOutputStream objStream=new ObjectOutputStream(toDOS);
			objStream.writeObject(models);
			objStream.flush();
			objStream.close();
		} catch (IOException ioe) {	
			ioe.printStackTrace () ;
		}
	} // method getDBClassModels ends.


	/**
	*	Retrieve the ClassModel objects from the file system.
	*	and sends thems to the requestor.
	*	@param version The version of the ClassModels to get.
	**/
	void getFileClassModels (String version) {
		System.out.println("getFileClassModels "+version);
		//File dataFile = new File ("dataFile_" + version) ;

		String prjDir = null ;
		String ofDir = null; 
		String vmdHomePath = sProps.getProperty ("vmserver.homePath") ;
		String vmdHomeName = sProps.getProperty ("vmserver.homeName") ;

		// if vmserver.homePath is "." then vmdHomePath is uDir
		// otherwise it is the absolute path of the property itself.
		if (vmdHomePath.equals ("."))
			vmdHomePath = uDir ;

		prjDir = vmdHomePath + fSep + vmdHomeName + fSep + version  ;
		ofDir = prjDir + fSep + "OUTPUT" ;

		File dataFile = new File (ofDir + fSep + "dataFile") ;

		if (dataFile.exists ()) {
			System.out.println("datafile exists");
			try {	
				FileInputStream fiStream = new FileInputStream (dataFile) ;
				ObjectInputStream oiStream=new ObjectInputStream(fiStream);

				// GKJ: 
				//ClassModel [] models = (ClassModel[])oiStream.readObject();

				Object object = null ;
				object = oiStream.readObject () ;

				System.out.println ("DS: object " + object.getClass ().getName ()) ;

				ClassModel [] models = null ;
				models = (ClassModel[]) object ;

				System.out.println("read "+models.length+" class objects");

				if (DEBUG) {
				  for(int i=0; i< models.length; i++)
				  {	System.out.println("LOADED: "+ models[i].className);
				  }
				}
				ObjectOutputStream objStream=new ObjectOutputStream(toDOS);
				objStream.writeObject(models);
				objStream.flush();
				objStream.close();
			} catch (IOException ioe) {
				ioe.printStackTrace () ;
			}
			//catch(ClassNotFoundException cnfe)
			catch (Exception cnfe) {
				cnfe.printStackTrace () ;
			}
		} else {
			System.out.println("datafile DOES NOT EXIST");
		}
	} // method getFileClassModels ends.

	/**
	 * This method inserts a file (class file code / data file code) 
	 * into the data base.
	 */
	public void insertClass () {
		try {
			String version = null ;
			String fname = null ;
			String ftype = null ;

			// read project name.
			version = readString (fromDIS) ;
			System.out.println ("DS: insertClass version " + version) ;

			// read file name.
			fname = readString (fromDIS) ;
			System.out.println ("DS: insertClass fname " + fname) ;

			// read the file type.
			ftype = readString (fromDIS) ;
			System.out.println ("DS: insertClass ftype " + ftype) ;

			// read the file content.
			byte [] fcode = null ;

			ObjectInputStream fromOIS = new ObjectInputStream (fromDIS) ;
			fcode = (byte []) fromOIS.readObject () ;
			System.out.println ("DS: fcode length " + fcode.length) ;

			OracleDataAccess odAccess = new OracleDataAccess (sProps) ;
			// insert the class for the version specified.
			boolean result = false ;
			result = odAccess.insertClass (version, fname, ftype, fcode) ;

			// add code to check the result in the future.
			if (! result) {
				System.out.println ("DS: Error, inserting the class.") ;
			}

			// send the done signal.
			toDOS.writeInt (OPCodes.DONE) ;
		} catch (Exception e) {
			System.out.println ("DS: Exception Caught in insertClass.") ;
			e.printStackTrace () ;
		}
	} // method insertClass ends.


	/**
	 * This method fetches a file (class file code / data file code) 
	 * from the data base.
	 */
	public void fetchClass () {
		try {
			String version = null ;
			String fname = null ;
			String ftype = null ;

			// read the project name.
			version = readString (fromDIS) ;
			System.out.println ("DS: fetchClass version " + version) ;

			// read the file name.
			fname = readString (fromDIS) ;
			System.out.println ("DS: fetchClass fname " + fname) ;

			// read the file type.
			ftype = readString (fromDIS) ;
			System.out.println ("DS: fetchClass ftype " + ftype) ;

			OracleDataAccess odAccess = new OracleDataAccess (sProps) ;
			byte [] fCode = null ;
			// fetch the classes for the version specified.
			fCode = odAccess.fetchClass (version, fname, ftype) ;

			ObjectOutputStream toOOS = new ObjectOutputStream (toDOS) ;
			toOOS.writeObject (fCode) ;
			toOOS.flush () ;
			toOOS.close () ;
		} catch (Exception e) {
			System.out.println ("DS: Exception Caught in fetchClass.") ;
			e.printStackTrace () ;
		}
	} // method fetchClass ends.


	/**
	 * This method fetches the classes (class code) from the data base.
	 */
	public void fetchClasses () {
		try {
			String version = null ;

			// read the project name.
			version = readString (fromDIS) ;
			System.out.println ("DS: fetchClasses version " + version) ;

			OracleDataAccess odAccess = new OracleDataAccess (sProps) ;
			Hashtable pClassesH = null ;
			// fetch the classes for the version specified.
			pClassesH = odAccess.fetchClasses (version) ;

			ObjectOutputStream toOOS = new ObjectOutputStream (toDOS) ;
			toOOS.writeObject (pClassesH) ;
			toOOS.flush () ;
			toOOS.close () ;
		} catch (Exception e) {
			System.out.println ("DS: Exception Caught in fetchClasses.") ;
			e.printStackTrace () ;
		}
	} // method fetchClasses ends.


	/**
	*	This method returns:  "WebSprocket.VMServer.DataService".
	*	@return String The name of this service.
	**/
	public String getServiceName() {	
		return "WebSprocket.VMServer.DataService" ;
	} // method getServiceName ends.


	public void destroy () {
	} // method destroy ends.


	/**
    *   Sets the runtime properties.
    *   These are uniquely identified by form "DataService.propertyName".
	*	@param p The properties that may relate to this service.
    **/
	public void setProperties (java.util.Properties p) {	
		sProps = p ;
    } // method setProperties ends.


	/* Utility method that reads a string from an InputStream. */
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
			System.out.println ("DS: Exception while reading String.") ;
		}

		return (tString) ;
	} // method readString ends.


	// Data Members.
	DataInputStream fromDIS = null ;
	DataOutputStream toDOS = null ;
	java.util.Properties sProps = null ;

	// File Separator variable.
	private static final String fSep = System.getProperty ("file.separator") ;

	// User home directory variable
	private static final String uDir = System.getProperty ("user.dir") ;

	private static boolean DEBUG = false;

} // class DataService ends.
