package clientPackage;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class Log 
{
	//decalrations
		public PrintWriter pw;
		String file;
		//Constructor
		public Log(String in)
		{
			file = in;
		}
		//write to file
		public void lwrite(String msg)
		{
			try
			{
				pw = new PrintWriter(new FileWriter(file, true));
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
			pw.write(msg);
			pw.close();
		}
		//close file
		public void close()
		{
			pw.close();
		}
}
