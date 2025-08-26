//---------------------------------------------------------------------------------------------------------------------
// GABRIEL MALONE / CSCI 4525 / LAB 1 / FALL 2025
//---------------------------------------------------------------------------------------------------------------------

package com.stephengware.java.games.chess.bot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import com.stephengware.java.games.chess.state.*;


//---------------------------------------------------------------------------------------------------------------------
public class gmalone1 extends Bot {

	//-----------------------------------------------------------------------------------------------------------------
	private double MAX_MATERIAL_VAL = 64.0;
	Random random = new Random();
	private HashMap<String, Integer> pieceValues = new HashMap<>();
	private String[] chessPieces = {"Pawn", "Rook", "Bishop", "Knight", "Queen", "King"};

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
		return minimaxABpruning(root, 3, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, playerIsWhite(root)).state;
		//return greedy(root).state;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// private Result DFSID(State root, int maxDepth, int depth){
	// 	//-------------------------------------------------------------------------------------------------------------
		
	// 	Stack<ArrayList<State>> stateStack = new Stack<>(); 										   // stack for DFS
	// 	stateStack.push(getChildStates(root));										// get first level and add to stack
	// 	HashSet<State> visitedStates = new HashSet<>();				// 
	// 	while (! stateStack.empty() ) 		   // run ntil stack not empty, although this wil just run forever wont it?
	// 	{
	// 		// i think update minimiax to take in ArrayList<State> instead of root 
	// 	}

	// }


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
				Result result = minimaxABpruning(child, depth - 1, alpha, beta, true);
				if (result.utility < minimalState.utility) {
					minimalState.utility = result.utility;
					minimalState.state = child;
				}
				beta = Math.min(beta, minimalState.utility);// lowest score the minimizing player is guaranteed so far
				if (beta <= alpha) {
					break;
				}
			}
			return minimalState;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	private Result greedy(State root){ 															   // Greedy bot method
		//-------------------------------------------------------------------------------------------------------------
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
		if (isCheck(root) && playerIsWhite(root)){									    // black has put white in check
			utilityScore -= 15;
		}
		if (isCheck(root) && playerIsBlack(root)){										// white has put black in check
			utilityScore += 15;
		}
		return new Result(root, utilityScore);                   
	}
	//-----------------------------------------------------------------------------------------------------------------
	private double materialValueForPlayer(State root){
		//-------------------------------------------------------------------------------------------------------------
		HashMap<String, Integer> curWhitePeices  = getPieces(root.board, "WHITE");     // piece and how many
		HashMap<String, Integer> curBlackPeices  = getPieces(root.board, "BLACK");	  // piece and how many
		double whiteMaterialValue = evaluateValueOfPieces(curWhitePeices);					 // value of white's pieces
		double blackMaterialvalue = evaluateValueOfPieces(curBlackPeices);					 // value of black's pieces
		boolean endGame = isEndGame(whiteMaterialValue + blackMaterialvalue);

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
	private boolean isEndGame(Double materialVal){
		return materialVal <= (MAX_MATERIAL_VAL * 0.2);
		//-------------------------------------------------------------------------------------------------------------
	}

	//-----------------------------------------------------------------------------------------------------------------
	private double pawnPositionModifier(ArrayList<Piece> pieces, String player, boolean endGame){
		//-------------------------------------------------------------------------------------------------------------
		double val = 0;
		for (Piece p : pieces){
			if (isPawn(p) && player.equals("WHITE")){
				val += (p.rank - 1);							   // white pawn more valuable as it moves up the board
			}
			if (isPawn(p) && player.equals("BLACK")){
				val += (8 - p.rank);						     // black pawn more valuable as it moves down the board
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
	private ArrayList<Piece> getPieceOjbects(State root, String player ){  // iterate board to get all the piece objects 
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
		Set<State> visitedStates = new HashSet<>();  
		ArrayList<State> children = new ArrayList<>();	     // This list will hold all the children nodes of the root.
		Iterator<State> iterator = root.next().iterator();        // Generate all children nodes of the root - all the
		while(!root.searchLimitReached() && iterator.hasNext()) {// possible next states of the game and do not exceed
			State nexState = iterator.next(); 	
			if (!visitedStates.contains(nexState)){
				children.add(nexState); 
				visitedStates.add(nexState);										    // keep track of visited states
			}		
			if (root.searchLimitReached()) System.out.printf("search limit reached: %b", root.searchLimitReached());  	
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
	private boolean isCheck(State root){
		return root.check && !root.over;
	}
	private boolean playerIsWhite(State root){
		return root.player.name().equals("WHITE");
	}
	private boolean playerIsBlack(State root){
		return root.player.name().equals("BLACK");
	}
	//-----------------------------------------------------------------------------------------------------------------
	private void initPieceValues(){ 												   // set the values for each piece
		//-------------------------------------------------------------------------------------------------------------
		this.pieceValues.put("Pawn", 1);
		this.pieceValues.put("Knight", 3);
		this.pieceValues.put("Rook", 5);
		this.pieceValues.put("Bishop", 3);
		this.pieceValues.put("Queen", 9);
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
	private double[][] ROOK_TABLE = {
		// a     b     c     d     e     f     g     h
		{ 0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0 }, // rank 8
		{ 1.5,  2.0,  2.0,  2.0,  2.0,  2.0,  2.0,  1.5 }, 
		{-0.5,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0, -0.5 },
		{-0.5,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0, -0.5 },
		{-0.5,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0, -0.5 }, 
		{-0.5,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0, -0.5 }, 
		{-0.5,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0, -0.5 }, 
		{ 0.0,  0.0,  0.0,  0.5,  0.5,  0.0,  0.0,  0.0 }  
	};
	private double[][] ROOK_ENDGAME_TABLE = {
		{ 0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0 }, // rank 8
		{ 0.5,  1.0,  1.0,  1.0,  1.0,  1.0,  1.0,  0.5 }, // slightly more centralized
		{-0.5,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0, -0.5 },
		{-0.5,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0, -0.5 },
		{-0.5,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0, -0.5 },
		{-0.5,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0, -0.5 },
		{-0.5,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0, -0.5 },
		{ 0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0 }  
	};
	private double[][] QUEEN_TABLE = {
		{-0.4, -0.3, -0.3, -0.2, -0.2, -0.3, -0.3, -0.4}, // rank 8
		{-0.3, -0.2, -0.1,  0.0,  0.0, -0.1, -0.2, -0.3}, 
		{-0.3, -0.1,  0.1,  0.2,  0.2,  0.1, -0.1, -0.3},
		{-0.2,  0.0,  0.2,  0.3,  0.3,  0.2,  0.0, -0.2},
		{-0.2,  0.0,  0.2,  0.3,  0.3,  0.2,  0.0, -0.2},
		{-0.3, -0.1,  0.1,  0.2,  0.2,  0.1, -0.1, -0.3}, 
		{-0.3, -0.2, -0.1,  0.0,  0.0, -0.1, -0.2, -0.3}, 
		{-0.4, -0.3, -0.3, -0.2, -0.2, -0.3, -0.3, -0.4} 
	};
	private double[][] QUEEN_ENDGAME_TABLE = {			  // not as strong on edges
		{-0.8, -0.6, -0.6, -0.5, -0.5, -0.6, -0.6, -0.8}, // rank 8 
		{-0.6, -0.4, -0.2,  0.0,  0.0, -0.2, -0.4, -0.6},
		{-0.6, -0.2,  0.2,  0.3,  0.3,  0.2, -0.2, -0.6},
		{-0.5,  0.0,  0.3,  0.5,  0.5,  0.3,  0.0, -0.5}, // center is highly valued
		{-0.5,  0.0,  0.3,  0.5,  0.5,  0.3,  0.0, -0.5},
		{-0.6, -0.2,  0.2,  0.3,  0.3,  0.2, -0.2, -0.6},
		{-0.6, -0.4, -0.2,  0.0,  0.0, -0.2, -0.4, -0.6},
		{-0.8, -0.6, -0.6, -0.5, -0.5, -0.6, -0.6, -0.8}  
	};
	private double[][] KNIGHT_TABLE = {					  // stronger in middle than edges
		{-0.8, -0.6, -0.6, -0.6, -0.6, -0.6, -0.6, -0.8}, // rank 8
		{-0.6, -0.4,  0.0,  0.0,  0.0,  0.0, -0.4, -0.6}, 
		{-0.6,  0.0,  0.3,  0.4,  0.4,  0.3,  0.0, -0.6}, 
		{-0.6,  0.0,  0.4,  0.5,  0.5,  0.4,  0.0, -0.6}, 
		{-0.6,  0.0,  0.4,  0.5,  0.5,  0.4,  0.0, -0.6}, 
		{-0.6,  0.0,  0.3,  0.4,  0.4,  0.3,  0.0, -0.6}, 
		{-0.6, -0.4,  0.0,  0.0,  0.0,  0.0, -0.4, -0.6},
		{-0.8, -0.6, -0.6, -0.6, -0.6, -0.6, -0.6, -0.8}  
	};
	private double[][] KNIGHT_ENDGAME_TABLE = {
		{-1.2, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.2}, // bad
		{-1.0, -0.8,  0.0,  0.0,  0.0,  0.0, -0.8, -1.0},
		{-1.0,  0.0,  0.4,  0.5,  0.5,  0.4,  0.0, -1.0},
		{-1.0,  0.0,  0.5,  0.8,  0.8,  0.5,  0.0, -1.0}, // central squares are golden
		{-1.0,  0.0,  0.5,  0.8,  0.8,  0.5,  0.0, -1.0},
		{-1.0,  0.0,  0.4,  0.5,  0.5,  0.4,  0.0, -1.0},
		{-1.0, -0.8,  0.0,  0.0,  0.0,  0.0, -0.8, -1.0},
		{-1.2, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.2}  // also bad
	};
	private double[][] BISHOP_TABLE = {					  // stronger in middle than edges
		{-0.4, -0.3, -0.3, -0.3, -0.3, -0.3, -0.3, -0.4}, // rank 8
		{-0.3, -0.2,  0.0,  0.0,  0.0,  0.0, -0.2, -0.3},
		{-0.3,  0.0,  0.2,  0.3,  0.3,  0.2,  0.0, -0.3}, 
		{-0.3,  0.0,  0.3,  0.4,  0.4,  0.3,  0.0, -0.3},
		{-0.3,  0.0,  0.3,  0.4,  0.4,  0.3,  0.0, -0.3}, 
		{-0.3,  0.0,  0.2,  0.3,  0.3,  0.2,  0.0, -0.3}, 
		{-0.3, -0.2,  0.0,  0.0,  0.0,  0.0, -0.2, -0.3},
		{-0.4, -0.3, -0.3, -0.3, -0.3, -0.3, -0.3, -0.4} 
	};
	private double[][] BISHOP_ENDGAME_TABLE = {
		{-0.6, -0.5, -0.5, -0.5, -0.5, -0.5, -0.5, -0.6}, // rank 8
		{-0.5, -0.4,  0.0,  0.0,  0.0,  0.0, -0.4, -0.5},
		{-0.5,  0.0,  0.3,  0.4,  0.4,  0.3,  0.0, -0.5},
		{-0.5,  0.0,  0.4,  0.6,  0.6,  0.4,  0.0, -0.5}, // center Diagonals are best
		{-0.5,  0.0,  0.4,  0.6,  0.6,  0.4,  0.0, -0.5},
		{-0.5,  0.0,  0.3,  0.4,  0.4,  0.3,  0.0, -0.5},
		{-0.5, -0.4,  0.0,  0.0,  0.0,  0.0, -0.4, -0.5},
		{-0.6, -0.5, -0.5, -0.5, -0.5, -0.5, -0.5, -0.6} 
	};
	private double[][] KING_TABLE = {					  // midgame, want king to be stationary / protected
		{-0.8, -0.9, -1.0, -1.2, -1.2, -1.0, -0.9, -0.8}, // rank 8
		{-0.8, -0.9, -1.0, -1.2, -1.2, -1.0, -0.9, -0.8},
		{-0.8, -0.9, -1.0, -1.2, -1.2, -1.0, -0.9, -0.8},
		{-0.8, -0.9, -1.0, -1.2, -1.2, -1.0, -0.9, -0.8}, 
		{-0.6, -0.7, -0.8, -1.0, -1.0, -0.8, -0.7, -0.6}, 
		{-0.4, -0.5, -0.6, -0.7, -0.7, -0.6, -0.5, -0.4},
		{ 0.2,  0.2,  0.0, -0.2, -0.2,  0.0,  0.2,  0.2}, 
		{ 0.3,  0.4,  0.1, -0.2, -0.2,  0.1,  0.4,  0.3} 
	};
	private double[][] KING_ENDGAME_TABLE = { 			  // want king to become attacking piece 
		{-1.5, -1.2, -1.0, -0.8, -0.8, -1.0, -1.2, -1.5}, // rank 8 
		{-1.0, -0.5, -0.3,  0.0,  0.0, -0.3, -0.5, -1.0},
		{-0.8, -0.3,  0.3,  0.5,  0.5,  0.3, -0.3, -0.8},
		{-0.7, -0.2,  0.5,  0.8,  0.8,  0.5, -0.2, -0.7}, 
		{-0.7, -0.2,  0.5,  0.8,  0.8,  0.5, -0.2, -0.7},
		{-0.8, -0.3,  0.3,  0.5,  0.5,  0.3, -0.3, -0.8},
		{-1.0, -0.5, -0.3,  0.0,  0.0, -0.3, -0.5, -1.0},
		{-1.5, -1.2, -1.0, -0.8, -0.8, -1.0, -1.2, -1.5}  
	};

}


