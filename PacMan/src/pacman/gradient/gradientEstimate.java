package pacman.gradient;




public class gradientEstimate {

	private final float dMin = 0.005f;  
	private final float dMax = 0.1f;
	private int decimals = 1000; //used to change the dMin and dMax values to an integer.
	private float[] policy = {0,0,0,0,0,0,0,0};	//initial policy
	
	private int maxK = 16;					// max number of evaluations
	private float[] j = new float[maxK]; 	// evaluation for each k
	private float[] d = new float[maxK]; 	// delta theta for each k;
	
	
	public Gradient FiniteDifferenceGradientEvaluation(float[] policy){
		Gradient grad = new Gradient(8);
		for(int o = 0; o < policy.length; o++){
			// TODO change to real function
			float v0 = evaluateFunction();
			j[0] = v0;
			
			for(int k = 1; k < maxK; k++){
				d[k] = uniform(dMin, dMax);
				updatePolicy(o, d[k]);
				// TODO change to real function
				float vk = evaluateFunction();
				j[k] = vk;
			}
			
			grad.change(o,estimateGradientComponent());
		}
		return grad;
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
	private void updatePolicy(int o, float d){
		policy[o]+=d;
		if(policy[o] > 1){
			policy[o] = 1;
		}
	}


	//returns a randomNumber between dMin and dMax
	public float uniform(float dMin2, float dMax2) {
		float diff = (dMax*decimals)-(dMin*decimals); 	//getting the difference as Integer
		int rand = (int)(Math.random()*diff); 			//random number
		return ((float)(rand)/decimals)+dMin;			//dMin + random number between the difference of dMax&dMin
	}


	//dummy function
	private float evaluateFunction() {
		// TODO Auto-generated method stub
		return 60f;
	}
}
