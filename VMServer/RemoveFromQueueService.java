package WebSprocket.VMServer;

import java.io.*;
import java.net.*;
import java.util.*;
import WebSprocket.Router.*;


public class RemoveFromQueueService implements Service {


  private static final int REMOVEFROMQ = 1;
  private static final int STATUSONLY = 0;
  private static final int SUCCESS = 1;
  private static final int FAILURE = 0;

  public synchronized void handleRequest (ServiceRequest request, ServiceResponse response) {

    DataInputStream diStream = new DataInputStream(request.getInputStream());
    DataOutputStream doStream = new DataOutputStream(response.getOutputStream());

    try {
      int requestedArbID = diStream.readInt();
      int removeFromQ = diStream.readInt();

      if (removeFromQ == REMOVEFROMQ) {
        boolean ok = Router.removeFromQueue(requestedArbID);
        if (ok) doStream.writeInt(SUCCESS);   // SUCCESS
        else doStream.writeInt(FAILURE);      // FAILURE
      } else {
        int[] status = Router.queueStatus(requestedArbID);
	for (int i=0; i<3; i++) doStream.writeInt(status[i]);
      }
    } catch (Exception e) {
      try {
        doStream.writeInt(FAILURE);
      } catch (Exception ex) {}
    }
  }


        /**
        *       Last method to be called when this service is finished.
        **/
        public void destroy () {
        } // method destroy ends.


        /**
        *       Returns the name of this service.
        *       @return The service name.
        **/
        public String getServiceName () {
                return "RemoveFromQueueService" ;
        } // method getServiceName ends.


        /**
        *       Sets the runtime properties.
        *       These are uniquely identified by form "RemoveFromQueueService.propertyName".
        *       @param p The properties
        **/
    public void setProperties (Properties p) {
    } // method setProperties ends.

}


