package servicePackage;

/*Imports*/
import java.net.ServerSocket;
import java.util.concurrent.*;

/*The class*/
public class Service
{
	//Initializing
	private ServerSocket socket = null;
	
	//initialize executor object
	int poolSize = 2;							//at least that many threads
	int maxPoolSize = 4;						//at most that many threads
	long keepAliveTime = 10000000;				//how long will a request wait before terminated
	ArrayBlockingQueue<Runnable> queue = null;	//The blocking queue for holding excess connections			
	ThreadPoolExecutor pool = null;	
	
	//Logging
	static Log log;
	
	/* Constructor */
	public Service()
	{
		try
		{
			socket = new ServerSocket(Protocol.PORT); //create the socket
			log.lwrite("Created socket: "+socket.toString()+"\n");
			queue = new ArrayBlockingQueue<Runnable>(5);//initialize the queue
			log.lwrite("Created connection queue...\n");
			//create the threadpool
			pool = new ThreadPoolExecutor(poolSize, maxPoolSize, keepAliveTime, TimeUnit.SECONDS ,queue);
			log.lwrite("Created thread executor...\n");
		}
		catch(Exception e)
		{
			log.lwrite("Error occured at the constructor...\nDetails: ");
			e.printStackTrace(log.pw);
			log.lwrite("\n");
		}
	}
	
	/* This function contains the loop for the server.
	 * Its important to understand that the contructor of the chess AI 
	 * must implement the Runnable interface and 
	 * have a parameter of type Socket. So, in this loop, we will have this:
	 * pool.execute(new ChessAI(socket.accept())) 
	 * this way, every new connection will be directly handled in a new thread, 
	 * and every connection that must wait, will wait in the queue! */
	public void start()
	{
		log.lwrite("The service will now start receiving connections...\n");
		while(true)//instead of true, will have a flag
		{
			try
			{
				pool.execute(new ChessAI(socket.accept()));
			}
			catch(Exception e)
			{
				log.lwrite("Error in Service:start()...\nDetails: ");
				e.printStackTrace();
				log.lwrite("\n");
			}
		}
	}
	
	/* Main */
	public static void main(String args[])
	{
		try
		{//Create a Service object and run
			log = new Log("serviceLog.file");
			log.lwrite("Initializing service object...\n");
			Service serv = new Service();
			log.lwrite("Starting service...\n");
			serv.start();
			log.close();
			System.exit(0);
		}
		catch(Exception e)
		{
			log.lwrite("Error occured in main...\n Details: ");
			e.printStackTrace(log.pw);
			log.lwrite("\nExiting...");
			log.close();
			System.exit(0);
		}
	}
}
