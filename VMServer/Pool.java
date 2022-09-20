package WebSprocket.VMServer;

import java.util.*;

/**
 * <p> Implements Thread pooling.  A Thread Pool keeps 
 * a bunch of suspended threads around to do some work.
 * When a Thread is needed, it is popped off the stack and awakened.
 * When it finishes, the Thread pushes itself back on the stack
 * and returns to the suspended state.
 *
 * <p> A typical <code>performWork</code> method would be as follows:
 *
 * <pre>
 * <code>
 * public void performWork (Object data) {
 *     WorkerThread wThread = null ;
 *     ...
 *     // if the thread stack is empty, create a bunch of worker threads.
 *     // and push them into stack, else pop a thread from the stack and
 *     // put it to work on a request.
 *     if (stack.empty ()) {
 *         ...
 *         // create a bunch of worker thread and push them into the stack.
 *     } else {
 *         ...
 *         // pop a worker thread from the stack and put it to work.
 *         wThread = (WorkerThread) stack.pop () ;
 *     }
 *     
 *     // put it to work.
 *     wThread.wake (data) ;
 * } // method performWork ends.
 * </code>
 * </pre>
 *
 * @see WebSprocket.VMServer.WorkerThread
 * @see WebSprocket.VMServer.VMServer
 */

public class Pool {
	private Stack waitStack = new Stack();
	private int maxThreads = 10;	//default value
	Properties serverProperties = null;

	/**
	*	Creates a new Pool instance and initializes the Threads.
	*	@param max Max number of handler threads
	*	@param workerClass	Name of Worker implementation.
	**/
	public Pool (int max, java.util.Properties p) {
		System.out.println("Initializing pool of "+max+" Threads");
		serverProperties = p;
		maxThreads = max;
		WorkerThread wt;
		for(int i=0;i<maxThreads;i++)
		{
			wt = new WorkerThread("Worker#"+i, waitStack, maxThreads);
			wt.setProperties(serverProperties);
			wt.start();
			waitStack.push(wt);
		}
	} // constructor Pool ends.


	/**
	*	Request the Pool to perform some work.
	*	Pops a WorkerThread off the stack and starts it.
	*	@param data Data to give to the Worker.
	*	@return void
	**/
	public void performWork (Object data) {
		WorkerThread wt = null;

		synchronized (waitStack) {

			//used up all Pooled threads- need more
			if (waitStack.empty ())	{	
				wt=new WorkerThread ("additional worker",waitStack,maxThreads) ;
				wt.setProperties (serverProperties) ;
				wt.start () ;
			} else {	
				wt= (WorkerThread) waitStack.pop () ;
			}
		}

		wt.wake (data) ;
	} // method performWork ends.

	/**
	*	Sets the properties object which is passed to the Services
	*	which are run by the WorkerThread.
	*	@param p The runtime properties for this server.
	**/
	public void setProperties(Properties p)
	{	serverProperties = p;
	}
} // class Pool ends.
