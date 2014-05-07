package clientPackage;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class Board extends JFrame
{
	//UID
	private static final long serialVersionUID = 43396044059589187L;

	/*Globals*/
	BorderLayout bl = null;
	Container cont = null;
	Log log = null;
	BoardCanvas chessBoard;
	
	//the actions controlling the buttons
	private class newAction extends AbstractAction
	{
		private static final long serialVersionUID = 1953684388529973247L;
		//
		protected newAction(String name, String desc)
		{
			super(name);
			putValue(SHORT_DESCRIPTION, desc);
		}
		//
		public void actionPerformed(ActionEvent arg0) 
		{
			log.lwrite("Querying service for new game...\n");
			//
			String res = Client.exchange(Protocol.codeStartGame);
			//
			if(res.startsWith(Protocol.codeAcknowledged)) 
			{
				log.lwrite("Game about to start...\n");
				chessBoard.newgame();
			}			
			else
			{
				log.lwrite("New game rejected or error\n answered: "+res+"\n");
			}			
		}		
	}	
	private newAction act1 = new newAction("New game", "Start a new game...");
	
	private class quitAction extends AbstractAction
	{
		private static final long serialVersionUID = -9039259256325789807L;
		//
		protected quitAction(String name, String desc)
		{
			super(name);
			putValue(SHORT_DESCRIPTION, desc);
		}
		//
		public void actionPerformed(ActionEvent arg0)
		{
			log.lwrite("Requesting end of game...\n");
			String res = Client.exchange(Protocol.codeEndGame);
			//
			log.lwrite("Ending game and exiting...\n");
			log.close();
			chessBoard.log.close();
			//
			try {
				Client.socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.exit(0);	
		}		
	}
	private quitAction quit = new quitAction("Quit game", 
			"Quit current game and exit application...");
	
	/*Buttons*/
	JButton newGame = new JButton(act1);
	JButton endGame = new JButton(quit);
	
	/*Left and right frames*/
	JPanel black = new JPanel();
	JPanel white = new JPanel();
	
	/*Constructor*/
	public Board()
	{
		//create the log file
		log = new Log("boardLog.file");
		
		//basic config
		this.setTitle("My Chess App");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setSize(352,410);
		this.setLocationRelativeTo(null);
		this.setVisible(true);
		this.setResizable(true);
			
		//set up container
		cont = this.getContentPane();
		
		/*Interface area config:
		 * - Center: board
		 * - North: New game button
		 * - South: Quit button
		 * - East and West: Banners*/
		black.setSize(200, 400);
		black.setBackground(Color.black);
		white.setSize(200, 400);
		white.setBackground(Color.white);
		
		bl = new BorderLayout();
		cont.setLayout(bl);
		chessBoard = new BoardCanvas();
		chessBoard.setSize(400, 400);
		cont.add(chessBoard, BorderLayout.CENTER);
		cont.add(black, BorderLayout.EAST);
		cont.add(white, BorderLayout.WEST);
		cont.add(newGame, BorderLayout.NORTH);
		cont.add(endGame, BorderLayout.SOUTH);
	}
	
	public static void shout(String message)
	{
		JOptionPane.showMessageDialog(null, message, null, JOptionPane.INFORMATION_MESSAGE);
	}
}