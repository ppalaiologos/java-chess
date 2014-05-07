package clientPackage;

public class Protocol 
{
	//server IP
		public static String IP = "localhost";
		
		//Communication global
		public static int PORT = 8080;		//the port
		
		//communication codes
		/* Structure of messages CODE{"|"MOVE}? 
		 * MOVE: {index}x{index} */
		public static String codeStartGame = "?STR?";
		public static String codeEndGame = "?END?";
		public static String codeQuerValidMove = "?MOV?";
		public static String codeAcknowledged = "!ACK!";
		public static String codeAIMove = "!SMO!";
		public static String codeRejected = "!NOP!";
		public static String codeSeparator = "|";
		public static String codeBlackWins = "!BLK!";
		public static String codeWhiteWins = "!WHT!";
		public static String codeDraw = "!DRW!";

}
