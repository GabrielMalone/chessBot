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
import com.stephengware.java.games.chess.state.*;


//---------------------------------------------------------------------------------------------------------------------
public class MyBot extends Bot {
	//-----------------------------------------------------------------------------------------------------------------
	Random random = new Random();
	private HashMap<String, Integer> pieceValues = new HashMap<>();
	private String[] chessPieces = {"Pawn", "Rook", "Bishop", "Knight", "Queen", "King"};

	//-----------------------------------------------------------------------------------------------------------------
	public MyBot() { // BOT CONSTRUCTOR
		//-------------------------------------------------------------------------------------------------------------
		super("gmalone1");    																			 // name my bot
	}

	@Override				
	//-----------------------------------------------------------------------------------------------------------------
	protected State chooseMove(State root) {														     // MAIN METHOD 
		//-------------------------------------------------------------------------------------------------------------
		initPieceValues();
		return greedy(root).state;
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
			if (utilityScore < 0) utilityScore += 500;								   // if losing, take the stalemate
			else 				  utilityScore -= 500;	 
			return new Result(root, utilityScore); 								// try to just avoid sttalemate for now
		}
		if (isStaleMate(root) && playerIsWhite(root)){												 // Black just went
			if (utilityScore > 0) utilityScore -= 500;								   // if losing, take the stalemate
			else 				  utilityScore += 500;	 
			return new Result(root, utilityScore); 							    // try to just avoid sttalemate for now
		}
		if (isCheck(root) && playerIsWhite(root)){									    // black has put white in check
			utilityScore -= 50;
		}
		if (isCheck(root) && playerIsBlack(root)){										// white has put black in check
			utilityScore += 50;
		}
		return new Result(root, utilityScore);                   
	}
	//-----------------------------------------------------------------------------------------------------------------
	private double pawnPositionModifier(ArrayList<Piece> pieces, String player){
		//-------------------------------------------------------------------------------------------------------------
		double val = 1;
		for (Piece p : pieces){
			if (isPawn(p) && player.equals("WHITE")){
				val *= (p.rank - 1);							   // white pawn more valuable as it moves up the board
			}
			if (isPawn(p) && player.equals("BLACK")){
				val *= (8 - p.rank);						     // black pawn more valuable as it moves down the board
			}
		}
		return val;
	}
	//-----------------------------------------------------------------------------------------------------------------
	private double materialValueForPlayer(State root){
		//-------------------------------------------------------------------------------------------------------------
		HashMap<String, Integer> curWhitePeices  = getPieces(root.board, "WHITE");     // piece and how many
		HashMap<String, Integer> curBlackPeices  = getPieces(root.board, "BLACK");	  // piece and how many
		double whiteMaterialValue = evaluateValueOfPieces(curWhitePeices);					 // value of white's pieces
		double blackMaterialvalue = evaluateValueOfPieces(curBlackPeices);					 // value of black's pieces
		whiteMaterialValue += pawnPositionModifier(getPieceOjbects(root, "WHITE"), "WHITE");
		blackMaterialvalue += pawnPositionModifier(getPieceOjbects(root, "BLACK"), "BLACK");

		return whiteMaterialValue - blackMaterialvalue; 					  // utility score from white's perspective
	}

	//-----------------------------------------------------------------------------------------------------------------
	private ArrayList<Piece> getPieceOjbects(State root, String player){      // iterate the board to get all the actual piece objects 
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
}

