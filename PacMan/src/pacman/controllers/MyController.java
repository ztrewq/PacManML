package pacman.controllers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import pacman.game.Constants.MOVE;
import pacman.game.Game;
import static pacman.utils.FeatureUtils.*;

public class MyController extends AController {

	private LinearFunction valueFunction;

	public MyController(float[] coefficients) {
		valueFunction = new LinearFunction(coefficients);
	}
	
	public MyController(String filename) {
		valueFunction = new LinearFunction(filename);
		
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

	public void writeToFile() {
		valueFunction.saveToFile();
	}
	}
	class LinearFunction {

		private float[] coefficients;

		public LinearFunction(float[] coefficients) {
			this.coefficients = Arrays.copyOf(coefficients, coefficients.length);
		}
		
		public LinearFunction(String filename) {
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(filename));
				String str;
				while ((str = br.readLine()) != null) {
					String[] strArray = str.split(",");
				float[] coeff = new float[strArray.length];
				for (int i = 0; i < strArray.length; i++) {
					String s = strArray[i].trim();
					coeff[i] = Float.parseFloat(s);
				}
				this.coefficients = Arrays.copyOf(coeff, coeff.length);
				}
			}
			catch (IOException e) {
				System.err.println("Error reading file.");
			}
			finally {
				try {br.close(); } catch (Exception e) { }
			}
		
		
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
		
		public void saveToFile() {
			String filename = "linController";
			BufferedWriter w = null;
			try
			{
				w = new BufferedWriter(new FileWriter(filename));
				for (int i = 0; i < getCoefficients().length -1 ; i++)
				w.write(Float.toString(getCoefficients()[i])+",");
				w.write(Float.toString(getCoefficients()[getCoefficients().length-1]));
				w.newLine();
			}
			catch (IOException e) {
				System.err.println("Couldn't create file");
			}
			finally {
				if (w != null)
					try { w.close(); }
					catch (IOException e) {e.printStackTrace();}
			}
		}

}
