//---------------------------------------------------------------------------------------------------------------------
// GABRIEL MALONE / CSCI 4525 / LAB 1 / FALL 2025
//---------------------------------------------------------------------------------------------------------------------

package com.stephengware.java.games.chess.bot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.HashSet;

import com.stephengware.java.games.chess.state.*;


//---------------------------------------------------------------------------------------------------------------------
public class gmalone1 extends Bot {

	//-----------------------------------------------------------------------------------------------------------------
	private double MAX_MATERIAL_VAL = 8000.0; 
	Random random = new Random();
	private HashMap<String, Integer> pieceValues = new HashMap<>();
	private String[] chessPieces = {"Pawn", "Rook", "Bishop", "Knight", "Queen", "King"};
	private HashSet<State> visited = new HashSet<>();						      // track visited states during a game
	int moves = 0; 

	//-----------------------------------------------------------------------------------------------------------------
	public gmalone1() { // BOT CONSTRUCTOR
		//-------------------------------------------------------------------------------------------------------------
		super("gmalone1");    																			 // name my bot
	}

	@Override				
	//-----------------------------------------------------------------------------------------------------------------
	protected State chooseMove(State root) {														     // MAIN METHOD 
		//-------------------------------------------------------------------------------------------------------------
		initPieceValues();							 
		//-------------------------------------------------------------------------------------------------------------
		Result res = minimaxABpruning(root, 4, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, playerIsWhite(root));
		System.out.printf("\nbest result from depth %d: %f\n", 3, res.utility);
		return res.state;
		//-------------------------------------------------------------------------------------------------------------
		//return greedy(root).state;
	}
	//-----------------------------------------------------------------------------------------------------------------
	private Result minimaxABpruning(State root, int depth, double alpha, double beta, boolean maximizingPlayer){
		//-------------------------------------------------------------------------------------------------------------
		// I addapted psuedocode provided by : https://www.youtube.com/watch?v=l-hh51ncgDI&t=189s
		//-------------------------------------------------------------------------------------------------------------
		if (depth == 0 || root.over) return evaluateState(root);             // base case , at a leaf or game has ended
		ArrayList<State> childStates = getChildStates(root);
		if (maximizingPlayer) {           
			Result maximalState = new Result(null, Double.NEGATIVE_INFINITY);           // find the maximal score 
			for (State child : childStates) {                             // recurse on child states from current state
				if (wasVisited(child)) continue;
				Result result = minimaxABpruning(child, depth - 1, alpha, beta, false);
				if (result.utility > maximalState.utility) { 
					maximalState.utility = result.utility;
					maximalState.state = child;  		 // need to set to child in the loop and not the returned state
				}									 // returned state will have invalid moves since deeper in the tree
				alpha = Math.max(alpha, maximalState.utility);  // highest score maximizing player is guaranteed so far
				if (beta <= alpha) {       // don't explore any other paths since black wont allow us to pick this path
					break;
				}

			}
			return maximalState;

		} else {
			Result minimalState = new Result(null, Double.POSITIVE_INFINITY);
			for (State child : childStates) {
				if (wasVisited(child)) continue;
				Result result = minimaxABpruning(child, depth - 1, alpha, beta, true);
				if (result.utility < minimalState.utility) {
					minimalState.utility = result.utility;
					minimalState.state = child;
				}
				beta = Math.min(beta, minimalState.utility);// lowest score the minimizing player is guaranteed so far 
				if (beta <= alpha) {  // white had better option earlier on in the tree, will never go down this route
					break;
				}
			}
			return minimalState;
		}
	}


	//-----------------------------------------------------------------------------------------------------------------
	private boolean wasVisited(State state){ // helper method for minimax above
		//-------------------------------------------------------------------------------------------------------------
		if (this.visited.contains(state)){ 														  // this has never run
			System.out.println("just saved you some time");
			return true;
		} 
		this.visited.add(state);
		return false;
	}

	//-----------------------------------------------------------------------------------------------------------------
	private Result greedy(State root){ 															   // Greedy bot method
		//--------------------------------------------------------------------------------------------------------------
		ArrayList<State> childStates = getChildStates(root);
		double optimumUtilityWhite = Double.NEGATIVE_INFINITY;
		double optimumUtilityBlack = Double.POSITIVE_INFINITY;
		State optimumState = null;
		double optimumUtility = 0;

		for (State c : childStates) {													    // iterate the child states
			Result r = evaluateState(c);													    // result of evaluation
			if (playerIsWhite(root) 
				&& r.utility > optimumUtilityWhite){				   // looking to maximize score (white perspective)
				optimumUtilityWhite = r.utility;	
				optimumState = r.state;
			}
			if (playerIsBlack(root) 
				&& r.utility < optimumUtilityBlack){			        // looking to minimze score (black perspective)
				optimumUtilityBlack = r.utility;
				optimumState = r.state;
			}
		} 				

		if (playerIsWhite(root)) {
			optimumUtility = optimumUtilityWhite; 					
		} else {
			optimumUtility = optimumUtilityBlack;
		}	
																  // if nothing stands out, just pick a random move
		if (optimumUtility == 0) optimumState = childStates.get(random.nextInt(childStates.size())); 
		return new Result(optimumState, optimumUtility);
	}
	//-----------------------------------------------------------------------------------------------------------------
	private Result evaluateState(State root) {
		//-------------------------------------------------------------------------------------------------------------
		
		double utilityScore = materialValueForPlayer(root);
		
		if (isCheckMate(root) && playerIsBlack(root)){								// White just went,so black checked
			utilityScore += 1000;	
			return new Result(root, utilityScore); 								// checkmate, no need to go any further
		}
		if (isCheckMate(root) && playerIsWhite(root)){								// Black just went,so white checked
			utilityScore -= 1000;	
			return new Result(root, utilityScore); 
		}
		if (isStaleMate(root) && playerIsBlack(root)){										  // white causes stalemate
			if (utilityScore < 0) utilityScore += 1000;								   // if losing, take the stalemate
			else			 	  utilityScore -= 1000;	 
			return new Result(root, utilityScore); 					
		}
		if (isStaleMate(root) && playerIsWhite(root)){												 // Black just went
		    if (utilityScore > 0) utilityScore -= 1000;								   // if losing, take the stalemate
			else 				  utilityScore += 1000;	 
			return new Result(root, utilityScore); 							   
		}
		// if (isCheck(root) && playerIsWhite(root)){									// black has put white in check
		// 	utilityScore -= 15;									// this just ends up making my player make stupid moves
		// }
		// if (isCheck(root) && playerIsBlack(root)){									// white has put black in check
		// 	utilityScore += 15;
		// }
		return new Result(root, utilityScore);                   
	}
	//-----------------------------------------------------------------------------------------------------------------
	private double materialValueForPlayer(State root){
		//-------------------------------------------------------------------------------------------------------------
		HashMap<String, Integer> curWhitePeices  = getPieces(root.board, "WHITE");     // piece and how many
		HashMap<String, Integer> curBlackPeices  = getPieces(root.board, "BLACK");	  // piece and how many
		double whiteMaterialValue = evaluateValueOfPieces(curWhitePeices);					 // value of white's pieces
		double blackMaterialvalue = evaluateValueOfPieces(curBlackPeices);					 // value of black's pieces
		boolean endGame = isEndGame(root.board);

		whiteMaterialValue += pawnPositionModifier	(getPieceOjbects(root, "WHITE"), "WHITE", endGame);
		whiteMaterialValue += rookPositionModifier	(getPieceOjbects(root, "WHITE"), "WHITE", endGame);
		whiteMaterialValue += queenPositionModifier	(getPieceOjbects(root, "WHITE"), "WHITE", endGame);
		whiteMaterialValue += knightPositionModifier(getPieceOjbects(root, "WHITE"), "WHITE", endGame);
		whiteMaterialValue += bishopPositionModifier(getPieceOjbects(root, "WHITE"), "WHITE", endGame);
		whiteMaterialValue += kingPositionModifier	(getPieceOjbects(root, "WHITE"), "WHITE", endGame);

		blackMaterialvalue += pawnPositionModifier	(getPieceOjbects(root, "BLACK"), "BLACK", endGame);
		blackMaterialvalue += rookPositionModifier	(getPieceOjbects(root, "BLACK"), "BLACK", endGame);
		blackMaterialvalue += queenPositionModifier	(getPieceOjbects(root, "BLACK"), "BLACK", endGame);
		blackMaterialvalue += knightPositionModifier(getPieceOjbects(root, "BLACK"), "BLACK", endGame);
		blackMaterialvalue += bishopPositionModifier(getPieceOjbects(root, "BLACK"), "BLACK", endGame);
		blackMaterialvalue += kingPositionModifier	(getPieceOjbects(root, "BLACK"), "BLACK", endGame);

		return whiteMaterialValue - blackMaterialvalue; 					  // utility score from white's perspective
	}
	//-----------------------------------------------------------------------------------------------------------------
	private boolean isEndGame(Board board){
		HashMap<String, Integer> pieces = new HashMap<>();						   		   // track pieces on the board 
		for (String p : this.chessPieces) pieces.put(p,0); 			         
		Iterator<Piece> piece_iterator = board.iterator();	  						   // init map with values set to 0
		// to determine material value for game phase calc all pieces except for pawns and kings
		while(piece_iterator.hasNext())
		{
			Piece curPeice = piece_iterator.next();
			if (isKnight(curPeice)) pieces.put("Knight",pieces.get("Knight") + 1);
			if (isRook(curPeice)) 	pieces.put("Rook", 	pieces.get("Rook")   + 1);
			if (isBishop(curPeice)) pieces.put("Bishop",pieces.get("Bishop") + 1);
			if (isQueen(curPeice)) 	pieces.put("Queen", pieces.get("Queen")  + 1);
		}
		return evaluateValueOfPieces(pieces) <= (MAX_MATERIAL_VAL * 0.7);
	}

	//-----------------------------------------------------------------------------------------------------------------
	private double pawnPositionModifier(ArrayList<Piece> pieces, String player, boolean endGame){
		//-------------------------------------------------------------------------------------------------------------
		double[][] PAWNTABLE = endGame ? PAWN_ENDGAME_TABLE : PAWN_TABLE;
		
		double val = 0;
		for (Piece p : pieces){
			if (isPawn(p) && player.equals("WHITE")){
				val = PAWNTABLE[p.rank][p.file];							   
			}
			if (isPawn(p) && player.equals("BLACK")){
				val = PAWNTABLE[7-p.rank][p.file];						
			}
		}
		return val;
	}
	//-----------------------------------------------------------------------------------------------------------------
	private double rookPositionModifier(ArrayList<Piece> pieces, String player, boolean endGame){
		//-------------------------------------------------------------------------------------------------------------
		
		double[][] ROOKTABLE = endGame ? ROOK_ENDGAME_TABLE : ROOK_TABLE;
		
		double val = 0;
		for (Piece p : pieces){
			if (isRook(p) && player.equals("WHITE")){
				val = (ROOKTABLE[p.rank][p.file]);						
			}
			if (isRook(p) && player.equals("BLACK")){
				val = (ROOKTABLE[7-p.rank][p.file]);							   
			}
		}
		return val;
	}
	//-----------------------------------------------------------------------------------------------------------------
	private double queenPositionModifier(ArrayList<Piece> pieces, String player, boolean endGame){
		//-------------------------------------------------------------------------------------------------------------
		
		double[][] QUEENTABLE = endGame ? QUEEN_ENDGAME_TABLE : QUEEN_TABLE;
		
		double val = 0;
		for (Piece p : pieces){
			if (isQueen(p) && player.equals("WHITE")){
				val = (QUEENTABLE[p.rank][p.file]);						
			}
			if (isQueen(p) && player.equals("BLACK")){
				val = (QUEENTABLE[7-p.rank][p.file]);							   
			}
		}
		return val;
	}
	//-----------------------------------------------------------------------------------------------------------------
	private double knightPositionModifier(ArrayList<Piece> pieces, String player, boolean endGame){
		//-------------------------------------------------------------------------------------------------------------
		
		double[][] KNIGHTTABLE = endGame ? KNIGHT_ENDGAME_TABLE : KNIGHT_TABLE;
		
		double val = 0;
		for (Piece p : pieces){
			if (isKnight(p) && player.equals("WHITE")){
				val = (KNIGHTTABLE[p.rank][p.file]);						
			}
			if (isKnight(p) && player.equals("BLACK")){
				val = (KNIGHTTABLE[7-p.rank][p.file]);						   
			}
		}
		return val;
	}
	//-----------------------------------------------------------------------------------------------------------------
	private double bishopPositionModifier(ArrayList<Piece> pieces, String player, boolean endGame){
		//-------------------------------------------------------------------------------------------------------------
		
		double[][] BISHOPTABLE = endGame ? BISHOP_ENDGAME_TABLE : BISHOP_TABLE;
		
		double val = 0;
		for (Piece p : pieces){
			if (isBishop(p) && player.equals("WHITE")){
				val = (BISHOPTABLE[p.rank][p.file]);						
			}
			if (isBishop(p) && player.equals("BLACK")){
				val = (BISHOPTABLE[7-p.rank][p.file]);						   
			}
		}
		return val;
	}
	//-----------------------------------------------------------------------------------------------------------------
	private double kingPositionModifier(ArrayList<Piece> pieces, String player, boolean endGame){
		//-------------------------------------------------------------------------------------------------------------
		
		double[][] KINGTABLE = endGame ? KING_ENDGAME_TABLE : KING_TABLE;
		
		double val = 0;
		for (Piece p : pieces){
			if (isKing(p) && player.equals("WHITE")){
				val = (KINGTABLE[p.rank][p.file]);						
			}
			if (isKing(p) && player.equals("BLACK")){
				val = (KINGTABLE[7-p.rank][p.file]);						   
			}
		}
		return val;
	}
	//-----------------------------------------------------------------------------------------------------------------
	private ArrayList<Piece> getPieceOjbects(State root, String player ){ // iterate board to get all the piece objects 
		//-------------------------------------------------------------------------------------------------------------
		ArrayList<Piece> boardPieces = new ArrayList<>();
		for (int i = 0 ; i < 8 ; i ++){
			for (int j = 0 ; j < 8 ; j ++){
				Piece p = root.board.getPieceAt(i, j);
				if (p != null && p.player.name().equals(player)) {
					boardPieces.add(p);
				}
			}
		}
		return boardPieces;
	}
	//-----------------------------------------------------------------------------------------------------------------
	private ArrayList<State> getChildStates(State root){ 						       // GET NEXT POSSIBLE GAME STATES
		//-------------------------------------------------------------------------------------------------------------
		ArrayList<State> children = new ArrayList<>();	     // This list will hold all the children nodes of the root.
		Iterator<State> iterator = root.next().iterator();        // Generate all children nodes of the root - all the
		while(!root.searchLimitReached() && iterator.hasNext()) {// possible next states of the game and do not exceed
			State nexState = iterator.next(); 
			children.add(nexState); 								   			
		}	
		return children; 		
	}
	//-----------------------------------------------------------------------------------------------------------------
	private HashMap<String, Integer> getPieces(Board board, String playerName){				    // TRACK PLAYERS PIECES
		//-------------------------------------------------------------------------------------------------------------
		HashMap<String, Integer> pieces = new HashMap<>();						   		   // track pieces on the board 
		for (String p : this.chessPieces) pieces.put(p,0); 			         
		Iterator<Piece> piece_iterator = board.iterator();	  						   // init map with values set to 0

		while(piece_iterator.hasNext())
		{
			Piece curPeice = piece_iterator.next();
			String playerMoving = curPeice.player.name();

			if (playerMoving.equals(playerName)) {               // iterate opp's pieces and add up pieces on the board
				if (isPawn(curPeice)) 	pieces.put("Pawn", 	pieces.get("Pawn")   + 1);
				if (isKnight(curPeice)) pieces.put("Knight",pieces.get("Knight") + 1);
				if (isRook(curPeice)) 	pieces.put("Rook", 	pieces.get("Rook")   + 1);
				if (isBishop(curPeice)) pieces.put("Bishop",pieces.get("Bishop") + 1);
				if (isQueen(curPeice)) 	pieces.put("Queen", pieces.get("Queen")  + 1);
				if (isKing(curPeice)) 	pieces.put("King", 	pieces.get("King") 	 + 1);
			}

		}
		return pieces;
	}
	//-----------------------------------------------------------------------------------------------------------------
	private double evaluateValueOfPieces(HashMap<String, Integer> Peices){
		//-------------------------------------------------------------------------------------------------------------
		double val = 0;
		for (String piece : this.chessPieces){ 								// iterate through pawn, knight, rook, etc. 
			val += Peices.get(piece) * this.pieceValues.get(piece);   // get the number of pawns or w/e piece * its val
		}
		return val;
	}
	//-----------------------------------------------------------------------------------------------------------------			
	private boolean isPawn(Piece piece){													// HELPER METHODS FOR ABOVE
		return piece.getClass() == Pawn.class;
	}
	private boolean isKnight(Piece piece){
		return piece.getClass() == Knight.class;
	}
	private boolean isRook(Piece piece){
		return piece.getClass() == Rook.class;
	}
	private boolean isBishop(Piece piece){
		return piece.getClass() == Bishop.class;
	}
	private boolean isQueen(Piece piece){
		return piece.getClass() == Queen.class;
	}
	private boolean isKing(Piece piece){
		return piece.getClass() == King.class;
	}
	//-----------------------------------------------------------------------------------------------------------------
	private boolean isCheckMate(State root){
		return root.over && root.check;
	}
	private boolean isStaleMate(State root){
		return root.over && !root.check;
	}
	// private boolean isCheck(State root){
	// 	return root.check && !root.over;
	// }
	private boolean playerIsWhite(State root){
		return root.player.name().equals("WHITE");
	}
	private boolean playerIsBlack(State root){
		return root.player.name().equals("BLACK");
	}
	//-----------------------------------------------------------------------------------------------------------------
	private void initPieceValues(){ 												   // set the values for each piece
		//-------------------------------------------------------------------------------------------------------------
		this.pieceValues.put("Pawn", 100);
		this.pieceValues.put("Knight", 320);
		this.pieceValues.put("Rook", 500);
		this.pieceValues.put("Bishop", 330);
		this.pieceValues.put("Queen", 900);
		this.pieceValues.put("King", 0);
	}
	//-----------------------------------------------------------------------------------------------------------------
	private static final class Result { 						  // so we can associate a state with its utility score
		//-------------------------------------------------------------------------------------------------------------
		public State state;
		public double utility;

		public Result(State state, double utility) {
			this.state = state;
			this.utility = utility;
		}
	}
	//-----------------------------------------------------------------------------------------------------------------
	// GAME BOARDS
	//-----------------------------------------------------------------------------------------------------------------
	private double[][] PAWN_TABLE = {
		{  0,  0,  0,  0,  0,  0,  0,  0},
		{ 50, 50, 50, 50, 50, 50, 50, 50},   
		{ 10, 10, 20, 35, 35, 20, 10, 10},
		{  5,  5, 10, 25, 25, 10,  5,  5},
		{  0,  0,  0, 15, 15,  0,  0,  0},
		{  5, -5,-10,  0,  0,-10, -5,  5},
		{  5, 10, 10,-20,-20, 10, 10,  5},
		{  0,  0,  0,  0,  0,  0,  0,  0}
	};
	private double[][] PAWN_ENDGAME_TABLE ={
		{  0,  5, 10, 15, 15, 10,  5,  0},
		{ 10, 15, 20, 25, 25, 20, 15, 10},
		{  5, 10, 15, 30, 30, 15, 10,  5},
		{  0,  5, 15, 25, 25, 15,  5,  0},
		{  0,  0, 10, 20, 20, 10,  0,  0},
		{  0,  0,  5, 15, 15,  5,  0,  0},
		{  0,  0,  0, 10, 10,  0,  0,  0},
		{  0,  0,  0,  0,  0,  0,  0,  0}
	};
	private double[][] ROOK_TABLE = {
		// a   b   c   d   e   f   g   h
		{  0,  0,  0,  5,  5,  0,  0,  0},
		{ -5,  0,  0,  0,  0,  0,  0, -5},
		{ -5,  0,  0,  0,  0,  0,  0, -5},
		{ -5,  0,  0,  5,  5,  0,  0, -5},
		{ -5,  0,  0,  5,  5,  0,  0, -5},
		{ -5,  0,  0,  0,  0,  0,  0, -5},
		{  5, 10, 10, 10, 10, 10, 10,  5}, 
		{  0,  0,  0,  0,  0,  0,  0,  0}
	};
	private double[][] ROOK_ENDGAME_TABLE = {
		{  0,  0,  0,  5,  5,  0,  0,  0},
		{  0,  5,  5, 10, 10,  5,  5,  0},
		{  0,  0,  5, 10, 10,  5,  0,  0},
		{  0,  0,  5, 10, 10,  5,  0,  0},
		{  0,  0,  5, 10, 10,  5,  0,  0},
		{  0,  0,  5, 10, 10,  5,  0,  0},
		{  5, 10, 10, 15, 15, 10, 10,  5},
		{  0,  0,  0,  0,  0,  0,  0,  0}
	};
	private double[][] QUEEN_TABLE = {
		{-20,-10,-10, -5, -5,-10,-10,-20},
		{-10,  0,  0,  0,  0,  0,  0,-10},
		{-10,  0,  5,  5,  5,  5,  0,-10},
		{ -5,  0,  5,  5,  5,  5,  0, -5},
		{ -5,  0,  5,  5,  5,  5,  0, -5},
		{-10,  0,  5,  5,  5,  5,  0,-10},
		{-10,  0,  0,  0,  0,  0,  0,-10},
		{-20,-10,-10, -5, -5,-10,-10,-20}
	};
	private double[][] QUEEN_ENDGAME_TABLE = {			 
		{-20,-10,-10, -5, -5,-10,-10,-20},
		{-10,  0,  0,  0,  0,  0,  0,-10},
		{-10,  0, 10, 10, 10, 10,  0,-10},
		{ -5,  0, 10, 15, 15, 10,  0, -5},
		{ -5,  0, 10, 15, 15, 10,  0, -5},
		{-10,  0, 10, 10, 10, 10,  0,-10},
		{-10,  0,  0,  0,  0,  0,  0,-10},
		{-20,-10,-10, -5, -5,-10,-10,-20}
	};
	private double[][] KNIGHT_TABLE = {				
		{-50,-40,-30,-30,-30,-30,-40,-50},
		{-40,-20,  0,  0,  0,  0,-20,-40},
		{-30,  0, 10, 15, 15, 10,  0,-30},
		{-30,  5, 15, 20, 20, 15,  5,-30},
		{-30,  0, 15, 20, 20, 15,  0,-30},
		{-30,  5, 10, 15, 15, 10,  5,-30},
		{-40,-20,  0,  5,  5,  0,-20,-40},
		{-50,-40,-30,-30,-30,-30,-40,-50} 
	};
	private double[][] KNIGHT_ENDGAME_TABLE = {
		{-50,-40,-30,-30,-30,-30,-40,-50},
		{-40,-20,  0,  0,  0,  0,-20,-40},
		{-30,  0, 10, 15, 15, 10,  0,-30},
		{-30,  5, 15, 20, 20, 15,  5,-30},
		{-30,  0, 15, 20, 20, 15,  0,-30},
		{-30,  5, 10, 15, 15, 10,  5,-30},
		{-40,-20,  0,  0,  0,  0,-20,-40},
		{-50,-40,-30,-30,-30,-30,-40,-50}
	};
	private double[][] BISHOP_TABLE = {					 
		{-20,-10,-10,-10,-10,-10,-10,-20},
		{-10,  0,  0,  0,  0,  0,  0,-10},
		{-10,  0,  5, 10, 10,  5,  0,-10},
		{-10,  5,  5, 10, 10,  5,  5,-10},
		{-10,  0, 10, 10, 10, 10,  0,-10},
		{-10, 10, 10, 10, 10, 10, 10,-10},
		{-10,  5,  0,  0,  0,  0,  5,-10},
		{-20,-10,-10,-10,-10,-10,-10,-20}
	};
	private double[][] BISHOP_ENDGAME_TABLE = {
		{-20,-10,-10,-10,-10,-10,-10,-20},
		{-10,  0,  0,  0,  0,  0,  0,-10},
		{-10,  0, 10, 15, 15, 10,  0,-10},
		{-10, 10, 15, 20, 20, 15, 10,-10},
		{-10, 10, 15, 20, 20, 15, 10,-10},
		{-10,  0, 10, 15, 15, 10,  0,-10},
		{-10,  0,  0,  0,  0,  0,  0,-10},
		{-20,-10,-10,-10,-10,-10,-10,-20}
	};
	private double[][] KING_TABLE = {				
		{-30,-40,-40,-50,-50,-40,-40,-30},
		{-30,-40,-40,-50,-50,-40,-40,-30},
		{-30,-35,-40,-50,-50,-40,-35,-30},
		{-30,-35,-40,-55,-55,-40,-35,-30},
		{-20,-30,-35,-45,-45,-35,-30,-20},
		{-10,-20,-20,-20,-20,-20,-20,-10},
		{ 20, 20,  0,  0,  0,  0, 20, 20},
		{ 30, 40, 10,  0,  0, 10, 40, 30}
	};
	private double[][] KING_ENDGAME_TABLE = { 			 
		{-50,-30,-10,  0,  0,-10,-30,-50},
    	{-30,-10, 10, 20, 20, 10,-10,-30},
   	 	{-10, 10, 20, 30, 30, 20, 10,-10},
    	{  0, 20, 30, 40, 40, 30, 20,  0},
    	{  0, 20, 30, 40, 40, 30, 20,  0},
    	{-10, 10, 20, 30, 30, 20, 10,-10},
    	{-30,-10, 10, 20, 20, 10,-10,-30},
    	{-50,-30,-10,  0,  0,-10,-30,-50}
	};

}


