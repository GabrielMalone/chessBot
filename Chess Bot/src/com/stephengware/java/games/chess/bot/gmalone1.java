//---------------------------------------------------------------------------------------------------------------------
// GABRIEL MALONE / CSCI 4525 / LAB 1 / FALL 2025
//---------------------------------------------------------------------------------------------------------------------

package com.stephengware.java.games.chess.bot;
import com.stephengware.java.games.chess.state.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

//---------------------------------------------------------------------------------------------------------------------
public class gmalone1 extends Bot {
	//-----------------------------------------------------------------------------------------------------------------
	private HashMap<String, Integer> pieceValues 	= new HashMap<>();		
	private String[] chessPieces 					= {"Pawn", "Rook", "Bishop", "Knight", "Queen", "King"};
    private double MAX_MATERIAL_VAL 				= 8000.0; 
	double alpha 									= Double.NEGATIVE_INFINITY;
	double beta 									= Double.POSITIVE_INFINITY;
	int moves 										= 0;
    boolean searchLimitReached	 					= false;
	//-----------------------------------------------------------------------------------------------------------------
	public gmalone1() { // BOT CONSTRUCTOR
		//-------------------------------------------------------------------------------------------------------------
		super("gmalone1");    		
		initPieceValues();																	
	}

	@Override				
	//-----------------------------------------------------------------------------------------------------------------
	protected State chooseMove(State root) {														     // MAIN METHOD 
		//-------------------------------------------------------------------------------------------------------------
        this.searchLimitReached = false;	     													 // reset each move
		return ID(root).state;																      	  // find best move
	}
	//-----------------------------------------------------------------------------------------------------------------
	private Result ID (State root) { // starting to implement iterative deepening, need to update the minimmax call tho
		//-------------------------------------------------------------------------------------------------------------

		Result bestResult = new Result(null, this.alpha);

		for (int depth = 0 ; depth <= 8 ; depth ++)
		{	
			Result res = minimaxABpruning(root,depth,this.alpha,this.beta,playerIsWhite(root), bestResult);
			System.out.printf("\ndepth: %d\n", depth);
			if (searchLimitReached) break;
			bestResult = res; 										 // only use result from last fully searched depth
		}
		return bestResult;
	}

	//-----------------------------------------------------------------------------------------------------------------
	private Result minimaxABpruning(State root, int depth, double alpha, double beta, boolean maximizingPlayer, Result prevBest){
		//-------------------------------------------------------------------------------------------------------------
		// 						 I addapted psuedocode provided by : https://www.youtube.com/watch?v=l-hh51ncgDI&t=189s
		//-------------------------------------------------------------------------------------------------------------
		if (depth == 0 || root.over) return evaluateState(root);             // base case , at a leaf or game has ended

		ArrayList<Result> childStates = getSortedChildStates(root, maximizingPlayer, prevBest);

		if (maximizingPlayer) {    
			
			Result maximalState = new Result(null, Double.NEGATIVE_INFINITY);           // find the maximal score 
			for (Result child : childStates) {                            // recurse on child states from current state				
				Result result = minimaxABpruning(child.state, depth - 1, alpha, beta, false, prevBest);
				if (result.utility > maximalState.utility) { 
					maximalState.utility = result.utility;
					maximalState.state = child.state;  	 // need to set to child in the loop and not the returned state
				}									 // returned state will have invalid moves since deeper in the tree
				alpha = Math.max(alpha, maximalState.utility);  // highest score maximizing player is guaranteed so far
				if (beta <= alpha) {       // don't explore any other paths since black wont allow us to pick this path
					break;
				}

			}
			return maximalState;

		} else {

			Result minimalState = new Result(null, Double.POSITIVE_INFINITY);
			for (Result child : childStates) {
				Result result = minimaxABpruning(child.state, depth - 1, alpha, beta, true, prevBest);
				if (result.utility < minimalState.utility) {
					minimalState.utility = result.utility;
					minimalState.state = child.state;
				}
				beta = Math.min(beta, minimalState.utility); // lowest score the minimizing player is guaranteed so far 
				if (beta <= alpha) {   // white had better option earlier on in the tree, will never go down this route
					break;
				}
			}
			return minimalState;
		}
	}


	//-----------------------------------------------------------------------------------------------------------------
	private ArrayList<Result> getSortedChildStates(State root, boolean maximizingPlayer, Result prefResult){
		//-------------------------------------------------------------------------------------------------------------

		ArrayList<State> childStates = getChildStates(root);				// get all the states we can from this root
		ArrayList<Result> childStateResults = new ArrayList<>();					   // evaluate all the child states
		for (State c : childStates) childStateResults.add(evaluateState(c));	 	   // evaluate all the child states

		Collections.sort(childStateResults); 		        // get child nodes in order of potentially best moves first
		if (!maximizingPlayer) Collections.reverse(childStateResults); 		  // reverse for Black (lower scores first)
		ArrayList<Result> copy = new ArrayList<>(); 	   // check to see if the preferred result has descendents here
		for (Result r : childStateResults) copy.add(r);
		ArrayList<Result> descendents = new ArrayList<>(); // check to see if the preferred result has descendents here
		if (prefResult.state != null) {								     	   // if we have a preferred result already
			for (Result r : copy) {                 	          // let's see if descendents from it are on this level
				if (isDescendent(prefResult.state, r.state)) { // might lead to better pruning and allow for more depth
					descendents.add(r);
					childStateResults.remove(r);
				}
			}
		}
		Collections.sort(descendents);				 									  // this is probably redundant												
		if (!maximizingPlayer) Collections.reverse(descendents);  
		descendents.addAll(childStateResults);
		return descendents;
	}
	//-----------------------------------------------------------------------------------------------------------------
	private boolean isDescendent(State ancestor, State child){ 	       // see if a child is a desendent of another node 
		//-------------------------------------------------------------------------------------------------------------
		while (child != null)
		{
			if (child.previous!=null && child.previous.board.equals(ancestor.board)){
				return true;
			} 
			child = child.previous;
		}
		return false;
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
		return new Result(root, utilityScore);                   
	}
	//-----------------------------------------------------------------------------------------------------------------
	private double materialValueForPlayer(State root){
		//-------------------------------------------------------------------------------------------------------------
		ArrayList<Piece> blackPiecesObjects = getPieceOjbects(root, "BLACK");
		ArrayList<Piece> whitePiecesObjects = getPieceOjbects(root, "WHITE");
		double whiteMaterialValue = evaluateValueOfPieces(getPieces(root.board, "WHITE"));				
		double blackMaterialvalue = evaluateValueOfPieces(getPieces(root.board, "BLACK"));			
		boolean endGame = isEndGame(root.board);

		whiteMaterialValue += pawnPositionModifier	(whitePiecesObjects, "WHITE", endGame);
		whiteMaterialValue += rookPositionModifier	(whitePiecesObjects, "WHITE", endGame);
		whiteMaterialValue += queenPositionModifier	(whitePiecesObjects, "WHITE", endGame);
		whiteMaterialValue += knightPositionModifier(whitePiecesObjects, "WHITE", endGame);
		whiteMaterialValue += bishopPositionModifier(whitePiecesObjects, "WHITE", endGame);
		whiteMaterialValue += kingPositionModifier	(whitePiecesObjects, "WHITE", endGame);

		blackMaterialvalue += pawnPositionModifier	(blackPiecesObjects, "BLACK", endGame);
		blackMaterialvalue += rookPositionModifier	(blackPiecesObjects, "BLACK", endGame);
		blackMaterialvalue += queenPositionModifier	(blackPiecesObjects, "BLACK", endGame);
		blackMaterialvalue += knightPositionModifier(blackPiecesObjects, "BLACK", endGame);
		blackMaterialvalue += bishopPositionModifier(blackPiecesObjects, "BLACK", endGame);
		blackMaterialvalue += kingPositionModifier	(blackPiecesObjects, "BLACK", endGame);

		return whiteMaterialValue - blackMaterialvalue; 					  // utility score from white's perspective
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
		Iterator<State> iterator = root.next().iterator();         // Generate all children nodes of the root - all the
		while(!root.searchLimitReached() && iterator.hasNext()) { // possible next states of the game and do not exceed
			State nexState = iterator.next(); 
			children.add(nexState); 		
            if (root.searchLimitReached())
            {
                this.searchLimitReached = true;
                System.out.println("search limit reached!");
            }						   			
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
    HashMap<String, Integer> phaseEvalPieces (Board board){
        //-------------------------------------------------------------------------------------------------------------
        HashMap<String, Integer> pieces = new HashMap<>();						   		   // track pieces on the board 
		for (String p : this.chessPieces) pieces.put(p,0); 			         
		Iterator<Piece> piece_iterator = board.iterator();	  						   
		while(piece_iterator.hasNext())    // material value for game phase: calc all pieces except for pawns and kings
		{
			Piece curPeice = piece_iterator.next();
			if (isKnight(curPeice)) pieces.put("Knight",pieces.get("Knight") + 1);
			if (isRook(curPeice)) 	pieces.put("Rook", 	pieces.get("Rook")   + 1);
			if (isBishop(curPeice)) pieces.put("Bishop",pieces.get("Bishop") + 1);
			if (isQueen(curPeice)) 	pieces.put("Queen", pieces.get("Queen")  + 1);
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
	private boolean playerIsWhite(State root){
		return root.player.name().equals("WHITE");
	}
	private boolean playerIsBlack(State root){
		return root.player.name().equals("BLACK");
	}
    //-----------------------------------------------------------------------------------------------------------------
    private boolean isEndGame(Board board){
		return evaluateValueOfPieces(phaseEvalPieces(board)) <= (MAX_MATERIAL_VAL * 0.4);  // 0.4 beats recommended 0.7
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
	private static final class Result implements Comparable<Result> {   // can associate a state with its utility score
		//-------------------------------------------------------------------------------------------------------------
		public State state;
		public double utility;

		public Result(State state, double utility) {
			this.state = state;
			this.utility = utility;
		}
		@Override 																						 // for sorting
		public int compareTo(Result other) {
			return Double.compare(other.utility, this.utility); 								    // Descending order
    	}
	}
	//-----------------------------------------------------------------------------------------------------------------
	// GAME BOARDS 	        // table values provided by https://www.chessprogramming.org/Simplified_Evaluation_Function
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
		{ 10, 15, 20, 25, 25, 20, 10, 10},
		{  5, 10, 15, 30, 30, 15, 10,  5},
		{  0,  5, 15, 25, 25, 15,  5,  0},
		{  0,  0, 10, 20, 20, 10,  0,  0},
		{  0,  0,  5, 15, 15,  5,  0,  0},
		{  0,  0,  0, 10, 10,  0,  0,  0},
		{  0,  0,  0,  0,  0,  0,  0,  0}
	};
	private double[][] ROOK_TABLE = {
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