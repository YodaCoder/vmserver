package WebSprocket.VMServer ;

import java.io.BufferedReader ;
import java.io.ByteArrayInputStream ;
import java.io.ByteArrayOutputStream ;
import java.io.File ;
import java.io.InputStream ;
import java.io.InputStreamReader ;
import java.io.IOException ;
import java.io.OutputStream ;

import java.sql.Blob ;
import java.sql.Connection ;
import java.sql.Date ;
import java.sql.DriverManager ;
import java.sql.PreparedStatement ;
import java.sql.ResultSet ;
import java.sql.Statement ;
import java.sql.SQLException ;

import java.util.Enumeration ;
import java.util.Hashtable ;
import java.util.Properties ;
import java.util.StringTokenizer ;
import java.util.Vector ;

import java.util.jar.JarEntry ;
import java.util.jar.JarInputStream ;

import oracle.jdbc.driver.OracleCallableStatement ;
import oracle.jdbc.driver.OraclePreparedStatement ;
import oracle.jdbc.driver.OracleResultSet ;
import oracle.sql.BLOB ;

import WebSprocket.Shared.* ;
import WebSprocket.FoundryShared.* ;

/**
*	Example implementation for an Oracle database.
*	Oracle JDBC driver version: 8.1.6.0.1 (thin driver)
*	<P>
*	NOTES:
*	No persistant database connection implemented.
*	Usually connection will be on a per-session basis.
*	The ClassModel.classData field contains binary information
*	specific to a target class.
*	<BR>
*	Binary data is stored via the Oracle.BLOB class.  This is 
*	not to be confused the the java.sql.Blob class.
*	The jdbc standard method for inserting binary data into 
*	a database is to use PreparedStatement.setBinaryStream().
*	But if that was used to write the binary data, 
*	it has a limit of 4000 bytes with the Oracle JDBC driver.
**/
public class OracleDataAccess implements WebSprocket.VMServer.DataAccess {

	//String dbURLstring = "jdbc:oracle:thin:@10.0.0.233:1521:SPARKY";
	private Statement stmt=null;

	private java.text.SimpleDateFormat sdFormat = 
		new java.text.SimpleDateFormat("yyyy-MM-dd"); 

	/**
	*	Basic noop constructor.
	**/
	public OracleDataAccess (Properties sProps) {
		// initialize the db server information.
		String dbIP = sProps.getProperty ("dbserver.address") ;
		int dbPort = Integer.parseInt (sProps.getProperty ("dbserver.port")) ;
		String dbName = sProps.getProperty("dbserver.dbname");

		dbURL = "jdbc:oracle:thin:@" +
				dbIP + ":" +
				dbPort + ":" +
				dbName;

		System.out.println ("ODA: DB URL " + dbURL) ;
	
		dbUsername = sProps.getProperty ("dbserver.username") ;
		dbPassword = sProps.getProperty ("dbserver.password") ;

		if (dbUsername == null || dbPassword == null) {
			System.err.println("\nError with the database properties:");
			if (dbUsername == null)
				System.err.println("NOT FOUND: dbserver.username");
			if (dbPassword == null)
				System.err.println("NOT FOUND: dbserver.password");
		}

		// initialize the data base tables.
		initTables () ;
	} // constructor OracleDataAccess ends.


	/**
	*	This method is intended to be called when the server 
	*	starts up.  It verifies that the proper tables exist in 
	*	the database and are accessible.
	**/
	private void initTables () {
		if (! openDB ()) {
			System.out.println ("ODA: Error, openDB in initTables.") ;
			return ;
		}

		Hashtable tableListH = new Hashtable () ;
		tableListH.put (cmtTable, cmtCreateStmt) ;
		tableListH.put (vctTable, vctCreateStmt) ;

		String tableName = null ;
		String tcStmt = null ;

		Enumeration enum = null ;
		enum = tableListH.keys () ;

		while (enum.hasMoreElements ()) {
			// get the table name.
			tableName = (String) enum.nextElement () ;
			// get the create statement.
			tcStmt = (String) tableListH.get (tableName) ;

	        //test if table exists. If it doesn't, then create it.
			boolean tableExists = true ;
			Statement stmt = null ;
		
			// check if each table exists
			try {
				stmt = dbCon.createStatement () ;
				String command = "select * from " + tableName ;

				System.out.println (command) ;

				ResultSet rs = stmt.executeQuery(command);

				System.out.println("result set = "+ rs);
				System.out.println("result type = "+ rs.getType());
				rs.close () ;
				stmt.close () ;
			} catch (SQLException sqle) {
				System.out.println ("ODA: table not found " + tableName) ;
				System.out.println ("ODA: create stmt is " + tcStmt) ;
				tableExists = false;
				//sqle.printStackTrace();
			}
	
			if (! tableExists) {
				try {
					stmt = dbCon.createStatement () ;
					System.out.println (tcStmt) ;
					stmt.executeUpdate (tcStmt) ;
				} catch (SQLException sqle) {
					System.out.println ("ODA: Unable to create " + tableName) ;
					sqle.printStackTrace () ;
				}
			}
		}

		// close the data base.
		closeDB () ;
	} // method initTables ends.


    /**
	 * Store a class model object's information into the database.
	 * @param classModel The data to be stored.
	 */
	public void storeClass (ClassModel classModel) {
		String iString = "insert into CLASSMODEL values "+
		"("+
			"'"+classModel.className+"', "+
			classModel.id +", "+
			"'"+classModel.version +"', "+
			"?, "+   //"'"+ sdFormat.format(classModel.dateCreated)+"' "+
			"'"+classModel.targetArch +"', "+
			"'"+classModel.classpath +"', "+
			"empty_blob()"+
		")";

		if (! openDB ()) {
			System.out.println ("ODA: Error, openDB in storeClass.") ;
			return ;
		}

		try {
			//faster if we don't use autoCommit
			dbCon.setAutoCommit (false) ; 

			if (DEBUG)
			System.out.println ("ODA: insertString " + iString) ;

			PreparedStatement iStmt = dbCon.prepareStatement (iString) ;

			iStmt.setDate (1, new Date (classModel.dateCreated.getTime ())) ;
			iStmt.executeUpdate () ;
			iStmt.close () ;

			//get ready to open the LOB for READWRITE
			OracleCallableStatement cstmt = 
				(OracleCallableStatement) dbCon.prepareCall(
				"BEGIN DBMS_LOB.OPEN(?, DBMS_LOB.LOB_READWRITE); END;");

			String uString = 
				"SELECT data FROM " + cmtTable + 
				" WHERE classname like '" + classModel.className + "'" + 
				" AND version like '" + classModel.version + "'" + 
				" AND id = " + classModel.id + 
				" FOR UPDATE" ;

			if (DEBUG)
			System.out.println ("ODA: updateString " + uString) ;

			Statement uStmt = dbCon.createStatement () ;
			OracleResultSet rs = (OracleResultSet) 
									uStmt.executeQuery (uString) ;

			BLOB atBLOB = null ;

			if (rs.next ()) {
				atBLOB = rs.getBLOB (1) ;
				cstmt.setBLOB (1, atBLOB) ;
				cstmt.execute () ;

				OutputStream aoStream = null ;
				ByteArrayInputStream afromBAIS = null ;
				aoStream = atBLOB.getBinaryOutputStream () ;
				afromBAIS = new ByteArrayInputStream (classModel.data) ;

				int bytesRead = 0 ;
				byte [] packet = new byte [atBLOB.getChunkSize ()] ;
				if (DEBUG)
				System.out.println ("ODA: chunk size " + packet.length) ;

				while ((bytesRead = afromBAIS.read (packet)) != -1) {
					aoStream.write (packet, 0, bytesRead) ;
				}

				afromBAIS.close();
				aoStream.flush () ;
				aoStream.close () ;
			} else {
				System.out.println ("ODA: Error, Unable to update.") ;
			}

			uStmt.close();

			//ALL open LOB's must be closed
			cstmt = (OracleCallableStatement) dbCon.prepareCall(
				"BEGIN DBMS_LOB.CLOSE(?); END;");
			cstmt.setBLOB(1, atBLOB);
			cstmt.execute();
			cstmt.close();

			dbCon.commit();

		} catch (SQLException sqle) {
			System.out.println ("ODA: Error, SQLException in storeClass.") ;
			System.out.println("PROBLEM DOING DB INSERT");
               sqle.printStackTrace();
			System.out.println("className = "+classModel.className);
			System.out.println("ID = "+classModel.id);
			System.out.println("VERSION = "+classModel.version);
			System.out.println(classModel.dateCreated);
			System.out.println(classModel.targetArch);
			System.out.println(classModel.classpath);
			System.out.println("binary length="+classModel.data.length);
		} catch (IOException ioe) {
			System.out.println ("ODA: Error, IOException in storeClass.") ;
			ioe.printStackTrace () ;
		} catch (Exception e) {
			System.out.println ("ODA: Error, Exception in storeClass.") ;
			e.printStackTrace () ;
		}

		// close the connection to data base.
		closeDB () ;
	} // method storeClass ends.


	/**
	*	Returns the size of the Class
	*	Useful information during the updating of clients.
	*	@param classID The id for a class.
	*	@return The class size in bytes.
	**/
	public int getClassSize(int classID)
	{	return 1;
	}	

	/**
	*	Returns a list of clients using a specified class.
	*	Need to know when doing versioning.
	*	@param classID The identifier for the class.
	*	@return The list of client IDs.
	**/
	public ClientModel []  getClientsWithClass(int classID)
	{	return new ClientModel[2];
	}

	/**
	*	Add a new class to the database.
	*	 @param model The new ClassModel
	public void addNewClass(ClassModel model)
	{
	}
	**/

	/**
	*	Get a class from the database.
	*	@param classID The identifier for the class.
	*	@return ClassModel object which encapsulates class information.
	**/
	public ClassModel getClassModel (int classID) {

		if (! openDB ())
			return null;

		String query = 
			"select * from " + cmtTable + 
			" where id = "+classID ;
		try {
			ClassModel classModel = new ClassModel();
			Statement stmt = dbCon.createStatement();
			System.out.println(query);
			ResultSet rs = stmt.executeQuery(query);
			if(!rs.next())
				return null;	//no results
			classModel.className = rs.getString(1);
			classModel.id = rs.getInt(2);
			//classModel.version = rs.getInt(3);
			classModel.version = rs.getString(3);
			classModel.dateCreated = (java.util.Date)rs.getDate(4);
			classModel.targetArch = rs.getString(5);
			classModel.classpath = rs.getString(6);
			Blob dataBlob = rs.getBlob(7);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte [] buf = new byte[1024];
			InputStream is = dataBlob.getBinaryStream();
			int count =0;
			while((count = is.read(buf,0, buf.length)) != -1)
				baos.write(buf,0, count);
			classModel.data = baos.toByteArray();
			return classModel;
		} catch (SQLException sqle) {	
			sqle.printStackTrace();
		}
		catch(IOException ioe)
		{	ioe.printStackTrace();
		}
		return null;
	}

	/**
	*	Get a class from the database.
	*	@param className The name of the class.
	*	@return A ClassModel object which encapsulates class information.
	**/
	public ClassModel getClassModel(String className) {
		if (! openDB ())
			return null ;
		String query = 
			"select * from " + cmtTable + 
			" where classname = " + className ;
		try
		{
			ClassModel classModel = new ClassModel();
			Statement stmt = dbCon.createStatement();
			System.out.println(query);
			ResultSet rs = stmt.executeQuery(query);
			if(!rs.next())
				return null;	//no results
			classModel.className = rs.getString(1);
			classModel.id = rs.getInt(2);
			//classModel.version = rs.getInt(3);
			classModel.version = rs.getString(3);
			classModel.dateCreated = (java.util.Date)rs.getDate(4);
			classModel.targetArch = rs.getString(5);
			classModel.classpath = rs.getString(6);
			Blob dataBlob = rs.getBlob(7);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte [] buf = new byte[1024];
			InputStream is = dataBlob.getBinaryStream();
			int count =0;
			while((count = is.read(buf,0, buf.length)) != -1)
				baos.write(buf,0, count);
			classModel.data = baos.toByteArray();
			return classModel;
		}
		catch(SQLException sqle)
		{	sqle.printStackTrace();
		}
		catch(IOException ioe)
		{	ioe.printStackTrace();
		}
		return null;
	}

	/**
	*	Delete a class from the database.
	*	@param classID The identifier for the class.
	**/
	public void deleteClassFromDB(int classID)
	{
	}

	/**
	*	Retrieves classmodel objects from the database 
	*	according to user provided conditions.
	*	For example, the user may provide "id > 1000" or
	*	"classname like '%'".
	*	@param conditions the SQL conditions for selecting.
	*	@return array of the result ClassModel objects.
	**/
	public ClassModel [] selectClassConditions(String conditions)
	{
		System.out.println("selectClassConditions()");
		if (! openDB ())
			return null;

		String command = 
			"select * from " + cmtTable + 
			" where "+ conditions ;
		try
		{
			stmt = dbCon.createStatement();
			System.out.println(command);
			ResultSet rs = stmt.executeQuery(command);
			return convertToClassModel(rs);	//this is proper return from method
		}
		catch(SQLException sqle)
		{	sqle.printStackTrace();
		}
		return null;	//this return is not desired
	}

	/**
	*	Utility method for converting a ResultSet to an
	*	array of ClassModel.  If a problem occurs, just return null.
	*	@param rs The ResultSet object to convert.
	*	@return An array of ClassModel objects containing the 
	*	converted ResultSet data.
	**/
	private ClassModel [] convertToClassModel (ResultSet rs) 
		throws SQLException {

		System.out.println("converting classes");
		java.util.Vector modelVector = new java.util.Vector();

		while (rs.next()) {
			ClassModel classModel = new ClassModel();
			classModel.className = rs.getString(1);
			classModel.id = rs.getInt(2);
			System.out.print("ClassID = "+ classModel.id);
			System.out.println(" "+ classModel.className);
			//classModel.version = rs.getInt(3);
			classModel.version = rs.getString(3);
			classModel.dateCreated = (java.util.Date)rs.getDate(4);
			classModel.targetArch = rs.getString(5);
			classModel.classpath = rs.getString(6);
			Blob dataBlob = rs.getBlob(7);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte [] buf = new byte[1024];
			InputStream is = dataBlob.getBinaryStream();
			int count =0;
			try
			{
				while((count = is.read(buf,0, buf.length)) != -1)
					baos.write(buf,0, count);
			}
            catch(IOException ioe)
            {	
				System.out.println("Error with retrieving BLOB");
				System.out.println("classname = "+classModel.className);
				System.out.println("id = "+classModel.id);
				ioe.printStackTrace();
			}
			classModel.data = baos.toByteArray();
			modelVector.addElement(classModel);
		}
		System.out.println("Got data of length "+ modelVector.size());
		ClassModel [] resultModels = new ClassModel[modelVector.size()];
		try	//if we can't cast it properly, then forget about it.
		{	
			for(int i=0; i< resultModels.length; i++)
				resultModels[i] = (ClassModel)modelVector.elementAt(i);
			return resultModels;
		}
		catch(ClassCastException cce)
		{	cce.printStackTrace();
		}
		return null;
	} // method convertToClassModel ends.

	/**
	*	Removes a version from the database.
	*	This is used when a user attempts to update
	*	a previously existing and identical version in the database.
	*	The old version "1.1" is removed before the new version "1.1"
	*	is added.
	**/
	void removeVersion (String version) {
		System.out.println ("ODA: removeVersion (" + version + ")") ;

		// open a connection to data base.
		if (! openDB ()) {
			// error opening a connection to data base.
			System.out.println ("ODA: Error, openDB in removeVersion v.") ;
			return ;
		}

		String command = 
			"delete from " + cmtTable + 
			" where version like '" + version + "'" ;

		try {
			stmt = dbCon.createStatement () ;
			System.out.println (command) ;
			stmt.executeUpdate (command) ;
            stmt.close () ;
		} catch (SQLException sqle) {	
			sqle.printStackTrace () ;
		}

		// close the connection to data base.
		closeDB () ;
	} // method removeVersion ends.


	// removes the classes from the vms_classestable table.
	private void removeVersion (String version, String tname) {
		System.out.println 
			("ODA: removeVersion (" + version + ", " + tname + ")") ;

		if (! openDB ()) {
			// error opening a connection to data base.
			System.out.println ("ODA: Error, openDB in removeVersion v, t.") ;
			return ;
		}

		String command = 
			"delete from " + tname + 
			" where projectName like '" + version + "'" ;

		try {
			stmt = dbCon.createStatement () ;
			System.out.println (command) ;
			stmt.executeUpdate (command) ;
            stmt.close () ;
		} catch (SQLException sqle) {	
			sqle.printStackTrace () ;
		}

		// close the connection to data base.
		closeDB () ;
	} // method removeVersion ends.


	// deletes the class (class / file) from the vms_classestable table.
	private boolean deleteClass (String version, String fname, String ftype) {
		Statement dStmt = null ;

		System.out.println ("ODA: deleteClass (" + 
			version + ", " + fname + ", " + ftype + ")") ;

		if (! openDB ())
			return (false) ; // failure

		String dString = 
			"delete from " + vctTable + 
			" where projectName like '" + version + "'" +
			" AND className like '" + fname + "'" +
			" AND classType like '" + ftype + "'" ;

		try {
			dStmt = dbCon.createStatement () ;
			System.out.println ("ODA: deleteClass " + dString) ;
			dStmt.executeUpdate (dString) ;
            dStmt.close () ;
		} catch (SQLException sqle) {	
			System.out.println ("ODA: Error, Unable to delete class.") ;
			sqle.printStackTrace () ;
			return (false) ;
		}

		// close the connection to data base.
		closeDB () ;

		return (true) ;
	} // method deleteClass ends.


	/* This method insert's a class (class / file) into the data base. */
	public boolean insertClass (String version, String fname, 
									String ftype, byte [] fcode) {
		// delete the class / file, if it already exists.
		if (! deleteClass (version, fname, ftype)) {
			System.out.println ("ODA: Error, Unable to insert class.") ;
			return (false) ;
		}

		// insert the class now.
		if (! insertRecord (fname, ftype, fcode, version)) {
			System.out.println ("ODA: Error, Unable to insert class.") ;
			return (false) ;
		}

		return (true) ;
	} // method insertClass ends.


	public byte [] fetchClass (String version, String fname, String ftype) {
		byte [] fCode = null ;

		// select all the which are either 
		// regular class files or regular data files.
		String fStmt =
			"select classCode from " + vctTable +
			" where ((projectName = ?) AND " +
			"(className = ?) AND " +
			"(classType = ?))" ;

		OraclePreparedStatement fetchStmt = null;

		try {
			// open the connection to data base.
			openDB () ;

            fetchStmt = (OraclePreparedStatement) 
			dbCon.prepareStatement (fStmt) ;

			fetchStmt.setString (1, version) ;
			fetchStmt.setString (2, fname) ;
			fetchStmt.setString (3, ftype) ;

			ResultSet rSet = null ;
			rSet = fetchStmt.executeQuery () ;

			System.out.println ("ODA: rSet " + rSet) ;

			BLOB cCodeBLOB = null ;
			InputStream iStream = null ;
			ByteArrayOutputStream toBAOS = null ;
			int bytesRead = 0 ;
			byte [] packet = new byte [2*1024] ;

			// read record and store in fCode.
			if (rSet.next ()) {
				// read the class code blob.
				cCodeBLOB = ((OracleResultSet) rSet).getBLOB (1) ;

				iStream = cCodeBLOB.getBinaryStream () ;

				toBAOS = new ByteArrayOutputStream () ;
				while((bytesRead = iStream.read (packet)) != -1) {
					toBAOS.write (packet, 0, bytesRead) ;
				}

				fCode = toBAOS.toByteArray () ;
				toBAOS.close () ;

				System.out.println ("ODA: fetchClass fCode " + 
					" " + fCode.length) ;
			} else {
				System.out.println ("ODA: fetchClass No record selected.") ;
			}

			// close the connection to data base.
			closeDB () ;
        } catch (Exception e) {
            System.out.println ("ODA: Exception Caught in fetchClass.") ;
			e.printStackTrace () ;
        }

		return (fCode) ;
	} // method fetchClass ends.


	public Hashtable fetchClasses (String version) {
		Hashtable tHashtable = null ;
		tHashtable = new Hashtable () ;

		// select all the which are either 
		// regular class files or regular data files.
		String fStmt =
			"select className, classCode from " + vctTable +
			" where ((projectName = ?) AND " +
			"((classType = 'REGULARCLASS_FILE') OR " +
			"(classType = 'REGULARDATA_FILE')))" ;

		OraclePreparedStatement fetchStmt = null;

		try {
			// open the connection to data base.
			openDB () ;

            fetchStmt = (OraclePreparedStatement) 
			dbCon.prepareStatement (fStmt) ;

			fetchStmt.setString (1, version) ;

			ResultSet rSet = null ;
			rSet = fetchStmt.executeQuery () ;

			String cName = null ;
			BLOB cCodeBLOB = null ;
			byte [] cCode = null ;
			InputStream iStream = null ;
			ByteArrayOutputStream toBAOS = null ;
			int bytesRead = 0 ;
			byte [] packet = new byte [2*1024] ;

			// while there are more records read them and store in hash table.
			while (rSet.next ()) {
				// read the class name.
				cName = rSet.getString (1) ;	
				// read the class code blob.
				cCodeBLOB = ((OracleResultSet) rSet).getBLOB (2) ;

				iStream = cCodeBLOB.getBinaryStream () ;

				toBAOS = new ByteArrayOutputStream () ;
				while((bytesRead = iStream.read (packet)) != -1) {
					toBAOS.write (packet, 0, bytesRead) ;
				}

				cCode = toBAOS.toByteArray () ;
				toBAOS.close () ;

				// put the class name, class code into the hash table.
				tHashtable.put (cName, cCode) ;

				/*
				System.out.println (
					"ODA: fetchClasses cName, cCode " +
					cName + " " + cCode.length) ;
				*/
			}

			// close the connection to data base.
			closeDB () ;
        } catch (Exception e) {
            System.out.println ("ODA: Exception Caught in fetchClasses.") ;
			e.printStackTrace () ;
        }

		return (tHashtable) ;
	} // method fetchClasses ends.


	public void processJar (String jarname, InputStream iStream) {
		try {
			jarName = jarname ;
			String projectName = jarName.substring (0, jarName.indexOf ('.')) ;

			// remove the existing classes from the data base.
			removeVersion (projectName, vctTable) ;

			// initialize the vector.
			sClasses = new Vector () ;

			ByteArrayOutputStream toBAOS = null ;
			toBAOS = new ByteArrayOutputStream () ;

			int bytesRead = -1 ;
			byte [] packet = new byte [2 * 1024] ;

			while ((bytesRead = iStream.read (packet)) != -1) {
				toBAOS.write (packet, 0, bytesRead) ;
			}

			jarCode = toBAOS.toByteArray () ;
			toBAOS.close () ;

			parseSupraNetClassNames () ;

			JarInputStream fromJIS = null ;
			// create a JarInputStream on the jar file.
			fromJIS = new JarInputStream (new ByteArrayInputStream (jarCode)) ;	

			JarEntry jEntry = null ;
			String jeName = null ;
			boolean isDir = false ;
			int jeSize ;
			byte [] jeData = null ;
			File jeFile = null ;

			while ((jEntry = fromJIS.getNextJarEntry ()) != null) {
				isDir = false ;

				jeName = jEntry.getName () ;

				if (appDebug)
					System.out.println ("IC: jeName " + jeName) ;

				// if the jar entry is a file, store it in data base.
				if (! jEntry.isDirectory ()) {
					// file type, could be REGULARCLASS_FILE, 
					// SUPRANETCLASS_FILE, REGULARDAT_FILE
					String fType = null ;
					if (jEntry.getMethod () == JarEntry.DEFLATED) {
						jeSize = (int) jEntry.getCompressedSize () ;
					} else {
						jeSize = (int) jEntry.getSize () ;
					}

					toBAOS = new ByteArrayOutputStream () ;

					if (! (jeSize == -1)) {
						jeData = new byte [jeSize] ;
						fromJIS.read (jeData, 0, jeSize) ;

						toBAOS.write (jeData) ;
					} else {
						while ((bytesRead = fromJIS.read 
							(packet, 0, packet.length)) != -1) {

							toBAOS.write (packet, 0, bytesRead) ;
						}
					}

					byte [] classCode = null ;
					classCode = toBAOS.toByteArray () ;

					if (jeName.endsWith (".class")) {
						if (sClasses.contains (jeName)) {
							System.out.println ("IC: SUPRANETCLASS added.") ;
							fType = "SUPRANETCLASS_FILE" ;
						} else
							fType = "REGULARCLASS_FILE" ;
					} else
						fType = "REGULARDATA_FILE" ;

					// strip off "classes/" if jeName begins with it.
					if (jeName.startsWith ("classes/"))
						jeName = jeName.substring (jeName.indexOf ('/') + 1) ;
		
					// insert the record into the data base.
					insertRecord (jeName, fType, classCode, projectName) ;

					toBAOS.close () ;
				} else {
					// if the jar entry is a directory, ignore it.
				}

				System.out.println (jeName) ;

				fromJIS.closeEntry () ;
			}

			fromJIS.close () ;
		} catch (Exception e) {
			System.out.println ("IC: Exception Caught in Application.") ;
			e.printStackTrace () ;
		}
	} // method processJar ends.


	public void parseSupraNetClassNames () {
		byte [] scInfo = null ;
		JarInputStream fromJIS = null ;
		try {
			// create a JarInputStream on the jar file.
			fromJIS = new JarInputStream (new ByteArrayInputStream (jarCode)) ;	

			JarEntry jEntry = null ;
			String jeName = null ;
			ByteArrayOutputStream toBAOS = null ;
			while ((jEntry = fromJIS.getNextJarEntry ()) != null) {
				jeName = jEntry.getName () ;

				if (jeName.endsWith (".supranet")) {
					toBAOS = new ByteArrayOutputStream () ;
					byte [] packet = new byte [2*1024] ;
					int bytesRead ;

					while ((bytesRead = fromJIS.read 
						(packet, 0, packet.length)) != -1) {

						toBAOS.write (packet, 0, bytesRead) ;
					}

					scInfo = toBAOS.toByteArray () ;
					toBAOS.close () ;

					BufferedReader fromBA = null ;
					fromBA = new BufferedReader (new InputStreamReader 
						(new ByteArrayInputStream (scInfo))) ;

					String nLine = null ;
					while ((nLine = fromBA.readLine ()) != null) {
						parseRecord (nLine) ;
					}	

					break ;
				} // end if
			} // end while

			fromJIS.close () ;

		} catch (Exception e) {
			System.out.println ("IC: Exception Caught in Application.") ;
			e.printStackTrace () ;
		}
	} // method parseSupraNetClassNames ends.


    public void parseRecord (String line) {
		String projectName = null ;
		String className = null ;
		projectName = jarName.substring (0, jarName.indexOf ('.')) ;
		if (appDebug)
			System.out.println ("IC: projectName " + projectName) ;

		StringTokenizer stzer = null ;

		stzer = new StringTokenizer (line, ";") ;

		String token = null ;
		int key ;
		String value ;
	
		while (stzer.hasMoreTokens ()) {
		    token = stzer.nextToken () ;
	
		    key = Integer.parseInt 
				(token.substring (0, (token.indexOf ('=')))) ;
		    value = token.substring (token.indexOf ('=') + 1) ;
	
		    switch (key) {
			case 100:
				className = value ;
			    break ;
	
			case 105:
				String tStr = null ;
			    if (value.equals ("true")) {
					tStr = className.substring 
							(className.indexOf (projectName) + 
								projectName.length () + 1) ;

					tStr = tStr.replace ('\\', '/') ;

					if (appDebug)
						System.out.println ("IC: className "+ tStr) ;
	
					// put the supranet class name in the vector.
					sClasses.addElement (tStr) ;
				}
			    break ;
		    }	
		}
    } // method parseRecord ends.


    public boolean openDB () {
		// register the JDBC driver.
        try {
			DriverManager.registerDriver 
					(new oracle.jdbc.driver.OracleDriver ()) ;
            //Class.forName ("sun.jdbc.odbc.JdbcOdbcDriver") ;
        } catch (Exception e) {
            System.out.println ("ODA: Failed to load JDBC driver.") ;
			e.printStackTrace () ;
            return (false) ;
        }

		// get a Connection object and create a Statement object.
        try {
            dbCon = DriverManager.getConnection (
                dbURL,
                dbUsername,
                dbPassword) ;
        } catch (Exception e) {
            System.out.println ("ODA: problems connecting to " + dbURL) ;
			e.printStackTrace () ;

			return (false) ;
        }

		// connection to data base was successful.
		return (true) ;
    } // method openDB ends.


	public boolean insertRecord (String cname,
		String ctype,
		byte [] ccode,
		String pname) {

		// statement for inserting class data into data base.
		String iStmt = "" +
			"insert into " + vctTable + " values " +
			"(?, ?, ?, empty_blob ())" ;

		OraclePreparedStatement insertStmt = null;

		try {
			// open the connection to data base.
			if (! openDB ()) {
				System.out.println ("ODA: Error, openDB in insertRecord.") ;
				return (false) ; // failure.
			}

            insertStmt = (OraclePreparedStatement) 
				dbCon.prepareStatement (iStmt) ;

			// set the auto commit to false.
			dbCon.setAutoCommit (false) ;

			insertStmt.setString (1, pname) ;
			insertStmt.setString (2, cname) ;
			insertStmt.setString (3, ctype) ;

			insertStmt.executeUpdate () ;

			updateBLOB (pname, cname, ctype, ccode) ;

			// close the insertStmt.
			insertStmt.close () ;

			// commit the write.
			dbCon.commit () ;

			// close the connection to data base.
			closeDB () ;
        } catch (Exception e) {
            System.out.println ("ODA: problems with insertStmt sent to " + 
				dbURL +
                ": "+ 
				e.getMessage ()) ;
			e.printStackTrace () ;

			// failure.
			return (false) ;
        }
		
		return (true) ;
	} // method insertRecord ends.


	public void updateBLOB (String pnameUB, 
		String cnameUB, 
		String ctypeUB,
		byte [] ccodeUB) {

		String uStmt = "" +
			"select classCode from " + vctTable +
			" where ((projectName = ?) AND " +
			"(className = ?) AND (classType = ?))" ;

		OraclePreparedStatement updateStmt = null;

		try {
            updateStmt = (OraclePreparedStatement) 
			dbCon.prepareStatement (uStmt) ;

			updateStmt.setString (1, pnameUB) ;
			updateStmt.setString (2, cnameUB) ;
			updateStmt.setString (3, ctypeUB) ;

			ResultSet rSet = null ;
			rSet = updateStmt.executeQuery () ;

			//BLOB cCodeB = new BLOB ((OracleConnection) dbCon, ccode) ;
			//BLOB cCodeB = new BLOB ((OracleConnection) dbCon) ;

			if (rSet.next ()) {
				BLOB cCodeBLOB = null ;
				cCodeBLOB = ((OracleResultSet) rSet).getBLOB (1) ;

				// open the BLOB.
				OracleCallableStatement cStmt = 
					(OracleCallableStatement) dbCon.prepareCall ("" +
					"BEGIN DBMS_LOB.OPEN (?, DBMS_LOB.LOB_READWRITE); END;") ;

				cStmt.setBLOB (1, cCodeBLOB) ;	
				cStmt.execute () ;

				int pSize = cCodeBLOB.getChunkSize () ;

				if (appDebug)
					System.out.println ("ODA: chunk size " + pSize) ;

				ByteArrayInputStream fromBAIS = null ;
				OutputStream toBLOB = null ;
				toBLOB = cCodeBLOB.getBinaryOutputStream () ;
				fromBAIS = new ByteArrayInputStream (ccodeUB) ;
	
				byte [] packet = new byte [pSize] ;	
				int bytesRead ;
	
				while ((bytesRead = fromBAIS.read (packet)) != -1) {
					toBLOB.write (packet, 0, bytesRead) ;				
				}

				fromBAIS.close () ;
				toBLOB.close () ;

				// close the BLOB.
				cStmt = (OracleCallableStatement) dbCon.prepareCall ("" +
					"BEGIN DBMS_LOB.CLOSE (?); END;") ; 
				cStmt.setBLOB (1, cCodeBLOB) ;
				cStmt.execute () ;
				cStmt.close () ;
			}

			// close the updateStmt.
			updateStmt.close () ;
        } catch (Exception e) {
            System.out.println ("ODA: Exception Caught in updateBLOB.") ;
			e.printStackTrace () ;
        }
	} // method updateBLOB ends.


	public void closeDB () {
        try {
            dbCon.close () ;
        } catch (Exception e) {
            System.out.println 
				("ODA: Exception Caught while closing Connection.") ;
			e.printStackTrace () ;
        }
	} // method closeDB ends.


	// Data Members.
	private String dbURL = null ;
	private String dbUsername = null ;
	private String dbPassword = null ;
	private Connection dbCon = null ;

	// Holds the name of the jar file to be unjarred.
	private String jarName = null ;

	// Holds the binary bytes of the jar file.
	private byte [] jarCode = null ;

	// Contains the names of the supranet classes.
	private Vector sClasses = null ;

	// Table Names.
	String cmtTable = "classmodel" ;
	String vctTable = "VMS_CLASSESTABLE" ;

	// create statement for classmodel table.
	private String cmtCreateStmt = 
		"create table " + cmtTable +
		" (classname VARCHAR(512), "+
		"id INT, "+
		"version VARCHAR(256), "+
		"datecreated DATE, "+
		"targetArch VARCHAR(64), "+
		"classpath VARCHAR(256), "+
		"data BLOB)";

	// create statement for VMS_CLASSESTABLE table.
	private String vctCreateStmt = 
		"create table " + vctTable +
		" (projectName varchar (32), " +
		"className varchar (250), " +
		"classType varchar (32), " +
		"classCode blob)" ;


	// Contains the file separator string.
	private static final String fSep = System.getProperty ("file.separator") ;

	// Debugging Variable.
	private boolean appDebug = false ;

	private boolean DEBUG = false ;

} // class OracleDataAccess ends.
