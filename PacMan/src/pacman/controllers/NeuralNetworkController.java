package pacman.controllers;

import neuralNetwork.NeuralNetwork;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import pacman.utils.FeatureUtils;

public class NeuralNetworkController extends AController {

	private static final int[] topology = {11,35,1};
	private NeuralNetwork valueFunction;
	
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
	
	public void writeToFile(String file) {
		valueFunction.saveToFile(file);
	}
	
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
	
	public float getValueFunctionEstimation(float[] input) {
		return valueFunction.getOutput(input)[0];
	}
	
	public float[] getPolicyParameters() {
		return valueFunction.getWeights();
	}
	
	public void setPolicyParameters(float[] parameters) {
		valueFunction = new NeuralNetwork(valueFunction.getTopology(), parameters);
	}

}
