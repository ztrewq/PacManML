package pacman.controllers;

import pacman.game.Constants.MOVE;
import pacman.game.Game;

public class MyController extends AController {

	private LinearFunction valueFunction;
	
	/**
	 * create a new controller
	 */
	public MyController(float[] coefficients) {
		valueFunction = new LinearFunction(coefficients);
	}
	
	@Override
	public MOVE getMove(Game game, long timeDue) {
		int currentNode = game.getPacmanCurrentNodeIndex();
		MOVE lastMove = game.getPacmanLastMoveMade();
		
		MOVE bestMove = MOVE.NEUTRAL;
		float bestMoveValueEstimation = Float.NEGATIVE_INFINITY;
		
		if (game.getNeighbour(currentNode, lastMove) != -1) {
			bestMove = lastMove;
			bestMoveValueEstimation = getValueFunctionEstimation(FeatureUtils.getFeatures(game, currentNode, lastMove));
		}
		
		for (MOVE move : game.getPossibleMoves(game.getPacmanCurrentNodeIndex())) {
			float[] features = FeatureUtils.getFeatures(game, game.getPacmanCurrentNodeIndex(), move);
			float estimation = getValueFunctionEstimation(features);
			if (bestMoveValueEstimation < estimation) {
				bestMoveValueEstimation = estimation;
				bestMove = move;
			}
		}
		
		return bestMove;
	}
	
	/**
	 * get the value estimation for the given state
	 */
	public float getValueFunctionEstimation(float[] input) {
		return valueFunction.getOutput(input);
	}
	
	/**
	 * get the coefficients used for the value function
	 */
	public float[] getCoefficients() {
		return valueFunction.getCoefficients();
	}
	
	/**
	 * set the coefficients used for the value function
	 */
	public void setCoefficients(float[] coefficients) {
		valueFunction.setCoefficients(coefficients);
	}

}
