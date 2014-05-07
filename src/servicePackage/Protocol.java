package servicePackage;

public class Protocol 
{	
	//Communication globals
	public static int PORT = 8080;		//the port
	
	//communication codes
	/* Structure of messages CODE"|"{MOVE}? 
	 * The structure of MOVE follows the chess algebraic notation */
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
