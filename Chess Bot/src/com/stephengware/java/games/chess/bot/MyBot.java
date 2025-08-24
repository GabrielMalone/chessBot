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

	//-----------------------------------------------------------------------------------------------------------------
	private static final class Result {
		//-------------------------------------------------------------------------------------------------------------
		public State state;
		public double maxBlackPieceThatCanBeTaken;
		public double maxWhitePieceThatCanBeTaken;
		public double utility;

		public Result(State state, double maxWhitePieceThatCanBeTaken, double maxBlackPieceThatCanBeTaken) {
			this.state = state;
			this.maxWhitePieceThatCanBeTaken = maxWhitePieceThatCanBeTaken;
			this.maxBlackPieceThatCanBeTaken = maxBlackPieceThatCanBeTaken;
			this.utility = maxWhitePieceThatCanBeTaken - maxBlackPieceThatCanBeTaken;
		}
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
		this.pieceValues.put("King", 100);
	}
	//-----------------------------------------------------------------------------------------------------------------
	private Result evaluateState(State root) {
		//-------------------------------------------------------------------------------------------------------------
		HashMap<String, Integer> prevWhitePeices = getPieces(root.previous.board, "WHITE");
		HashMap<String, Integer> curWhitePeices  = getPieces(root.board, "WHITE");
		HashMap<String, Integer> prevBlackPeices = getPieces(root.previous.board, "BLACK");
		HashMap<String, Integer> curBlackPeices  = getPieces(root.board, "BLACK");
			  
		double maxWhitePieceThatCanBeTaken = maxPieceValueOfState(prevWhitePeices, curWhitePeices) ;
		double maxBlackPieceThatCanBeTaken = maxPieceValueOfState(prevBlackPeices, curBlackPeices) ; 

		return new Result(root, maxWhitePieceThatCanBeTaken, maxBlackPieceThatCanBeTaken);
	}
	//-----------------------------------------------------------------------------------------------------------------
	private double maxPieceValueOfState(HashMap<String, Integer> initSate, HashMap<String, Integer> childState){
		//-------------------------------------------------------------------------------------------------------------
		double max = 0;
		for (String p : this.chessPieces) {
			if (initSate.get(p) != childState.get(p) && this.pieceValues.get(p) > max){   // if piece taken, see if mvp
				max = this.pieceValues.get(p);
			}
		}
		return max;
	}
	//-----------------------------------------------------------------------------------------------------------------
	private Result greedy(State root){ 															   // Greedy bot method
		//-------------------------------------------------------------------------------------------------------------
		ArrayList<State> childrenStates = getChildStates(root);
		Result optimalResult = evaluateState(childrenStates.get(0));            // set base choice to first child 
		for (State c : childrenStates) {
			Result r = evaluateState(c);
			if (r.state.check && r.state.over) return r; 									   // if check mate take it
			if (r.state.check) return r; 													       // if check, take it
			if (this.curGame.player.name().equals("WHITE") 
				&& r.maxBlackPieceThatCanBeTaken > optimalResult.maxBlackPieceThatCanBeTaken){
				System.out.printf("I am the white player and choosing the highest black peice that can be taken: %f\n",
					 r.maxBlackPieceThatCanBeTaken);
				optimalResult = r;
			}
			if (this.curGame.player.name().equals("BLACK") 
				&& r.maxWhitePieceThatCanBeTaken > optimalResult.maxWhitePieceThatCanBeTaken){
				System.out.printf("I am the black player and choosing the highest white peice that can be taken: %f\n", 
					r.maxWhitePieceThatCanBeTaken);
				optimalResult = r;
			}
		} 																
		return optimalResult;
	}
	//-----------------------------------------------------------------------------------------------------------------
	// private Result minimax(State root, int depth, boolean maximizingPlayer) {
	// 	if (root.over || depth == 0) return evaluateState(root);
	// 	ArrayList<State> childStates = getChildStates(root);
	// }
	//-----------------------------------------------------------------------------------------------------------------
}

