package WebSprocket.VMServer ;

import java.io.* ;

/**
*   This class provides client information for a service response, 
*	such as an OutputStream for writing data to the client.
*   Authentication and encryption of the stream is
*   done in a higher layer than this class.
**/
public class ServiceResponse 
{
	OutputStream outStream;

	/**
	 * Constructor.
	 */
	public ServiceResponse(OutputStream outStream)
	{	this.outStream = outStream;
	}

	/**
	*	Returns an OutputStream for writing to the client.
	*	@return The OutputStream object.
	**/
	public OutputStream getOutputStream() 
	{	return outStream;
	}
}
