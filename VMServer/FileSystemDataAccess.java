package WebSprocket.VMServer ;

import java.io.BufferedReader ;
import java.io.ByteArrayInputStream ;
import java.io.ByteArrayOutputStream ;
import java.io.File ;
import java.io.FileOutputStream ;
import java.io.InputStream ;
import java.io.InputStreamReader ;

import java.util.Properties ;
import java.util.StringTokenizer ;
import java.util.Hashtable ;
import java.util.Vector ;

import java.util.jar.JarEntry ;
import java.util.jar.JarInputStream ;

import WebSprocket.FoundryShared.ClassModel ;

import WebSprocket.Shared.ServerProperties ;

public class FileSystemDataAccess implements DataAccess {
	/**
	 *	Basic Constructor.
	 **/
	public FileSystemDataAccess () {
	} // constructor FileSystemDataAcccess ends.


	/**
	 * Open's a connection to the data base.
	 */
	private boolean openDB () {
		try {	
        } catch (Exception e) {	
			e.printStackTrace () ;
			return false ;
        }

        //test if table exists. If it doesn't, then create it.
        boolean tableExists = true;
        try {
        } catch (Exception e) {
            System.out.println ("table not found") ;
            e.printStackTrace() ;
            tableExists = false ;
        }

        if (! tableExists) {
			try { 
            } catch (Exception e) {
                e.printStackTrace () ;
            }
        }

		return true;
	} // method openDB ends.


	/**
	 * This method stores a class data object into the data base.
	 * @param classModel the data of a class.
	 */
	public void storeClass (ClassModel classModel) {
	} // method storeClass ends.


	/**
	 * This method gets the size of a class give the class id.
	 * @param classID the class id.
	 * @return The class size in bytes.
	 */
	public int getClassSize (int classID) {	
		return 0 ;
	} // method getClassSize ends.	


	/**
	 * This method returns all the clients that has a specific class.
	 * @param classID the class id.
	 * @return The list of client IDs.
	 */
	public ClientModel [] getClientsWithClass (int classID) {	
		return null ;
	} // method getClientsWithClass ends.


	/**
	 *	Add a new class to the database.
	 *	@param model The new ClassModel
	 */
	public void addNewClass (ClassModel model) {
	} // method addNewClass ends.


	/**
	*	Get a class from the database.
	*	@param classID The identifier for the class.
	*	@return ClassModel object which encapsulates class information.
	**/
	public ClassModel getClassModel (int classID) {
		return null ;
	} // method getClassModel ends.


	/**
	*	Get a class from the database.
	*	@param className The name of the class.
	*	@return A ClassModel object which encapsulates class information.
	**/
	public ClassModel getClassModel (String className) {
		return null ;
	} // method getClassModel ends.


	/**
	*	Delete a class from the database.
	*	@param classID The identifier for the class.
	**/
	public void deleteClassFromDB (int classID) {
	} // method deleteClassFromDB ends.


	/**
	*	Retrieves classmodel objects from the database 
	*	according to user provided conditions.
	*	For example, the user may provide "id > 1000" or
	*	"classname like '%'".
	*	@param conditions the SQL conditions for selecting.
	*	@return array of the result ClassModel objects.
	**/
	public ClassModel [] selectClassConditions (String conditions) {
		return null ;
	} // method selectClassConditions ends.


	void removeVersion (String version) {
		System.out.println ("FSDA: removeVersion (" + version + ")") ;

		Properties sProps = ServerProperties.getProperties () ;
		String vmsdhPath = sProps.getProperty ("vmserver.homePath") ;		
		String vmsdhName = sProps.getProperty ("vmserver.homeName") ;		

		String prjHome = vmsdhPath + fSep + vmsdhName + fSep + version ;
	
		File tFile = null ;

		tFile = new File (prjHome) ;

		// delete the directory with project name and all its contents.
		if (tFile.exists ()) {
			deleteDir (prjHome) ;
		}
	} // method removeVersion ends.


    /**
     * Delete a directory the Java API way.
     */
    public void deleteDir (String dir) {
		String [] files ;	

		if (appDebug)
		    System.out.println ("FSDA: Directory: " + dir) ;

		File file = new File (dir) ;
		files = file.list () ;

		int index = 0 ;
		boolean result = false ;
		File tmpFile = null ;
	
		for (index=0; index<files.length; index++) {
		    tmpFile = new File (dir, files [index]) ;	
	
		    if (tmpFile.isFile ()) {
	
			    result = tmpFile.delete () ;
				//System.out.println ("FSDA: Result: " + result) ;

				if (appDebug && ( ! result)) {
					System.out.println 
						("FSDA: Unable to delete File: " + tmpFile.getName ()) ;
				}
		    } else if (tmpFile.isDirectory ()) {
				deleteDir (dir + fSep + files [index]) ;
		    }
		}
	
		result = file.delete () ;
	
		if (appDebug && (! result))
		    System.out.println 
				("FSDA: Unable to delete file : " + file.getName ()) ;
    } // method deleteDir ends.


	// This method the jar file and store the entries in the file system.
	/**
	 * This method reads a jar stream processes it and saves all the
	 * jar entries into the flat file data base.
	 * @param jarname the name of the jar file.
	 * @param iStream the jar stream to be processed.
	 */
	public void processJar (String jarname, InputStream iStream) {
		try {
			jarName = jarname ;
			String projectName = jarName.substring (0, jarName.indexOf ('.')) ;

			// remove the existing classes from the data base.
			removeVersion (projectName) ;

			ByteArrayOutputStream toBAOS = null ;
			toBAOS = new ByteArrayOutputStream () ;

			int bytesRead = -1 ;
			byte [] packet = new byte [2 * 1024] ;

			while ((bytesRead = iStream.read (packet)) != -1) {
				toBAOS.write (packet, 0, bytesRead) ;
			}

			jarCode = toBAOS.toByteArray () ;

			toBAOS.close () ;
			Properties vmProps = ServerProperties.getProperties () ;
			String vmDataHomePath = vmProps.getProperty ("vmserver.homePath") ;
			String vmDataHomeName = vmProps.getProperty ("vmserver.homeName") ;
			destDir = vmDataHomePath + fSep + 
						vmDataHomeName + fSep + projectName ;

			if (! new File (destDir).exists ()) {
				new File (destDir).mkdir () ;	
			}

			// initialize the vector.
			sClasses = new Vector () ;

			parseSupraNetClassNames () ;

			JarInputStream fromJIS = null ;
			// create a JarInputStream on the jar file.
			fromJIS = new JarInputStream (new ByteArrayInputStream (jarCode)) ;	

			JarEntry jEntry = null ;
			String jeName = null ;
			int jeSize ;
			byte [] jeData = null ;
			File jeFile = null ;

			FileOutputStream toFOS = null ;
			while ((jEntry = fromJIS.getNextJarEntry ()) != null) {
				jeName = jEntry.getName () ;

				if (appDebug)
					System.out.println ("FSDA: jeName " + jeName) ;

				// if the jar entry is a file, store it in file system.
				if (! jEntry.isDirectory ()) {
					// file type, could be REGULARCLASS_FILE, 
					// SUPRANETCLASS_FILE, REGULARDAT_FILE
					String fType = null ;
					if (jEntry.getMethod () == JarEntry.DEFLATED) {
						jeSize = (int) jEntry.getCompressedSize () ;
					} else {
						jeSize = (int) jEntry.getSize () ;
					}

					if (jeName.endsWith (".class")) {
						if (sClasses.contains (jeName)) {
							System.out.println ("FSDA: SUPRANETCLASS added.") ;
							fType = "SUPRANETCLASS_FILE" ;
						} else
							fType = "REGULARCLASS_FILE" ;
					} else
						fType = "REGULARDATA_FILE" ;

					// check to see if the parent directory exists create
					// it if required.
					fName = destDir + fSep +
							fType + fSep + 
							jeName.replace ('/', fSep.charAt (0)) ;
					jeFile = new File (fName) ;

					File pFile = jeFile.getParentFile () ; 
					if (! pFile.exists ()) {
						pFile.mkdirs () ;
					}

					toFOS = new FileOutputStream (jeFile) ;

					if (! (jeSize == -1)) {
						jeData = new byte [jeSize] ;
						fromJIS.read (jeData, 0, jeSize) ;

						toFOS.write (jeData) ;
					} else {
						while ((bytesRead = fromJIS.read 
							(packet, 0, packet.length)) != -1) {

							toFOS.write (packet, 0, bytesRead) ;
						}
					}

					toFOS.close () ;
				} else {
					// if the jar entry is a directory, ignore it.
				}

				System.out.println (jeName) ;

				fromJIS.closeEntry () ;
			}

			fromJIS.close () ;
		} catch (Exception e) {
			System.out.println ("FSDA: Exception Caught in Application.") ;
			e.printStackTrace () ;
		}
	} // method processJar ends.


	/**
	 * This method parses all the supranet classes from 
	 * <project name>.supranet file.
	 */
	private void parseSupraNetClassNames () {
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
					int bytesRead = -1 ;
					byte [] packet = new byte [2*1024] ;

					while ((bytesRead = fromJIS.read 
						(packet, 0, packet.length)) != -1) {

						//toFOS.write (packet, 0, bytesRead) ;
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
				
				// close the JarEntry.
				fromJIS.closeEntry () ;
			} // end while

			fromJIS.close () ;

		} catch (Exception e) {
			System.out.println ("FSDA: Exception Caught in Application.") ;
			e.printStackTrace () ;
		}
	} // method parseSupraNetClassNames ends.


	/**
	 * This method parses a line from the <project name>.supranet file.
	 */
    private void parseRecord (String line) {
		String projectName = null ;
		String className = null ;
		projectName = jarName.substring (0, jarName.indexOf ('.')) ;
		if (appDebug)
			System.out.println ("FSDA: projectName " + projectName) ;

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
						System.out.println ("FSDA: className "+ tStr) ;
	
					// put the supranet class name in the vector.
					sClasses.addElement (tStr) ;
				}
			    break ;
		    }	
		}
    } // method parseRecord ends.


	// SupraNet DB Management method.
	/**
	 * This method creates a file and returns the status.
	 */
	public String createFile (String pname, String ftype, 
								String fname, byte [] fdata) {
		String FILE_CREATED = "yes" ;
		String FILE_NOT_CREATED = "no" ;

		Properties sProps = ServerProperties.getProperties () ;

		String vmsdhPath = sProps.getProperty ("vmserver.homePath") ;		
		String vmsdhName = sProps.getProperty ("vmserver.homeName") ;		

		// if vmserver.homePath is "." then vmdHomePath is uDir
		// otherwise it is the absolute path of the property itself.
		if (vmsdhPath.equals ("."))
			vmsdhPath = uDir ;

		fname = fname.replace ('/', fSepc) ;
		String fName = vmsdhPath + fSep + vmsdhName + fSep + 
						pname + fSep + ftype + fSep + "classes" + fSep + fname ;

		System.out.println ("FSDA: fName " + fName) ;

		File tFile = null ;
		tFile = new File (fName) ;

		try {
			FileOutputStream toFOS = null ;
			ByteArrayInputStream fromBAIS = null ;
			
			int bytesRead ;
			byte [] packet = new byte [2*1024] ;
	
			toFOS = new FileOutputStream (tFile) ;
			fromBAIS = new ByteArrayInputStream (fdata) ;
	
			while ((bytesRead = fromBAIS.read (packet)) != -1) {
				toFOS.write (packet, 0, bytesRead) ;
			}
	
			fromBAIS.close () ;
			toFOS.close () ;
		} catch (Exception e) {
			System.out.println ("FSDA: Exception Caught in createFile.") ;
			e.printStackTrace () ;

			return (FILE_NOT_CREATED) ;
		}

		if (tFile.exists ()) {
			return (FILE_CREATED) ;
		} else {
			return (FILE_NOT_CREATED) ;
		}
	} // method createFile ends.


	// SupraNet DB Management method.
	/**
	 * This method deletes a file and returns the status. 
	 */
	public String deleteFile (String pname, String ftype, String fname) {
		String FILE_DELETED = "yes" ;
		String FILE_NOT_DELETED = "no" ;
		String FILE_DOES_NOT_EXIST = "! exists" ;
		Properties sProps = ServerProperties.getProperties () ;

		String vmsdhPath = sProps.getProperty ("vmserver.homePath") ;		
		String vmsdhName = sProps.getProperty ("vmserver.homeName") ;		

		// if vmserver.homePath is "." then vmdHomePath is uDir
		// otherwise it is the absolute path of the property itself.
		if (vmsdhPath.equals ("."))
			vmsdhPath = uDir ;

		fname = fname.replace ('/', fSepc) ;
		String fName = vmsdhPath + fSep + vmsdhName + fSep + 
						pname + fSep + ftype + fSep + "classes" + fSep + fname ;

		System.out.println ("FSDA: fName " + fName) ;

		File tFile = null ;
		tFile = new File (fName) ;

		if (tFile.exists ()) {
			if (tFile.delete ()) {
				return (FILE_DELETED) ;
			} else {
				return (FILE_NOT_DELETED) ;
			}
		} else {
			return (FILE_DOES_NOT_EXIST) ;
		}
	} // method deleteFile ends.


	// SupraNet DB Management method.
	/**
	 * This method returns a list of projects.
	 */
	public Vector getProjectList () {
		Properties sProps = ServerProperties.getProperties () ;

		Vector pList = null ;
		pList = new Vector () ;

		String vmsdhPath = sProps.getProperty ("vmserver.homePath") ;		
		String vmsdhName = sProps.getProperty ("vmserver.homeName") ;		

		// if vmserver.homePath is "." then vmdHomePath is uDir
		// otherwise it is the absolute path of the property itself.
		if (vmsdhPath.equals ("."))
			vmsdhPath = uDir ;

		String vmsdHome = vmsdhPath + fSep + vmsdhName ;
		File tFile = null ;

		System.out.println ("FSDA: vmsdHome " + vmsdHome) ;
		tFile = new File (vmsdHome) ;

		if (tFile.exists ()) {
			String [] fileList = null ;
			fileList = tFile.list () ;

			int nofFiles = fileList.length ;
			System.out.println ("FSDA: nofFiles " + nofFiles) ;
			int index = 0 ;
			for (index=0; index<nofFiles; index++) {
				System.out.println ("FSDA: adding " + fileList [index]) ;
				pList.addElement (fileList [index]) ;
			}

			return (pList) ;
		} else {
			return (pList) ;
		}
	} // method getProjectList ends.


	// SupraNet DB Management method.
	/**
	 * This method returns a list of files from a project.
	 * The files list could be REGULARDATA_FILE, REGULARCLASS_FILE
	 * SUPRANETCLASS_FILE.
	 */
	public Hashtable getFileList (String pname, String ftype) {
		Properties sProps = ServerProperties.getProperties () ;

		Hashtable fList = null ;
		fList = new Hashtable () ;

		String vmsdhPath = sProps.getProperty ("vmserver.homePath") ;		
		String vmsdhName = sProps.getProperty ("vmserver.homeName") ;		

		// if vmserver.homePath is "." then vmdHomePath is uDir
		// otherwise it is the absolute path of the property itself.
		if (vmsdhPath.equals ("."))
			vmsdhPath = uDir ;

		String fDir = vmsdhPath + fSep + vmsdhName + fSep + 
						pname + fSep + ftype ;
		File tFile = null ;

		System.out.println ("FSDA: fDir " + fDir) ;
		tFile = new File (fDir) ;

		if (tFile.exists () && tFile.isDirectory ()) {
			addFiles (tFile, fList) ;
			return (fList) ;
		} else {
			return (fList) ;
		}
	} // method getFileList ends.


	private void addFiles (File rootF, Hashtable listV) {
		File tFile = null ;
		String [] fileList = null ;
		fileList = rootF.list () ;
		String fPath = null ;
		String tStr = null ;

		int nofFiles = fileList.length ;
		System.out.println ("FSDA: nofFiles " + nofFiles) ;
		int index = 0 ;
		for (index=0; index<nofFiles; index++) {
			tFile = new File (rootF, fileList [index]) ;

			if (tFile.isDirectory ()) {
				addFiles (tFile, listV) ;
			} else {
				fPath = tFile.getAbsolutePath () ;

				if (fPath.indexOf ("classes") != -1) {
					tStr = fPath.substring 
							(fPath.indexOf ("classes") + 
								"classes".length () + 1) ;

					tStr = tStr.replace ('\\', '/') ;
					fPath = tStr ;
				} else if (fPath.indexOf ("src") != -1) {
					tStr = fPath.substring 
							(fPath.indexOf ("src") + 
								"src".length () + 1) ;

					tStr = tStr.replace ('\\', '/') ;
					fPath = tStr ;
				} else {
					fPath = tFile.getName () ;
				}

				System.out.println 
					("FSDA: adding " + fPath + ":" + tFile.getName ()) ;
				listV.put (fPath, tFile.getName ()) ;
			}
		}
	} // method addFiles ends.


	// Data Members.
	// Holds the name of the jar file to be unjarred.
	private String jarName = null ;

	// Holds the binary bytes of the jar file.
	private byte [] jarCode = null ;

	// Contains the destination directory for unjarred files.
	private String destDir = "Output" ;

	// Contains the file name of each unjarred entry.
	private String fName = null ;

	// Contains the file separator string.
	private static final String fSep = System.getProperty ("file.separator") ;

	// Contains the names of the supranet classes.
	private Vector sClasses = null ;

	// User home directory variable
	private static final String uDir = System.getProperty ("user.dir") ;

	// File Separator char variable.
	private static final char fSepc = File.separatorChar ;

	// Debugging Variable.
	boolean appDebug = false ;
} // class FileSystemDataAccess ends.
