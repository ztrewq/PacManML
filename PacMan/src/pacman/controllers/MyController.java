package pacman.controllers;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;

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

		// get possible moves and sane moves
		MOVE[] possibleMoves = game.getPossibleMoves(currentNode);
		MOVE[] saneMoves = filterSaneMoves(possibleMoves, game, currentNode);
		
		// compute move "possibilities"
		EnumMap<MOVE, Double> moveChances = new EnumMap<MOVE, Double>(MOVE.class);
		for (MOVE move : saneMoves) {
			Vector features = extendFeatures(getFeatures(game, currentNode, move));
			double value = getValueFunctionEstimation(features);
			double expValue = Math.exp(value);
			moveChances.put(move, expValue);
		}
		
		// compute actual possibilities
		double scale = getMovesNormalizationScale(saneMoves, moveChances);
		for (MOVE move : saneMoves) {
			moveChances.put(move, moveChances.get(move) * scale);
		}
		
		// select move
		double random = Math.random();
		double aux = 0;
		for (MOVE move : saneMoves) {
			aux += moveChances.get(move);
			if (aux >= random)
				return move;
		}

		return MOVE.NEUTRAL;
	}

	//sum up e^estimation of all moves
	public double getMovesNormalizationScale(MOVE[] saneMoves, EnumMap<MOVE, Double> moveValues){
		double sum = 0;
		for (MOVE move : saneMoves) {
			sum += moveValues.get(move);
		}
		return 1 / sum;
	}
	
	// check if a single move is sane
	public boolean isMoveSane(MOVE poss, Game game, int nodeIndex){
        return !anyGhostFasterToJunction(game, nodeIndex, poss);
	}
		
	public boolean contains(int[] path, int node) {
		for (int i : path) {
			if (i == node) return true;
		}
		return false;
	}
	
	//filter the possible moves to saneMoves
	public MOVE[] filterSaneMoves(MOVE[] possibleMoves, Game game, int nodeIndex) {
		ArrayList<MOVE> saneMoves = new ArrayList<MOVE>();
		for (MOVE move : possibleMoves) {
			if (isMoveSane(move, game, nodeIndex))
				saneMoves.add(move);
		}
		return saneMoves.toArray(new MOVE[0]);
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
