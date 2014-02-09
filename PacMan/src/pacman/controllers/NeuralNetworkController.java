package pacman.controllers;

import neuralNetwork.NeuralNetwork;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import pacman.utils.FeatureUtils;
import pacman.utils.Vector;

public class NeuralNetworkController extends AController {

	private static final int[] topology = {12,25,1};
	private NeuralNetwork valueFunction;
	
	public NeuralNetworkController() {
		valueFunction = new NeuralNetwork(topology);
	}
	
	@Override
	public MOVE getMove(Game game, long timeDue) {
		int currentNode = game.getPacmanCurrentNodeIndex();
		MOVE lastMove = game.getPacmanLastMoveMade();
		
		MOVE bestMove = MOVE.NEUTRAL;
		double bestMoveValueEstimation = Float.NEGATIVE_INFINITY;
		
		if (game.getNeighbour(currentNode, lastMove) != -1) {
			bestMove = lastMove;
			bestMoveValueEstimation = getValueFunctionEstimation(FeatureUtils.getFeatures(game, currentNode, lastMove));
		}
		
		for (MOVE move : game.getPossibleMoves(game.getPacmanCurrentNodeIndex())) {
			Vector features = FeatureUtils.getFeatures(game, game.getPacmanCurrentNodeIndex(), move);
			double estimation = getValueFunctionEstimation(features);
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
	
	public double getValueFunctionEstimation(Vector input) {
		return valueFunction.getOutput(input.getValues())[0];
	}
	
	public Vector getPolicyParameters() {
		return new Vector(valueFunction.getWeights());
	}
	
	public void setPolicyParameters(Vector parameters) {
		valueFunction = new NeuralNetwork(valueFunction.getTopology(), parameters.getValues());
	}

	@Override
	public AController copy() {
		NeuralNetworkController nnc = new NeuralNetworkController();
		nnc.valueFunction = new NeuralNetwork(valueFunction.getTopology(), valueFunction.getWeights());
		return nnc;
	}

}
