package pacman;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import neuralNetwork.NeuralNetwork;
import org.encog.ml.MLRegression;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.persist.EncogDirectoryPersistence;
import org.encog.util.simple.EncogUtility;
import pacman.controllers.AController;
import pacman.controllers.Controller;
import pacman.controllers.HumanController;
import pacman.controllers.KeyBoardInput;
import pacman.controllers.MyController;
import pacman.controllers.NeuralNetworkController;
import pacman.controllers.RBFController;
import pacman.controllers.StateValuePair;
import pacman.controllers.examples.AggressiveGhosts;
import pacman.controllers.examples.Legacy;
import pacman.controllers.examples.Legacy2TheReckoning;
import pacman.controllers.examples.NearestPillPacMan;
import pacman.controllers.examples.NearestPillPacManVS;
import pacman.controllers.examples.RandomGhosts;
import pacman.controllers.examples.RandomNonRevPacMan;
import pacman.controllers.examples.RandomPacMan;
import pacman.controllers.examples.StarterGhosts;
import pacman.controllers.examples.StarterPacMan;
import pacman.game.Game;
import pacman.game.GameView;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.utils.FeatureUtils;
import pacman.utils.ParaValueList;
import pacman.utils.ParaValuePair;
import pacman.utils.Savelist;
import pacman.utils.Utils;
import pacman.utils.Vector;
import static pacman.game.Constants.*;
import static pacman.utils.Utils.*;
import static pacman.utils.FeatureUtils.extendFeatures;

/**
 * This class may be used to execute the game in timed or un-timed modes, with or without
 * visuals. Competitors should implement their controllers in game.entries.ghosts and 
 * game.entries.pacman respectively. The skeleton classes are already provided. The package
 * structure should not be changed (although you may create sub-packages in these packages).
 */
@SuppressWarnings("unused")
public class Executor
{	
	/**
	 * The main method. Several options are listed - simply remove comments to use the option you want.
	 *
	 * @param args the command line arguments
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException
	{
//		NeuralNetworkController nnC = NeuralNetworkController.createFromFile("neurocontroller");
//		runGame(nnC, new StarterGhosts(), true, 10);
//		StateValuePair[] svp = getStateValuePairs(loadReplay("replay"), nnC);
//		StateValuePair[] esvp = extendStateValuePairs(svp);
//		writeSVPairs(loadReplay("replay"), nnC);
//		Vector coefficients = getLinearRegressionCoefficients(esvp);
//		runGame(new MyController(coefficients), new StarterGhosts(), true, 10);
		MyController ctrl = MyController.createFromFile("linearcontroller");
//		runGame(ctrl, new StarterGhosts(), true, 10);
		train(ctrl);

//		RBFController rbfc = new RBFController(29, 5, 1);
//		RBFController rbfc = new RBFController("rbfcontroller2");
//		// Using Encog method for training
//		rbfc.getTrainingData("training.csv");
//		EncogUtility.trainToError(RBFController.getRbfnet(), new BasicMLDataSet(RBFController.INPUT, RBFController.IDEAL), 0.006);
//        EncogDirectoryPersistence.saveObject(new File("rbfcontroller2"), RBFController.getRbfnet());
        
		// Using own method for training
//		rbfc.trainNetwork("training.csv", "rbfcontroller2");
//		runGame(rbfc, new StarterGhosts(), true, 10);
	
//		MLDataSet data = new BasicMLDataSet(rbfc.getTrainingData("training.csv"));
//		EncogUtility.evaluate(RBFController.getRbfnet(), data);
		
		//policy evaluation averaging results from samples (x trials with same seed)
//		int numTrials=10;
//		float controllerScore = exec.evalPolicy(NeuralNetworkController.createFromFile("controller"), new StarterGhosts(), numTrials);
//		exec.runGame(NeuralNetworkController.createFromFile("controller"), new StarterGhosts(), true, 10);
		
		/*
		//run multiple games in batch mode - good for testing.
		int numTrials=10;
		exec.runExperiment(new RandomPacMan(),new RandomGhosts(),numTrials);
		 */
		
		/*
		//run a game in synchronous mode: game waits until controllers respond.
		int delay=5;
		boolean visual=true;
		exec.runGame(new RandomPacMan(),new RandomGhosts(),visual,delay);
  		 */
		
		/*
		//run the game in asynchronous mode.
		boolean visual=true;
//		exec.runGameTimed(new NearestPillPacMan(),new AggressiveGhosts(),visual);
		exec.runGameTimed(new StarterPacMan(),new StarterGhosts(),visual);
//		exec.runGameTimed(new HumanController(new KeyBoardInput()),new StarterGhosts(),visual);	
		*/
		
		/*
		//run the game in asynchronous mode but advance as soon as both controllers are ready  - this is the mode of the competition.
		//time limit of DELAY ms still applies.
		boolean visual=true;
		boolean fixedTime=false;
		exec.runGameTimedSpeedOptimised(new RandomPacMan(),new RandomGhosts(),fixedTime,visual);
		*/
		
		/*
		//run game in asynchronous mode and record it to file for replay at a later stage.
		boolean visual=true;
		String fileName="replay.txt";
		exec.runGameTimedRecorded(new HumanController(new KeyBoardInput()),new RandomGhosts(),visual,fileName);
		//exec.replayGame(fileName,visual);
		 */
	}
	
	public static void train(MyController pacManController) {
		int run = 1;
		while (true) {
			train(new MyController(pacManController.getPolicyParameters()), 50, run);
		}
	}
	
	public static void train(MyController pacManController, int numTrials, int i) {
//		GameView gameView = new GameView(new Game(0)).showGame();
		float bestEvaluation = Utils.evalPolicy(pacManController, numTrials);
		ParaValueList paraValue = new ParaValueList();
		System.out.println("\nrun " + i + " evaluation before training: " + bestEvaluation);
		
		// train
		int n = pacManController.getPolicyParameters().getDimension(); 
		Vector updateValues = new Vector(n, 1); // vector with initial update values
		Vector oldGradient = new Vector(n); // 0 vector
		Vector newGradient = new Vector(n); // 0 vector
		while (true) {
			oldGradient = newGradient;
			newGradient = getGradient(pacManController, numTrials);
//			newGradient.add(- newGradient.getMean());
			updateValues = getNewUpdateValues(updateValues, oldGradient, newGradient);
			pacManController.setPolicyParameters(pacManController.getPolicyParameters().add(updateValues));
			float currentEvaluation = Utils.evalPolicy(pacManController, numTrials);
			writeValues(pacManController, currentEvaluation, "valuesLin" + i + ".txt");
			System.out.println("new evaluation : " + currentEvaluation);
			paraValue.add(new ParaValuePair(pacManController.getPolicyParameters(), currentEvaluation));
			ParaValueList.writeToFile(paraValue, "parametersValuePair" + i);
			paraValue.printLast();
			if (currentEvaluation > bestEvaluation) {
				bestEvaluation = currentEvaluation;
				pacManController.writeToFile("linearcontroller" + i);
				System.out.println("saved");
			}
			
			// demonstrate controller 
//			Game game = new Game(System.currentTimeMillis());
//			gameView.setGame(game);
//			StarterGhosts ghostController = new StarterGhosts();
//			while(!game.gameOver()) {
//				game.advanceGame(pacManController.getMove(game, -1), ghostController.getMove(game,-1));
//		        try{Thread.sleep(2);}catch(Exception e){}
//		        gameView.repaint();
//			}
		}
	}
	
	// write evaluation values to a values.txt-file (valuesLin.txt or valuesNeural.txt)
	public static void writeValues(AController controller, float value, String file){
		BufferedWriter br = null;
		try {
			br = new BufferedWriter(new FileWriter(file));
			br.write(Float.toString(value)+",");
		}
		catch (IOException e) {
			System.err.println("Error writing to file");
		}
		finally {
			try {br.close();} catch (IOException e){ };
		}
	
	}
	
//	//gradient estimation
//	public static float[] gradientEstimation(AController pacManController,Controller<EnumMap<GHOST,MOVE>> ghostController) {
//		int numTrials=10;
//		Gradient grad;
//		gradientEstimate gr = new gradientEstimate();
//		return gr.FiniteDifferenceGradientEvaluation(pacManController, ghostController, numTrials);
//	}

    public static void writeSVPairs(ArrayList<String> replayStates, NeuralNetworkController nnC) {
        BufferedWriter br = null;
        try {
            br = new BufferedWriter(new FileWriter("training.csv"));
            StateValuePair[] svPairs = getStateValuePairs(replayStates, nnC);
            StateValuePair[] extSvPairs = extendStateValuePairs(svPairs);
            int lncnt = 0;
            for (StateValuePair sv : extSvPairs) {
            	if (lncnt < 4000) {
            		double[] features = sv.getState().getValues();
            		double estimation = sv.getValue();
            		for (int i = 0; i < features.length;i++)
            			br.write(Double.toString(features[i])+",");
            			br.write(Double.toString(estimation));
            			br.newLine();
            	}
            	lncnt++;
            }
        }
        catch (IOException e) {
            System.err.println("Error creating training file");
        }
        finally {
            try { br.close(); } catch (Exception e) { };
        }
    }

	public static StateValuePair[] getStateValuePairs(ArrayList<String> replayStates, NeuralNetworkController neuralNetworkController) {
		LinkedList<StateValuePair> stateValuePairList = new LinkedList<StateValuePair>();
		
		Game game=new Game(0);
		for(String state : replayStates) {
			game.setGameState(state);	
			int currentNode = game.getPacmanCurrentNodeIndex();
			for (MOVE move : game.getPossibleMoves(game.getPacmanCurrentNodeIndex())) {
				Vector features = FeatureUtils.getFeatures(game, currentNode, move);
				double estimation = neuralNetworkController.getValueFunctionEstimation(features);
				stateValuePairList.add(new StateValuePair(features, estimation));
			}
		}
		
		StateValuePair[] stateValuePairs = new StateValuePair[stateValuePairList.size()];
		int i = 0;
		for (StateValuePair stateValuePair : stateValuePairList) {
			stateValuePairs[i++] = stateValuePair;
		}
		
		return stateValuePairs;
	}
	
	public static StateValuePair[] extendStateValuePairs(StateValuePair[] stateValuePairs) {
		StateValuePair[] extendedStateValuePairs = new StateValuePair[stateValuePairs.length];
		int i = 0;
		for (StateValuePair svp : stateValuePairs) {
			extendedStateValuePairs[i++] = new StateValuePair(extendFeatures(svp.getState()), svp.getValue());
		}
		
		return extendedStateValuePairs;
	}
	
	public static Vector getLinearRegressionCoefficients(StateValuePair[] stateValuePairs) {
		if (stateValuePairs.length == 0)
			throw new IllegalArgumentException();
		
		double[] coefficients = new double[stateValuePairs[0].getState().getDimension()];
		NeuralNetwork nn = new NeuralNetwork(new int[] {coefficients.length, 1});
		
		double[][] inputValues = new double[stateValuePairs.length][coefficients.length];
		double[][] outputValues = new double[stateValuePairs.length][1];
		
		for (int i = 0; i < stateValuePairs.length; i++) {
			inputValues[i] = stateValuePairs[i].getState().getValues();
			outputValues[i][0] = stateValuePairs[i].getValue();
		}
		
		nn.train(inputValues, outputValues, 15000);
		double[] weights = nn.getWeights();
		
		for (int i = 0; i < coefficients.length; i++) {
			coefficients[i] = weights[i+1];
		}
		
		return new Vector(coefficients);
	}
	
    /**
     * For running multiple games without visuals. This is useful to get a good idea of how well a controller plays
     * against a chosen opponent: the random nature of the game means that performance can vary from game to game. 
     * Running many games and looking at the average score (and standard deviation/error) helps to get a better
     * idea of how well the controller is likely to do in the competition.
     *
     * @param pacManController The Pac-Man controller
     * @param ghostController The Ghosts controller
     * @param trials The number of trials to be executed
     */
    public static void runExperiment(Controller<MOVE> pacManController,Controller<EnumMap<GHOST,MOVE>> ghostController,int trials)
    {
    	double avgScore=0;
    	
    	Random rnd=new Random(0);
		Game game;
		
		for(int i=0;i<trials;i++)
		{
			game=new Game(rnd.nextLong());
			
			while(!game.gameOver())
			{
		        game.advanceGame(pacManController.getMove(game.copy(),System.currentTimeMillis()+DELAY),
		        		ghostController.getMove(game.copy(),System.currentTimeMillis()+DELAY));
			}
			
			avgScore+=game.getScore();
			System.out.println(i+"\t"+game.getScore());
		}
		
		System.out.println(avgScore/trials);
    }
	
	/**
	 * Run a game in asynchronous mode: the game waits until a move is returned. In order to slow thing down in case
	 * the controllers return very quickly, a time limit can be used. If fasted gameplay is required, this delay
	 * should be put as 0.
	 *
	 * @param pacManController The Pac-Man controller
	 * @param ghostController The Ghosts controller
	 * @param visual Indicates whether or not to use visuals
	 * @param delay The delay between time-steps
	 */
	public static void runGame(Controller<MOVE> pacManController,Controller<EnumMap<GHOST,MOVE>> ghostController,boolean visual,int delay)
	{
		Game game=new Game(0);

		GameView gv=null;
		
		if(visual)
			gv=new GameView(game).showGame();
		
		while(!game.gameOver())
		{
	        game.advanceGame(pacManController.getMove(game.copy(),-1),ghostController.getMove(game.copy(),-1));
	        
	        try{Thread.sleep(delay);}catch(Exception e){}
	        
	        if(visual)
	        	gv.repaint();
		}
	}
	
	/**
     * Run the game with time limit (asynchronous mode). This is how it will be done in the competition. 
     * Can be played with and without visual display of game states.
     *
     * @param pacManController The Pac-Man controller
     * @param ghostController The Ghosts controller
	 * @param visual Indicates whether or not to use visuals
     */
    public static void runGameTimed(Controller<MOVE> pacManController,Controller<EnumMap<GHOST,MOVE>> ghostController,boolean visual)
	{
		Game game=new Game(0);
		
		GameView gv=null;
		
		if(visual)
			gv=new GameView(game).showGame();
		
		if(pacManController instanceof HumanController)
			gv.getFrame().addKeyListener(((HumanController)pacManController).getKeyboardInput());
				
		new Thread(pacManController).start();
		new Thread(ghostController).start();
		
		while(!game.gameOver())
		{
			pacManController.update(game.copy(),System.currentTimeMillis()+DELAY);
			ghostController.update(game.copy(),System.currentTimeMillis()+DELAY);

			try
			{
				Thread.sleep(DELAY);
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}

	        game.advanceGame(pacManController.getMove(),ghostController.getMove());	   
	        
	        if(visual)
	        	gv.repaint();
		}
		
		pacManController.terminate();
		ghostController.terminate();
	}
	
    /**
     * Run the game in asynchronous mode but proceed as soon as both controllers replied. The time limit still applies so 
     * so the game will proceed after 40ms regardless of whether the controllers managed to calculate a turn.
     *     
     * @param pacManController The Pac-Man controller
     * @param ghostController The Ghosts controller
     * @param fixedTime Whether or not to wait until 40ms are up even if both controllers already responded
	 * @param visual Indicates whether or not to use visuals
     */
    public static void runGameTimedSpeedOptimised(Controller<MOVE> pacManController,Controller<EnumMap<GHOST,MOVE>> ghostController,boolean fixedTime,boolean visual)
 	{
 		Game game=new Game(0);
 		
 		GameView gv=null;
 		
 		if(visual)
 			gv=new GameView(game).showGame();
 		
 		if(pacManController instanceof HumanController)
 			gv.getFrame().addKeyListener(((HumanController)pacManController).getKeyboardInput());
 				
 		new Thread(pacManController).start();
 		new Thread(ghostController).start();
 		
 		while(!game.gameOver())
 		{
 			pacManController.update(game.copy(),System.currentTimeMillis()+DELAY);
 			ghostController.update(game.copy(),System.currentTimeMillis()+DELAY);

 			try
			{
				int waited=DELAY/INTERVAL_WAIT;
				
				for(int j=0;j<DELAY/INTERVAL_WAIT;j++)
				{
					Thread.sleep(INTERVAL_WAIT);
					
					if(pacManController.hasComputed() && ghostController.hasComputed())
					{
						waited=j;
						break;
					}
				}
				
				if(fixedTime)
					Thread.sleep(((DELAY/INTERVAL_WAIT)-waited)*INTERVAL_WAIT);
				
				game.advanceGame(pacManController.getMove(),ghostController.getMove());	
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
 	        
 	        if(visual)
 	        	gv.repaint();
 		}
 		
 		pacManController.terminate();
 		ghostController.terminate();
 	}
    
	/**
	 * Run a game in asynchronous mode and recorded.
	 *
     * @param pacManController The Pac-Man controller
     * @param ghostController The Ghosts controller
     * @param visual Whether to run the game with visuals
	 * @param fileName The file name of the file that saves the replay
	 */
	public static void runGameTimedRecorded(Controller<MOVE> pacManController,Controller<EnumMap<GHOST,MOVE>> ghostController,boolean visual,String fileName)
	{
		StringBuilder replay=new StringBuilder();
		
		Game game=new Game(0);
		
		GameView gv=null;
		
		if(visual)
		{
			gv=new GameView(game).showGame();
			
			if(pacManController instanceof HumanController)
				gv.getFrame().addKeyListener(((HumanController)pacManController).getKeyboardInput());
		}		
		
		new Thread(pacManController).start();
		new Thread(ghostController).start();
		
		while(!game.gameOver())
		{
			pacManController.update(game.copy(),System.currentTimeMillis()+DELAY);
			ghostController.update(game.copy(),System.currentTimeMillis()+DELAY);

			try
			{
				Thread.sleep(DELAY);
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}

	        game.advanceGame(pacManController.getMove(),ghostController.getMove());	        
	        
	        if(visual)
	        	gv.repaint();
	        
	        replay.append(game.getGameState()+"\n");
		}
		
		pacManController.terminate();
		ghostController.terminate();
		
		saveToFile(replay.toString(),fileName,false);
	}
	
	/**
	 * Replay a previously saved game.
	 *
	 * @param fileName The file name of the game to be played
	 * @param visual Indicates whether or not to use visuals
	 */
	public static void replayGame(String fileName,boolean visual)
	{
		ArrayList<String> timeSteps=loadReplay(fileName);
		
		Game game=new Game(0);
		
		GameView gv=null;
		
		if(visual)
			gv=new GameView(game).showGame();
		
		for(int j=0;j<timeSteps.size();j++)
		{			
			game.setGameState(timeSteps.get(j));

			try
			{
				Thread.sleep(DELAY);
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
	        if(visual)
	        	gv.repaint();
		}
	}
	
	//save file for replays
    public static void saveToFile(String data,String name,boolean append)
    {
        try 
        {
            FileOutputStream outS=new FileOutputStream(name,append);
            PrintWriter pw=new PrintWriter(outS);

            pw.println(data);
            pw.flush();
            outS.close();

        } 
        catch (IOException e)
        {
            System.out.println("Could not save data!");	
        }
    }  

    //load a replay
    private static ArrayList<String> loadReplay(String fileName)
	{
    	ArrayList<String> replay=new ArrayList<String>();
		
        try
        {         	
        	BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));	 
            String input=br.readLine();		
            
            while(input!=null)
            {
            	if(!input.equals(""))
            		replay.add(input);

            	input=br.readLine();	
            }
            br.close();
        }
        catch(IOException ioe)
        {
            ioe.printStackTrace();
        }
        
        return replay;
	}
		
}