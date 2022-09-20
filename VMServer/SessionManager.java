package WebSprocket.VMServer;

import WebSprocket.Shared.*;
import java.util.*;
import java.io.* ;

/**
 * SessionManager keeps track of all ongoing sessions and manages them.
 * @see WebSprocket.VMServer.VMServer
 */
public class SessionManager {
    /**
     *	Basic constructor.
     */
    public SessionManager() {
		// GKJ: sessions = new Vector () ;
		sessions = new Object [MAX_SESSIONS] ;
    } // constructor SessionManager ends.

	/**
	*	Constructor to specify network.
	**/
	public SessionManager(NetworkManager netMgr)
	{
	}

    /**
     *	Create a new session to the remote system.
     *	@param id The remote system identifier.
     */
    public Session createSession (InputStream pIS, OutputStream pOS) {
		ServerSession ssession = new ServerSession (pIS, pOS) ;

		// GKJ: sessions.addElement (ssession) ;
		sessions [sessionIndex] = ssession ;
		sessionIndex ++ ;

		return ssession ;
    } // method createSession ends.


    /**
     *	Get all live session objects.
     *	@return An iterator of the sessions.
     **/
    public Object [] getSessions() {
		return sessions ;
    } // method getSessions ends.


	/**
	 * Returns true if there are any sessions currently handled by
	 * the server else returns false.
	 */
	public boolean hasSessions()
	{	return false;
	}


    // DataMembers.
    /**
     * Vector stores the Session objects that it manages.
     */
    // GKJ: Vector sessions = null ;
    Object [] sessions = null ;

	private int MAX_SESSIONS = 50 ;
	private int sessionIndex = 0 ;
} // class SessionManager ends.
