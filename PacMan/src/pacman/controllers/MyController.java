package pacman.controllers;

import java.util.Arrays;

import pacman.game.Constants.MOVE;
import pacman.game.Game;
import static pacman.utils.FeatureUtils.*;

public class MyController extends AController {

	private LinearFunction valueFunction;

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
			bestMoveValueEstimation = getValueFunctionEstimation(extendFeatures(getFeatures(game, currentNode, lastMove)));
		}

		for (MOVE move : game.getPossibleMoves(game.getPacmanCurrentNodeIndex())) {
			float[] features = extendFeatures(getFeatures(game, game.getPacmanCurrentNodeIndex(), move));
			float estimation = getValueFunctionEstimation(features);
			if (bestMoveValueEstimation < estimation) {
				bestMoveValueEstimation = estimation;
				bestMove = move;
			}
		}

		return bestMove;
	}

	public float getValueFunctionEstimation(float[] input) {
		return valueFunction.getOutput(input);
	}

	public float[] getPolicyParameters() {
		return valueFunction.getCoefficients();
	}

	public void setPolicyParameters(float[] coefficients) {
		valueFunction.setCoefficients(coefficients);
	}

	private class LinearFunction {

		private float[] coefficients;

		public LinearFunction(float[] coefficients) {
			this.coefficients = Arrays.copyOf(coefficients, coefficients.length);
		}

		public float getOutput(float[] input) {
			if (input.length != coefficients.length)
				throw new IllegalArgumentException();

			float result = 0;
			for (int i = 0; i < coefficients.length; i++) {
				result += input[i] * coefficients[i];
			}

			return result;
		}

		public void setCoefficients(float[] coefficients) {
			if (coefficients.length != this.coefficients.length)
				throw new IllegalArgumentException();

			this.coefficients = Arrays.copyOf(coefficients, coefficients.length);
		}

		public float[] getCoefficients() {
			return Arrays.copyOf(coefficients, coefficients.length);
		}

	}

	@Override
	public AController copy() {
		return new MyController(valueFunction.coefficients);
	}

}
