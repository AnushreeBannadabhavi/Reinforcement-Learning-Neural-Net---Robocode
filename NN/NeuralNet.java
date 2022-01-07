package CPEN502.NN;

import CPEN502.Interface.NeuralNetInterface;
import robocode.RobocodeFileOutputStream;

import java.io.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class NeuralNet implements NeuralNetInterface {

    private int numInputs;
    private int numHiddenUnits;
    private double learningRate;
    private double momemtumTerm;
    private int a;
    private int b;
    private double[][] weightsHiddenToOutput;
    private double[][] weightsInputToHidden;
    private double[][] previousWeightsInputToHidden;
    private double[][] previousWeightsHiddenToOutput;
    private double[] activationUnitsInHiddenLayer;
    private double[] derivativeActivationUnits;
    private double[] outputNeuralNet;
    private double derivativeOutput;
    private boolean weightsInitialized;
    private int numOutputs;

    public static Writer weightsWrite;

    public enum enumAction {FORWARD, BACKWARD, DIAGRIGHT, DIAGLEFT, FIRE}

    ;

    public NeuralNet (
            int argNumInputs,
            int argOutput,
            double argLearningRate,
            double argMomentumTerm,
            int argA,
            int argB ){
        numInputs = argNumInputs;
        numHiddenUnits = 6;
        learningRate = argLearningRate;
        momemtumTerm = argMomentumTerm;
        numOutputs = argOutput;
        a = argA;
        b = argB;
        weightsInputToHidden = new double[numInputs + 1][numHiddenUnits];
        weightsHiddenToOutput = new double[numHiddenUnits + 1][1];
        previousWeightsInputToHidden = new double[numInputs + 1][numHiddenUnits];
        previousWeightsHiddenToOutput = new double[numHiddenUnits + 1][1];
        activationUnitsInHiddenLayer =  new double[numHiddenUnits];
        derivativeActivationUnits =  new double[numHiddenUnits];
        outputNeuralNet = new double[numOutputs];
        weightsInitialized = false;
    }

    public double sigmoid(double x){
        return (1/( 1 + Math.pow(Math.E,(-1*x))));
    }

    public double customSigmoid(double x){

        return 0.0;
    }

    public double sigmoidBipolar(double x){
        return (-1 + (2/( 1 + Math.pow(Math.E,(-1*x)))));
    }

    public double activationFunction(double x){
        switch(a){
            case 0 : return (1/( 1 + Math.pow(Math.E,(-1*x))));
            case -1: return (-1 + (2/( 1 + Math.pow(Math.E,(-1*x)))));
            default: return 0.0;
        }
    }

    public void initializeWeights(){
        Random r = new Random();
        for(int i = 0; i < numInputs+1; i++){
            for(int j = 0; j < numHiddenUnits; j++) {
                double randomValue = -0.5 + (0.5 - (-0.5)) * r.nextDouble();
                randomValue = Math.floor(randomValue * 100) / 100;
                weightsInputToHidden[i][j] = randomValue;
            }
        }

        for(int i = 0; i < numHiddenUnits+1; i++){
            for(int j = 0; j < numOutputs; j++) {
                weightsHiddenToOutput[i][j] = -0.5 + (0.5 - (-0.5)) * r.nextDouble();
            }
        }
        weightsInitialized = true;
    }

    public void setWeights(double[][] wInputToHidden, double[][] wHiddenToOutput){
        weightsInputToHidden = wInputToHidden;
        weightsHiddenToOutput = wHiddenToOutput;
    }

    public void zeroWeights(){

    }

    public double outputFor(double [] X){

        return forwardPropagation(X);
    }

    public double outputFor(int[] stateAction){
        return 0.0;
    }

    public double train(double [] X, double expectedOutputVal){
        if(!weightsInitialized){
            System.out.println("!weightsInitialized");
            initializeWeights();
        }
        double actualOutputVal = forwardPropagation(X);
        backPropagation(actualOutputVal, expectedOutputVal, X);
        double error = calcError(actualOutputVal, expectedOutputVal);
        return error;
    }

    /************************************* NOT UNIT TESTED **************************************/
    public void computeWeightDifference(double[][] weightDifferenceInputToHidden, double[][] weightDifferenceHiddenToOutput){
        for(int i = 0; i < numInputs+1; i++){
            for(int j = 0; j < numHiddenUnits; j++) {
                weightDifferenceInputToHidden[i][j] = (weightsInputToHidden[i][j] - previousWeightsInputToHidden[i][j]);
                previousWeightsInputToHidden[i][j] = weightsInputToHidden[i][j];
            }
        }

        for(int i = 0; i < numHiddenUnits+1; i++){
            for(int j = 0; j < numOutputs; j++) {
                weightDifferenceHiddenToOutput[i][j] = (weightsHiddenToOutput[i][j] - previousWeightsHiddenToOutput[i][j]);
                previousWeightsHiddenToOutput[i][j] = weightsHiddenToOutput[i][j];
            }
        }
    }

    public void backPropagation(double actualOutputVal, double expectedOutputVal, double [] X){
        double[][] weightDifferenceInputToHidden = new double[numInputs + 1][numHiddenUnits];
        double[][] weightDifferenceHiddenToOutput = new double[numHiddenUnits + 1][1];
        computeWeightDifference(weightDifferenceInputToHidden, weightDifferenceHiddenToOutput);
        double deltaOutputLayer = computeDeltaOutputLayer(actualOutputVal, expectedOutputVal);
        updateWeightsHiddenToOutput(deltaOutputLayer, weightDifferenceHiddenToOutput);
        double[] deltaHiddenLayer = new double[numHiddenUnits];
        computeDeltaHiddenLayer(deltaOutputLayer, weightsHiddenToOutput, deltaHiddenLayer);
        updateWeightsInputToHidden(deltaHiddenLayer, X, weightDifferenceInputToHidden);
    }

    public void updateWeightsInputToHidden(double[] deltaHiddenLayer, double [] X, double[][] weightDifferenceInputToHidden){
        for(int i = 0; i < numInputs+1; i++) {
            for (int j = 0; j < numHiddenUnits; j++) {
                if(i == 0) {
                    weightsInputToHidden[i][j] = weightsInputToHidden[i][j] + (momemtumTerm * weightDifferenceInputToHidden[i][j]) + (learningRate * deltaHiddenLayer[j]);
                }
                else {
                    weightsInputToHidden[i][j] = (weightsInputToHidden[i][j] + (momemtumTerm * weightDifferenceInputToHidden[i][j]) + (learningRate * deltaHiddenLayer[j] * X[i - 1]));
                }
            }
        }
    }

    public void computeDeltaHiddenLayer(double deltaOutputLayer, double[][] weightsHiddenToOutput, double[] deltaHiddenLayer){
        for(int i = 0; i < numHiddenUnits; i++) {
            deltaHiddenLayer[i] = (derivativeActivationUnits[i]) * deltaOutputLayer * weightsHiddenToOutput[i+1][0];
        }
    }

    public void updateWeightsHiddenToOutput(double deltaOutputLayer, double[][] weightDifferenceHiddenToOutput){
        weightsHiddenToOutput[0][0] = weightsHiddenToOutput[0][0] + (learningRate * deltaOutputLayer);
        for(int i = 1; i < numHiddenUnits+1; i++) {
            for (int j = 0; j < numOutputs; j++) {
                weightsHiddenToOutput[i][j] = weightsHiddenToOutput[i][j] + (momemtumTerm * weightDifferenceHiddenToOutput[i][j]) + (learningRate * deltaOutputLayer * activationUnitsInHiddenLayer[i-1]);
            }
        }
    }

    public double computeDeltaOutputLayer(double actualOutputVal, double expectedOutputVal){
        return (derivativeOutput * (expectedOutputVal - actualOutputVal));
    }

    /*****************************************************************************************/

    public double activationFunctionDerivation(double x){
        switch(a){
            case 0 : return (activationFunction(x) * (1 - activationFunction(x)));
            case -1: return (0.5 * (1 + activationFunction(x)) * (1 - activationFunction(x)));
            default: return 0.0;
        }
    }

    public double calcError(double actualOutputVal, double expectedOutputVal){
        return ((Math.pow((actualOutputVal - expectedOutputVal),2)));
    }

    public double forwardPropagation(double [] X){
        for(int i = 0; i < numHiddenUnits; i++){
            double weightedSum = calcWeightedSum(X, i+1, weightsInputToHidden);
            activationUnitsInHiddenLayer[i] = activationFunction(weightedSum);
            derivativeActivationUnits[i] = activationFunctionDerivation(weightedSum);
        }
        double outputY = calcWeightedSum(activationUnitsInHiddenLayer, 1, weightsHiddenToOutput);
        derivativeOutput = activationFunctionDerivation(outputY);
        return activationFunction(outputY);
    }

    public double calcWeightedSum(double [] inputVector, int activationUnitNum, double[][] weights) {
        double weightedSum = 0.0;
        if (weights.length - 1 != inputVector.length)
            throw new ArrayIndexOutOfBoundsException();
        else{
            weightedSum += weights[0][activationUnitNum-1];
            for(int j = 1; j < weights.length; j++) {
                double sum = weights[j][activationUnitNum-1] * inputVector[j - 1];
                weightedSum += sum;
            }
        }
        return weightedSum;
    }

    public void save(File argFile){

    }

    public void load(File file) throws IOException{

    }



    public void saveWinsAndReward(int numWinGamesPerHundred, File file, ArrayList<Integer> winsArrayList1){
        try{
            RobocodeFileOutputStream is = new RobocodeFileOutputStream((file));
            OutputStreamWriter osw = new OutputStreamWriter(is);
            Writer w = new BufferedWriter(osw);

            for (int x: winsArrayList1)
            {
                w.write(Integer.toString(x));
                ((BufferedWriter) w).newLine();
            }
            w.close();
        }
        catch (IOException e){
            System.out.println( "*** Could not create output stream for Wins and Rewards save file.");
        }
    }

    public void saveWeights(){

        try {
            weightsWrite = new FileWriter("weights.txt");
            for(int i = 0; i < numInputs+1; i++){
                for(int j = 0; j < numHiddenUnits; j++) {
                    weightsWrite.append(String.valueOf(weightsInputToHidden[i][j]+" "));
                }
                weightsWrite.write(System.getProperty("line.separator"));
            }
            for(int i = 0; i < numHiddenUnits+1; i++){
                for(int j = 0; j < numOutputs; j++) {
                    weightsWrite.append(String.valueOf(weightsHiddenToOutput[i][j]+" "));
                }
                weightsWrite.write(System.getProperty("line.separator"));
            }
            weightsWrite.close();
        } catch (IOException e){
            e.printStackTrace();
        }

    }

    public void saveError(File file, ArrayList<Double> errorList){

        try{
            RobocodeFileOutputStream is = new RobocodeFileOutputStream((file));
            OutputStreamWriter osw = new OutputStreamWriter(is);
            Writer w = new BufferedWriter(osw);

            for (double x: errorList)
            {
                w.write(Double.toString(x));
                ((BufferedWriter) w).newLine();
            }
            w.close();
        }
        catch (IOException e){
            System.out.println( "*** Could not create output stream for Error save file.");
        }

    }

    public void saveWeights1(File file){
        PrintStream weightsWrite = null;

        try {
            weightsWrite = new PrintStream( new RobocodeFileOutputStream(file));
            for(int i = 0; i < numInputs+1; i++){
                for(int j = 0; j < numHiddenUnits; j++) {
                    weightsWrite.println(String.valueOf(weightsInputToHidden[i][j]+" "));
                }
                weightsWrite.println(System.getProperty("line.separator"));
            }
            for(int i = 0; i < numHiddenUnits+1; i++){
                for(int j = 0; j < numOutputs; j++) {
                    weightsWrite.println(String.valueOf(weightsHiddenToOutput[i][j]+" "));
                }
                weightsWrite.println(System.getProperty("line.separator"));
            }
            weightsWrite.close();
        } catch (IOException e){
            e.printStackTrace();
        }

    }

    public void loadWeights(File file) throws IOException{
        BufferedReader myReader = null;
        try {
            myReader = new BufferedReader(new FileReader(file));
            int lineNum = 0;

                for (int i = 0; i < numInputs + 1; i++) {
                    String data = myReader.readLine();
                    String[] dataSplitted = data.split(" ");
                    for (int j = 0; j < dataSplitted.length; j++) {
                        weightsInputToHidden[lineNum][j] = Double.parseDouble(dataSplitted[j]);
                    }
                    lineNum++;
                }
                int lineNum2 = 0;
                for(int i = 0; i < numHiddenUnits+1; i++){
                    String data = myReader.readLine();
                    for(int j = 0; j < numOutputs; j++) {
                        weightsHiddenToOutput[lineNum2][j] = Double.parseDouble(data);
                    }
                    lineNum2++;
                }
        } catch(IOException e){
            e.printStackTrace();
        }
        myReader.close();
    }

    public int chooseActionToPerform(double[] state){
        double[] stateAction = new double[6];
        System.arraycopy(state, 0, stateAction, 1, state.length);
        double max = 0;
        int action = -1;

        stateAction[0] = enumAction.BACKWARD.ordinal();
        double qVal = forwardPropagation(stateAction);
        if(qVal > max) {
            max = qVal;
            action = (int) stateAction[0];
        }

        stateAction[0] = enumAction.FORWARD.ordinal();
        qVal = forwardPropagation(stateAction);
        if(qVal > max) {
            max = qVal;
            action = (int) stateAction[0];
        }

        stateAction[0] = enumAction.DIAGLEFT.ordinal();
        qVal = forwardPropagation(stateAction);
        if(qVal > max) {
            max = qVal;
            action = (int) stateAction[0];
        }

        stateAction[0] = enumAction.DIAGRIGHT.ordinal();
        qVal = forwardPropagation(stateAction);
        if(qVal > max) {
            max = qVal;
            action = (int) stateAction[0];
        }

        stateAction[0] = enumAction.FIRE.ordinal();
        qVal = forwardPropagation(stateAction);
        if(qVal > max) {
            max = qVal;
            action = (int) stateAction[0];
        }

        return action;
    }

    public void printWeights(){
            for(int i = 0; i < numInputs+1; i++){
                for(int j = 0; j < numHiddenUnits; j++) {
                    System.out.println(weightsInputToHidden[i][j]+" ");
                }
                System.out.println("\n");
            }
            for(int i = 0; i < numHiddenUnits+1; i++){
                for(int j = 0; j < numOutputs; j++) {
                    System.out.println(weightsHiddenToOutput[i][j]+" ");
                }
                System.out.println("\n");
            }
    }

}
