package servicePackage;

/* Imports */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.StringTokenizer;

/* The class */
public class ChessAI implements Runnable
{
	//communication objects
	PrintWriter pw;
	Socket socket;
	BufferedReader br;
	Log log;
	boolean qFlag = true;
	
	//the board matrix
	int[] board = new int[120];
	
	//useful variables
	int[] movelist = new int[250];
	int moveCo = 0;
	int color = 1;
	
	//evaluation variables
	//variables for the evaluation
		float [] posvalues = 
			{	0.00f,	0.00f, 	0.00f, 	0.00f, 	0.00f, 	0.00f, 	0.00f, 	0.00f, 	0.00f, 	0.00f,
				0.00f,	0.00f,	0.00f,	0.00f,	0.00f,	0.00f,	0.00f,	0.00f,	0.00f,	0.00f,
				0.00f,	0.00f,	0.01f,	0.02f,	0.03f,	0.03f,	0.02f,	0.01f,	0.00f,	0.00f,//8
				0.00f,	0.01f,	0.04f,	0.04f,	0.04f,	0.04f,	0.04f,	0.04f,	0.01f,	0.00f,//7
				0.00f,	0.03f,	0.04f,	0.06f,	0.06f,	0.06f,	0.06f,	0.04f,	0.02f,	0.00f,//6
				0.00f,	0.03f,	0.04f,	0.06f,	0.08f,	0.08f,	0.06f,	0.04f,	0.03f,	0.00f,//5
				0.00f,	0.03f,	0.04f,	0.06f,	0.08f,	0.08f,	0.06f,	0.04f,	0.03f,	0.00f,//4
				0.00f,	0.02f,	0.04f,	0.06f,	0.06f,	0.06f,	0.06f,	0.04f,	0.02f,	0.00f,//3
				0.00f,	0.01f,	0.04f,	0.04f,	0.04f,	0.04f,	0.04f,	0.04f,	0.01f,	0.00f,//2
				0.00f,	0.00f,	0.01f,	0.02f,	0.03f,	0.03f,	0.02f,	0.01f,	0.00f,	0.00f,//1
				0.00f,	0.00f,	0.00f,	0.00f,	0.00f,	0.00f,	0.00f,	0.00f,	0.00f,	0.00f, 
				0.00f,	0.00f,	0.00f,	0.00f,	0.00f,	0.00f,	0.00f,	0.00f,	0.00f,	0.00f };
	//minimax variables
	int deep = 0; 		//action deep
	int target = 4;		//target deep
	float value = 0;
	float[] minimax = new float[10];
	int aiMove = 0;		//AI move
		
	//move execution variables
	int code = 0;  //movelist index
	int start = 21;//used to point to the starting field
	int alt = 21;  //might be useful
	int end = 21;  //used to point to the destination field
	//x and y coords
	int x = 0;
	int y = 0;
	
	//The constructor
	public ChessAI(Socket s)
	{
		this.socket = s;
		log = new Log(socket.hashCode()+"Log.file");
		log.lwrite("AI connected to "+socket.getInetAddress().toString()+"\n");
		log.lwrite("Initializing communication objects...\n");
		try
		{
			pw = new PrintWriter(socket.getOutputStream(), true);
			br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		}
		catch(Exception e)
		{
			log.lwrite("Error initializing pw or br...\nDetails: ");
			e.printStackTrace(log.pw);
			log.lwrite("\n");
		}
		
		newgame();
		run();
	}
	
	/*This function resets the AI to start a new game*/
	private void newgame()
	{
		log.lwrite("newgame(): called");
		log.lwrite("recreating data...\n");
		//this is the starting position
		/* 1st digit saves the ability to castle (moved/not moved)
		 * that's why it appears in six fields, the rooks and the kings
		 * 2nd digit is the player; 1 for white, 2 for black
		 * 3rd digit is the piece;
		 * 1 for pawn, 2 for bishop, 3 for knight, 4 for rook,
		 * 5 for queen and 6 for king;
		 *  99s are the borders and 00s are the empty spaces. */
		int [] org = {
				   99, 99, 99, 99, 99, 99, 99, 99, 99, 99,
				   99, 99, 99, 99, 99, 99, 99, 99, 99, 99,
				   99,124, 22, 23, 25,126, 23, 22,124, 99,
				   99, 21, 21, 21, 21, 21, 21, 21, 21, 99,
				   99, 00, 00, 00, 00, 00, 00, 00, 00, 99,
				   99, 00, 00, 00, 00, 00, 00, 00, 00, 99,
				   99, 00, 00, 00, 00, 00, 00, 00, 00, 99,
				   99, 00, 00, 00, 00, 00, 00, 00, 00, 99,
				   99, 11, 11, 11, 11, 11, 11, 11, 11, 99,
				   99,114, 12, 13, 15,116, 13, 12,114, 99,
				   99, 99, 99, 99, 99, 99, 99, 99, 99, 99,
				   99, 99, 99, 99, 99, 99, 99, 99, 99, 99 };
		
		//update the board
		for (int i=0; i < 120; i++) board [i] = org [i];
		
		//move generation
		moveCo = 0;
		color = 1;
		deep = 0;
		target = 1;
		genmove();
		
		//exiting
		log.lwrite("newgame(): return\n");
		return;
	}
	
	/*This function executes the move of the player*/
	private String execute(int start, int end)
	{
		String answer = Protocol.codeAIMove;
		log.lwrite("execute(): called\n");
		//swap position and empty start-field
		board[end] = board[start];
		board[start] = 0;
		//castling
		if (board [end] % 10 == 6)
		{
			if( end == start + 2)
			{	//little
				board [start + 1] = board [start + 3] % 100;
				board [start + 3] = 0;
			}
			if( end == start - 2)
			{	//big
				board [start - 1] = board [start - 4] % 100;
				board [start - 4] = 0;		
			}
		}
		//then the server calculates the possble moves after the human move
		color = 2;
		//look for best move
		deep = 0;
		target = 2;
								
		//look for best move
		moveCo = 0;
		genmove ();			

		if (moveCo == 0)	//no moves -> end of game
		{
			if (ischeck ())return Protocol.codeWhiteWins;
			else return Protocol.codeDraw;
		}
		//execute CPU move
		start = aiMove/100;
		end = aiMove%100;
		board[end] = board[start];
		board[start] = 0;
		
		color =1;
		//generate moves for the board
		log.lwrite("generating moves\n");
		moveCo =0;
		deep = 0;
		target = 1;
		genmove();
		//check if there are moves available
		if(moveCo==0)
		{
			if(ischeck()) answer = Protocol.codeBlackWins;
			else return Protocol.codeDraw;
		}			
		//exiting
		log.lwrite("execute(): return\n");
		return answer+Protocol.codeSeparator+start+"x"+end;
	}
	
	/*This function checks if a move is valid*/
	private boolean isvalid(int move)
	{
		log.lwrite("isvalid(): called\n");
		for(int i=0; i<moveCo; i++)
		{
			if(movelist[i]==move) 
			{
				log.lwrite("isvalid(): return 'true'\n");
				return true;
			}
		}
		log.lwrite("isvalid(): return 'false'\n");
		return false;
	}
	
	/*simulizes a move, and if not moving out of border, adds it*/
	private void simulize(int start, int end)
	{
		log.lwrite("simulize(): called\n");
		//checks if the move moves out of the border
		if((board[end]== 99)||(board[end]%100/10 == color)) 
		{
			log.lwrite("illegal move; return\n");
			return;
		}
		
		//next check for check
		int oStart = board[start];
		int oEnd = board[end];
		//execute the move
		board[end] = board[start];
		board[start] = 0;
		//check if king is under check. If not, add move
		if(!ischeck())
		{
			log.lwrite("not incheck\n");
			//if not, continue
			if(deep==1)
			{
				log.lwrite("Deep==1");
				movelist[moveCo] = start*100+end;
				log.lwrite("move "+start*100+end+" added at "+moveCo+"\n");
				moveCo++;
			}
			//calculate value of node
			if(target == deep)
			{
				log.lwrite("deep = target = "+deep+"\n");
				value = evaluation();
			}
			else
			{
				log.lwrite("deep <> target \n");
				if(color==1) color=2;
				else color=1;
				log.lwrite("calling genmove()\n");
				genmove();
				log.lwrite("registering...\n");
				value = minimax[deep+1];
				
				if(color==1) color=2;
				else color=1;
			}
			log.lwrite("minimax adjustment...\n");
			//Minimax
			if(deep%2==0)
			{	//human
				log.lwrite("human:value="+value+"\n");
				
				if(value>minimax[deep]) {minimax[deep]=value; log.lwrite("deep="+deep+", mm[deep]="+minimax[deep]+"\n");}				
			}
			else
			{	//AI
				log.lwrite("AI:value="+value+"\n");
				if(value<minimax[deep])
				{
					log.lwrite("deep="+deep+", mm[deep]="+minimax[deep]+"\n");
					minimax[deep]=value;
					if(deep==1) aiMove = start*100+end;
				}
			}
		}//finally, undo the move
		board [start] = oStart;
		board [end] = oEnd;
		//exiting
		log.lwrite("simulize(): return\n");
		return;
	}
	
	/*simulizes the moves of the rook, bishop and queen*/
	private void multisimulize(int start, int inc)
	{
		log.lwrite("multisimulize(): called\n");
		
		int to = start;
		//the fucntion simulizes moves until blocked
		log.lwrite("starting while loop\n");
		while((board[to+inc]!=99)&&(board[to+inc]%100/10!=color))
		{
			to+=inc;
			log.lwrite("to="+to+"\n");
			
			if(board[to]!=0)
			{//found barrier
				log.lwrite("found barrier at "+to+"\n");
				simulize(start,to);
				log.lwrite("multisimulize(): return\n");
				return;
			}
			//no barriers, simulize
			log.lwrite("move to "+to+" valid, simulizing\n");
			simulize(start,to);
		}
		//move is already barred
		simulize(start,to);
		log.lwrite("multisimulize(): return\n");
		return;
	}
	
	//is king in check?
	public boolean ischeck () 
	{
		log.lwrite("ischeck(): called\n");
		int king = 0;
		
		//search king
		for ( int i = 21; i < 99; i++)
		{
			if ((board [i] % 100 / 10 == color) && (board [i] % 10 == 6))
			{
				king = i;
				break;
			}	
						
			if ( i % 10 == 8)
				i += 2;
		}
		
		//knight
		if ((board [king-21] % 10 == 2) && (board [king-21] % 100 / 10 != color))
			return true;
	 	if ((board [king+21] % 10 == 2) && (board [king+21] % 100 / 10 != color))
			return true;
		if ((board [king-19] % 10 == 2) && (board [king-19] % 100 / 10 != color))
			return true; 
		if ((board [king+19] % 10 == 2) && (board [king+19] % 100 / 10 != color))
			return true;
		if ((board [king- 8] % 10 == 2) && (board [king- 8] % 100 / 10 != color))
			return true;
		if ((board [king+ 8] % 10 == 2) && (board [king+ 8] % 100 / 10 != color))
			return true;
		if ((board [king-12] % 10 == 2) && (board [king-12] % 100 / 10 != color))
			return true;
		if ((board [king+12] % 10 == 2) && (board [king+12] % 100 / 10 != color))
			return true;		 
	 
	   	//bishop
		int j = king;
		while (board [j - 9] != 99)
		{		
			j -= 9;
			if (board [j] % 100 / 10 == color)
				break;
			if (board [j] == 0)
				continue;
			if ((board [j] % 10  == 3) || (board [j] % 10  == 5))
				return true;
			else
				break;
		}
						
		j = king;
		while (board [j+9] != 99)		
		{
			j += 9;
			if (board [j] % 100 / 10 == color)
				break;
			if (board [j] == 0)
				continue;						
			if ((board [j] % 10 == 3) || (board [j] % 10 == 5))
				return true;
			else
				break;
		}
		
		j = king;
		while (board [j-11] != 99)
		{
			j -= 11;
			if (board [j] % 100 / 10 == color)
				break;
			if (board [j] == 0)
				continue;
			if ( (board [j] % 10 == 3) || (board [j] % 10 == 5))
				return true;
			else
				break;
		}
		
		j = king;
		while (board [j+11] != 99)
		{
			j +=11;
			if (board [j] % 100 / 10 == color)
				break;
			if (board [j] == 0)
				continue;
			if ( (board [j] % 10 == 3) || (board [j] % 10 == 5))
				return true;
			else
				break;
		}  
		
		//rook
		j = king;
		while (board [j-10] != 99)
		{
			j -= 10;
			if (board [j] % 100 / 10 == color)
				break;
			if (board [j] == 0)
				continue;
			if ((board [j] % 10 == 4) || (board [j] % 10 == 5))
				return true;
			else
				break;
		}
		j = king;
		while (board [j+10] != 99)
		{
			j += 10;
			if (board [j] % 100 / 10 == color)
				break;
			if (board [j] == 0)
				continue;
			if ((board [j] % 10 == 4) || (board [j] % 10 == 5))
				return true;
			else
				break;
		}
		j = king;
		while (board [j-1] != 99)	
		{
			j -=1;
			if (board [j] % 100 / 10 == color)
				break;
			if (board [j] == 0)
				continue;					
			if ((board [j] % 10 == 4) || (board [j] % 10 == 5))
				return true;
			else
				break;						
		}
		j = king;
		while (board [j+1] != 99)	
		{
			j +=1;
			if (board [j] % 100 / 10 == color)
				break;
			if (board [j] == 0)
				continue;
			if ((board [j] % 10 == 4) || (board [j] % 10 == 5))
				return true;
			else
				break;
		}
		
		//pawn
		if (color == 1)
		{
			if ((board [king-11] % 10 == 1) && (board [king-11] % 100 / 10 == 2))
				return true;
			if ((board [king- 9] % 10 == 1) && (board [king- 9] % 100 / 10 == 2))
				return true;	
		} else {
			if ((board [king+11] % 10 == 1) && (board [king+11] % 100 / 10 == 1))
				return true;
			if ((board [king+ 9] % 10 == 1) && (board [king+ 9] % 100 / 10 == 1)) 
				return true;
		}
		
		//king
		if ( board [king+ 1] % 10 == 6 )  
			return true;
		if ( board [king- 1] % 10 == 6 )   
			return true;
		if ( board [king+10] % 10 == 6 )   
			return true;
		if ( board [king-10] % 10 == 6 )   
			return true;
		if ( board [king+11] % 10 == 6 )   
			return true;
		if ( board [king-11] % 10 == 6 )   
			return true;
		if ( board [king+ 9] % 10 == 6 )   
			return true;
		if ( board [king- 9] % 10 == 6 )   
			return true;
		log.lwrite("ischeck(): returning false...\n");
		return false;
	}
	
	/*generates valid moves*/
	public void genmove () 
	{
		log.lwrite("genmove(): called\n");
		//minimax preparation
		deep++;
		if(deep%2!=0)//AI
		{
			log.lwrite("CPU moves\n");
			minimax[deep] = 2000.0f;
		}
		else		//Human
		{
			minimax[deep] = -2000.0f;
		}
		//
		log.lwrite("Turn if "+color+" \n");
		for (int i = 21; i < 99; i++)	
		{
			log.lwrite("i="+i+"\n");
			if (board [i] % 100 / 10 == color)	//check color
			{
				switch (board [i] % 10) 
				{
					case 1:	//pawn	
						if (color == 1)	//white pawn ?
						{
							log.lwrite("wpawn simple move\n");
							if (board [i-10] == 0)
								simulize ( i, i-10);		
							log.lwrite("wpawn capture left\n");
							if (board [i- 9] % 100 / 10 == 2)
								simulize ( i, i-9 );		
							log.lwrite("wpawn capture right\n");
							if (board [i-11] % 100 / 10 == 2)
								simulize ( i, i-11);			
							log.lwrite("wpawn double move\n");
							log.lwrite(board [i-10]+", "+board [i-20]);
							if ( (i>80) && ( ( board [i-10] == 0) && (board [i-20] == 0))) 
								simulize ( i, i-20);													
						} else {	//black pawn
							log.lwrite("bpawn simple move\n");
							if (board [i+10] == 0)
								simulize ( i, i+10);			
							log.lwrite("bpawn capture left\n");
							if (board [i+9] % 100 / 10 == 1)
								simulize (i, i+9);
							log.lwrite("bpawn capture right");
							if (board [i+11] % 100 / 10 == 1)
								simulize (i, i+11);				
							log.lwrite("bpawn double move");
							if ( (i<39) && ( (board [i+10] == 0) && (board [i+20] == 0)))
								simulize (i, i+20);								
						}					
						break;
					case 2:	//knight	
						log.lwrite("knight moves\n");
						simulize (i, i+12);							
						simulize (i, i-12);							
						simulize (i, i+21);							
						simulize (i, i-21);							
						simulize (i, i+19);							
						simulize (i, i-19);						
						simulize (i, i+8 );					
						simulize (i, i-8 );					
						break;
					case 5:	//queen
					case 3:	//bishop
						multisimulize ( i,  -9);
						multisimulize ( i, -11);
						multisimulize ( i,  +9);
						multisimulize ( i, +11);
						
						if (board [i] % 10 == 3)
							break;
					case 4:	//rook
						multisimulize (i, -10);
					 	multisimulize (i, +10);
						multisimulize (i,  -1);
						multisimulize (i,  +1);
						break;
					case 6:	//king	
						if ((board [i] / 100 == 1) && (! ischeck ()))
						{//castling
							if (((board [i+1] == 0) && (board [i+2] == 0)) && (board [i+3] / 100 == 1))
							{//little castling
								board [i+1] = board [i] % 100;
								board [i] = 0;
								if (!ischeck())
								{
									//king back
									board [i] = board [i+1];																											
									//move rook
									board [i + 1] = board [i + 3] % 100;
									board [i + 3] = 0;
									
									simulize (i, i+2);
									
									//takeback
									board [i + 3] = board [i + 1] + 100;
									board [i+1] = board [i];
								}
								//rebuild original position
								board [i] = board [i + 1] + 100;
								board [i + 1] = 0;	
							}
							if (((board [i-1] == 0) && (board [i-2] == 0)) && ((board [i-3] == 0) && (board [i-4] / 100 == 1)))
							{//big castling
								board [i-1] = board [i] % 100;
								board [i] = 0;														
								if (!ischeck())
								{
									//king back
									board [i] = board [i-1];																											
									//move rook
									board [i - 1] = board [i - 4] % 100;
									board [i - 4] = 0;
									
									simulize (i, i-2);
									
									//tackeback
									board [i - 4] = board [i - 1] + 100;
									board [i - 1] = board [i];
								}								
								//rebuild original position
								board [i] = board [i - 1] + 100;
								board [i - 1] = 0;
							}
						}
						simulize (i, i+1); 
						simulize (i, i-1);
						simulize (i, i+10);
						simulize (i, i-10);
						simulize (i, i+9);
						simulize (i, i-9);
						simulize (i, i+11);
						simulize (i, i-11);	
				}
			}			
			if ( i%10 == 8) i += 2;
		}
		deep--;
	}
	
	/**/
	private String respond(String mes)
	{
		log.lwrite("respond(): called with input '"+mes+"'\n");
		if(mes.startsWith(Protocol.codeStartGame))//request = new game
		{
			log.lwrite("New game requested...\n");
			mes = Protocol.codeAcknowledged;
			newgame();
		}
		else if(mes.startsWith(Protocol.codeQuerValidMove))//request = move validity
		{
			log.lwrite("Move validation requested...\n");
			StringTokenizer strtok = new StringTokenizer(mes,Protocol.codeSeparator);
			strtok.nextToken();
			String move = strtok.nextToken();
			log.lwrite("Is "+move+" valid?\n");
			StringTokenizer strtok2 = new StringTokenizer(move,"x");
			int start = Integer.parseInt(strtok2.nextToken());
			int end = Integer.parseInt(strtok2.nextToken());
			if(isvalid(start*100+end)) mes = execute(start, end);
			else mes = Protocol.codeRejected;
		}
		else if(mes.startsWith(Protocol.codeEndGame))
		{
			log.lwrite("PLayer quits...\nExiting...\n");
			mes = Protocol.codeAcknowledged;
			qFlag = false;
		}
		else//request = garbage message 
		{
			log.lwrite("Request not applicable...\n");
			mes = Protocol.codeRejected;
		}
		log.lwrite("respond(): return '"+mes+"'\n");
		return mes;
	}
	
	/*Runnable method run()
	 * It is actually the main "skeleton" of the AI program*/
	public void run()
	{
		log.lwrite("run(): started\n");
		String mes = null; 
		while(qFlag)
		{
			try 
			{
				mes = readMessage();
				if(mes == null)
				{
					mes = Protocol.codeRejected;
					log.lwrite("null received\n");
				}				
				pw.println(mes);
				pw.flush();
				log.lwrite("Response sent...\n");
				if(!qFlag)//ending game
				{
					log.lwrite("Cleaning up and quiting...\n");
					pw.close();
					br.close();
					log.close();
					this.socket.close();
					break;
				}
			} 
			catch (Exception e) 
			{
				log.lwrite("Error in run()...\nDetails: "+e.toString()+"\n");
				e.printStackTrace();				
				break;
			}
		}
		Thread.currentThread().interrupt();
		return;
	}
	
	//waits until something other than null is read, then returns the message
	private String readMessage() throws IOException
	{
		log.lwrite("readMessage(): called\n");
		String res = null;
		while((res = br.readLine())!=null)
		{
			log.lwrite("run(): Message received: "+res+"\n");
			res = respond(res);
			log.lwrite("readMessage(): return '"+res+"'\n");
			return res;
		}
		return null;
	}
	
	//evaluate a position
	public float evaluation ( ) 
	{
		float value = 0;
		float figur = 0;
		
		for (int i = 21; i < 99; i++)
		{
			if ( board [i] != 0 )
			{	
				//material
				switch (board [i] % 10)
				{
					case 1:
						figur = 1.0f;
						break;
					case 2:
					case 3:
						figur = 3.0f;
						break;
					case 4:
						figur = 4.5f;
						break;
					case 5:
						figur = 9.0f;
						break;
					case 6:
						figur = 0.0f;
				}
				
				//position
				figur += posvalues [i];
				
				if ( board [i] % 100  / 10 == color)
					value += figur;		
				else
					value -= figur;
			}	
			
			if ( i%10 == 8)
				i += 2;
		}
		return value;	
	}
}
