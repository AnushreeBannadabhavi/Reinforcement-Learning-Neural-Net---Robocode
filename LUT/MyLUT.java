package CPEN502.LUT;

import CPEN502.Interface.CommonInterface;
import robocode.RobocodeFileOutputStream;

import java.io.*;
import java.util.ArrayList;
import java.util.Random;

public class MyLUT implements CommonInterface {
    public double lutQTable[][][][][][];
    private int lutStateActionTableSize;
    private int[] lutStateActionTableRowVals;
    static ArrayList<Integer> winsArrayList = new ArrayList<Integer>();

    public MyLUT(int stateActionTableSize, int[] stateActionTableRowVals) {
        lutStateActionTableSize = stateActionTableSize;
        lutStateActionTableRowVals = stateActionTableRowVals;
        initializeLUT();

    }

    public void initializeLUT() {
        Random number = new Random();
        lutQTable = new double[lutStateActionTableRowVals[5]][lutStateActionTableRowVals[4]][lutStateActionTableRowVals[3]][lutStateActionTableRowVals[2]][lutStateActionTableRowVals[1]][lutStateActionTableRowVals[0]];
        for (int i = 0; i < lutStateActionTableRowVals[5]; i++) {
            for (int j = 0; j < lutStateActionTableRowVals[4]; j++) {
                for (int k = 0; k < lutStateActionTableRowVals[3]; k++) {
                    for (int l = 0; l < lutStateActionTableRowVals[2]; l++) {
                        for (int m = 0; m < lutStateActionTableRowVals[1]; m++) {
                            for (int n = 0; n < lutStateActionTableRowVals[0]; n++) {
                                lutQTable[i][j][k][l][m][n] = number.nextDouble();
                            }
                        }
                    }
                }
            }
        }
    }

    public double outputFor(int[] stateAction){
        assert(stateAction.length == this.lutStateActionTableSize);
        return lutQTable[stateAction[5]][stateAction[4]][stateAction[3]][stateAction[2]][stateAction[1]][stateAction[0]];
    }

    public int actionCorrespondingToMaxQ(int[] state)
    {
        assert(state.length == this.lutStateActionTableSize - 1);
        assert(state[4] < lutStateActionTableRowVals[5]);
        assert(state[3] < lutStateActionTableRowVals[4]);
        assert(state[2] < lutStateActionTableRowVals[3]);
        assert(state[1] < lutStateActionTableRowVals[2]);
        assert(state[0] < lutStateActionTableRowVals[1]);
        int action = 0;
        double max = Double.NEGATIVE_INFINITY;
        double[] setOfQValsForActions = lutQTable[state[4]][state[3]][state[2]][state[1]][state[0]];
        for(int i=0; i < lutStateActionTableRowVals[0]; i++)
        {
            if(setOfQValsForActions[i] > max)
            {
                max = setOfQValsForActions[i];
                action = i;
            }
        }
        return action;
    }

    public double train(int [] stateAction, double argValue){
        assert(stateAction.length == this.lutStateActionTableSize);
        lutQTable[stateAction[5]][stateAction[4]][stateAction[3]][stateAction[2]][stateAction[1]][stateAction[0]] = argValue;
        return 0.0;
    }

    public double train(double [] stateAction, double argValue) {
        return 0.0;
    }

    public void save(File file){
        PrintStream saveFile = null;

        try {
            saveFile = new PrintStream( new RobocodeFileOutputStream(file));
        }
        catch (IOException e) {
            System.out.println( "*** Could not create output stream for LUT save file.");
        }

        for (int i = 0; i < lutStateActionTableRowVals[5]; i++)
        { // 5
            for (int j = 0; j < lutStateActionTableRowVals[4]; j++)
            { // 4
                for (int k = 0; k < lutStateActionTableRowVals[3]; k++)
                { // 3
                    for (int l = 0; l < lutStateActionTableRowVals[2]; l++)
                    { // 2
                        for (int m = 0; m < lutStateActionTableRowVals[1]; m++)
                        { // 1
                            for (int n = 0; n < lutStateActionTableRowVals[0]; n++)
                            { //0
                                saveFile.println(String.valueOf(lutStateActionTableRowVals[n]) + " " + String.valueOf(lutStateActionTableRowVals[m]) +" " + String.valueOf(lutStateActionTableRowVals[l]) +" " + String.valueOf(lutStateActionTableRowVals[k]) +" " + String.valueOf(lutStateActionTableRowVals[j]) +" " + String.valueOf(lutStateActionTableRowVals[i]) +" " + String.valueOf(lutQTable[i][j][k][l][m][n])); //+ System.getProperty("line.separator")
                            }
                        }
                    }
                }
            }
        }


        saveFile.close();
    }

    public void load(File file) throws IOException{
        BufferedReader r = null;
        try {
            r = new BufferedReader(new FileReader(file));

            for (int i = 0; i < lutStateActionTableRowVals[5]; i++) { // 5
                for (int j = 0; j < lutStateActionTableRowVals[4]; j++) { // 4
                    for (int k = 0; k < lutStateActionTableRowVals[3]; k++) { // 3
                        for (int l = 0; l < lutStateActionTableRowVals[2]; l++) { // 2
                            for (int m = 0; m < lutStateActionTableRowVals[1]; m++) { // 1
                                for (int n = 0; n < lutStateActionTableRowVals[0]; n++) { //0
                                    lutQTable[i][j][k][l][m][n] = Double.parseDouble(r.readLine());
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (IOException e)
        {
            System.out.println("IOException trying to open reader of file: " + e);
        }
        r.close();
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
}




