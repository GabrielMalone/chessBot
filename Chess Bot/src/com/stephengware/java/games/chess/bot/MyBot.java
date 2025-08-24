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
	private State curGame;
	private Random random = new Random();
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
		this.curGame = root;
		initPieceValues();
		return greedy(root).state;
	}
	//-----------------------------------------------------------------------------------------------------------------
	private Result greedy(State root){ 															   // Greedy bot method
		//-------------------------------------------------------------------------------------------------------------
		ArrayList<State> childrenStates = getChildStates(root);
		System.out.printf("child states: %d", childrenStates.size());
		double optimumUtilityWhite = Double.NEGATIVE_INFINITY;
		double optimumUtilityBlack = Double.POSITIVE_INFINITY;
		State optimumState = null;
		double optimumUtility = 0;
		for (State c : childrenStates) {
			Result r = evaluateState(c);
			if (r.state.check && r.state.over) return r; 									   // if check mate take it
			if (this.curGame.player.name().equals("WHITE") 
				&& r.utility >= optimumUtilityWhite){
				optimumUtilityWhite = r.utility;	
				optimumState = r.state;
			}
			if (this.curGame.player.name().equals("BLACK") 
				&& r.utility <= optimumUtilityBlack){
				System.out.printf("\nutility score: %f\n", r.utility);
				optimumUtilityBlack = r.utility;
				optimumState = r.state;
			}
		} 						

		if (this.curGame.player.name().equals("WHITE")) {
			optimumUtility = optimumUtilityWhite;
		} else {
			optimumUtility = optimumUtilityBlack;
		}

		return new Result(optimumState, optimumUtility);
	}
	//-----------------------------------------------------------------------------------------------------------------
	private Result evaluateState(State root) {
		//-------------------------------------------------------------------------------------------------------------
		HashMap<String, Integer> curWhitePeices  = getPieces(root.board, "WHITE");
		HashMap<String, Integer> curBlackPeices  = getPieces(root.board, "BLACK");

		double whiteMaterialValue = evaluateValueOfPieces(curWhitePeices);
		double blackMaterialvalue = evaluateValueOfPieces(curBlackPeices);

		System.out.printf("\nwhite value: %f\n", whiteMaterialValue); // check validity
		System.out.printf("black value: %f\n", blackMaterialvalue); // check validity

		return new Result(root, whiteMaterialValue - blackMaterialvalue);
	}
	// private double pieceModificationTable(){

	// }
	//-----------------------------------------------------------------------------------------------------------------
	private ArrayList<State> getChildStates(State root){ 						   // GET NEXT POSSIBLE GAME STATES
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
	// private boolean isCheckMate(State root){
	// 	return root.over && root.check;
	// }
	// private boolean isStaleMate(State root){
	// 	return root.over && !root.check;
	// }
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
	private static final class Result {
		//-------------------------------------------------------------------------------------------------------------
		public State state;
		public double utility;

		public Result(State state, double utility) {
			this.state = state;
			this.utility = utility;
		}
	}
}

