package pacman.utils;

import java.util.EnumMap;
import java.util.Random;
import Jama.Matrix;
import pacman.controllers.AController;
import pacman.controllers.Controller;
import pacman.controllers.examples.StarterGhosts;
import pacman.game.Game;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;

public class Utils {

	/**
	 * get the policy gradient of the current pacmanController using finite-difference
	 */
	public static Vector getGradient(AController pacmanController, int numTrials) {
		Vector initialParameters = pacmanController.getPolicyParameters();
		double initialEvaluation = evalPolicy(pacmanController, numTrials);
		int dimension = initialParameters.getDimension();
		
		int runs = 16;
		double[][] parametersVariations = new double[runs][];
		double[][] parametersEvaluations = new double[runs][1];
		for (int i = 0; i < runs; i++) {
			Vector parametersVariation = Vector.getRandomVector(dimension, 0, 0.05);
			Vector newParameters = initialParameters.copy().add(parametersVariation);
			pacmanController.setPolicyParameters(newParameters);
			parametersVariations[i] = parametersVariation.getValues();
			parametersEvaluations[i][0] = evalPolicy(pacmanController, numTrials) - initialEvaluation;
		}
		
		pacmanController.setPolicyParameters(initialParameters);
		
		try {
			Matrix theta = new Matrix(parametersVariations);
			Matrix j = new Matrix(parametersEvaluations);
			Matrix g = (((((theta.transpose()).times(theta)).inverse()).times(theta.transpose())).times(j));
			return new Vector(g.getColumnPackedCopy());
		} catch (Exception e) {
			return new Vector(initialParameters.getDimension()); // 0 vector
		}
	}
	
	public static Vector getNewUpdateValues(Vector oldUpdateValues, Vector oldGradient, Vector newGradient) {
		double lambdaPlus = 1.2;
		double lambdaMinus = 0.5;
		double min = 1e-7;
		double max = 0.01;
		
		double[] newUpdateValues = new double[oldUpdateValues.getDimension()];
		for (int i = 0; i < newUpdateValues.length; i++) {
			double x = oldGradient.getAt(i) * newGradient.getAt(i);
			double updateValue = oldUpdateValues.getAt(i);
			
			if (x > 0)
				updateValue *= lambdaPlus;
			else if (x < 0)
				updateValue *= lambdaMinus;
			
			updateValue = clamp(Math.abs(updateValue), min, max);
			
			if (newGradient.getAt(i) > 0)
				newUpdateValues[i] = updateValue;
			else if (newGradient.getAt(i) < 0)
				newUpdateValues[i] = -updateValue;
		}
		
		return new Vector(newUpdateValues);
	}
	
	public static float[] subtractMean(float[] x) {
		float mean = 0;
		for (int i = 0; i < x.length; i++) {
			mean += x[i];
		}
		mean /= x.length;
		
		float[] y = new float[x.length];
		for (int i = 0; i < x.length; i++) {
			y[i] = x[i] - mean;
		}
		
		return y;
	}
	
	/**
	 * clamp x between min and max
	 */
	private static double clamp(double x, double min, double max) {
		if (max < min)
			throw new IllegalArgumentException();
		if (x < min)
			return min;
		if (x > max)
			return max;
		return x;
	}

	public static float evalPolicy(AController pacManController, int trials) {
		Random rnd=new Random(0);
		Thread[] threads = new Thread[trials];
		AccumulatedScore accumulatedScore = new AccumulatedScore();
		
		for (int i = 0; i < trials; i++) {
			threads[i] = new Evaluator(new Game(rnd.nextLong()), pacManController.copy(), accumulatedScore);
			threads[i].start();
		}
		
		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		return (float) accumulatedScore.getScore() / trials;
	}
	
	private static class Evaluator extends Thread {

		private static final int maxTimeSteps = 7500;
		private Game game;
		private Controller<MOVE> pacmanController;
		private Controller<EnumMap<GHOST,MOVE>> ghostController;
		private AccumulatedScore accumulatedScore;
		
		public Evaluator(Game game, Controller<MOVE> pacmanController, AccumulatedScore accumulatedScore) {
			this.game = game;
			this.pacmanController = pacmanController;
			this.ghostController = new StarterGhosts();
			this.accumulatedScore = accumulatedScore;
		}
		
		@Override
		public void run() {
			int timeStep = 0;
			while(!game.wasPacManEaten() && timeStep <= maxTimeSteps) {
				game.advanceGame(pacmanController.getMove(game, -1), ghostController.getMove(game, -1));
				timeStep++;
			}
			accumulatedScore.addScore(game.getScore());
		}
	}

	private static class AccumulatedScore {
		
		private int accumulatedScore;
		
		public AccumulatedScore() {
			accumulatedScore = 0;
		}
		
		public void addScore(int score) {
			synchronized(this) {
				accumulatedScore += score;
			}
		}
		
		public int getScore() {
			return accumulatedScore;
		}
	}

}
