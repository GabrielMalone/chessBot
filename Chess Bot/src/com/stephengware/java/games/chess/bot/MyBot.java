//---------------------------------------------------------------------------------------------------------------------
// GABRIEL MALONE / CSCI 4524 / LAB 1 / FALL 2025
//---------------------------------------------------------------------------------------------------------------------

package com.stephengware.java.games.chess.bot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.HashSet;
import java.util.Set;

import com.stephengware.java.games.chess.state.*;


// as it is now, my bot is greedy, it will find the highest value piece it can take, and take it. otherwise, random

// work on making evaluate states recurssive 

//---------------------------------------------------------------------------------------------------------------------
public class MyBot extends Bot {
	//-----------------------------------------------------------------------------------------------------------------
	
	private Random random = new Random();															  // class lvl vars
	private State curGame;
	private HashMap<String, Integer> pieceValues = new HashMap<>();
	private String[] chessPieces = {"Pawn", "Rook", "Bishop", "Knight", "Queen", "King"};

	//-----------------------------------------------------------------------------------------------------------------
	public MyBot() { // BOT CONSTRUCTOR
		//-------------------------------------------------------------------------------------------------------------
		super("gmalone1");    																			 // name my bot
	}



	// private Result minimax(State root, int depth, boolean maximizingPlayer) {
	// 	if (depth == 0 || root.over) return evaluateState(root);
		
	// 	ArrayList<State> childrenStates = getChildrenStates(root); 

	// 	if (maximizingPlayer) {
	// 		Result maxResult = evaluateState(root);
	// 		for (State child : childrenStates) {  
	// 			Result result = minimax(child, depth - 1, false);
	// 			if (result.utility > maxResult.utility) {
	// 				maxResult.utility = result.utility;
	// 				maxResult.state = result.state;
	// 			}
	// 		}
	// 		return maxResult;
	// 	}

	// 	else {
	// 		Result minResult = evaluateState(root);
	// 		for (State child : childrenStates) {
	// 			Result result = minimax(child, depth - 1, true);
	// 			if (result.utility < minResult.utility) {
	// 				minResult.utility = result.utility;
	// 				minResult.state = result.state;
	// 			}
	// 		}
	// 		return minResult;
	// 	}

	// }


	@Override				
	//-----------------------------------------------------------------------------------------------------------------
	protected State chooseMove(State root) {														     // MAIN METHOD 
		//-------------------------------------------------------------------------------------------------------------
		curGame = root;
		initPieceValues();
		return evaluateStates(getChildrenStates(root)).state;
		// return minimax(root, 2, true).state;
	}

	// //-----------------------------------------------------------------------------------------------------------------
	// private Result evaluateState(State root) {
	// 	//-------------------------------------------------------------------------------------------------------------
	// 	int maxVal = 0;
	// 	HashMap<String, Integer> oppPieces = getPieces(root.board, whoIam());
	// 	MyBot.Result result = new MyBot.Result(root, maxVal);

	// 	if (isCheckMate(root)) {											      		  // if check available take it
	// 		result.utility = 1000;
	// 		return result;
	// 	}
	// 	if (root.check) {
	// 		result.utility = 500;
	// 		return result;
	// 	};  									  				  					
	// 	HashMap<String, Integer> OppPiecesInChildState = getPieces(root.board, whoIam());
	// 	if (ableToTakePiece(oppPieces, OppPiecesInChildState)) {   	       // if state results in fewer pieces on board
	// 		int childStateMaxVal = maxPieceValueOfState(oppPieces, OppPiecesInChildState);   	  // find most val take
	// 		if (childStateMaxVal > maxVal){ 					  	  // track overall most valuable take in all states
	// 			maxVal = childStateMaxVal;
	// 			result.utility = maxVal;										
	// 		}
	// 	}				
	// 	return result;
	// }

	//-----------------------------------------------------------------------------------------------------------------
	private boolean ableToTakePiece(HashMap<String, Integer> initSate, HashMap<String, Integer> childState){
		//-------------------------------------------------------------------------------------------------------------
		for (String p : this.chessPieces) {
			if (initSate.get(p) != childState.get(p)) return true;
		}
		return false;
	}

	//-----------------------------------------------------------------------------------------------------------------
	private int maxPieceValueOfState(HashMap<String, Integer> initSate, HashMap<String, Integer> childState){
		//-------------------------------------------------------------------------------------------------------------
		int max = 0;
		for (String p : this.chessPieces) {
			if (initSate.get(p) != childState.get(p) && this.pieceValues.get(p) > max){
				max = this.pieceValues.get(p);
			}
		}
		return max;
	}

	//-----------------------------------------------------------------------------------------------------------------
	private ArrayList<State> getChildrenStates(State root){ 						   // GET NEXT POSSIBLE GAME STATES
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
		HashMap<String, Integer> OppBoardPieces = new HashMap<>();						   // track pieces on the board 
		for (String p : this.chessPieces) OppBoardPieces.put(p,0); 			   // init map with values set to 0
		Iterator<Piece> piece_iterator = board.iterator();

		while(piece_iterator.hasNext())
		{
			Piece curPeice = piece_iterator.next();
			if (!curPeice.player.name().equals(playerName)) {    // iterate opp's pieces and add up pieces on the board
				if (isPawn(curPeice)) 	OppBoardPieces.put("Pawn", 	OppBoardPieces.get("Pawn")   + 1);
				if (isKnight(curPeice)) OppBoardPieces.put("Knight",OppBoardPieces.get("Knight") + 1);
				if (isRook(curPeice)) 	OppBoardPieces.put("Rook", 	OppBoardPieces.get("Rook")   + 1);
				if (isBishop(curPeice)) OppBoardPieces.put("Bishop",OppBoardPieces.get("Bishop") + 1);
				if (isQueen(curPeice)) 	OppBoardPieces.put("Queen", OppBoardPieces.get("Queen")  + 1);
				if (isKing(curPeice)) 	OppBoardPieces.put("King", 	OppBoardPieces.get("King") 	 + 1);
			}
		}

		return OppBoardPieces;
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
	// private boolean isStaleMate(State root){
	// 	return root.over && !root.check;
	// }
	//-----------------------------------------------------------------------------------------------------------------
	private String whoIam(){ 																				// WHO AM I 
		//-------------------------------------------------------------------------------------------------------------
		return this.curGame.player.name();
	}

	//-----------------------------------------------------------------------------------------------------------------
	private void initPieceValues(){ 												   // set the values for each piece
		//-------------------------------------------------------------------------------------------------------------
		this.pieceValues.put("Pawn", 1);
		this.pieceValues.put("Knight", 3);
		this.pieceValues.put("Rook", 5);
		this.pieceValues.put("Bishop", 3);
		this.pieceValues.put("Queen", 9);
		this.pieceValues.put("King", 100);
	}
	//-----------------------------------------------------------------------------------------------------------------
	private static final class Result {

		public State state;
		public int utility;

		public Result(State state, int utility) {
			this.state = state;
			this.utility = utility;
		}

	}
	//-----------------------------------------------------------------------------------------------------------------

	//-----------------------------------------------------------------------------------------------------------------
	private Result evaluateStates(ArrayList<State> childrenStates){ 						     // EVALUATE EACH STATE
		//-------------------------------------------------------------------------------------------------------------
		int maxVal = 0;
		State initalState = childrenStates.get(0);
		HashMap<String, Integer> oppPieces = getPieces(initalState.board, whoIam());
		MyBot.Result result = new MyBot.Result(initalState, maxVal);

		for (State childState : childrenStates) {
			if (isCheckMate(childState)) {											      // if check available take it
				result.state = childState;
				result.utility = 1000;
				return result;
			}
			if (childState.check) {
				result.state = childState;
				result.utility = 500;
				return result;
			};  									  				  					
			HashMap<String, Integer> OppPiecesInChildState = getPieces(childState.board, whoIam());
			if (ableToTakePiece(oppPieces, OppPiecesInChildState)) {   	   // if state results in fewer pieces on board
				int childStateMaxVal = maxPieceValueOfState(oppPieces, OppPiecesInChildState);    // find most val take
				if (childStateMaxVal > maxVal){ 					  // track overall most valuable take in all states
					maxVal = childStateMaxVal;
					result.state = childState;
					result.utility = maxVal;										
				}
			}				
		}
		if (maxVal > 0) return result;
		else {
			result.state = childrenStates.get(random.nextInt(childrenStates.size()));   // no good options, pick random
			return result;
		}
	}
}