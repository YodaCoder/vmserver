package WebSprocket.VMServer ;

import java.io.* ;

/**
*	This class provides client information to a service, such 
*	as an InputStream for reading data from the client.
*	Authentication and encryption of the stream is 
*	done in a higher layer than this class.
**/
public class ServiceRequest 
{
	InputStream inStream;
	java.util.Hashtable attributeTable = new java.util.Hashtable();
	String remoteSystem;
	int arbID = 0;

	/**
	 * Default Constructor.
	 */
	public ServiceRequest(InputStream inStream) 
	{
		this.inStream = inStream;
	}

	/**
	 * Constructor assigning a random id
	 */
	public ServiceRequest(InputStream inStream, int arbID) 
	{
		this.inStream = inStream;
		this.arbID = arbID;
	}

	/**
	 *	Returns an InputStream of the network connection to a client.
	 *	@return The InputStream to the client.
	 */
	public InputStream getInputStream() 
	{	return inStream;
	}

	/**
	*	Retrieves an attribute in this request.
	*	@param name The name of the attribute to retrieve. 
	*	(ie. "contentLength" or "date")
	*	@return The attribute object, which can be a String.
	**/
	public Object getAttribute(String name)
	{
		return attributeTable.get(name);
	}

	/**
	*	Returns an Enumeration of the attribute names associated 
	*	with this request.
	*	@return The names of attributes as String objects.
	**/
	public java.util.Enumeration getAttributeNames()
	{
		return attributeTable.keys();
	}

	/**
	*	Stores an attribute in this request.
	*	@param name The name of the attribute ("date").
	*	@param attribute The attribute value ("Wed Feb  7 12:36:40 PST 2001").
	**/
	public void setAttribute(String name, Object attribute)
	{
		attributeTable.put(name, attribute);
	}

	/**
	*	Returns the name or id of the client that requested a service.
	*	@return The String representation of the remote system.
	**/
	public String getRemoteSystem()
	{
		return remoteSystem;
	}

	/**
	*	Returns the name or id of the client that requested a service.
	*	@param remoteSystem Sets the String representation of a system.
	**/
	public void setRemoteSystem(String remoteSystem)
	{
		this.remoteSystem = remoteSystem;
	}

	/**
	*	Returns the session object for multiple Service interactions.
	**/
	public Object getSession()
	{
		return new Object();
	}


	/**
	 * Random ID if the WorkerThread chooses to assign
	 */
	public int getArbID()
	{
		return arbID;
	}
}
