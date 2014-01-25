package pacman.utils;

import java.util.EnumMap;
import java.util.Random;

import Jama.Matrix;
import pacman.controllers.AController;
import pacman.controllers.Controller;
import pacman.game.Game;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;

public class Utils {

	/**
	 * get the policy gradient of the current pacmanController using finite-difference
	 */
	public static float[] getGradient(AController pacmanController, Controller<EnumMap<GHOST,MOVE>> ghostController, int numTrials) {
		float[] initialCoefficients = normalize(pacmanController.getCoefficients());
		float initialEvaluation = evalPolicy(pacmanController, ghostController, numTrials);
		
		int runs = 16;
		float[][] coeffVariations = new float[runs][];
		float[][] coeffEvaluations = new float[runs][1];
		
		for (int i = 0; i < runs; i++) {
			coeffVariations[i] = add(copy(initialCoefficients), getRandomVector(initialCoefficients.length, 0, 0.05f));
			pacmanController.setCoefficients(coeffVariations[i]);
			coeffEvaluations[i][0] = evalPolicy(pacmanController, ghostController, numTrials) - initialEvaluation;
		}
		
		Matrix theta = new Matrix(getDoubles(coeffVariations));
		Matrix j = new Matrix(getDoubles(coeffEvaluations));
		Matrix g = (((((theta.transpose()).times(theta)).inverse()).times(theta.transpose())).times(j));
		return getFloats(g.getColumnPackedCopy());
	}
	
	/**
	 * return a copy of vector x
	 */
	public static float[] copy(float[] x) {
		float[] y = new float[x.length];
		for (int i = 0; i < x.length; i++) {
			y[i] = x[i];
		}
		
		return y;
	}
	
	/**
	 * add vector y to vector x
	 */
	public static float[] add(float[] x, float[] y) {
		for (int i = 0; i < x.length; i++) {
			x[i] += y[i];
		}
		return x;
	}
	
	/**
	 * normalize vector x to unit length
	 */
	public static float[] normalize(float[] x) {
		float length = getLength(x);
		scale(x, 1 / length);
		return x;
	}
	
	/**
	 * get the length of vector x
	 */
	public static float getLength(float[] x) {
		float length = 0;
		for (int i = 0; i < x.length; i++) {
			length += x[i] * x[i];
		}
		
		return (float) Math.sqrt(length);
	}
	
	/**
	 * scale every entry of vector x with scalar s
	 */
	public static float[] scale(float[] x, float s) {
		for (int i = 0; i < x.length; i++) {
			x[i] *= s;
		}
		
		return x;
	}
	
	/**
	 * get a random vector of length n containing random values between min and max
	 */
	public static float[] getRandomVector(int n, float min, float max) {
		float[] x = new float[n];
		for (int i = 0; i < x.length; i++) {
			x[i] = (float)Math.random() * (max - min) + min;
		}
		
		return x;
	}
	
	public static double[][] getDoubles(float[][] f) {
		double[][] d = new double[f.length][];
		for (int i = 0; i < d.length; i++) {
			d[i] = new double[f[i].length];
			for (int j = 0; j < d[i].length; j++) {
				d[i][j] = f[i][j];
			}
		}
		
		return d;
	}
	
	public static float[] getFloats(double[] d) {
		float[] f = new float[d.length];
		for (int i = 0; i < d.length; i++) {
			f[i] = (float) d[i];
		}
		
		return f;
	}

	public static float evalPolicy(Controller<MOVE> policy, Controller<EnumMap<GHOST,MOVE>> ghostController, int trials) {
		Random rnd=new Random(0);
		Thread[] threads = new Thread[trials];
		AccumulatedScore accumulatedScore = new AccumulatedScore();
		
		for (int i = 0; i < trials; i++) {
			threads[i] = new Evaluator(new Game(rnd.nextLong()), policy, ghostController, accumulatedScore);
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
		
		public Evaluator(Game game, Controller<MOVE> pacmanController, Controller<EnumMap<GHOST,MOVE>> ghostController, AccumulatedScore accumulatedScore) {
			this.game = game;
			this.pacmanController = pacmanController;
			this.ghostController = ghostController;
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
		
		public synchronized void addScore(int score) {
			accumulatedScore += score;
		}
		
		public int getScore() {
			return accumulatedScore;
		}
	}

}
