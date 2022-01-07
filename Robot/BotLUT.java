package CPEN502.Robot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import robocode.*;
import CPEN502.LUT.*;
import static robocode.util.Utils.normalRelativeAngleDegrees;

public class BotLUT extends AdvancedRobot {

    //Rewards
    private final double wallCollisionReward = -0.5;
    private final double robotCollisionReward = -0.5;
    private final double onBulletHitByEnemy = -0.5;
    private final double onHitEnemyWithBullet = +0.5;
    private final double loseGameReward = -1;
    private final double winGameReward = 1;

    // RL hyperParameters
    private static double alpha = 0.1;
    private static double gamma = 0.9;
    private static double epsilon = 0.8;
    private static boolean on_policy = false;

    private double intermediateReward = 0.0;
    private double terminalReward = 0.0;
    private double reward = 0.0;

    static ArrayList<Integer> winsArrayList = new ArrayList<Integer>();

    //states
    public enum enumEnergy {low, medium, high}

    ;

    public enum enumDistanceToEnemy {veryClose, near, far}

    ;

    public enum enumXPos {step1, step2, step3, step4, step5, step6, step7, step8}

    ;

    public enum enumYPos {step1, step2, step3, step4, step5, step6}

    ;

    public enum enumBearing {low, mid, high}

    ;

    public enum enumAction {FORWARD, BACKWARD, DIAGRIGHT, DIAGLEFT, FIRE}

    ;

    public static final int MY_ENERGY = 5;
    public static final int DIST = 4;
    public static final int X_DIST = 3;
    public static final int Y_DIST = 2;
    public static final int BEARING = 1;
    public static final int ACTION = 0;
    private byte moveDirection = 1;

    double enemyDistance = 0.0;
    double enemyEnergy = 0.0;
    double enemyHeading = 0.0;
    double enemyBearing = 0.0;

    double enemyBearingRadians = 0.0;
    double enemyVelocity = 0.0;
    double enemyHeadingRadians = 0.0;


    private int[] prevState = new int[stateActionTableSize - 1];
    private int[] currState = new int[stateActionTableSize - 1];
    private int[] prevStateAction = new int[stateActionTableSize];
    private int[] currStateAction = new int[stateActionTableSize];
    private int[] currStateActionGreedy = new int[stateActionTableSize];

    private static int stateActionTableSize = 6;
    private static int stateActionDimVals[] = {enumAction.values().length, enumBearing.values().length,
            enumYPos.values().length, enumXPos.values().length,
            enumDistanceToEnemy.values().length, enumEnergy.values().length};

    public static MyLUT lut = new MyLUT(stateActionTableSize, stateActionDimVals);

    public static int totalNumRounds = 0;
    public static int numWinGamesPerHundred = 0;

    boolean terminalRewardOnly = false;

    public void run() {
        /*try
        {
            lut.load(getDataFile("QTable1.dat"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }*/
        while (true) {
            if (totalNumRounds > 10000) epsilon = 0.0;

            //scan

            setAdjustRadarForRobotTurn(true);
            setAdjustGunForRobotTurn(true);
            setAdjustRadarForGunTurn(true);
            turnRadarLeft(90);
            intermediateReward = 0;
            reward = 0;
            terminalReward = 0;
        }
    }

    public int[] selectAction(int[] state) {
        int actionToPerform = -1;
        int[] stateActionChosen = new int[stateActionTableSize];
        System.arraycopy(state, 0, stateActionChosen, 1, state.length);
        Random number = new Random();
        double rand = number.nextDouble();

        if (rand <= epsilon) {
            // store action for off policy
            actionToPerform = number.nextInt(enumAction.values().length) + 0;
            stateActionChosen[0] = actionToPerform;
            if (!on_policy) {
                actionToPerform = lut.actionCorrespondingToMaxQ(state);
                System.arraycopy(state, 0, currStateActionGreedy, 1, state.length);
                currStateActionGreedy[0] = actionToPerform;
            }
        } else {
            actionToPerform = lut.actionCorrespondingToMaxQ(state);
            stateActionChosen[0] = actionToPerform;
            System.arraycopy(stateActionChosen, 0, currStateActionGreedy, 0, stateActionChosen.length);
        }
        return stateActionChosen;
    }

    public void executeAction(int action) {
        enumAction act = enumAction.values()[action];
        switch (act) {
            case FIRE:
                double absBearing = enemyBearingRadians + getHeadingRadians();
                double gunTurnAmt = robocode.util.Utils.normalRelativeAngle(absBearing - getGunHeadingRadians());
                //double turn = getHeading() - getGunHeading() + enemyBearing;
                setTurnGunRightRadians(gunTurnAmt);
                setFire(Math.min(400 / enemyDistance, 3));
                execute();
                break;
            case FORWARD:
                setAhead(moveDirection * 100);
                execute();
                break;
            case BACKWARD:
                setBack(moveDirection * 100);
                execute();
                break;
            case DIAGLEFT:
                setTurnLeft(45);
                setAhead(moveDirection * 100);
                execute();
                break;
            case DIAGRIGHT:
                setTurnRight(45);
                setAhead(moveDirection * 100);
                execute();
                break;
            default:
                break;
        }
    }

    public int[] getStateVector() {
        int[] state = new int[stateActionTableSize - 1];

        if (getEnergy() <= 30) {
            state[MY_ENERGY - 1] = 0;
        } else if (getEnergy() > 30 && getEnergy() <= 60) {
            state[MY_ENERGY - 1] = 1;
        } else {
            state[MY_ENERGY - 1] = 2;
        }

        // x pos
        state[X_DIST - 1] = (int) (getX() / 100);
        // y pos
        state[Y_DIST - 1] = (int) (getY() / 100);

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
        executeAction(currStateAction[0]);
        updateLUT(prevStateAction, currStateAction);

    }

    public void updateLUT(int[] prevStateAction, int[] currStateAction) {
        if(!terminalRewardOnly){
            reward = intermediateReward;
        }else{
            reward = terminalReward;
        }
        double prevQValue = lut.outputFor(prevStateAction);
        double currQValue = lut.outputFor(currStateAction);
        double updatedQVal = 0.0;
        if (on_policy) {
            updatedQVal = computeQ(prevQValue, currQValue);
        } else {
            double currGreedyQValue = lut.outputFor(currStateActionGreedy);
            updatedQVal = computeQ(prevQValue, currGreedyQValue);
        }
        lut.train(prevStateAction, updatedQVal);
    }

    double computeQ(double prevQValue, double latestQVal){
        return prevQValue + alpha * (reward + gamma * latestQVal - prevQValue);
    }

    public double normalizeBearing(double relBearing) {
        while (relBearing > 180)
            relBearing -= 360;
        while (relBearing < -180)
            relBearing += 360;
        return relBearing;
    }

    public void onBulletHit(BulletHitEvent e) {
        if(!terminalRewardOnly) {
            intermediateReward += onHitEnemyWithBullet;
        }
    }

    public void onHitByBullet(HitByBulletEvent e) {
        if(!terminalRewardOnly) {
            intermediateReward += onBulletHitByEnemy;
        }
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
        if(!terminalRewardOnly) {
            intermediateReward += wallCollisionReward;
        }
        if (getVelocity() == 0)
            moveDirection *= -1;
        setAhead(200 * moveDirection);
    }

    public void onHitRobot(HitRobotEvent e) {
        if(!terminalRewardOnly) {
            intermediateReward += robotCollisionReward;
        }
    }

    public void onDeath(DeathEvent e) {
        System.out.println("Game lost");
        intermediateReward += loseGameReward;
        terminalReward = -10;
        updateLUT(prevStateAction, currStateAction);
    }

    public void onWin(WinEvent event) {
        System.out.println("Game won");
        numWinGamesPerHundred++;
        intermediateReward += winGameReward;
        terminalReward = 10;
        updateLUT(prevStateAction, currStateAction);
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
        lut.saveWinsAndReward(numWinGamesPerHundred, getDataFile("WinsAndRewards.dat"), winsArrayList);
        lut.save(getDataFile("QTable.dat"));
    }

}

