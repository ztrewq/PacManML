package pacman.controllers;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import pacman.utils.Vector;
import static pacman.utils.FeatureUtils.*;

public class MyController extends AController implements Serializable{

	private static final long serialVersionUID = 1L;
	private LinearFunction valueFunction;

	public MyController(Vector parameters) {
		valueFunction = new LinearFunction(parameters);
	}

	@Override
	public MOVE getMove(Game game, long timeDue) {
		int currentNode = game.getPacmanCurrentNodeIndex();
		MOVE lastMove = game.getPacmanLastMoveMade();

		MOVE bestMove = MOVE.NEUTRAL;
		double bestMoveValueEstimation = Float.NEGATIVE_INFINITY;

		if (game.getNeighbour(currentNode, lastMove) != -1) {
			bestMove = lastMove;
			bestMoveValueEstimation = getValueFunctionEstimation(extendFeatures(getFeatures(game, currentNode, lastMove)));
		}

		MOVE[] possMove = game.getPossibleMoves(game.getPacmanCurrentNodeIndex());
		MOVE[] filterMove = new MOVE[possMove.length];
		for (int i = 0; i < game.getPossibleMoves(game.getPacmanCurrentNodeIndex()).length; i++) {			
			int j = 0;
			MOVE move = game.getPossibleMoves(game.getPacmanCurrentNodeIndex())[i];
			if (isMoveSane(move, game, currentNode)) {
				filterMove[j] = move;
				j++;
			}
        }

		for (MOVE fmove : filterMove) {
				Vector features = extendFeatures(getFeatures(game, game.getPacmanCurrentNodeIndex(), fmove));
				double estimation = getValueFunctionEstimation(features);
				if (bestMoveValueEstimation < estimation) {
					bestMoveValueEstimation = estimation;
					bestMove = fmove;
			}
		}
		return bestMove;
	}

	// check if a single move is sane
	
	public boolean isMoveSane(MOVE poss, Game game, int nodeIndex){
        return anyGhostFasterToJunction(game,nodeIndex,poss);
	}
		
		
	public boolean contains(int[] path, int node) {
		for (int i : path) {
			if (i == node) return true;
		}
		return false;
	}
	
	//filter the possible moves to saneMoves
	public MOVE[] filterSaneMoves(MOVE[] poss, Game game, int nodeIndex){
		MOVE[] saneMoves = null;
		int size = 0; // size of the Array saneMoves[]
		for (MOVE move : poss) {
			if(getPillsInDirection(game, nodeIndex, move) > 0 ){
				double distance = getJunctionDistance(game, nodeIndex, move); 
				
			
			}
		}
		return saneMoves;
	}
	
	public double getValueFunctionEstimation(Vector input) {
		return valueFunction.getOutput(input);
	}

	public Vector getPolicyParameters() {
		return valueFunction.getCoefficients();
	}

	public void setPolicyParameters(Vector parameters) {
		valueFunction.setCoefficients(parameters);
	}

	private class LinearFunction implements Serializable{

		private static final long serialVersionUID = 1L;
		private Vector coefficients;

		public LinearFunction(Vector coefficients) {
			this.coefficients = coefficients.copy();
		}

		public double getOutput(Vector input) {
			return coefficients.dot(input);
		}

		public void setCoefficients(Vector coefficients) {
			if (coefficients.getDimension() != this.coefficients.getDimension())
				throw new IllegalArgumentException();

			this.coefficients = coefficients.copy();
		}

		public Vector getCoefficients() {
			return coefficients.copy();
		}

	}

	@Override
	public AController copy() {
		return new MyController(valueFunction.coefficients);
	}

	/**
	 * Loads Controller from file
	 */
	public static MyController createFromFile(String file){
		
		try{
			FileInputStream fin = new FileInputStream(file+".sav");
			ObjectInputStream in = new ObjectInputStream(fin);
			MyController controller = (MyController)in.readObject();
			in.close();
			fin.close();
			return controller;
		}catch(Exception e){
			System.out.println("CANT CREATE MYCONTROLLER FROM FILE: " +file+".sav :"+e);
		}
		return null;
	}
	
	/**
	 * Writes Controller to file
	 */
	public void writeToFile(String file){
		try{
			FileOutputStream fout = new FileOutputStream(file+".sav");
			ObjectOutputStream out = new ObjectOutputStream(fout);
			out.writeObject(this);
			out.close();
			fout.close();
		}catch(Exception e){
			System.out.println("SAVE ERROR WRITING TO FILE: " +file+".sav :"+e);
		}
	}
	
}
