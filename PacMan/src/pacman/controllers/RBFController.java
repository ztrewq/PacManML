package pacman.controllers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.encog.mathutil.rbf.RBFEnum;
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
import static pacman.utils.FeatureUtils.extendFeatures;

public class RBFController extends AController {

        public static double[][] INPUT;
        public static double[][] IDEAL;
        
        private static RBFNetwork rbfnet;
        
        public RBFController(int input, int hidden, int output) {
         //		setRbfnet(new RBFNetwork(input, hidden, output, RBFEnum.Gaussian));
         //		int hidden = (int)Math.pow(dim, input);
                RadialBasisPattern rbfpat = new RadialBasisPattern(); 
                rbfpat.addHiddenLayer(hidden);
                rbfpat.setInputNeurons(input);
                rbfpat.setOutputNeurons(output);
                
                rbfnet = (RBFNetwork) rbfpat.generate();
        }

        public RBFController(String filename){
                setRbfnet((RBFNetwork)EncogDirectoryPersistence.loadObject(new File(filename)));
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
        
        public void trainNetwork(String csvfile, String outputfile) throws IOException {
        		File f = new File(csvfile);
        		if (f.isFile()) {
        			MLDataSet trainingD = getTrainingData("training.csv");
        			final ResilientPropagation train = new ResilientPropagation(getRbfnet(),trainingD);
        			
        			train.iteration();
        			System.out.println(train.getError());
        			int epoch = 1;
        			int epochSave = 1;
        			while (train.getError() > 0.001) {
                        train.iteration();
                        System.out.println("Iteration: " + epoch + " Error: "+train.getError());
                        epoch++;
                        if ((epoch - epochSave)> 15) {
                                saveNetwork(outputfile);
                                epochSave = epoch;
                        }
        			}
        		}
        			saveNetwork(outputfile);
        }
        
        public static void saveNetwork(String outputfile) {
                EncogDirectoryPersistence.saveObject(new File(outputfile), getRbfnet());
        }
        @Override
        public MOVE getMove(Game game, long timeDue) {
                int currentNode = game.getPacmanCurrentNodeIndex();
                MOVE lastMove = game.getPacmanLastMoveMade();
                
                MOVE bestMove = MOVE.NEUTRAL;
                double bestMoveValueEstimation = Double.NEGATIVE_INFINITY;
                
                if (game.getNeighbour(currentNode, lastMove) != -1) {
                        bestMove = lastMove;
                        double[] curFeatures = new double[29];
                        for (int i = 0; i < extendFeatures(getFeatures(game, currentNode, lastMove)).length; i++){
                                curFeatures[i] = extendFeatures(getFeatures(game, currentNode, lastMove))[i];
                        }
                        bestMoveValueEstimation = getValueFunctionEstimation(curFeatures)[0];
                }
                
                for (MOVE move : game.getPossibleMoves(game.getPacmanCurrentNodeIndex())) {
                        double[] features = new double[29];
                        for (int i = 0; i < extendFeatures(getFeatures(game, game.getPacmanCurrentNodeIndex(), move)).length;i++){
                                features[i] = extendFeatures(getFeatures(game, game.getPacmanCurrentNodeIndex(), move))[i];
                        }
                        double estimation = getValueFunctionEstimation(features)[0];
                        System.out.println(Double.toString(estimation));
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
                double[] inputDouble = mlinput.getData();
                return getRbfnet().compute(mlinput).getData();
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

		@Override
		public AController copy() {
			// TODO Auto-generated method stub
			return null;
		}

		public static RBFNetwork getRbfnet() {
			return rbfnet;
		}

		public static void setRbfnet(RBFNetwork rbfnet) {
			RBFController.rbfnet = rbfnet;
		}
}