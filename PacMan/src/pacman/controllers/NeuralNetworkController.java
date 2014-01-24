package pacman.controllers;

import neuralNetwork.NeuralNetwork;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

public class NeuralNetworkController extends ABController {

	private static final int[] topology = {9,11,1};
	private NeuralNetwork valueFunction;
	
	/**
	 * create a new controller
	 */
	public NeuralNetworkController() {
		valueFunction = new NeuralNetwork(topology);
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
	 * write this controller to a file
	 */
	public void writeToFile(String file) {
		valueFunction.saveToFile(file);
	}
	
	/**
	 * create a new controller from a file. returns null if the file is invalid or not existent
	 */
	public static NeuralNetworkController createFromFile(String file) {
		NeuralNetwork valueFunction = NeuralNetwork.createFromFile(file);
		if (valueFunction != null) {
			int[] topologyValueFunction = valueFunction.getTopology();
			
			// assert correct topology
			for (int i = 0; i < topologyValueFunction.length; i++) {
				if (topologyValueFunction[i] != topology[i]) 
					return null;
			}
			
			NeuralNetworkController controller = new NeuralNetworkController();
			controller.valueFunction = valueFunction;
			return controller;
		}
		
		return null;
	}
	
	/**
	 * get the value estimation for the given state
	 */
	public float getValueFunctionEstimation(float[] input) {
		return valueFunction.getOutput(input)[0];
	}
	
	/**
	 * get the coefficients used for the value function
	 */
	public float[] getCoefficients() {
		return valueFunction.getWeights();
	}
	
	/**
	 * set the coefficients used for the value function
	 */
	public void setCoefficients(float[] coefficients) {
		valueFunction = new NeuralNetwork(valueFunction.getTopology(), coefficients);
	}

}
