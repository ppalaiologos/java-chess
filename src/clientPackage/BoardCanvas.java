package clientPackage;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.StringTokenizer;

import javax.swing.ImageIcon;

public class BoardCanvas extends Canvas implements MouseMotionListener,
		MouseListener 
{
	//UID
	private static final long serialVersionUID = 4546804433641633612L;

	/*globals*/
	//the chess board array
	int[] board = new int[120]; 
	
	//the image handlers
	Image[] images = new Image[18];
	
	//mouse tracking
	int start = 21;  //the start field
	int alt = 21; 	 //new field?
	int end = 21; 	 //end field
	int x = 0; 		 //x coord
	int y = 0; 		 //y coord
	
	//logging
	clientPackage.Log log = null;
	
	/*Constructor*/
	public BoardCanvas()	
	{
		//logging
		log = new Log("canvasLog.file");
		
		//load the images
		log.lwrite("Loading images...\n");
		images[1] = new ImageIcon("pieces/wp.gif").getImage();
		images[2] = new ImageIcon("pieces/wn.gif").getImage();
		images[3] = new ImageIcon("pieces/wb.gif").getImage();
		images[4] = new ImageIcon("pieces/wr.gif").getImage();
		images[5] = new ImageIcon("pieces/wq.gif").getImage();
		images[6] = new ImageIcon("pieces/wk.gif").getImage();
		images[11] = new ImageIcon("pieces/bp.gif").getImage();
		images[12] = new ImageIcon("pieces/bn.gif").getImage();
		images[13] = new ImageIcon("pieces/bb.gif").getImage();
		images[14] = new ImageIcon("pieces/br.gif").getImage();
		images[15] = new ImageIcon("pieces/bq.gif").getImage();
		images[16] = new ImageIcon("pieces/bk.gif").getImage();
		//add mouse listeners
		newgame();
		addMouseListener(this);
		addMouseMotionListener(this);
	}	
	
	/*mouseClicked: tracks the cell of the board that was "clicked"
	 * and checks if it is a move. Then sends the appropriate message 
	 * to the service and, depending on the response, makes the move or 
	 * resets the board. */
	@Override
	public void mouseClicked(MouseEvent arg0) 
	{
		log.lwrite("mouseClicked(): just said...\n");
		//x coordinate
		x = arg0.getX()/40;//calculate row of board
		if(x<0) x=0; //error checking 
		if(x>7) x=7; //error checking
		//y coordinate
		y = arg0.getY()/40;//calculate column of board
		if(y<0) y=0; //error checking
		if(y>7) y=7; //error checking
		//construct the board index
		alt = 21 + y*10 +x;		
		Graphics g = getGraphics();
		//then, check if a move is beginning or ending
		if(start==21)//if beginning a move
		{
			log.lwrite("Initiating move! Starting field is :"+alt);
			//typically alt becomes is start so copy it
			start = alt;
			//mark the start field
			g.setColor(Color.BLUE);
			g.fillRect(x*40, y*40, 40, 40);
			try
			{
				g.drawImage(images[(board[start]%100)-10], x*40, y*40, 40, 40, null);
			}
			catch(IndexOutOfBoundsException e){}
		}
		else //if already began another move
		{//alt is the destination coord aka end OR alt is start
			if(start == alt)//if clicking the same square 
			{//means he takes back the move
				log.lwrite("Took back the move...\n");
				repaint();
			}
			else//else, means alt is a new square
			{//and so must check if valid and 
				log.lwrite("It *is* a move! Destination is: "+end+"\n");
				end = alt;
				g.setColor(Color.GREEN);
				g.fillRect(x*40, y*40, 40, 40);
				try
				{
					g.drawImage(images[(board[alt]%100)-10], x*40, y*40, 40, 40, null);
				}
				catch(IndexOutOfBoundsException e){}
				//send validity query to server		
				boolean ex = false;
				String msg = Protocol.codeQuerValidMove+"|"+start+"x"+end;
				log.lwrite("Qeyring service for move validity. Message sent: "+msg+"\n");
				String res = Client.exchange(msg);
				//if valid
				paintField(start);
				paintField(end);
				log.lwrite("Reply: "+res+"\n");
				if((!res.startsWith(Protocol.codeRejected))&&(res != null))
				{	
					log.lwrite("Move accepted! Results pending...\n");
					execute(start, end);
					if((res.startsWith(Protocol.codeAIMove))||(res.startsWith(Protocol.codeBlackWins)))
					{
						log.lwrite("AI move is being processed...\n");
						//tok1[0]="code", tok[1]="move"
						StringTokenizer tok1 = new StringTokenizer(res,Protocol.codeSeparator);
						tok1.nextToken();
						//tok2[0]=start, tok2[1]=end
						StringTokenizer tok2 = new StringTokenizer(tok1.nextToken(),"x");
						int start = Integer.parseInt(tok2.nextToken());
						int end = Integer.parseInt(tok2.nextToken());
						log.lwrite("Start="+start+", end="+end+"\n");
						execute(start,end);
						if(res.startsWith(Protocol.codeBlackWins)) Board.shout("Black wins!!");
					}
					else
					{
						if(res.startsWith(Protocol.codeWhiteWins))
						{
							Board.shout("White wins!!");
						}
						else
						{
							Board.shout("It's a draw!!");
						}
					}
				}
			}
			start = 21;
			alt = start;
			end = start;
		}		
	}
	
	/* This function actually executes the move */
	public void execute(int start, int end)
	{
		log.lwrite("execute(): executing "+start+" to "+end+"\n");
		//swap contents of the fields
		board[end] = board[start];
		board[start] = 0;
		//castling		
		if (board [end] % 10 == 6)
		{
			if( end == start + 2)
			{	//little
				board [start + 1] = board [start + 3] % 100;
				board [start + 3] = 0;
					
				paintField (start + 3);
				paintField (start + 1);
			}
			if( end == start - 2)
			{	//big
				board [start - 1] = board [start - 4] % 100;
				board [start - 4] = 0;
					
				paintField (start - 4);
				paintField (start - 1);			
			}
		}
		
		//paint the results
		paintField(end);
		paintField(start);
	}
	
	/* Here we paint the chess board;
	 * paint() iterates through the board fields and calls paintField();
	 * paint() loops over the border fields */
	public void paint(Graphics g)
	{
		for(int i=21; i<99; i++)
		{
			paintField(i);//call paintField
			if(i%10 == 8) i+=2; //loop over the border
		}
	}
	
	/* This function finds the indexed field 
	 * and paints it depending on its location */
	public void paintField(int index)
	{
		//load graphics reference
		Graphics g = getGraphics();
		//create x and y off index
		int x = (index-21)%10;
		int y = (index-21)/10;
		//paint field
		if(((x*11)+y)%2 == 0)g.setColor(Color.WHITE);
		else g.setColor(Color.BLACK);
		g.fillRect(x*40,y*40,40,40);
		//paint image
		try
		{
			g.drawImage(images[(board[index]%100)-10],x*40,y*40,40,40, null);
		}
		catch(IndexOutOfBoundsException e)
		{}
	}

	/* Resets everything */
	public void newgame()
	{
		log.lwrite("newgame(): Recreating board...\n");
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
		
		//copy starting position to board
		for(int i=0; i<120; i++) board[i] = org[i];
		//update screen
		repaint();
	}
	
	//unused functions
	public void mouseEntered(MouseEvent arg0) {}
	public void mouseExited(MouseEvent arg0) {}
	public void mousePressed(MouseEvent arg0) {}
	public void mouseReleased(MouseEvent arg0) {}
	public void mouseDragged(MouseEvent arg0) {}
	public void mouseMoved(MouseEvent arg0) {}

}
