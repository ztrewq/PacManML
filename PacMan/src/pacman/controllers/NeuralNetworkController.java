package pacman.controllers;

import neuralNetwork.NNR;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import pacman.utils.FeatureUtils;
import pacman.utils.Vector;

public class NeuralNetworkController extends Controller<MOVE> {

	private static final int[] topology = {14,20,20,1};
	private NNR valueFunction;
	
	public NeuralNetworkController() {
		valueFunction = new NNR(topology);
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
		NNR valueFunction = NNR.createFromFile(file);
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
}
