package WebSprocket.VMServer;

import WebSprocket.Shared.*;
import WebSprocket.FoundryShared.*;

/**
*	An abstraction for accessing Database information from the Server.
*	Implementation of this interface depends on the database to be used.
*
**/
public interface DataAccess
{
	/**
	*	Store class in database.
	*	Used to store complete class info.
	*	ie. loading class first time.
	*	@param classID The id for a class.
	*	@param data The binary data for the class.
	**/
	public void storeClass(ClassModel classModel);

	/**
	*	Get the size of the Class
	*	Useful information during the updating of clients.
	*	@param classID The id for a class.
	*	@return The class size in bytes.
	**/
	public int getClassSize(int classID);

	/**
	*	Get a list of clients using a specified class.
	*	Need to know when doing versioning.
	*	@param classID The identifier for the class.
	*	@return The list of client IDs.
	**/
	public ClientModel []  getClientsWithClass(int classID);

	/**
	*	Get a class from the database.
	*	@param classID The identifier for the class.
	*	@return A ClassModel object which encapsulates class information.
	**/
	public ClassModel getClassModel(int classID);

	/**
	*	Get a class from the database.
	*	@param className The name of the class.
	*	@return A ClassModel object which encapsulates class information.
	**/
	public ClassModel getClassModel(String className);

	/**
	*	Delete a class from the database.
	*	@param classID The identifier for the class.
	**/
	public void deleteClassFromDB(int classID);

}
