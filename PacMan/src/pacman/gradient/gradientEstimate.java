package pacman.gradient;

import java.util.Arrays;
import java.util.EnumMap;

import pacman.Executor;
import pacman.controllers.ABController;
import pacman.controllers.Controller;
import pacman.controllers.NeuralNetworkController;
import pacman.controllers.examples.StarterGhosts;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;




public class gradientEstimate {

	private final float dMin = 0.005f;  
	private final float dMax = 0.1f;
	private int decimals = 1000; //used to change the dMin and dMax values to an integer.
	private float[] policy = {0,0,0,0,0,0,0,0};	//initial policy
	
	private int maxK = 16;					// max number of evaluations
	private float[] j = new float[maxK]; 	// evaluation for each k
	private float[] d = new float[maxK]; 	// delta theta for each k;
	
	
	public float[] FiniteDifferenceGradientEvaluation(ABController pacmanController, Controller<EnumMap<GHOST,MOVE>> ghostController, int numTrials) {
		float[] policy = pacmanController.getCoefficients();
		Gradient grad = new Gradient(policy.length);
		for(int o = 0; o < policy.length; o++){
			float v0 = Executor.evalPolicy(pacmanController, ghostController, numTrials);
			j[0] = v0;
			
			for(int k = 1; k < maxK; k++){
				d[k] = uniform(dMin, dMax);
				pacmanController.setCoefficients(updatePolicy(policy, o, d[k]));
				float vk = Executor.evalPolicy(pacmanController, ghostController, numTrials);
				j[k] = vk;
			}
			
			grad.change(o,estimateGradientComponent());
		}
		return grad.getValues();
	}
	
	
	
	//estimates one component of the gradient
	private float estimateGradientComponent() {
		float denominator = 0f;
		float numerator = 0f;
		
		for(int k = 1; k < maxK; k++){
			float n = d[k]*d[k];
			denominator += n;
		}
		for(int k = 1; k < maxK; k++){
			float n = d[k]*(j[k]-j[0]);
			numerator += n; 
		}
		return numerator/denominator;
	}


	//updates the policy at position o by adding d,
	//and checking it does not exceed 1
	private float[] updatePolicy(float[] policy, int o, float d){
		policy = Arrays.copyOf(policy, policy.length);
		policy[o]+=d;
		return policy;
	}


	//returns a randomNumber between dMin and dMax
	public float uniform(float dMin2, float dMax2) {
		float diff = (dMax*decimals)-(dMin*decimals); 	//getting the difference as Integer
		int rand = (int)(Math.random()*diff); 			//random number
		return ((float)(rand)/decimals)+dMin;			//dMin + random number between the difference of dMax&dMin
	}
}
