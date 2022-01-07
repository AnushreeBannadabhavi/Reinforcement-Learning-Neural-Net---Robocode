package CPEN502.Robot;

import CPEN502.NN.StateActionQVal;
import CPEN502.ReplayMemory.ReplayMemory;
import robocode.AdvancedRobot;
import CPEN502.Interface.CommonInterface;
import CPEN502.NN.NeuralNet;
import robocode.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class BotNNReplay extends AdvancedRobot {

    //Rewards
    private final double wallCollisionReward = -0.5;
    private final double robotCollisionReward = -0.5;
    private final double onBulletHitByEnemy = -0.5;
    private final double onHitEnemyWithBullet = +0.5;
    private final double loseGameReward = -1;
    private final double winGameReward = 1;

    double enemyDistance = 0.0;
    double enemyEnergy = 0.0;
    double enemyHeading = 0.0;
    double enemyBearing = 0.0;

    double enemyBearingRadians = 0.0;
    double enemyVelocity = 0.0;
    double enemyHeadingRadians = 0.0;

    public static final int MY_ENERGY = 5;
    public static final int DIST = 4;
    public static final int X_DIST = 3;
    public static final int Y_DIST = 2;
    public static final int BEARING = 1;
    public static final int ACTION = 0;
    private byte moveDirection = 1;

    private double intermediateReward = 0.0;
    private double terminalReward = 0.0;
    private double reward = 0.0;

    private static int stateActionTableSize = 6;

    private double[] currState = new double[stateActionTableSize - 1];
    private double[] currStateAction = new double[stateActionTableSize];
    private double[] prevState = new double[stateActionTableSize - 1];
    private double[] prevStateAction = new double[stateActionTableSize];
    private double[] currStateActionGreedy = new double[stateActionTableSize];

    // RL hyperParameters
    private static double alpha = 0.1;
    private static double gamma = 0.9;
    private static double epsilon = 0.9;
    private static boolean on_policy = false;

    public static int totalNumRounds = 0;
    public static int numWinGamesPerHundred = 0;

    static ArrayList<Integer> winsArrayList = new ArrayList<Integer>();

    public static NeuralNet NN = new NeuralNet(stateActionTableSize, 1, 0.02, 0.2, -1, 1);
    public static ReplayMemory<StateActionQVal> repMem = new ReplayMemory<>(60);
    public static int replayMemCounter = 0;

    public void run() {
        /* OFFLINE TRAINING
        try{
            NN.loadWeights(getDataFile("weights.txt"));
        } catch(IOException e){
            e.printStackTrace();
        }*/

        while (true) {
            if (totalNumRounds > 5000) epsilon = 0.0;

            setAdjustRadarForRobotTurn(true);
            setAdjustGunForRobotTurn(true);
            setAdjustRadarForGunTurn(true);
            turnRadarLeft(90);
            intermediateReward = 0;
            reward = 0;
            terminalReward = 0;
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {

        //enemy stats
        enemyDistance = e.getDistance();
        enemyEnergy = e.getEnergy();
        enemyHeading = e.getHeading();
        enemyBearing = e.getBearing();
        enemyBearingRadians = e.getBearingRadians();
        enemyVelocity = e.getVelocity();
        enemyHeadingRadians = e.getHeadingRadians();


        System.arraycopy(currStateAction, 0, prevStateAction, 0, prevStateAction.length);
        currState = getStateVector();
        currStateAction = selectAction(currState);
        executeAction((int)currStateAction[0]);
        updateNN(prevStateAction, currStateAction);

    }

    public double[] selectAction(double[] state) {
        int actionToPerform = -1;
        double[] stateActionChosen = new double[stateActionTableSize];
        System.arraycopy(state, 0, stateActionChosen, 1, state.length);
        //actionToPerform = NN.chooseActionToPerform(state); OFFLINE
        //stateActionChosen[0] = actionToPerform; OFFLINE
        Random number = new Random();
        double rand = number.nextDouble();

        if (rand <= epsilon) {
            // store action for off policy
            actionToPerform = number.nextInt(BotLUT.enumAction.values().length) + 0;
            stateActionChosen[0] = actionToPerform;
            if (!on_policy) {
                actionToPerform = NN.chooseActionToPerform(state);
                System.arraycopy(state, 0, currStateActionGreedy, 1, state.length);
                currStateActionGreedy[0] = actionToPerform;
            }
        } else {
            actionToPerform = NN.chooseActionToPerform(state);
            stateActionChosen[0] = actionToPerform;
            System.arraycopy(stateActionChosen, 0, currStateActionGreedy, 0, stateActionChosen.length);
        }
        return stateActionChosen;
    }

    public void executeAction(int action) {
        //BotLUT.enumAction act = BotLUT.enumAction.values()[action];
        switch (action) {
            case 4:
                double absBearing = enemyBearingRadians + getHeadingRadians();
                double gunTurnAmt = robocode.util.Utils.normalRelativeAngle(absBearing - getGunHeadingRadians());
                //double turn = getHeading() - getGunHeading() + enemyBearing;
                setTurnGunRightRadians(gunTurnAmt);
                setFire(Math.min(400 / enemyDistance, 3));
                execute();
                break;
            case 0:
                setAhead(moveDirection * 100);
                execute();
                break;
            case 1:
                setBack(moveDirection * 100);
                execute();
                break;
            case 3:
                setTurnLeft(45);
                setAhead(moveDirection * 100);
                execute();
                break;
            case 2:
                setTurnRight(45);
                setAhead(moveDirection * 100);
                execute();
                break;
            default:
                break;
        }
    }

    public double[] getStateVector() {
        double[] state = new double[stateActionTableSize - 1];

        if (getEnergy() <= 30) {
            state[MY_ENERGY - 1] = 0;
        } else if (getEnergy() > 30 && getEnergy() <= 60) {
            state[MY_ENERGY - 1] = 1;
        } else {
            state[MY_ENERGY - 1] = 2;
        }

        // x pos
        state[X_DIST - 1] =  (getX() / 100);
        // y pos
        state[Y_DIST - 1] =  (getY() / 100);

        if (enemyDistance <= 150) {
            state[DIST - 1] = 0;
        } else if (enemyDistance > 100 && enemyDistance <= 333) {
            state[DIST - 1] = 1;
        } else {
            state[DIST - 1] = 2;
        }

        double bearing = Math.abs(normalizeBearing(getHeading() - enemyHeading));

        if (bearing <= 60) {
            state[BEARING - 1] = 0;
        } else if (bearing > 60 && bearing <= 120) {
            state[BEARING - 1] = 1;
        } else {
            state[BEARING - 1] = 2;
        }

        return state;
    }

    public void updateNN(double[] prevStateAction, double[] currStateAction) {
        reward = intermediateReward;
        double prevQValue = NN.outputFor(prevStateAction);
        double currQValue = NN.outputFor(currStateAction);
        double updatedQVal = 0.0;
        if (on_policy) {
            updatedQVal = computeQ(prevQValue, currQValue);
        } else {
            double currGreedyQValue = NN.outputFor(currStateActionGreedy);
            updatedQVal = computeQ(prevQValue, currGreedyQValue);
        }
        StateActionQVal stateActionQVal = new StateActionQVal(prevStateAction, updatedQVal);
        repMem.add(stateActionQVal);
        replayMemCounter++;
        if(replayMemCounter >= 60){
            Object[] samples = repMem.sample(60);
            for(int i = 0; i < samples.length; i++){
                StateActionQVal stateActionQVal1 = (StateActionQVal)samples[i];
                double[] stateAction = stateActionQVal1.getStateAction();
                double QVal = stateActionQVal1.getQval();
                NN.train(stateAction, QVal);
            }
        }
    }

    double computeQ(double prevQValue, double latestQVal){
        double qVal = prevQValue + (alpha * (reward + gamma * latestQVal - prevQValue));
        double minQ = -8.159069807342675;
        double maxQ = -0.0342549390326078;
        qVal = (qVal - minQ) / (maxQ - minQ);
        double bipolar_min = -1.0;
        double bipolar_max = 1.0;
        qVal = qVal * (bipolar_max - bipolar_min) + bipolar_min;
        return qVal;
    }

    public double normalizeBearing(double relBearing) {
        while (relBearing > 180)
            relBearing -= 360;
        while (relBearing < -180)
            relBearing += 360;
        return relBearing;
    }

    public void onBulletHit(BulletHitEvent e) {
        intermediateReward += onHitEnemyWithBullet;

    }

    public void onHitByBullet(HitByBulletEvent e) {
        intermediateReward += onBulletHitByEnemy;

        setTurnRight(e.getBearing() + 90 -
                30 * moveDirection);

        if (enemyDistance <= 250)
            moveDirection = -1;
        else
            moveDirection = 1;

        setAhead((enemyDistance / 4 + 25) * moveDirection);
        execute();
    }

    public void onHitWall(HitWallEvent e) {
        intermediateReward += wallCollisionReward;

        if (getVelocity() == 0)
            moveDirection *= -1;
        setAhead(200 * moveDirection);
    }

    public void onHitRobot(HitRobotEvent e) {
        intermediateReward += robotCollisionReward;

    }

    public void onDeath(DeathEvent e) {
        System.out.println("NN Game lost");
        intermediateReward += loseGameReward;
        terminalReward = -10;
        updateNN(prevStateAction, currStateAction);
    }

    public void onWin(WinEvent event) {
        System.out.println("NN Game won");
        numWinGamesPerHundred++;
        intermediateReward += winGameReward;
        terminalReward = 10;
        updateNN(prevStateAction, currStateAction);
    }

    public void onRoundEnded(RoundEndedEvent event) {
        totalNumRounds++;
        if ((totalNumRounds % 100) == 0) {
            System.out.println("totalNumRounds");
            System.out.println(totalNumRounds);
            System.out.println("numWinGamesPerHundred");
            System.out.println(numWinGamesPerHundred);
            winsArrayList.add(numWinGamesPerHundred);

            numWinGamesPerHundred = 0;
        }

    }

    public void onBattleEnded(BattleEndedEvent e) {
        NN.saveWinsAndReward(numWinGamesPerHundred, getDataFile("WinsAndRewards.dat"), winsArrayList);
        NN.saveWeights1(getDataFile("weights.dat"));
        //NN.printWeights();
        //lut.save(getDataFile("QTable.dat"));
    }

}

