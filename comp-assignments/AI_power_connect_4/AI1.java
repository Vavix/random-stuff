import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class AI1 {
	static char board[][] = new char[8][8];
	static char bkpBoard[][] = new char[8][8];
	static char color = '0'; //default agent color is white
	static char opponentColor = '1'; //default opponent color is black
	static move myNextMove;

	public static void main(String[] args) throws Exception {
		initializeBoard(board);
		initializeBoard(bkpBoard);
		selectColor('0');
		System.out.println("initial board:");
		printBoard(board);
//		timePerf(5, true);
		playGame(args);
	}

	public static void timePerf(int depth, boolean alphabeta) throws Exception {
		double sum=0;
		if (alphabeta) {
			for (int i =0; i<100; i++) {
				double t1 = System.nanoTime();
				maxV(depth, board, Integer.MIN_VALUE, Integer.MAX_VALUE);
				double t2 = System.nanoTime();
				sum+=(t2-t1);
			}
		}
		else {
			for (int i =0; i<100; i++) {
				double t1 = System.nanoTime();
				minimax(true, depth, board);
				double t2 = System.nanoTime();
				sum+=(t2-t1);
			}
		}
		System.out.println("average over 100 tries:"+(sum/100));
	}
	public static void playGame(String[] args) throws Exception {

		String sentence;   
		Socket clientSocket = new Socket("localhost", 12345);
		DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
		BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

		System.out.println(args[0]+" "+args[1]+"\n");
		boolean black=false;
		if (args[1].equals("black"))
			black = true;
		
		outToServer.writeBytes(args[0]+" "+args[1]+"\n");
		sentence = inFromServer.readLine();
		System.out.println("FROM SERVER GAME INFO: " + sentence);
		
		char ac = '0';
		char oc = '1';
		
		if (black) {
			//set agent and opponent colors
			ac = '1';
			oc = '0';
		}
		
		//if black receive first
		while (true) {
			printBoard(board);
			//check if someone won
			Scores s = calcNumWin(color, opponentColor);
			if (s.agentScore>0 || s.oppScore>0) {
				if (s.agentScore==s.oppScore)
					System.out.println("tie");
				else if (s.agentScore>s.oppScore)
					System.out.println("I win");
				else
					System.out.println("opp wins");

				break;
			}
			
			if (black) {
				selectColor(ac);
				sentence = inFromServer.readLine();
				System.out.println("FROM SERVER: " + sentence);
				move m = parseMove(sentence);
				
				//select opponent color temporarily to apply their move
				selectColor(oc);
				board = applyMove(m, board);
				selectColor(ac);
			}

			//determine and apply my move
			printBoard(board);
			maxV(4, board, Integer.MIN_VALUE, Integer.MAX_VALUE);
			board = applyMove(myNextMove, board);
			
			//send my move
			String moveString = constructMoveString(myNextMove);
			System.out.println("TO SERVER:"+moveString);
			outToServer.writeBytes(moveString);
			
			//confirm receipt of my move
			sentence = inFromServer.readLine();
			System.out.println("FROM SERVER (CONFIRMATION): " + sentence);
			
			printBoard(board);

			
			if (!black) {
				//receive answer from black, if we are white
				sentence = inFromServer.readLine();
				System.out.println("FROM SERVER: " + sentence);
				move m=parseMove(sentence);

				//select opponent color temporarily to apply their move
				selectColor(oc);
				board = applyMove(m, board);
				selectColor(ac);
			}
		}

		clientSocket.close();  
	}

	public static move getHumanMove(Scanner reader) {
		//get user input for a
		System.out.println("1: D 2:L 3:R");
		int a=reader.nextInt();
		if (a==1) {
			System.out.println("column:");
			int c = reader.nextInt();
			return new move(c);
		}
		else if (a==2) {
			System.out.println("x:");
			int x = reader.nextInt();
			System.out.println("y:");
			int y = reader.nextInt();
			return new move('L', x, y);
		}
		else {
			System.out.println("x:");
			int x = reader.nextInt();
			System.out.println("y:");
			int y = reader.nextInt();
			return  new move('R', x, y);
		}
	}

	public static int minimax(boolean myTurn, int depth, char[][] b) throws Exception {
		if (depth==0) {
			Scores s = calcH(color, opponentColor, b);
			//System.out.println("depth 0, returning score: "+ (s.agentScore-s.oppScore));
			return (s.agentScore-s.oppScore);
		}
		else if (myTurn) {
			//System.out.println("my turn");
			List<move> l = getPossibleMoves(b);
			move bestMove = null;
			int max = Integer.MIN_VALUE;
			for (move m : l) {
				char[][] newb = applyMove(m, b);
				//				printBoard(newb);
				int val = minimax(false, depth-1, newb);
				if (val>max) {
					bestMove = m;
					max = val;
				}

			}
			myNextMove = bestMove;
			return (max);
		}
		else {
			//System.out.println("opp turn");
			char oc = opponentColor;
			char c = color;
			selectColor(oc);
			List<move> l = getPossibleMoves(b);
			move bestMove = null;
			int min = Integer.MAX_VALUE;

			for (move m : l) {
				selectColor(oc);
				char[][] newb = applyMove(m, b);

				selectColor(c);
				int val = minimax(true, depth-1, newb);
				if (val<min) {
					bestMove = m;
					min = val;
				}
			}
			selectColor(c);
			return (min);
		}
	}

	public static move parseMove(String s) {
		if (s.charAt(0)=='D')
			return (new move(s.charAt(2)-'0'));
		else return (new move(s.charAt(0), (s.charAt(2) - '0'), (s.charAt(4) - '0')));
	}

	public static String constructMoveString(move m) {
		if (m.type=='D') {
			return "D "+ m.column+"\n";
		}
		else {
			return m.type+" "+m.row+" "+m.column;
		}
	}

	public static int maxV(int depth, char[][] b, int alpha, int beta) throws Exception {
		if (depth==0) {
			Scores s = calcBetterH(color, opponentColor, b);
			return (s.agentScore-s.oppScore);
		}
		int v = Integer.MIN_VALUE;

		//System.out.println("my turn");
		List<move> l = getPossibleMoves(b);
		move bestMove = null;
		for (move m : l) {
			char[][] newb = applyMove(m, b);
			try{
				System.out.println(newb[1][1]);
			} catch (Exception e) {
				System.out.println("exc move:"+m.type + " column"+m.column+" row "+m.row);
			}

			int temp = minV(depth-1, newb, alpha, beta);
			if (temp>v) {
				bestMove = m;
				v = temp;
			}
			//			printBoard(newb);
			if (v>=beta) {
				myNextMove = m;
				return v;
			}
			alpha = Math.max(alpha, v);
		}
		l.clear();

		myNextMove = bestMove;
		return v;

	}

	public static int minV(int depth, char[][] b, int alpha, int beta) throws Exception {
		if (depth==0) {
			Scores s = calcBetterH(color, opponentColor, b);
			return (s.agentScore-s.oppScore);
		}

		int v = Integer.MAX_VALUE;

		//System.out.println("opp turn");
		char oc = opponentColor;
		char c = color;
		selectColor(oc);
		List<move> l = getPossibleMoves(b);
		move bestMove = null;
		for (move m : l) {
			selectColor(oc);
			char[][] newb = applyMove(m, b);
			selectColor(c);
			int temp = maxV(depth-1, newb, alpha, beta);
			if (temp<v) {
				bestMove = m;
				v = temp;
			}
			selectColor(c);
			if (v<=alpha) {
				myNextMove = m;
				return v;
			}
			alpha = Math.min(beta, v);
		}

		myNextMove = bestMove;
		return v;
	}


	public static char[][] applyMove(move m, char[][] b) {
		if (m.type=='D') return D(m.column, b);
		else if (m.type=='R') return R(m.row, m.column, b);
		else if (m.type=='L') return L(m.row, m.column, b);
		else {
			System.out.println("bad move fields. type:"+m.type);
			return null;
		}
	}

	public static List<move> getPossibleMoves(char[][] b) {
		List<move> l = new ArrayList<move>();
		copyFromTo(b, bkpBoard);
		for (int i=0; i<8; i++) {
			if (bkpBoard[0][i] == ' ') {
				l.add(new move(i+1));
			}
		}

		for (int i=0; i<8; i++) {
			for (int j=0; j<6; j++) {
				if (R(i+1, j+1, b)!=null) {
					l.add(new move('R', i+1, j+1));
					copyFromTo(bkpBoard, b);
				}

				if (L(i+1, j+3, b)!=null) {
					l.add(new move('L', i+1, j+3));
					copyFromTo(bkpBoard, b);
				}
			}
		}

		return l;
	}

	public static void copyFromTo(char[][] from, char[][] to) {
		for(int i = 0; i < 8; i++){
			for (int j = 0; j < 8; j++){
				to[i][j] = from[i][j];
			}
		}
	}

	public static Scores calcH(char c, char oc, char[][] b) {
		Scores s = new Scores();
		int aCount=0, oCount=0;
		int i = 0, j = 0;
		//check rows
		for (j=0; j<8; j++) {
			while (i<8) {
				if (b[j][i] == ' ') {//ensure i advances
					i++;
					continue;
				}

				while ((i<8) && (b[j][i] == c)) {
					aCount++;
					i++;
				}
				if (aCount>1) {
					s.addWin();
				}

				while ((i<8) && (b[j][i] == oc)) {
					oCount++;
					i++;
				}
				if (oCount>1) {
					s.addOWin();
				}

				aCount=0;
				oCount=0;
			}
			i=0;
		}

		j=0;
		//check columns
		for (i=0; i<8; i++) {
			while (j<8) {
				if (b[j][i] == ' ') {//ensure i advances
					j++;
					continue;
				}

				while ((j<8) && (b[j][i] == c)) {
					aCount++;
					j++;
				}
				if (aCount>1) {
					s.addWin();
				}

				while ((j<8) && (b[j][i] == oc)) {
					oCount++;
					j++;
				}
				if (oCount>1) {
					s.addOWin();
				}

				aCount=0;
				oCount=0;
			}
			j=0;
		}

		for (i=1; i<8; i++) {
			int k=i;
			j=0;
			//check top / diagonals
			while (k>=0 && j<8) {
				if (b[k][j] == ' ') {//ensure i and j advance
					j++;
					k--;
					continue;
				}

				while ((j<8) && (k>=0) && (b[k][j] == c)) {
					aCount++;
					j++;
					k--;
				}
				if (aCount>1) {
					s.addWin();
				}

				while ((j<8) && (k>=0) && (b[k][j] == oc)) {
					oCount++;
					j++;
					k--;
				}
				if (oCount>1) {
					s.addOWin();
				}

				aCount=0;
				oCount=0;

			}

			//check top \ diagonals
			j=7;
			k=i;
			while (k>=0 && j>=0) {
				if (b[k][j] == ' ') {//ensure i and j advance
					j--;
					k--;
				}

				while ((j>=0) && (k>=0) && (b[k][j] == c)) {
					aCount++;
					j--;
					k--;
				}
				if (aCount>1) {
					s.addWin();
				}

				while ((j>=0) && (k>=0) && (b[k][j] == oc)) {
					oCount++;
					j--;
					k--;
				}
				if (oCount>1) {
					s.addOWin();
				}

				aCount=0;
				oCount=0;

			}
			k=i-1;
			j=0;
			//check bottom \ diagonals
			while (k<8 && j<8) {
				if (b[k][j] == ' ') {//ensure i and j advance
					j++;
					k++;
					continue;
				}

				while ((j<8) && (k<8) && (b[k][j] == c)) {
					aCount++;
					j++;
					k++;
				}
				if (aCount>1) {
					//				System.out.println("adding win");
					s.addWin();
				}

				while ((j<8) && (k<8) && (b[k][j] == oc)) {
					//				System.out.println("adding oc at j:"+j+" and k:"+k);
					oCount++;
					j++;
					k++;
				}
				if (oCount>1) {
					//				System.out.println("adding owin");
					s.addOWin();
				}

				aCount=0;
				oCount=0;
			}

			//check bottom / diagonals
			k=i-1;
			j=7;
			while (k<8 && j>=0) {
				if (b[k][j] == ' ') {//ensure i and j advance
					j--;
					k++;
					continue;
				}

				while ((j>=0) && (k<8) && (b[k][j] == c)) {
					aCount++;
					j--;
					k++;
				}
				if (aCount>1) {
					s.addWin();
				}

				while ((j>=0) && (k<8) && (b[k][j] == oc)) {
					oCount++;
					j--;
					k++;
				}
				if (oCount>1) {
					s.addOWin();
				}

				aCount=0;
				oCount=0;
			}
		}
		return s;
	}

	public static Scores calcBetterH(char c, char oc, char[][] b) {
		Scores s = new Scores();
		int aCount=0, oCount=0, aMax=0, oMax=0;
		int i = 0, j = 0;
		//check rows
		for (j=0; j<8; j++) {
			while (i<8) {
				if (b[j][i] == ' ') {//ensure i advances
					i++;
					continue;
				}

				while ((i<8) && (b[j][i] == c)) {
					aCount++;
					i++;
				}
				if (aCount+2>aMax) {
					aMax=aCount+2;
				}

				while ((i<8) && (b[j][i] == oc)) {
					oCount++;
					i++;
				}
				if (oCount+2>oMax) {
					oMax=oCount+2;
				}

				aCount=0;
				oCount=0;
			}
			i=0;
		}

		j=0;
		//check columns
		for (i=0; i<8; i++) {
			while (j<8) {
				if (b[j][i] == ' ') {//ensure i advances
					j++;
					continue;
				}

				while ((j<8) && (b[j][i] == c)) {
					aCount++;
					j++;
				}
				if (aCount>aMax) {
					aMax=aCount;
				}

				while ((j<8) && (b[j][i] == oc)) {
					oCount++;
					j++;
				}
				if (oCount>oMax) {
					oMax=oCount;
				}

				aCount=0;
				oCount=0;
			}
			j=0;
		}

		for (i=1; i<8; i++) {
			int k=i;
			j=0;
			//check top / diagonals
			while (k>=0 && j<8) {
				if (b[k][j] == ' ') {//ensure i and j advance
					j++;
					k--;
					continue;
				}

				while ((j<8) && (k>=0) && (b[k][j] == c)) {
					//					System.out.println("adding c at j:"+j+" and k:"+k);
					aCount++;
					j++;
					k--;
				}
				if (aCount+1>aMax) {
					aMax=aCount+1;
				}

				while ((j<8) && (k>=0) && (b[k][j] == oc)) {
					oCount++;
					j++;
					k--;
				}
				if (oCount+1>oMax) {
					oMax=oCount+1;
				}

				aCount=0;
				oCount=0;

			}

			//check top \ diagonals
			j=7;
			k=i;
			while (k>=0 && j>=0) {
				if (b[k][j] == ' ') {//ensure i and j advance
					j--;
					k--;
				}

				while ((j>=0) && (k>=0) && (b[k][j] == c)) {
					aCount++;
					j--;
					k--;
				}
				if (aCount+1>oMax) {
					aMax=aCount+1;
				}

				while ((j>=0) && (k>=0) && (b[k][j] == oc)) {
					oCount++;
					j--;
					k--;
				}
				if (oCount+1>oMax) {
					oMax=oCount+1;
				}

				aCount=0;
				oCount=0;

			}
			k=i-1;
			j=0;
			//check bottom \ diagonals
			while (k<8 && j<8) {
				if (b[k][j] == ' ') {//ensure i and j advance
					j++;
					k++;
					continue;
				}

				while ((j<8) && (k<8) && (b[k][j] == c)) {
					aCount++;
					j++;
					k++;
				}
				if (aCount+1>aMax) {
					aMax=aCount+1;
				}

				while ((j<8) && (k<8) && (b[k][j] == oc)) {
					oCount++;
					j++;
					k++;
				}
				if (oCount+1>oMax) {
					oMax=oCount+1;
				}

				aCount=0;
				oCount=0;
			}

			//check bottom / diagonals
			k=i-1;
			j=7;
			while (k<8 && j>=0) {
				if (b[k][j] == ' ') {//ensure i and j advance
					j--;
					k++;
					continue;
				}

				while ((j>=0) && (k<8) && (b[k][j] == c)) {
					aCount++;
					j--;
					k++;
				}
				if (aCount+1>aMax) {
					aMax=aCount+1;
				}

				while ((j>=0) && (k<8) && (b[k][j] == oc)) {
					oCount++;
					j--;
					k++;
				}
				if (oCount+1>oMax) {
					oMax=oCount+1;
				}

				aCount=0;
				oCount=0;
			}
		}

		s.agentScore=aMax;
		s.oppScore=oMax;
		return s;
	}


	public static void initializeBoard(char[][] b) {
		for (int i=0; i<8; i++) {
			for (int j=0; j<8; j++) {
				b[i][j] = ' ';
			}
		}
	}

	public static void selectColor(char c) throws InvalidColorException {
		if (c=='0') {
			color = '0';
			opponentColor = '1';
		}
		else if (c=='1') {
			color = '1';
			opponentColor='0';
		}
		else throw new InvalidColorException();

	}

	public static boolean D(int column) { //column is 1-indexed
		if (column<1 || column>8) return false;
		if (board[0][column-1]!=' ') return false; //column full

		int i=0;
		while (i<7 && board[i+1][column-1] == ' ') {
			i++;
		}
		board[i][column-1] = color;
		return true;
	}

	public static char[][] D(int column, char[][] b) { //column is 1-indexed
		char[][] c = new char[8][8];
		copyFromTo(b, c);
		if (column<1 || column>8) return null;
		if (c[0][column-1]!=' ') return null; //column full

		int i=0;
		while (i<7 && c[i+1][column-1] == ' ') {
			i++;
		}
		c[i][column-1] = color;
		return c;
	}

	private static void refactorColumn(int column) {//column is 0-indexed
		//		System.out.println("refactoring column "+(column+1));
		//find the top and the bottom pieces that will fall down
		int bottom = 7;
		while (board[bottom][column] != ' ') {
			if (bottom<0) return; //nothing to do here
			bottom--;
		}
		//		System.out.println("found bottom: "+bottom);
		int bottomPiece = bottom;
		while (bottomPiece>=0 && board[bottomPiece][column] == ' ') {
			bottomPiece--; //advance to the lowest piece to drop
		}

		if (bottomPiece == -1) return; //no action needed

		int dropHeight = bottom - bottomPiece;

		//start the 'dropping' process
		for (int i=bottom; i>=dropHeight; i--) {
			board[i][column] = board[i-dropHeight][column];
			board[i-dropHeight][column] = ' ';
		}
	}

	private static void refactorColumn(int column, char[][] b) {//column is 0-indexed
		//		System.out.println("refactoring column "+(column+1));
		//		printBoard();
		//find the top and the bottom pieces that will fall down
		int bottom = 7;
		while (b[bottom][column] != ' ') {
			if (bottom<0) return; //nothing to do here
			bottom--;
		}
		int bottomPiece = bottom;
		while (bottomPiece>=0 && b[bottomPiece][column] == ' ') {
			bottomPiece--; //advance to the lowest piece to drop
		}

		if (bottomPiece == -1) return; //no action needed

		int dropHeight = bottom - bottomPiece;
		//		System.out.println("found bottomPiece: " + bottomPiece);

		//start the 'dropping' process
		for (int i=bottom; i>=dropHeight; i--) {
			b[i][column] = b[i-dropHeight][column];
			b[i-dropHeight][column] = ' ';
		}
	}

	public static boolean L(int x, int y) {//x and y 1-indexed
		x=x-1;
		y=y-1;
		if (y<2) return false;
		if (board[x][y]==color && board[x][y-1]==color) { //two agent pieces in a row followed by a...
			if ((board[x][y-2]==color) && (y>2) && (board[x][y-3]==opponentColor)) {//third agent piece followed by opponent
				if (y-4<0) {//pushing opponent piece off the grid
					board[x][y-3]=color;
					board[x][y]=' ';
					refactorColumn(y);
				}
				else if (board[x][y-4]==' ') {//pushing opponent piece to free spot
					board[x][y-4]=opponentColor;
					board[x][y-3]=color;
					board[x][y]=' ';
					refactorColumn(y);
					refactorColumn(y-4);
				}
				else {
					return false;
				}
			}
			else if (board[x][y-2]==opponentColor) {//opponent piece
				if (y-3<0) {//pushing opponent piece off the grid
					board[x][y-2]=color;
					board[x][y]=' ';
					refactorColumn(y);
				}
				else if (board[x][y-3]==' ') {//pushing opponent piece to free spot
					board[x][y-3]=opponentColor;
					board[x][y-2]=color;
					board[x][y]=' ';
					refactorColumn(y);
					refactorColumn(y-3);
				}
				else {
					return false;
				}
			}
			else {
				return false;
			}
		}
		else {
			return false;
		}
		return true;
	}

	public static char[][] L(int x, int y, char[][] a) {//x and y 1-indexed
		char[][] b = new char[8][8];
		copyFromTo(a, b);
		x=x-1;
		y=y-1;
		if (y<2) return null;
		if (b[x][y]==color && b[x][y-1]==color) { //two agent pieces in a row followed by a...
			if ((b[x][y-2]==color) && (y>2) && (b[x][y-3]==opponentColor)) {//third agent piece followed by opponent
				if (y-4<0) {//pushing opponent piece off the grid
					b[x][y-3]=color;
					b[x][y]=' ';
					refactorColumn(y, b);
				}
				else if (b[x][y-4]==' ') {//pushing opponent piece to free spot
					b[x][y-4]=opponentColor;
					b[x][y-3]=color;
					b[x][y]=' ';
					refactorColumn(y, b);
					refactorColumn(y-4, b);
				}
				else {
					return null;
				}
			}
			else if (b[x][y-2]==opponentColor) {//opponent piece
				if (y-3<0) {//pushing opponent piece off the grid
					b[x][y-2]=color;
					b[x][y]=' ';
					refactorColumn(y, b);
				}
				else if (b[x][y-3]==' ') {//pushing opponent piece to free spot
					b[x][y-3]=opponentColor;
					b[x][y-2]=color;
					b[x][y]=' ';
					refactorColumn(y, b);
					refactorColumn(y-3, b);
				}
				else {
					return null;
				}
			}
			else {
				return null;
			}
		}
		else {
			return null;
		}
		return b;
	}

	public static boolean R(int x, int y) {//x and y 1-indexed
		x=x-1;
		y=y-1;
		if (y>5) return false;
		if (board[x][y]==color && board[x][y+1]==color) { //two agent pieces in a row followed by a...
			if ((board[x][y+2]==color) && (y<5) && (board[x][y+3]==opponentColor)) {//third agent piece followed by opponent
				if (y+4>7) {//pushing opponent piece off the grid
					board[x][y-3]=color;
					board[x][y]=' ';
					refactorColumn(y);
				}
				else if (board[x][y+4]==' ') {//pushing opponent piece to free spot
					board[x][y+4]=opponentColor;
					board[x][y+3]=color;
					board[x][y]=' ';
					refactorColumn(y);	
					refactorColumn(y+4);
				}
				else {
					return false;
				}
			}
			else if (board[x][y+2]==opponentColor) {//opponent piece
				if (y+3>7) {//pushing opponent piece off the grid
					board[x][y+2]=color;
					board[x][y]=' ';
					refactorColumn(y);
				}
				else if (board[x][y+3]==' ') {//pushing opponent piece to free spot
					board[x][y+3]=opponentColor;
					board[x][y+2]=color;
					board[x][y]=' ';
					refactorColumn(y);
					refactorColumn(y+3);
				}
				else {
					return false;
				}
			}
			else {
				return false;
			}
		}
		else {
			return false;
		}
		return true;
	}

	public static char[][] R(int x, int y, char[][] a) {//x and y 1-indexed
		x=x-1;
		y=y-1;
		if (y>5) return null;
		char[][] b = new char[8][8];
		copyFromTo(a, b);
		if (b[x][y]==color && b[x][y+1]==color) { //two agent pieces in a row followed by a...
			if ((b[x][y+2]==color) && (y<5) && (b[x][y+3]==opponentColor)) {//third agent piece followed by opponent
				if (y+4>7) {//pushing opponent piece off the grid
					b[x][y-3]=color;
					b[x][y]=' ';
					refactorColumn(y, b);
				}
				else if (b[x][y+4]==' ') {//pushing opponent piece to free spot
					b[x][y+4]=opponentColor;
					b[x][y+3]=color;
					b[x][y]=' ';
					refactorColumn(y, b);	
					refactorColumn(y+4, b);
				}
				else {
					return null;
				}
			}
			else if (b[x][y+2]==opponentColor) {//opponent piece
				if (y+3>7) {//pushing opponent piece off the grid
					b[x][y+2]=color;
					b[x][y]=' ';
					refactorColumn(y, b);
				}
				else if (b[x][y+3]==' ') {//pushing opponent piece to free spot
					b[x][y+3]=opponentColor;
					b[x][y+2]=color;
					b[x][y]=' ';
					refactorColumn(y, b);
					refactorColumn(y+3, b);
				}
				else {
					return null;
				}
			}
			else {
				return null;
			}
		}
		else {
			return null;
		}
		return b;
	}

	public static Scores calcNumWin(char c, char oc) {
		int i, j;
		Scores wins = new Scores();

		//count vertical and horizontal wins
		for (j=0; j<8; j++) {
			for (i=0; i<5; i++) {
				if ((board[i][j]==c)&&(board[i+1][j]==c)&&(board[i+2][j]==c)&&(board[i+3][j]==c))
					wins.addWin();
				if ((board[j][i]==c)&&(board[j][i+1]==c)&&(board[j][i+2]==c)&&(board[j][i+3]==c))
					wins.addWin();
				if ((board[i][j]==oc)&&(board[i+1][j]==oc)&&(board[i+2][j]==oc)&&(board[i+3][j]==oc))
					wins.addOWin();
				if ((board[j][i]==oc)&&(board[j][i+1]==oc)&&(board[j][i+2]==oc)&&(board[j][i+3]==oc))
					wins.addOWin();
			}
		}

		//count diagonal wins
		for (i=3; i<8; i++) {
			for (j=0; j<5; j++) {
				if ((board[i][j]==c)&&(board[i-1][j+1]==c)&&(board[i-2][j+2]==c)&&(board[i-3][j+3]==c))
					wins.addWin();
				if ((board[i][7-j]==c)&&(board[i-1][6-j]==c)&&(board[i-2][5-j]==c)&&(board[i-3][4-j]==c))
					wins.addWin();
				if ((board[i][j]==oc)&&(board[i-1][j+1]==oc)&&(board[i-2][j+2]==oc)&&(board[i-3][j+3]==oc))
					wins.addOWin();
				if ((board[i][7-j]==oc)&&(board[i-1][6-j]==oc)&&(board[i-2][5-j]==oc)&&(board[i-3][4-j]==oc))
					wins.addOWin();
			}
		}

		return wins;
	}

	public static void printBoard(char[][] b) {
		for (int i=0; i<8; i++) {
			for (int j=0; j<8; j++) {
				System.out.print(b[i][j]+((j!=7)?",":""));
			}
			System.out.println();
		}
		System.out.println();
	}


}  

class move {
	char type;
	int row;
	int column;
	public move(int c) {
		type = 'D';
		column = c;
	}

	public move(char t, int r, int c) {
		type=t;
		row=r;
		column=c;
	}
}


class InvalidColorException extends Exception {
	//Parameterless Constructor
	public InvalidColorException() {}

	//Constructor that accepts a message
	public InvalidColorException(String message)
	{
		super(message);
	}
}

class Scores {
	int agentScore;
	int oppScore;

	public Scores() {
		agentScore=0;
		oppScore=0;
	}

	public void addWin() {
		agentScore++;
	}

	public void addOWin() {
		oppScore++;
	}
}

