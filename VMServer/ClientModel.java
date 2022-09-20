package WebSprocket.VMServer;

/**
*	Encapsulates the information of a client.
*	Used for updating the database, or getting info from the database.
**/
public class ClientModel
{
	/**
	*	The ID of the client.
	**/
	public String clientID;

	/**
	*	The hardware architechure
	**/
	public String architecture;

	/**
	*	The total memory available.
	**/
	public int memoryTotal;

	/**
	*	The memory used.
	**/
	public int memoryUsed;

	/**
	*	Basic constructor for this class.
	**/
	public ClientModel()
	{
	}

}
