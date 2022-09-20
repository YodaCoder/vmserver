package WebSprocket.VMServer;

import java.io.*;

/**
 * <p> A Service in the WebSprocket paradigm represents a real time 
 * service like an authentication service, encryption service, billing 
 * service etc.
 *
 * <p> VMServer processes requests coming from a variety of clients
 * and of various types of services. It allows to add new services
 * using this interface.
 *
 * <p> For example: Here is a simple code for adding a new service.
 *
 * <p><code>
 * <pre>
 * public class ExampleService implements Service {
 *     ...
 *     // give the implementation of what this service should do 
 *     // in the handleRequest method. This method is automatically
 *     // invoked by the WorkerThread when this service is created.
 *     public void handleRequest (ServiceRequest, sReq, ServiceResponse sRes) {
 *         ...
 *         // write the logic for this service.
 *         ...
 *     } // method handleRequest ends.
 *     ...
 * } // class ExampleService ends.
 * </pre>
 * </code>
 *
 * And VMServer automatically handles any request of type newService.
 *
 * @see WebSprocket.VMServer.ClassLoadService
 * @see WebSprocket.VMServer.DataService
 * @see WebSprocket.VMServer.SupraNetDBMService
 */

public interface Service {
	/**
	 *	Process a request received for this service.
	 *	@param request
	 *	@param response
	 */
	public void handleRequest 
		(ServiceRequest request, ServiceResponse response) ;

	/**
	 * 	Method for cleaning up resources.
	 *	This method is automatically called when Service finishes
	 *	the handleRequest method.
	 */
	public void destroy () ;

	/**
	 *	Returns the service name.
	 *	@return The name of this service.
	 */
	public String getServiceName () ;

	/**
	*   Sets the properties for this Service.
	*	These are typically of the form: 
	*	<i>ServiceName.propertyName</i>.
	*   @param p The runtime properties for this server.
	**/
	public void setProperties (java.util.Properties props) ;
} // interface Service ends.
