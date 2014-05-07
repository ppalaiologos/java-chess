package clientPackage;

/*Imports*/
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import javax.swing.*;


public class Client 
{
	
	/*Globals*/
	static Socket socket = null; 			//Socket object
	static InetAddress ip = null;			//Ip to the server
	static PrintWriter pw = null;			//the stream writer for messages to the server
	static BufferedReader br = null;		//buffered reader for messages from thr server
	static Board board = null;
	static Log log;
	static Log exLog;
	
	/* exchange: the function exchange sends a message to the server and receives
	 * the response. It is called from the object board */
	public static String exchange(String message)
	{
		try
		{
			exLog.lwrite("Sending message: "+message+"\n");
			
			pw.println(message);
			pw.flush();
			//pw.close();
			exLog.lwrite("Waiting for answer... \n");
			message = br.readLine();
			exLog.lwrite("Answer received: "+message+"\n");
		}
		catch(Exception e)
		{
			exLog.lwrite("Error sending or receiving message! Details: \n"+e.toString());
			exLog.lwrite("\n Returning rejection \n");
			message = Protocol.codeRejected;
		}		
		return message;
	}
	
	public static void main(String[] args) 
	{
		try
		{
			log = new Log("clientLog.file");
			exLog = new Log("exchangeLog.file");
			ip = InetAddress.getByName(Protocol.IP);
			log.lwrite("Acquired service IP address"+ ip.toString()+ "...\n");
			socket = new Socket(ip, Protocol.PORT);			
			log.lwrite("Connection established: \n     "+socket.toString()+"\n");
			br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			pw = new PrintWriter(socket.getOutputStream(), true);
			log.lwrite("Created communication objects pw and br...\n");
			SwingUtilities.invokeLater(
					new Runnable()
					{
						public void run()
						{
							board = new Board();	
						}
					});
			log.lwrite("Initialized board object...\n");
		}
		catch(Exception e)
		{
			log.lwrite("Error occured in Client.Client()...\n Details: "+e.toString());
			log.lwrite("Exiting....\n Error code: 0");
			log.close();
			//board.log.close();
			System.exit(0);
		}
	}

}
