package pacman.controllers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;
import org.encog.neural.pattern.RadialBasisPattern;
import org.encog.neural.rbf.RBFNetwork;
import org.encog.persist.EncogDirectoryPersistence;
import org.encog.util.csv.ReadCSV;

import pacman.game.Game;
import pacman.game.Constants.MOVE;
import static pacman.utils.FeatureUtils.getFeatures;

public class RBFController extends AController {

        public static double[][] INPUT;
        public static double[][] IDEAL;
        
        private static RBFNetwork rbfnet;
        
        public RBFController(int input, int hidden, int output) {
                RadialBasisPattern rbfpat = new RadialBasisPattern(); 
                rbfpat.addHiddenLayer(hidden);
                rbfpat.setInputNeurons(input);
                rbfpat.setOutputNeurons(output);
                rbfnet = (RBFNetwork) rbfpat.generate();
        }

        public RBFController(String filename){
                rbfnet = (RBFNetwork)EncogDirectoryPersistence.loadObject(new File(filename));
        }
        public MLDataSet getTrainingData(String csvfile) throws IOException {
                File f = new File(csvfile);
                int lncount = 0;
                FileReader fs = new FileReader(f);
                BufferedReader br = new BufferedReader(fs);
                br.readLine();
                while (br.readLine() != null) {
                        lncount++;
                }
                if (f.isFile()) {
                        ReadCSV reader = new ReadCSV(csvfile,true,',');
                        // System.out.println(reader.getColumnNames());
                        int columns = reader.getColumnNames().size();
                        if (columns > 0) {
                                INPUT = new double[lncount][columns];
                                IDEAL = new double[lncount][columns];
                                int j = 0;
                                FileReader fs2 = new FileReader(f);
                                BufferedReader br2 = new BufferedReader(fs2);
                                br2.readLine();
                                String row = br2.readLine();
                                while (row != null) {
                                        String[] splitted = row.split(",");
                                        for (int i = 0; i < columns -1; i++) {
                        //                        System.out.println("Feature " + (i+1) +": "+ splitted[i]+" ");
                                                INPUT[j][i] = Double.parseDouble(splitted[i]);
                                        }
                        //                System.out.println("Result: "+splitted[columns-1]);
                                        IDEAL[j][0] = Double.parseDouble(splitted[columns-1]);
                                        row = br2.readLine();
                                        j++;
                        }
                                br.close();
                                br2.close();
                                return new BasicMLDataSet(INPUT, IDEAL);
                        }
                        System.out.println("Something went wrong, no training data there.");
                        br.close();
                        return null;
                }
                else {
                        System.out.println("Couldn't find training data.");
                        br.close();
                        return null;
                }
        }
        
        public void trainNetwork(String csvfile) throws IOException {
        		File f = new File(csvfile);
        		if (f.isFile()) {
        			MLDataSet trainingD = getTrainingData("training.csv");
        			final ResilientPropagation train = new ResilientPropagation(rbfnet,trainingD);
        			
        			train.iteration();
        			System.out.println(train.getError());
        			int epoch = 1;
        			int epochSave = 1;
        			while (train.getError() > 0.01) {
                        train.iteration();
                        System.out.println("Iteration: " + epoch + " Error: "+train.getError());
                        epoch++;
                        if ((epoch - epochSave)> 15) {
                                saveNetwork();
                                epochSave = epoch;
                        }
        			}
        		}
        			saveNetwork();
        }
        
        public static void saveNetwork() {
                EncogDirectoryPersistence.saveObject(new File("rbfcontroller"), rbfnet);
        }
        @Override
        public MOVE getMove(Game game, long timeDue) {
                int currentNode = game.getPacmanCurrentNodeIndex();
                MOVE lastMove = game.getPacmanLastMoveMade();
                
                MOVE bestMove = MOVE.NEUTRAL;
                double bestMoveValueEstimation = Double.NEGATIVE_INFINITY;
                
                if (game.getNeighbour(currentNode, lastMove) != -1) {
                        bestMove = lastMove;
                        double[] curFeatures = {};
                        for (int i = 0; i < getFeatures(game, currentNode, lastMove).length; i++){
                                curFeatures[i] = getFeatures(game, currentNode, lastMove)[i];
                        }
                        bestMoveValueEstimation = getValueFunctionEstimation(curFeatures)[0];
                }
                
                for (MOVE move : game.getPossibleMoves(game.getPacmanCurrentNodeIndex())) {
                        double[] features = {};
                        for (int i = 0; i < getFeatures(game, game.getPacmanCurrentNodeIndex(), move).length;i++){
                                features[i] = getFeatures(game, game.getPacmanCurrentNodeIndex(), move)[i];
                        }
                        double estimation = getValueFunctionEstimation(features)[0];
                        if (bestMoveValueEstimation < estimation) {
                                bestMoveValueEstimation = estimation;
                                bestMove = move;
                        }
                }
                
                return bestMove;
        }
        /**
         * get the value estimation for the given state
         */
        public double[] getValueFunctionEstimation(double[] input) {
                MLData mlinput = new BasicMLData(input);
                return rbfnet.compute(mlinput).getData();
        }
        
        public float[] getValueFunctionEstimation(float[] input) {
        		double[] inputD = new double[input.length];
        		for (int i = 0; i<input.length;i++)
        			inputD[i] = input[i];
                MLData mlinput = new BasicMLData(inputD);
                double[] outputD = rbfnet.compute(mlinput).getData();
                float[] output = new float[outputD.length];
                for (int j = 0; j < outputD.length; j++)
                	output[j] = (float) outputD[j];
                return output;
        }

        @Override
        public float[] getPolicyParameters() {
                // TODO Auto-generated method stub
                return null;
        }

        @Override
        public void setPolicyParameters(float[] parameters) {
                // TODO Auto-generated method stub
                
        }
}